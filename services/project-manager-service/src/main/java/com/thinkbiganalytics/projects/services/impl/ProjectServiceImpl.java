package com.thinkbiganalytics.projects.services.impl;

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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.thinkbiganalytics.metadata.api.MetadataAccess;
import com.thinkbiganalytics.metadata.api.project.Project;
import com.thinkbiganalytics.metadata.api.project.security.ProjectAccessControl;
import com.thinkbiganalytics.metadata.modeshape.JcrMetadataAccess;
import com.thinkbiganalytics.metadata.modeshape.project.JcrProject;
import com.thinkbiganalytics.metadata.modeshape.project.providers.ProjectProvider;
import com.thinkbiganalytics.projects.exceptions.NotebookIoException;
import com.thinkbiganalytics.projects.exceptions.ProjectManagerException;
import com.thinkbiganalytics.projects.security.ProjectSecurityService;
import com.thinkbiganalytics.projects.security.RoleChangeListener;
import com.thinkbiganalytics.projects.services.NotebookFileSystemService;
import com.thinkbiganalytics.projects.services.ProjectService;
import com.thinkbiganalytics.projects.services.ProjectsTransform;
import com.thinkbiganalytics.projects.utils.NotebookRepoObjUtils;
import com.thinkbiganalytics.security.UsernamePrincipal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.xml.ws.Holder;

public class ProjectServiceImpl implements ProjectService {

    public static Logger logger = LoggerFactory.getLogger(ProjectServiceImpl.class);

    @Inject
    private MetadataAccess metadata;

    @Inject
    private ProjectProvider projectProvider;

    @Inject
    private NotebookFileSystemService notebookFileSystemService;

    @Inject
    private NotebookRepoObjUtils notebookRepoObjUtils;

    @Inject
    private ProjectSecurityService projectSecurityService;

    @Inject
    private ProjectsTransform projectsTransform;

    @Value("${notebooks.master.repository}")
    private File masterRepo;

    @Value("${notebooks.users.repository}")
    private File usersRepo;

    @PostConstruct
    private void postConstruct() {
        projectSecurityService.addRoleChangeListener(new RoleChangeListener() {
            @Override
            public void userGrantedRole(UsernamePrincipal user, Project.ID projectId, String roleName) {
                Project project = projectProvider.findById(projectId);
                ensureProjectRepo(project);

                //TODO: ensureProjectMounts needs to replicate master project repo for user?  Think it does already
                notebookFileSystemService.ensureProjectMounts(user.getName(), ImmutableList.of(project));
            }

            @Override
            public void userRevokedRole(UsernamePrincipal user, Project.ID projectId, String roleName) {
                Project project = projectProvider.findById(projectId);

                notebookFileSystemService.deleteProjectRepo(user.getName(), project.getSystemName());
            }
        });
    }

    /**
     * Use cases: UC1:  Create Project Populates project data in JCR If “NotebookFolderName” is populated then string field tba:Project.notebookFolderName is set.  See also NB1 NB1:  User create’s
     * notebookFolder File structure in master repo, and file structure in user repo must be created.  No notebooks/files as of yet..
     *
     * @implNote takes a Project rest object, suitable for JSON serialization (not JcrProject )
     */
    //  services should take a REST model object, create the domain object from it, and perform any needed functionality
    @Override
    public Project createProject(com.thinkbiganalytics.metadata.rest.model.Project project) {
            /*
            actionsProvider.getAllowedActions(AllowedActions.PROJECTS).ifPresent(allowed -> allowed.enableAll(TEST_USER1));
            actionsProvider.getAllowedActions(AllowedActions.PROJECTS).ifPresent(allowed -> allowed.enableAll(TEST_USER2));

            this.roleProvider.createRole(SecurityRole.PROJECT, "testEditor", "Editor", "Can edit projects")
                .setPermissions(ProjectAccessControl.EDIT_PROJECT, FeedAccessControl.ENABLE_DISABLE, FeedAccessControl.EXPORT);
            this.roleProvider.createRole(SecurityRole.PROJECT, "testViewer", "Viewer", "Can view projects only")
                .setPermissions(ProjectAccessControl.ACCESS_PROJECT);
                */
        final Holder<Project> domainHolder = new Holder<>();
        metadata.commit(() -> {
            Project domain = projectProvider.createProject(project.getSystemName());
            projectsTransform.restToDomain(project, domain);

            ensureProjectRepo(domain);
            domainHolder.value = domain;
        });
        return domainHolder.value;
    }

    private void ensureProjectRepo(Project domain) {
        ensureProjectRepo(domain, domain.isNotebookFolderEnabled());
    }

    private void ensureProjectRepo(Project domain, boolean ensureUserRepo) {
        notebookFileSystemService.ensureMasterProjectRepository(domain.getSystemName());
        if (ensureUserRepo) {
            UsernamePrincipal user = JcrMetadataAccess.getActiveUser();
            notebookFileSystemService.ensureProjectMounts(user.getName(), ImmutableList.of(domain));
        }
    }

    /**
     * UC2:  Update Project - Updates project data in JCR If “NotebookFolderName” is changed then string field tba:Project.notebookFolderName is changed # This also means that the directory in master
     * repo will need to be renamed, and all user repos as well.
     *
     * NB2:  User rename’s notebookFolder note all users with the notebookFolder’s on the file system ( becomes the list to replace) delete all the notebookFolder's in all user repos rename the
     * notebookFolder in master repo call FileUtils.replicateFileTreeWithHardLinks for all user’s who need access (determine that from list created in first step)
     */
    @Override
    public void updateProject(com.thinkbiganalytics.metadata.rest.model.Project project) {
        // TODO: need to check for edit privs?
        //this.accessController.checkPermission(AccessController.SERVICES, ProjectAccessControl.EDIT_PROJECT);

        metadata.commit(() -> {

            Optional<Project> domainHolder = projectProvider.findProjectByName(project.getSystemName());
            if (domainHolder.isPresent()) {
                Project domain = domainHolder.get();

                // Handle change in notebook folder enabled status
                if (project.isNotebookFolderEnabled() != domain.isNotebookFolderEnabled()) {
                    // a change to a notebook folder occurred
                    if (!domain.isNotebookFolderEnabled() && project.isNotebookFolderEnabled()) {
                        // notebook folder was enabled...
                        //  TODO: file structure should include a path for the notebook folder too.. for now just projects?
                        ensureProjectRepo(domain, true);

                        // TODO: handle case where project already contains a list of users with the right roles and enable them..

                    } else if (domain.isNotebookFolderEnabled() && !project.isNotebookFolderEnabled()) {
                        // The notebook folder is disabled and should be removed.
                        // .. so delete the notebook project repos
                        notebookFileSystemService.deleteAllProjectRepos(domain.getSystemName());
                    }
                }

                // Update the domain entity
                final Project domainProject = projectProvider.update(projectsTransform.restToDomain(project, domain));

                // Repopulate identifier
                project.setId(domainProject.getId().toString());

                ///update access control
                //TODO only do this when modifying the access control
                if (domainProject.getAllowedActions().hasPermission(ProjectAccessControl.CHANGE_PERMS)) {
                    project.toRoleMembershipChangeList().stream()
                        .forEach(roleMembershipChange -> projectSecurityService.changeProjectRoleMemberships(domainProject.getId().toString(), roleMembershipChange));
                }

            } else {
                throw new ProjectManagerException("no project with that name found");
            }
        });
    }

    @Override
    public void deleteProject(String id) {
        metadata.commit(() -> {
            Project.ID pid = new JcrProject.ProjectId(id);

            Project domain = projectProvider.findById(pid);

            if (domain != null) {
                if (domain.isNotebookFolderEnabled()) {
                    notebookFileSystemService.deleteAllProjectRepos(domain.getSystemName());
                }

                projectProvider.deleteProject(domain);
            } else {
                throw new ProjectManagerException(String.format("no project with the ID '%s' found", pid));
            }
        });
    }

    @Override
    public void userFileCreated(Path userRepo, File file) {
        logger.debug("userFileCreated({},{}) called", userRepo, file);

        Path relativeToUserRepo = userRepo.relativize(file.toPath());
        // extract user name
        String originatingUser = relativeToUserRepo.getName(0).toString();
        relativeToUserRepo = relativeToUserRepo.subpath(1, relativeToUserRepo.getNameCount());
        Path dest = masterRepo.toPath().resolve(relativeToUserRepo);

        // step1: synchronize the create to the master repo.
        notebookRepoObjUtils.linkOrReplicate(file, dest);

        // step 2: synchronize the create to all projects in all user repos.
        //     get the accessors to the project via JcrProjectProvider.getProjectAccessors.
        // TODO: need to distinguish between readers and writers
        Path projectName = relativeToUserRepo.getName(0);

        Set<UsernamePrincipal> users = getUsersWithProjectAccess(projectName.toString());
        for (UsernamePrincipal user : users) {
            if (user.getName().equals(originatingUser)) {
                // skip originating user
                continue;
            }
            String userName = user.getName();
            Path userDest = usersRepo.toPath().resolve(userName).resolve(relativeToUserRepo);

            // synchronize the create to other user repos
            notebookRepoObjUtils.linkOrReplicate(file, userDest);
        }
    }

    @Override
    public void userFileDeleted(Path userRepo, File file) {
        logger.debug("userFileDeleted({},{}) called", userRepo, file);

        Path relativeToUserRepo = userRepo.relativize(file.toPath());
        // extract user name
        String originatingUser = relativeToUserRepo.getName(0).toString();
        relativeToUserRepo = relativeToUserRepo.subpath(1, relativeToUserRepo.getNameCount());
        Path dest = masterRepo.toPath().resolve(relativeToUserRepo);

        // step1: synchronize the create to the master repo.
        try {
            File destFile = dest.toFile();
            if (destFile.exists()) {
                org.apache.commons.io.FileUtils.forceDelete(destFile);
            }
        } catch (IOException e) {
            logger.error("Unable to remove master repo link '{}' when user fie system object '{}' was removed by user", dest, file);
            throw new NotebookIoException(String.format("Unable to remove file '%s', could be possible race condition", dest));
        }

        // step 2: synchronize the create to all projects in all user repos.
        //     get the accessors to the project via JcrProjectProvider.getProjectAccessors.
        // TODO: need to distinguish between readers and writers
        Path projectName = relativeToUserRepo.getName(0);

        Set<UsernamePrincipal> users = getUsersWithProjectAccess(projectName.toString());
        for (UsernamePrincipal user : users) {
            if (user.getName().equals(originatingUser)) {
                // skip originating user
                continue;
            }
            String userName = user.getName();
            Path userDest = usersRepo.toPath().resolve(userName).resolve(relativeToUserRepo);

            try {
                File userDestFile = userDest.toFile();
                if (userDestFile.exists()) {
                    org.apache.commons.io.FileUtils.forceDelete(userDestFile);
                }
            } catch (IOException e) {
                logger.error("Unable to remove user repo link '{}' when another user fie system object '{}' was removed by user", userDest, file);
                throw new NotebookIoException(String.format("Unable to remove file '%s', could be possible race condition", userDest));
            }
        }
    }

    @Override
    public Collection<com.thinkbiganalytics.metadata.rest.model.Project> getProjects() {
        Collection<com.thinkbiganalytics.metadata.rest.model.Project> projects = Lists.newLinkedList();
        metadata.read(() -> projectProvider.getProjects().stream()
            .forEach(jp -> projects.add(projectsTransform.domainToRest(jp))));

        return projects;
    }

    @Override
    public com.thinkbiganalytics.metadata.rest.model.Project getProject(String systemName) {
        final Holder<Project> holder = new Holder<>();
        metadata.read(() -> {
            holder.value = projectProvider.findProjectByName(systemName).get();
            logger.debug("holder.value = {}", holder.value);
        });

        com.thinkbiganalytics.metadata.rest.model.Project rest = null;
        if (holder.value != null) {
            rest = projectsTransform.domainToRest(holder.value);
        }

        return rest;
    }


    /**
     * Queries metadata repository for all users with any kind of access to a Project with the name given
     *
     * @implNote uses the metadata service account.  This OK as we are not running in the name of any one user and details are not exposed to a user.
     */
    private Set<UsernamePrincipal> getUsersWithProjectAccess(String projectName) {
        Set<UsernamePrincipal> users = Sets.newHashSet();

        metadata.read(() -> {
            Project project = projectProvider.findProjectByName(projectName.toString())
                // TODO: specialize me
                .orElseThrow(() -> new RuntimeException("A user file was created in a project space that is not tracked by Kylo"));

            if (project.getOwner() instanceof UsernamePrincipal) {
                users.add((UsernamePrincipal) project.getOwner()); // owner's always have access
            }
            users.addAll(projectProvider.getProjectUsers(project));

        }, JcrMetadataAccess.SERVICE);
        return users;
    }

}
