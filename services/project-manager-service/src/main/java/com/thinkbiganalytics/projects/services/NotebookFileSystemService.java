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

import com.thinkbiganalytics.metadata.api.project.Project;

import java.nio.file.Path;
import java.util.List;

public interface NotebookFileSystemService {

    /**
     * @implNote repositories are where data is kept, mounts are where they hard linked for users
     */
    Path ensureMasterProjectRepository(String projectPath);

    /**
     * Ensures the current user's list of accessible notebooks.
     *
     * Example usage:
     *  a) At notebook launch time get a list of all projects the user has access to and ensure hard links.
     *  b) The current user is granting access to a given user for a given project
     */
    void ensureProjectMounts(String userName, List<Project> projects);

    /**
     * delete's master project repo and all user repo's
     */
    void deleteAllProjectRepos(String systemName);

    /**
     * remove's a user's project repo
     */
    void deleteProjectRepo(String user, String systemName);

}
