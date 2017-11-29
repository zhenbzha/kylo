package com.thinkbiganalytics.projects.serialization;

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
import com.thinkbiganalytics.projects.transforms.ProjectSerializer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.support.AnnotationConfigContextLoader;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.springframework.util.StreamUtils;
import org.testng.annotations.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import javax.inject.Inject;

import static org.assertj.core.api.Assertions.assertThat;


@ContextConfiguration(classes = {
    TestProjectSerialization.Config.class}, loader = AnnotationConfigContextLoader.class)
public class TestProjectSerialization extends AbstractTestNGSpringContextTests {

    private static final Logger logger = LoggerFactory.getLogger(TestProjectSerialization.class);

    @Inject
    public String project1Json;

    String id = "7286d5d1-163d-49cc-a678-7705327c6859";
    String systemName = "Project1";
    String projectName = "Project1";
    String projectDescription = "Project1 description";
    String projectIcon = "question";
    String projectIconColor = "blue";
    boolean projectNotebookFolder = true;

    /**
     * @implNote taken from: JcrFeedAllowedActionsTest createFeeds()
     */
    @Test
    public void testCreateProject() {
        Project rest = new Project();
        rest.setId(id);
        rest.setSystemName(systemName);
        rest.setProjectName(projectName);
        rest.setDescription(projectDescription);
        rest.setIcon(projectIcon);
        rest.setIconColor(projectIconColor);
        rest.setNotebookFolderEnabled(projectNotebookFolder);

        String json = ProjectSerializer.serialize(rest);
        logger.debug("{}", json);

        Project deserializedProject = ProjectSerializer.deserialize(json, Project.class);
        validateProject1(deserializedProject);
        assertThat(rest).isEqualTo(deserializedProject);
    }

    @Test
    public void testLoadProject() {
        Project deserializedProject = ProjectSerializer.deserialize(project1Json, Project.class);
        validateProject1(deserializedProject);
    }

    private void validateProject1(Project project) {
        assertThat(project)
            .extracting(Project::getId, Project::getSystemName, Project::getProjectName, Project::getDescription, Project::getIcon,
                        Project::getIconColor, Project::isNotebookFolderEnabled)
            .containsExactly(id, systemName, projectName, projectDescription, projectIcon, projectIconColor, projectNotebookFolder);
    }

    @Configuration
    static class Config {

        @Value("classpath:com/thinkbiganalytics/projects/serialization/project1.json")
        private Resource project1;

        @Bean
        public String project1Json() throws IOException {
            try (InputStream is = project1.getInputStream()) {
                return StreamUtils.copyToString(is, StandardCharsets.UTF_8);
            }
        }
    }

}
