package com.thinkbiganalytics.projects.config;

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
import com.google.common.cache.CacheBuilder;
import com.thinkbiganalytics.projects.controllers.ProjectController;
import com.thinkbiganalytics.projects.security.DefaultProjectSecurityService;
import com.thinkbiganalytics.projects.security.ProjectSecurityService;
import com.thinkbiganalytics.projects.services.NotebookFileSystemService;
import com.thinkbiganalytics.projects.services.ProjectService;
import com.thinkbiganalytics.projects.services.ProjectsTransform;
import com.thinkbiganalytics.projects.services.RecursiWatcherService;
import com.thinkbiganalytics.projects.services.UserRepoListener;
import com.thinkbiganalytics.projects.services.files.NotebookRepoObjService;
import com.thinkbiganalytics.projects.services.impl.NotebookFileSystemServiceImpl;
import com.thinkbiganalytics.projects.services.impl.ProjectServiceImpl;
import com.thinkbiganalytics.projects.utils.NotebookRepoObjUtils;
import com.thinkbiganalytics.projects.utils.tracking.TrackingUtils;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.File;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

@Configuration
public class ProjectManagerConfig {

    @Value("${notebooks.users.repository}")
    private File userRepo;

    @Bean
    public NotebookFileSystemService notebookFileSystemService() {
        return new NotebookFileSystemServiceImpl();
    }

    @Bean
    public RecursiWatcherService recursiWatcherService() {
        RecursiWatcherService rws = new RecursiWatcherService();

        // TODO: Temporary code to watch all user repos in ${notebooks.users.repository}.  Later,
        // TODO: ... as a performance improvement, we could decide to only create listeners for active notebook containers.
        rws.registerListener(new UserRepoListener(projectService(), trackedPaths(), userRepo.toPath()));

        return rws;
    }

    @Bean
    public ProjectService projectService() {
        return new ProjectServiceImpl();
    }

    @Bean
    public ProjectSecurityService projectSecurityService() {
        return new DefaultProjectSecurityService();
    }

    @Bean
    public ProjectController projectController() {
        return new ProjectController();
    }

    @Bean
    public ProjectsTransform projectsTransform() {
        return new ProjectsTransform();
    }

    @Bean
    public NotebookRepoObjService notebookRepoObjService() {
        return new NotebookRepoObjService();
    }

    @Bean
    public NotebookRepoObjUtils notebookRepoObjUtils() {
        return new NotebookRepoObjUtils();
    }

    @Bean
    public TrackingUtils trackingUtils() {
        return new TrackingUtils();
    }

    @Bean
    public Cache<Path, Boolean> trackedPaths() {
        Cache<Path, Boolean> trackedPaths = CacheBuilder.newBuilder()
            .maximumSize(10000)
            .expireAfterWrite(20, TimeUnit.MINUTES)
            .build();

        return trackedPaths;
    }
}
