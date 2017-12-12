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

import com.thinkbiganalytics.projects.controllers.ProjectController;
import com.thinkbiganalytics.projects.security.DefaultProjectSecurityService;
import com.thinkbiganalytics.projects.security.ProjectSecurityService;
import com.thinkbiganalytics.projects.services.NotebookFileSystemService;
import com.thinkbiganalytics.projects.services.ProjectService;
import com.thinkbiganalytics.projects.services.ProjectsTransform;
import com.thinkbiganalytics.projects.services.impl.NotebookFileSystemServiceImpl;
import com.thinkbiganalytics.projects.services.impl.ProjectServiceImpl;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.File;

@Configuration
public class ProjectManagerConfig {

    @Value("${notebooks.users.repository}")
    private File userRepo;

    @Bean
    public NotebookFileSystemService notebookFileSystemService() {
        return new NotebookFileSystemServiceImpl();
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

}
