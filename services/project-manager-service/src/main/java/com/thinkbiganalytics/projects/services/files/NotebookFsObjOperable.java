package com.thinkbiganalytics.projects.services.files;

import org.apache.commons.lang.Validate;

import java.io.File;
import java.nio.file.Path;

/**
 * NOTE:
 * 1) repoUrl must exist on the file system
 * 2) child class will be a validated file, and can perform operations
 */
public class NotebookFsObjOperable extends NotebookFsObj {

    public NotebookFsObjOperable(Builder builder) {
        super(builder);
    }

    public NotebookFsObjOperable(NotebookFsObj notebookFsObj) {
        this.repoUrl = notebookFsObj.repoUrl;
        this.userName = notebookFsObj.userName;
        this.accessType = notebookFsObj.accessType;
        this.projectName = notebookFsObj.projectName;
        this.fsObj = notebookFsObj.fsObj;
    }


    public void validate() {
        File validatedFile = absPath().toFile();
        Validate.isTrue( validatedFile.exists(),
                         String.format( "Cannot validate the path '%s'", this) );
    }

    public Path ensureProject() {
        Path path = this.absPath();

        // TODO: place all the ops for creating all paths except fsObj;
        throw new UnsupportedOperationException();
    }

}
