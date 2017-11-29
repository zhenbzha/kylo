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


import com.thinkbiganalytics.security.rest.model.ActionGroup;
import com.thinkbiganalytics.security.rest.model.PermissionsChange;
import com.thinkbiganalytics.security.rest.model.PermissionsChange.ChangeType;
import com.thinkbiganalytics.security.rest.model.RoleMembership;
import com.thinkbiganalytics.security.rest.model.RoleMembershipChange;
import com.thinkbiganalytics.security.rest.model.RoleMemberships;

import java.security.Principal;
import java.util.Optional;
import java.util.Set;

/**
 *
 */
public interface ProjectSecurityService {

    Optional<ActionGroup> getAvailableProjectActions(String id);

    Optional<ActionGroup> getAllowedProjectActions(String id, Set<Principal> principals);

    Optional<ActionGroup> changeProjectPermissions(String id, PermissionsChange change);

    Optional<RoleMemberships> getProjectRoleMemberships(String id);

    Optional<RoleMembership> changeProjectRoleMemberships(String id, RoleMembershipChange change);

    Optional<PermissionsChange> createProjectPermissionChange(String id, ChangeType changeType, Set<Principal> members);

    void addRoleChangeListener(RoleChangeListener roleChangeListener);
}
