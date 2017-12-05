package com.thinkbiganalytics.projects.utils;

import org.apache.commons.lang3.SystemUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;

import javax.annotation.Nonnull;

public class WalkTreeAndCreateLinks implements FileVisitor<Path> {

    private static final Logger logger = LoggerFactory.getLogger(WalkTreeAndCreateLinks.class);

    private boolean failOnCollisions;
    private Path srcPath;
    private Path destPath;
    private NotebookRepoObjUtils notebookRepoObjUtils;

    WalkTreeAndCreateLinks(NotebookRepoObjUtils notebookRepoObjUtils, Path srcPath, Path destPath) {
        this.notebookRepoObjUtils = notebookRepoObjUtils;
        this.srcPath = srcPath;
        this.destPath = destPath;
        this.failOnCollisions = true;
    }

    @Override
    public FileVisitResult preVisitDirectory(@Nonnull Path dir,
                                             BasicFileAttributes attrs) {
        Path pathRelative = srcPath.relativize(dir);
        Path destSubdir = destPath.resolve(pathRelative);

        if (!destSubdir.toFile().exists()) {
            notebookRepoObjUtils.createPrivateDirectory(destSubdir.toFile());
        }

        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFile(@Nonnull Path file,
                                     @Nonnull BasicFileAttributes attr) {
        Path pathRelative = srcPath.relativize(file);
        Path destFile = destPath.resolve(pathRelative);

        if (attr.isSymbolicLink()) {
            // TODO: how to deal with symbolic links?
            logger.warn("Symbolic link found: {} ", file);
        } else if (attr.isRegularFile()) {
            logger.debug("Creating hard link from:'{}' to: '{}'", destFile, file);

            try {
                // TODO: track link creation
                notebookRepoObjUtils.createLink(destFile, file);
            } catch (IOException e) {
                logger.error("Unable to create the necessary hard link from: '{}' to: '{}' for unknown reasons.", destFile, file);
                logger.error("Exception: ", e);
            } catch (UnsupportedOperationException x) {
                logger.error("Hard links are not supported by operating system name:'{}' arch:'{}' version:'{}'",
                             SystemUtils.OS_NAME, SystemUtils.OS_ARCH, SystemUtils.OS_VERSION);
            }
        } else {
            // TODO: what are unknown types?
            logger.warn("Other file type encountered: %s ", file);
        }
        logger.debug("({} bytes)", attr.size());
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult postVisitDirectory(@Nonnull Path dir,
                                              IOException exc) {
        if (exc == null) {
            logger.debug("Directory processed: {}", dir);
        } else {
            logger.error("IOException whilst walking files in directory '{}'", dir);
        }
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFileFailed(@Nonnull Path file,
                                           @Nonnull IOException exc) {
        logger.error("Unable to visit file: '{}'", exc);
        return FileVisitResult.CONTINUE;
    }
}