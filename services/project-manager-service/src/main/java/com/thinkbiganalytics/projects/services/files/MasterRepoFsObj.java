package com.thinkbiganalytics.projects.services.files;

import org.apache.commons.lang.Validate;

import java.nio.file.Path;

/**
 * NOTE:
 * 1) repoUrl must exist on the file system
 * 2) child class will be a validated file, and can perform operations
 */
public class MasterRepoFsObj {

    protected Path repoUrl;
    protected String project;
    protected String fsObj;

    protected MasterRepoFsObj() {
    }

    protected MasterRepoFsObj(Builder builder) {
        this.repoUrl = builder.repoUrl;
        this.project = builder.project;
        this.fsObj = builder.fsObj;
    }

    public Path getRepoUrl() {
        return repoUrl;
    }

    public String getProject() {
        return project;
    }

    public Path absPath() {
        Path absPath = repoUrl.resolve(project).resolve(fsObj);

        return absPath;
    }

    public NotebookFsObj toNotebookFsObj(String userName, boolean readOnly) {
        return new NotebookFsObj.Builder().repoUrl(this.repoUrl)
            .userName(userName).accessType(readOnly ? "read" : "write")
            .project(this.project).fsObj(this.fsObj).build();
    }

    @Override
    public String toString() {
        return absPath().toString();
    }

    public static class Builder {

        private Path repoUrl;
        private String project;
        private String fsObj;

        public Builder repoUrl(Path repoUrl) {
            Validate.isTrue(repoUrl.toFile().exists(), "Cannot construct a MasterRepoFsObj with non-existent repoUrl");
            Validate.isTrue(repoUrl.toFile().canRead(), "Cannot construct a MasterRepoFsObj with a repoUrl that is not readable to Kylo");
            Validate.isTrue(repoUrl.toFile().canWrite(), "Cannot construct a MasterRepoFsObj with a repoUrl that is not readable to Kylo");
            Validate.isTrue(repoUrl.toFile().canExecute(), "Cannot construct a MasterRepoFsObj with a repoUrl hat is not readable to Kylo");

            this.repoUrl = repoUrl;
            return this;
        }

        public Builder project(String project) {
            this.project = project;
            return this;
        }

        public Builder fsObj(String fsObj) {
            this.fsObj = fsObj;
            return this;
        }

        public MasterRepoFsObj build() {
            return new MasterRepoFsObj(this);
        }
    }
}
