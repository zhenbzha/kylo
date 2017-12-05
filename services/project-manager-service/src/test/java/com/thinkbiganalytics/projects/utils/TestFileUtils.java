package com.thinkbiganalytics.projects.utils;

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
import com.thinkbiganalytics.metadata.modeshape.ModeShapeEngineConfig;
import com.thinkbiganalytics.projects.config.ProjectManagerConfig;
import com.thinkbiganalytics.projects.services.JcrTestConfig;
import com.thinkbiganalytics.security.rest.controller.SecurityModelTransform;
import com.thinkbiganalytics.security.service.user.UserService;

import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.support.AnnotationConfigContextLoader;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import javax.inject.Inject;

import static org.assertj.core.api.Assertions.assertThat;

@ContextConfiguration(classes = {ModeShapeEngineConfig.class, JcrTestConfig.class, ProjectManagerConfig.class,
                                 TestFileUtils.Config.class}, loader = AnnotationConfigContextLoader.class)
@TestPropertySource
public class TestFileUtils extends AbstractTestNGSpringContextTests {

    private static final Logger logger = LoggerFactory.getLogger(TestFileUtils.class);

    private Path tmpDir;
    private Path srcPath;
    private Path test1_txt;

    @Inject
    public NotebookRepoObjUtils notebookRepoObjUtils;

    @Inject
    public Cache<Path,Boolean> trackedPaths;

    @BeforeClass
    public void beforeClass() throws IOException {
        tmpDir = Files.createTempDirectory("kylo-");

        srcPath = tmpDir.resolve("source");
        FileUtils.createPrivateDirectory(srcPath.toFile());

        test1_txt = srcPath.resolve("test1.txt");
        FileUtils.writeFile(test1_txt.toString(), "some content for test1.txt\n");

        Path subdir = srcPath.resolve("subdir");
        FileUtils.createPrivateDirectory(subdir.toFile());
        FileUtils.writeFile(subdir.resolve("test2.txt").toString(), "some content for test2.txt\n");

    }

    @AfterClass
    public void afterClass() throws IOException {
        org.apache.commons.io.FileUtils.deleteDirectory(tmpDir.toFile());
    }

    /**
     * @implNote taken from: JcrFeedAllowedActionsTest createFeeds()
     */
    @Test
    public void testReplicateWithNoCollisions() throws IOException {
        Path destPath = tmpDir.resolve("dest");
        FileUtils.createPrivateDirectory(destPath.toFile());

        notebookRepoObjUtils.replicateFileTreeWithHardLinks(srcPath, destPath, false);

        // TODO: Assert stuff
        assertThat(test1_txt.toFile().exists()).isTrue();
        assertThat(destPath.resolve("test1.txt").toFile().exists()).isTrue();

        for( Path p : trackedPaths.asMap().keySet() ){
            logger.info("trackePath p={}", p);
        }
    }

    @Test(dependsOnMethods = "testReplicateWithNoCollisions")
    public void testReplicateWithCollisions() {
    }

    @Configuration
    static class Config {
        @Bean
        public SecurityModelTransform securityModelTransform() {
            return Mockito.mock(SecurityModelTransform.class);
        }

        @Bean
        public UserService userService() {
            return Mockito.mock(UserService.class);
        }

        @Bean
        public PropertySourcesPlaceholderConfigurer placeHolderConfigurer() {
            final Properties properties = new Properties();
            properties.setProperty("security.entity.access.controlled", "true");

            final PropertySourcesPlaceholderConfigurer configurer = new PropertySourcesPlaceholderConfigurer();
            configurer.setProperties(properties);
            return configurer;
        }

    }
}
