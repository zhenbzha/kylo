package com.thinkbiganalytics.projects.services;

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

import com.thinkbiganalytics.metadata.rest.model.Project;

import java.util.Collection;

public interface ProjectService {

    /**
     * Use cases: UC1:  Create Project Populates project data in JCR If “NotebookFolderName” is populated then string field tba:Project.notebookFolderName is set. See also NB1:  User create’s
     * notebookFolder File structure in master repo, and file structure in user repo must be created.  No notebooks/files as of yet..
     *
     * @implNote takes a Project rest object, suitable for JSON serialization (not JcrProject )
     */
    com.thinkbiganalytics.metadata.api.project.Project createProject(Project project);

    /**
     * UC2:  Update Project - Updates project data in JCR If “NotebookFolderName” is changed then string field tba:Project.notebookFolderName is changed # This also means that the directory in master
     * repo will need to be renamed, and all user repos as well.
     *
     * NB2:  User rename’s notebookFolder note all users with the notebookFolder’s on the file system ( becomes the list to replace) delete all the notebookFolder's in all user repos rename the
     * notebookFolder in master repo call FileUtils.replicateFileTreeWithHardLinks for all user’s who need access  (determine that from list created in first step)
     */
    void updateProject(Project project);

    /**
     * UC3:  Delete Project Delete project data in JCR If the project contained a value for tba:notebookFolderName then the folder must be removed from master repository and all repositories NB3: User
     * delete’s notebookFolder delete all user noteboookFolder’s delete master notebookFolder
     */
    void deleteProject(String id);

    /**
     * Get the list of projects
     *
     * @return a collection of projects
     */
    Collection<Project> getProjects();

    /**
     * get a project by it's systemName
     * @param systemName  the systemName of the project to get
     * @return the project
     * @throws com.thinkbiganalytics.projects.exceptions.ProjectManagerException if the project cannot be found
     *
     */
    Project getProject(String systemName);
}
