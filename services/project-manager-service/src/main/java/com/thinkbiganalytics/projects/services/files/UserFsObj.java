package com.thinkbiganalytics.projects.services.files;

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

import org.apache.commons.lang.Validate;

import java.nio.file.Path;

/**
 * NOTE:
 * 1) repoUrl must exist on the file system
 * 2) child class will be a validated file, and can perform operations
 */
public class UserFsObj implements NotebookFsObj {

    protected final Path repoUrl;
    protected final String userName;
    protected final String accessType;
    protected final String projectName;
    protected final String fsObj;

    protected UserFsObj(Builder builder) {
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

    public String getProjectName() {
        return projectName;
    }

    public String getAccessType() {
        return accessType;
    }

    public Path getRepoWithUser() {
        return repoUrl.resolve(userName);
    }

    public Path getRepoWithProject() {
        return getRepoWithUser().resolve(projectName);
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
            .projectName(projectName).fsObj(fsObj).build();
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
            // TODO: remove this..
            Validate.isTrue(repoUrl.toFile().exists(), "Cannot construct a UserFsObj with non-existent repoUrl");
            Validate.isTrue(repoUrl.toFile().canRead(), "Cannot construct a UserFsObj with a repoUrl that is not readable to Kylo");
            Validate.isTrue(repoUrl.toFile().canWrite(), "Cannot construct a UserFsObj with a repoUrl that is not readable to Kylo");
            Validate.isTrue(repoUrl.toFile().canExecute(), "Cannot construct a UserFsObj with a repoUrl hat is not readable to Kylo");

            this.repoUrl = repoUrl;
            return this;
        }

        public Builder userName(String userName) {
            this.userName = userName;
            return this;
        }

        public Builder accessType(ACCESS accessType) {
            this.accessType = accessType == ACCESS.WRITE ? "readWrite":"readOnly";
            return this;
        }

        public Builder projectName(String projectName) {
            this.projectName = projectName;
            return this;
        }

        public Builder fsObj(String fsObj) {
            this.fsObj = fsObj;
            return this;
        }

        public UserFsObj build() {
            return new UserFsObj(this);
        }
    }

    public enum ACCESS { READ_ONLY, WRITE};
}
