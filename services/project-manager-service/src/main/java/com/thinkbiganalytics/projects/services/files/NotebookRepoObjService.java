package com.thinkbiganalytics.projects.services.files;

import com.thinkbiganalytics.projects.utils.FileUtils;
import com.thinkbiganalytics.projects.utils.NotebookRepoObjUtils;
import com.thinkbiganalytics.projects.utils.tracking.TrackingUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import javax.inject.Inject;

public class NotebookRepoObjService {
    private static final Logger logger = LoggerFactory.getLogger(FileUtils.class);

    @Inject
    private TrackingUtils trackingUtils;

    @Inject
    private NotebookRepoObjUtils notebookRepoObjUtils;

    // TODO: there may be more efficient ways of doing this..
    public void ensureProject(UserFsObj userFsObj) {
        notebookRepoObjUtils.ensurePath(userFsObj.getRepoUrl().toFile());
        notebookRepoObjUtils.ensurePath(userFsObj.getRepoWithUser().toFile());
        notebookRepoObjUtils.ensurePath(userFsObj.getRepoWithProject().toFile());
    }

    public Path deleteProject(UserFsObj userFsObj) {
        File dir = userFsObj.getProjectPath().toFile();
        try {
            trackingUtils.deleteDirectory(dir);
        } catch (IOException e) {
            logger.error("Unable to remove directory '{}' and all it's contents", dir);
        }

        return userFsObj.getProjectPath();
    }

    public void ensure(MasterRepoFsObj masterRepoFsObj) {
        notebookRepoObjUtils.ensurePath(masterRepoFsObj.getRepoUrl().toFile());
        notebookRepoObjUtils.ensurePath(masterRepoFsObj.getRepoWithProjectPath().toFile());
    }
}
