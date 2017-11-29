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

import com.google.common.collect.Sets;
import com.thinkbiganalytics.metadata.api.project.Project;
import com.thinkbiganalytics.projects.services.NotebookFileSystemService;
import com.thinkbiganalytics.projects.utils.FileUtils;

import org.apache.commons.lang.SystemUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

public class NotebookFileSystemServiceImpl implements NotebookFileSystemService {

    private static final Logger logger = LoggerFactory.getLogger(NotebookFileSystemServiceImpl.class);

    @Value("${notebooks.master.repository}")
    private File masterRepo;

    @Value("${notebooks.users.repository}")
    private File usersRepo;

    /**
     * Using the configuration given, creates and ensures the main project repository is fit for purpose.
     */
    @Override
    public Path ensureProjectRepository(String project) {
        logger.debug("project={}", project);
        logger.debug("masterRepo={}", masterRepo);
        Path repoPath = ensurePath(masterRepo);

        Path projectRepo = ensurePath(repoPath.resolve(project).toFile());
        logger.debug("projectRepo={}", projectRepo);
        return projectRepo;
    }


    /**
     * Ensures the current user's list of accessible notebook folders.
     *
     * Example usage: a) At notebook launch time get a list of all projects the user has access to and ensure hard links.  This must be done for all file/directory resources for any project found,
     * since it all may have changed since time the container was launched. NOTE: Truly? what if the file watcher has kept all this in synch?  Technically, notebooks can be accessed outside of kylo
     * and there is potential for that.  Also, host OS could put files in the master notebook repository (which is not being watched) (who cares?)
     *
     * b) The current user is granting access to a given user for a given project.  The notebook folder directory in the notebooks master repository will not have been synchronized to the new user, so
     * this is a must.
     */
    @Override
    public void ensureProjectMounts(String userName, List<Project> projects) {
        logger.debug("userName='{}', projects='{}'", userName, projects);

        // TODO: /opt/kylo/fs/nbUsers/write vs /opt/kylo/fs/nbUsers/read
        ensurePath(usersRepo);

        // projects under userRepo are links to the master repo project
        for (Project project : projects) {
            Path projPath = usersRepo.toPath().resolve(userName).resolve(project.getProjectName());
            logger.debug("projectRepo={}", projPath);
            ensurePath(projPath);

            FileUtils.replicateFileTreeWithHardLinks(masterRepo.toPath().resolve(project.getProjectName()),
                                                         projPath, false);
        } // end for
    } // end method

    private File ensurePath(Path path) {
        return ensurePath( path.toFile()).toFile();
    }

    private Path ensurePath(File path) {
        if (path.exists()) {
            // TODO: need to ensure this path is exactly rwx for owner only
            if (!(path.isDirectory() &&
                  path.canRead() &&
                  path.canWrite() &&
                  path.canExecute())) {
                throw new RuntimeException(String.format("masterRepo='%s' is unsuitable as a notebook "
                                                         + "repository location as it cannot be managed by the user='%s",
                                                         path,
                                                         SystemUtils.USER_NAME));
            } // end if
            return path.toPath();
        } else {
            return FileUtils.createPrivateDirectory(path);
        }
    }


    // TODO: this method should get user permitted access to the project and delete those.  Then as validation could check for project directories that still exist
    @Override
    public void deleteAllProjectRepos(String systemName) {
        // Find the project in the master repo and add to our removeDirs
        File[] directories = masterRepo.listFiles(new FileFilter() {
            @Override
            public boolean accept(File file) {
                return file.isDirectory() && file.getName().equals(systemName);
            }
        });
        Set<File> removeDirs = Sets.newHashSet(directories);

        usersRepo.listFiles(file -> {
            File userProj = file.toPath().resolve(systemName).toFile();
            if( userProj.isDirectory() ) {
                removeDirs.add(userProj);
            }
            return false;
        });

        for( File dir : removeDirs ) {
            logger.info("Removing directory '{}' and all it's contents", dir);
            try {
                org.apache.commons.io.FileUtils.deleteDirectory(dir);
            } catch (IOException e) {
                logger.error("Unable to remove directory '{}' and all it's contents", dir);
            }
        }
    }


    @Override
    public void deleteProjectRepo(String user, String systemName) {
        throw new UnsupportedOperationException("need code to delete user's repo");
    }
}
