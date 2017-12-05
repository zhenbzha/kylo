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
import com.thinkbiganalytics.metadata.rest.model.Project;
import com.thinkbiganalytics.projects.config.ProjectManagerConfig;
import com.thinkbiganalytics.projects.utils.FileUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.support.AnnotationConfigContextLoader;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.annotations.Test;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Properties;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

@ContextConfiguration(classes = {ProjectManagerConfig.class,
                                 TestRecursiWatcherService.Config.class}, loader = AnnotationConfigContextLoader.class)
@TestPropertySource
public class TestRecursiWatcherService extends AbstractTestNGSpringContextTests {

    private static final Logger logger = LoggerFactory.getLogger(TestRecursiWatcherService.class);
    public static File staticMasterRepo;

    @Inject
    private RecursiWatcherService recursiWatcherService;
    @Inject
    private ProjectService projectService;
    @Inject
    private Cache<Path, Boolean> trackedPaths;


    @Value("${notebooks.master.repository}")
    private File masterRepo;
    @Value("${notebooks.users.repository}")
    private File usersRepo;

    @PostConstruct
    public void postConstruct() {
        staticMasterRepo = masterRepo;
    }

    @Test(enabled = false)
    public void testRecursi() throws InterruptedException {
        File f = Paths.get(recursiWatcherService.getRootFolder().getAbsolutePath(), "stop").toFile();
        if (f.exists()) {
            f.delete();
        }

        /*
        logger.info("Create {} to exit . . . ", f);
        while (true) {
            if (f.exists()) {
                break;
            }
            Thread.sleep(100);
        }*/
    }

    @Test(enabled = false, dependsOnMethods = "testRecursi")
    public void testRecursiUser() throws InterruptedException {
        File noUser = Paths.get(recursiWatcherService.getRootFolder().getAbsolutePath(), "service").toFile();
        noUser.mkdirs();

        recursiWatcherService.registerListener(new UserRepoListener(projectService, trackedPaths,
                                                                    recursiWatcherService.getRootFolder().toPath()));

        File f = Paths.get(recursiWatcherService.getRootFolder().getAbsolutePath(), "stop").toFile();
        if (f.exists()) {
            f.delete();
        }

        logger.info("Create {} to exit . . . ", f);
        while (true) {
            if (f.exists()) {
                break;
            }
            Thread.sleep(100);
        }
    }

    @Configuration
    static class Config {

        @Bean
        public ProjectService projectService() {
            return new MockProjectService();
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

    static class MockProjectService implements ProjectService {

        @Override
        public com.thinkbiganalytics.metadata.api.project.Project createProject(Project project) {
            return null;
        }

        @Override
        public void updateProject(Project project) {
        }

        @Override
        public void deleteProject(String id) {
        }

        @Override
        public void userFileCreated(Path userRepo, File file) {
            logger.debug("{}     {} ", userRepo, file);

            Path projectName = userRepo.relativize(file.toPath()).getName(0);
            Path dest = staticMasterRepo.toPath()
                .resolve(projectName)
                .resolve(file.getName());
            logger.debug("{}", dest);
            FileUtils.linkOrReplicate(file, dest);
        }

        @Override
        public void userFileDeleted(Path userRepo, File file) {
        }

        @Override
        public Collection<Project> getProjects() {
            return null;
        }

        @Override
        public Project getProject(String systemName) {
            return null;
        }
    }
}
