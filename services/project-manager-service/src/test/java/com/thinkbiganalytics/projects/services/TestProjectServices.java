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

import com.thinkbiganalytics.metadata.api.MetadataAccess;
import com.thinkbiganalytics.metadata.api.project.security.ProjectAccessControl;
import com.thinkbiganalytics.metadata.modeshape.JcrMetadataAccess;
import com.thinkbiganalytics.metadata.modeshape.MetadataRepositoryException;
import com.thinkbiganalytics.metadata.modeshape.ModeShapeEngineConfig;
import com.thinkbiganalytics.metadata.rest.model.Project;
import com.thinkbiganalytics.projects.config.ProjectManagerConfig;
import com.thinkbiganalytics.projects.services.files.UserFsObj;
import com.thinkbiganalytics.projects.services.impl.ProjectServiceImpl;
import com.thinkbiganalytics.projects.utils.FileUtils;
import com.thinkbiganalytics.security.UsernamePrincipal;
import com.thinkbiganalytics.security.action.AllowedActions;
import com.thinkbiganalytics.security.action.config.ActionsModuleBuilder;
import com.thinkbiganalytics.security.rest.controller.SecurityModelTransform;
import com.thinkbiganalytics.security.service.user.UserService;

import org.mockito.Mockito;
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
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.inject.Inject;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


@ContextConfiguration(classes = {ModeShapeEngineConfig.class, JcrTestConfig.class, ProjectManagerConfig.class,
                                 TestProjectServices.TestProjectServiceConfig.class}, loader = AnnotationConfigContextLoader.class)
@TestPropertySource
public class TestProjectServices extends AbstractTestNGSpringContextTests {

    private static final Logger logger = LoggerFactory.getLogger(TestProjectServices.class);

    private static final UsernamePrincipal TEST_USER1 = new UsernamePrincipal("tester1");
    private static final UsernamePrincipal TEST_USER2 = new UsernamePrincipal("tester2");
    Path pathToMasterProject4;
    Path pathToServiceProject4;
    Path pathToTester1Project4;
    Path pathToTester2Project4;
    @Inject
    private JcrMetadataAccess metadata;
    @Resource
    private ProjectService projectService;
    @Inject
    private ActionsModuleBuilder builder;
    @Value("${notebooks.master.repository}")
    private File masterRepo;
    @Value("${notebooks.users.repository}")
    private File usersRepo;

    @BeforeSuite
    public static void beforeSuite() throws IOException {
        org.apache.commons.io.FileUtils.deleteDirectory(Paths.get("/opt/kylo/fs/notebooks/Project4").toFile());
        org.apache.commons.io.FileUtils.deleteDirectory(Paths.get("/opt/kylo/fs/nbUsers/service/Project4").toFile());
    }

    @BeforeClass
    public void beforeClass() {
        metadata.commit(() -> builder
            .module(AllowedActions.PROJECTS)
            .action(ProjectAccessControl.EDIT_PROJECT)
            .action(ProjectAccessControl.ACCESS_PROJECT)
            .action(ProjectAccessControl.CHANGE_PERMS)
            .action(ProjectAccessControl.DELETE_PROJECT)
            .add()
            .build(), MetadataAccess.SERVICE);
    }

    @PostConstruct
    public void postConstruct() {
        pathToMasterProject4 = masterRepo.toPath().resolve("Project4");
        pathToServiceProject4 = usersRepo.toPath().resolve("service").resolve("Project4");
        pathToTester1Project4 = usersRepo.toPath().resolve("tester1").resolve("Project4");
        pathToTester2Project4 = usersRepo.toPath().resolve("tester2").resolve("Project4");
    }

    /**
     * @implNote taken from: JcrFeedAllowedActionsTest createFeeds()
     */
    @Test
    public void testCreateProject() {
        String systemName = "Project4";

        metadata.commit(() -> {
            /*
            actionsProvider.getAllowedActions(AllowedActions.PROJECTS).ifPresent(allowed -> allowed.enableAll(TEST_USER1));
            actionsProvider.getAllowedActions(AllowedActions.PROJECTS).ifPresent(allowed -> allowed.enableAll(TEST_USER2));

            this.roleProvider.createRole(SecurityRole.PROJECT, "testEditor", "Editor", "Can edit projects")
                .setPermissions(ProjectAccessControl.EDIT_PROJECT, FeedAccessControl.ENABLE_DISABLE, FeedAccessControl.EXPORT);
            this.roleProvider.createRole(SecurityRole.PROJECT, "testViewer", "Viewer", "Can view projects only")
                .setPermissions(ProjectAccessControl.ACCESS_PROJECT);
                */

            Project rest = new Project();
            rest.setSystemName(systemName);
            rest.setProjectName("Project4");
            rest.setDescription("Project4 description");
            rest.setNotebookFolderEnabled(true);
            rest.setIcon("question");
            rest.setIconColor("blue");
            // NOTE: discard modification history dates from rest objects

            com.thinkbiganalytics.metadata.api.project.Project project = projectService.createProject(rest);

            project.getRoleMembership(ProjectAccessControl.ROLE_EDITOR).ifPresent(role -> role.addMember(TEST_USER1));
            project.getRoleMembership(ProjectAccessControl.ROLE_READER).ifPresent(role -> role.addMember(TEST_USER2));
            FileUtils.createPrivateDirectory(usersRepo.toPath().resolve("tester1").toFile());
            FileUtils.createPrivateDirectory(pathToTester1Project4.toFile());
            FileUtils.createPrivateDirectory(usersRepo.toPath().resolve("tester2").toFile());
            FileUtils.createPrivateDirectory(pathToTester2Project4.toFile());

            assertThat(masterRepo.exists()).isTrue();
            assertThat(usersRepo.exists()).isTrue();
            assertThat(usersRepo.toPath().resolve(JcrMetadataAccess.getActiveUser().getName())
                           .resolve(systemName).toFile().exists()).isTrue();
        }, JcrMetadataAccess.SERVICE);


    }

    @Test(dependsOnMethods = "testCreateProject")
    public void testUpdateProject() {
        Project rest = new Project();
        rest.setSystemName("Project4");
        rest.setProjectName("Project4");
        rest.setDescription("Project4 description");
        rest.setNotebookFolderEnabled(true);
        rest.setIcon("question");
        rest.setIconColor("blue");

        metadata.commit(() -> {
            projectService.updateProject(rest);
        }, MetadataAccess.SERVICE);

    }

    @Test(dependsOnMethods = "testCreateProject")
    public void testFileSystem() throws IOException, InterruptedException {
        /*metadata.commit(() -> {
            projectService.addUserRepoListener();
        }, MetadataAccess.SERVICE);*/

        Thread.sleep(2000);

        String projectName = "Project4";
        UserFsObj u1 = new UserFsObj.Builder().repoUrl(usersRepo.toPath())
            .userName(MetadataAccess.SERVICE.getName())
            .accessType(UserFsObj.ACCESS.WRITE)
            .projectName(projectName)
            .fsObj("file1")
            .build();

        File f1 = u1.getProjectPath().toFile();
        assertThat(f1.exists()).isTrue();

        FileUtils.writeFile(u1.absPath().toString(), "Yabba dabba doo");

        assertThat(u1.absPath().toFile().exists()).isTrue();

        /*
        File f = Paths.get(masterRepo.getAbsolutePath(), "stop").toFile();
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
        */

        //File expectedMasterLink = masterRepo.toPath().resolve("Project4").resolve("file1").toFile();
        File expectedMasterLink = u1.toMasterRepoFsObj().absPath().toFile();
        waitForFile(expectedMasterLink, true);


        // should have created a hard link in master repo,
        assertThat(expectedMasterLink.exists()).isTrue();

        // ... and every user.
        File expectedTester1File = pathToTester1Project4.resolve("file1").toFile();
        waitForFile(expectedTester1File, true);
        logger.debug("expectedTester1File={}", expectedTester1File);
        assertThat(expectedTester1File.exists()).isTrue();

        File expectedTester2File = pathToTester2Project4.resolve("file1").toFile();
        waitForFile(expectedTester2File, true);
        logger.debug("expectedTester2File={}", expectedTester2File);
        assertThat(expectedTester2File.exists()).isTrue();

        // Now delete the file
        f1.delete();

        // and wait for it to get spotted by the watcher
        waitForFile(expectedMasterLink, false);

        // should have deleted a hard link in master repo,
        assertThat(expectedMasterLink.exists()).isFalse();

        // ... and every user.  In this case no logged in users.
        waitForFile(expectedTester1File, false);
        assertThat(expectedTester1File.exists()).isFalse();
        waitForFile(expectedTester1File, false);
        assertThat(expectedTester2File.exists()).isFalse();

    }

    private boolean waitForFile( File file, boolean toExist) throws InterruptedException {
        return waitForFile(file, toExist, 150);
    }

    private boolean waitForFile( File file, boolean toExist, int maxChecks) throws InterruptedException {
            for (int numChecks = 0; numChecks < maxChecks; numChecks++) {
                Thread.sleep(100);
                if( toExist && file.exists()) {
                    return true;
                } else if( !toExist && !file.exists()) {
                    return true;
                }
            }
            return false;
    }

    @Test(dependsOnMethods = "testUpdateProject")
    public void testDeleteProject() {
        metadata.commit(() -> {
            assertThat(pathToMasterProject4).exists();
            assertThat(pathToServiceProject4).exists();
            assertThat(pathToTester1Project4).exists();
            assertThat(pathToTester2Project4).exists();

            Project prj = projectService.getProject("Project4");
            assertThat(prj).isNotNull();

            projectService.deleteProject(prj.getId());

            assertThatThrownBy(() -> projectService.getProject("Project4"))
                .isInstanceOf(MetadataRepositoryException.class);

            // check file system is correct
            assertThat(pathToMasterProject4).doesNotExist();
            assertThat(pathToServiceProject4).doesNotExist();
            assertThat(pathToTester1Project4).doesNotExist();
            assertThat(pathToTester2Project4).doesNotExist();
        });
    }


    @Configuration
    static class TestProjectServiceConfig {

        @Bean
        public ProjectService projectService() {
            return new ProjectServiceImpl();
        }

        @Bean
        public PropertySourcesPlaceholderConfigurer placeHolderConfigurer() {
            final Properties properties = new Properties();
            properties.setProperty("security.entity.access.controlled", "true");

            final PropertySourcesPlaceholderConfigurer configurer = new PropertySourcesPlaceholderConfigurer();
            configurer.setProperties(properties);
            return configurer;
        }

        @Bean
        public SecurityModelTransform securityModelTransform() {
            return Mockito.mock(SecurityModelTransform.class);
        }

        @Bean
        public UserService userService() {
            return Mockito.mock(UserService.class);
        }

    }

}
