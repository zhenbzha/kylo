package com.thinkbiganalytics.projects.security;

/*-
 * #%L
 * kylo-project-manager-service
 * %%
 * Copyright (C) 2017 ThinkBig Analytics
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.thinkbiganalytics.metadata.api.MetadataAccess;
import com.thinkbiganalytics.metadata.api.project.Project;
import com.thinkbiganalytics.metadata.api.project.security.ProjectAccessControl;
import com.thinkbiganalytics.metadata.modeshape.project.JcrProject;
import com.thinkbiganalytics.metadata.modeshape.project.providers.ProjectProvider;
import com.thinkbiganalytics.security.AccessController;
import com.thinkbiganalytics.security.UsernamePrincipal;
import com.thinkbiganalytics.security.action.AllowedActions;
import com.thinkbiganalytics.security.action.AllowedEntityActionsProvider;
import com.thinkbiganalytics.security.rest.controller.SecurityModelTransform;
import com.thinkbiganalytics.security.rest.model.ActionGroup;
import com.thinkbiganalytics.security.rest.model.PermissionsChange;
import com.thinkbiganalytics.security.rest.model.PermissionsChange.ChangeType;
import com.thinkbiganalytics.security.rest.model.RoleMembership;
import com.thinkbiganalytics.security.rest.model.RoleMembershipChange;
import com.thinkbiganalytics.security.rest.model.RoleMemberships;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.Principal;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response.Status;

/**
 * Default service implementation for changing the permissions and role memberships of project metadata entities.
 */
public class DefaultProjectSecurityService implements ProjectSecurityService {

    private static final Supplier<Optional<Set<com.thinkbiganalytics.metadata.api.security.RoleMembership>>> EMPTY_SUPPLIER = () -> Optional.empty();
    public static Logger logger = LoggerFactory.getLogger(DefaultProjectSecurityService.class);

    @Inject
    ProjectProvider projectProvider;

    @Inject
    private SecurityModelTransform securityTransform;

    @Inject
    private AllowedEntityActionsProvider actionsProvider;

    @Inject
    private MetadataAccess metadata;

    @Inject
    private AccessController accessController;

    private Collection<RoleChangeListener> roleChangeListeners = Lists.newLinkedList();

    @Override
    public Optional<ActionGroup> getAvailableProjectActions(String id) {
        return getAvailableActions(() -> accessProject(id).flatMap(c -> actionsProvider.getAvailableActions(AllowedActions.PROJECTS)));
    }

    @Override
    public Optional<ActionGroup> getAllowedProjectActions(String id, Set<Principal> principals) {
        return getAllowedActions(principals, supplyProjectActions(id));
    }

    @Override
    public Optional<ActionGroup> changeProjectPermissions(String id, PermissionsChange change) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Optional<RoleMemberships> getProjectRoleMemberships(String id) {
        return getRoleMemberships(supplyProjectRoleMemberships(id));
    }

    @Override
    public Optional<RoleMembership> changeProjectRoleMemberships(String id, RoleMembershipChange change) {
        // TODO: determine the set of changes that are going to be applied, and modify notebook folders on filesystem
        switch (change.getChange()) {
            case ADD:
            case REMOVE:
            case REPLACE:
                Set<UsernamePrincipal> usersGrantedRole = usersGrantedRole(id, change);
                Set<UsernamePrincipal> usersRevokedRole = usersRevokedRole(id, change);

                // The list of users being revoked the Role
                usersGrantedRole.stream()
                    .forEach(u -> logger.debug("User '{}' was granted '{}' access", u, change.getRoleName()));

                // The list of users being granted the Role
                usersRevokedRole.stream()
                    .forEach(u -> logger.debug("User '{}' was revoked '{}' access", u, change.getRoleName()));
            default:
                break;
        }

        return changeRoleMemberships(change, supplyProjectRoleMembership(id, change.getRoleName()));
    }

    private Set<UsernamePrincipal> usersGrantedRole(String id, RoleMembershipChange change) {
        Set<UsernamePrincipal> newUsers = Sets.newHashSet(securityTransform.asUserPrincipals(change.getUsers()));
        Set<UsernamePrincipal> users = projectProvider.getProjectMembersWithRoleById(id, change.getRoleName());

        Set<UsernamePrincipal> grantedUsers = diffSet(newUsers, users);
        grantedUsers.stream().forEach(
            u -> roleChangeListeners.stream().forEach(l -> l.userGrantedRole(u, new JcrProject.ProjectId(id), change.getRoleName()))
        );

        return grantedUsers;
    }

    private Set<UsernamePrincipal> usersRevokedRole(String id, RoleMembershipChange change) {
        Set<UsernamePrincipal> newUsers = Sets.newHashSet(securityTransform.asUserPrincipals(change.getUsers()));
        Set<UsernamePrincipal> users = projectProvider.getProjectMembersWithRoleById(id, change.getRoleName());

        Set<UsernamePrincipal> revokedUsers = diffSet(users, newUsers);
        revokedUsers.stream().forEach(
            u -> roleChangeListeners.stream().forEach(l -> l.userRevokedRole(u, new JcrProject.ProjectId(id), change.getRoleName()))
        );

        return revokedUsers;
    }

    private <T> Set<T> diffSet(Set<T> minuend, Set<T> subtrahend) {
        return minuend.stream().
            filter(m -> !subtrahend.contains(m))
            .collect(Collectors.toSet());
    }

    @Override
    public Optional<PermissionsChange> createProjectPermissionChange(String id, ChangeType changeType, Set<Principal> members) {
        throw new UnsupportedOperationException();
    }

    private Optional<Project> accessProject(String id) {
        this.accessController.checkPermission(AccessController.SERVICES, ProjectAccessControl.ACCESS_PROJECT);

        Project.ID prjId = projectProvider.resolveId(id);
        Project project = projectProvider.findById(prjId);

        return Optional.ofNullable(project);
    }

    private Supplier<Optional<AllowedActions>> supplyProjectActions(String id) {
        return () -> accessProject(id).map(Project::getAllowedActions);
    }

    private Supplier<Optional<Set<com.thinkbiganalytics.metadata.api.security.RoleMembership>>> supplyProjectRoleMemberships(String id) {
        return () -> accessProject(id).map(Project::getRoleMemberships);
    }

    private Supplier<Optional<com.thinkbiganalytics.metadata.api.security.RoleMembership>> supplyProjectRoleMembership(String id, String roleName) {
        return () -> accessProject(id).flatMap(p -> p.getRoleMembership(roleName));
    }

    private Optional<ActionGroup> getAllowedActions(Set<Principal> principals, Supplier<Optional<AllowedActions>> allowedSupplier) {
        return this.metadata.read(() -> allowedSupplier.get().map(allowed -> this.securityTransform.toActionGroup(null).apply(allowed)), principals.stream().toArray(Principal[]::new));
    }

    private Optional<ActionGroup> getAvailableActions(Supplier<Optional<AllowedActions>> allowedSupplier) {
        return this.metadata.read(() -> Optional.of(actionsProvider.getAvailableActions(AllowedActions.TEMPLATE)
                                                        .map(this.securityTransform.toActionGroup(AllowedActions.TEMPLATE))
                                                        .orElseThrow(() -> new WebApplicationException("The available actions were not found",
                                                                                                       Status.NOT_FOUND))));
    }

    private Optional<RoleMemberships> getRoleMemberships(Supplier<Optional<Set<com.thinkbiganalytics.metadata.api.security.RoleMembership>>> assignedSupplier) {
        return getRoleMemberships(EMPTY_SUPPLIER, assignedSupplier);
    }

    private Optional<RoleMemberships> getRoleMemberships(Supplier<Optional<Set<com.thinkbiganalytics.metadata.api.security.RoleMembership>>> inheritedSupplier,
                                                         Supplier<Optional<Set<com.thinkbiganalytics.metadata.api.security.RoleMembership>>> assignedSupplier) {
        return this.metadata.read(() -> {
            Optional<Map<String, RoleMembership>> inheritedMap = inheritedSupplier.get()
                .map(members -> members.stream()
                    .collect(Collectors.toMap(m -> m.getRole().getSystemName(),
                                              securityTransform.toRoleMembership())));
            Optional<Map<String, RoleMembership>> assignedMap = assignedSupplier.get()
                .map(members -> members.stream()
                    .collect(Collectors.toMap(m -> m.getRole().getSystemName(),
                                              securityTransform.toRoleMembership())));
            if (assignedMap.isPresent()) {
                return Optional.of(new RoleMemberships(inheritedMap.orElse(null), assignedMap.get()));
            } else {
                return Optional.empty();
            }
        });
    }

    private Optional<RoleMembership> changeRoleMemberships(RoleMembershipChange change, Supplier<Optional<com.thinkbiganalytics.metadata.api.security.RoleMembership>> domainSupplier) {
        return this.metadata.commit(() -> {
            return domainSupplier.get().map(domain -> {
                switch (change.getChange()) {
                    case ADD:
                        Arrays.stream(securityTransform.asUserPrincipals(change.getUsers())).forEach(p -> domain.addMember(p));
                        Arrays.stream(securityTransform.asGroupPrincipals(change.getGroups())).forEach(p -> domain.addMember(p));
                        break;
                    case REMOVE:
                        Arrays.stream(securityTransform.asUserPrincipals(change.getUsers())).forEach(p -> domain.removeMember(p));
                        Arrays.stream(securityTransform.asGroupPrincipals(change.getGroups())).forEach(p -> domain.removeMember(p));
                        break;
                    case REPLACE:
                        domain.setMemebers(securityTransform.asUserPrincipals(change.getUsers()));
                        domain.setMemebers(securityTransform.asGroupPrincipals(change.getGroups()));
                        break;
                    default:
                        break;
                }

                return securityTransform.toRoleMembership().apply(domain);
            });
        });
    }

    @Override
    public void addRoleChangeListener(RoleChangeListener roleChangeListener) {
        roleChangeListeners.add(roleChangeListener);
    }

}
