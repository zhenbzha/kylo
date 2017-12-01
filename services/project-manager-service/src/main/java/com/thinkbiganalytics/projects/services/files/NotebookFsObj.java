package com.thinkbiganalytics.projects.services.files;

import org.apache.commons.lang.Validate;

import java.nio.file.Path;

/**
 * NOTE:
 * 1) repoUrl must exist on the file system
 * 2) child class will be a validated file, and can perform operations
 */
public class NotebookFsObj {

    protected Path repoUrl;
    protected String userName;
    protected String accessType;
    protected String projectName;
    protected String fsObj;

    protected NotebookFsObj() {
    }

    protected NotebookFsObj(Builder builder) {
        this.repoUrl = builder.repoUrl;
        this.userName = builder.userName;
        this.accessType = builder.accessType;
        this.projectName = builder.projectName;
        this.fsObj = builder.fsObj;
    }

    public Path getRepoUrl() {
        return repoUrl;
    }

    public String getUserName() {
        return userName;
    }


    public String getAccessType() {
        return accessType;
    }

    public String getProjectName() {
        return projectName;
    }

    public Path absPath() {
        return repoUrl.resolve(userName).resolve(accessType)
            .resolve(projectName).resolve(fsObj);
    }

    public Path getProjectPath() {
        return repoUrl.resolve(userName).resolve(accessType);
    }


    public MasterRepoFsObj toMasterRepoFsObj() {
        return new MasterRepoFsObj.Builder().repoUrl(repoUrl)
            .project(projectName).fsObj(fsObj).build();
    }

    @Override
    public String toString() {
        return absPath().toString();
    }

    public static class Builder {

        private Path repoUrl;
        private String userName;
        private String accessType;
        private String projectName;
        private String fsObj;

        public Builder repoUrl(Path repoUrl) {
            Validate.isTrue(repoUrl.toFile().exists(), "Cannot construct a NotebookFsObj with non-existent repoUrl");
            Validate.isTrue(repoUrl.toFile().canRead(), "Cannot construct a NotebookFsObj with a repoUrl that is not readable to Kylo");
            Validate.isTrue(repoUrl.toFile().canWrite(), "Cannot construct a NotebookFsObj with a repoUrl that is not readable to Kylo");
            Validate.isTrue(repoUrl.toFile().canExecute(), "Cannot construct a NotebookFsObj with a repoUrl hat is not readable to Kylo");

            this.repoUrl = repoUrl;
            return this;
        }

        public Builder userName(String userName) {
            this.userName = userName;
            return this;
        }

        public Builder accessType(String accessType) {
            this.accessType = accessType;
            return this;
        }

        public Builder project(String project) {
            this.projectName = project;
            return this;
        }

        public Builder fsObj(String fsObj) {
            this.fsObj = fsObj;
            return this;
        }

        public NotebookFsObj build() {
            return new NotebookFsObj(this);
        }
    }
}
