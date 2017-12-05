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

import com.google.common.cache.Cache;

import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Path;

import javax.annotation.Nonnull;

public class UserRepoListener implements RecursiWatcherService.IFileListener {

    private static final Logger logger = LoggerFactory.getLogger(UserRepoListener.class);

    private Path userRepo;

    private ProjectService projectService;

    private Cache<Path, Boolean> trackedPaths;

    public UserRepoListener(@Nonnull ProjectService projectService, @Nonnull Cache<Path, Boolean> trackedPaths, @Nonnull Path userRepo) {
        this.projectService = projectService;
        this.trackedPaths = trackedPaths;

        Validate.isTrue(userRepo.toFile().exists(),
                        String.format("The user repo '%s' does not exist, create the repo before creating the listener", userRepo));
        this.userRepo = userRepo;
    }

    @Override
    public void fileModified(File file) {
        throw new UnsupportedOperationException("Don't expect to respond to any file modification events");
    }

    @Override
    public void fileDeleted(File file) {
        logger.debug("fileDeleted({}) called", file);
        if (trackedPaths.asMap().containsKey(file.toPath())) {
            projectService.userFileDeleted(userRepo, file);
        }
    }

    @Override
    public void fileCreated(File file) {
        logger.debug("fileCreated({}) called", file);
        if (trackedPaths.asMap().containsKey(file.toPath())) {
            projectService.userFileCreated(userRepo, file);
        }
    }
}
