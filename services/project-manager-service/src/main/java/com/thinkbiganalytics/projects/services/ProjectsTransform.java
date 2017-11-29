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
import com.thinkbiganalytics.security.rest.controller.SecurityModelTransform;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

public class ProjectsTransform {
    public static Logger logger = LoggerFactory.getLogger(ProjectsTransform.class);

    @Inject
    SecurityModelTransform securityModelTransform;

    public Project domainToRest(com.thinkbiganalytics.metadata.api.project.Project domain) {
        logger.trace("ProjectsTransform.domainToRest({}) called.", domain);

        Project rest = new Project();
        securityModelTransform.applyAccessControl(domain, rest);

        rest.setId(domain.getId().toString());
        rest.setSystemName(domain.getSystemName());
        rest.setProjectName(domain.getProjectName());
        rest.setDescription(domain.getDescription());
        rest.setIcon(domain.getIcon());
        rest.setIconColor(domain.getIconColor());
        rest.setNotebookFolderEnabled(domain.isNotebookFolderEnabled());

        return rest;
    }

    public com.thinkbiganalytics.metadata.api.project.Project restToDomain(Project rest, com.thinkbiganalytics.metadata.api.project.Project domain) {
        logger.trace("ProjectsTransform.restToDomain(rest={}, domainNotProvidedInLog) called.", rest);

        domain.setSystemName(rest.getSystemName());
        domain.setProjectName(rest.getProjectName());
        domain.setDescription(rest.getDescription());
        domain.setIcon(rest.getIcon());
        domain.setIconColor(rest.getIconColor());
        domain.setNotebookFolderEnabled(rest.isNotebookFolderEnabled());

        return domain;
    }
}
