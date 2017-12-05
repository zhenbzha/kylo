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

import com.google.common.collect.ImmutableSet;
import com.thinkbiganalytics.projects.exceptions.NotebookIoException;
import com.thinkbiganalytics.projects.utils.tracking.TrackingUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Set;

import javax.inject.Inject;

public class NotebookRepoObjUtils {

    private static final Logger logger = LoggerFactory.getLogger(NotebookRepoObjUtils.class);

    private static final boolean isPosix =
        FileSystems.getDefault().supportedFileAttributeViews().contains("posix");

    @Inject
    private TrackingUtils trackingUtils;

    /**
     * @implNote Assumes failure to create the directory as required is a fatal un-recoverable error and throws RuntimeException()
     */
    public Path createPrivateDirectory(File directory) {
        Path theDir = trackingUtils.createDirectories(directory.toPath());

        // make sure this directory is usable by current OS user only
        // NOTE: if umask is 022 then dir will be 755
        theDir.toFile().setReadable(true, true);
        theDir.toFile().setWritable(true, true);
        theDir.toFile().setExecutable(true, true);

        if (isPosix) {
            Set<PosixFilePermission> set = ImmutableSet.of(
                PosixFilePermission.OWNER_EXECUTE,
                PosixFilePermission.OWNER_READ,
                PosixFilePermission.OWNER_WRITE);
            try {
                Files.setPosixFilePermissions(theDir, set);
            } catch (IOException e) {
                throw new NotebookIoException(String.format("Unable to set posix permissions on directory '%s'", directory));
            }
        } else {
            // TODO: implemention for ACL based system needed
        }
        return theDir;
    }

    public void replicateFileTreeWithHardLinks(Path source, Path destination, boolean failOnCollisions) {
        FileVisitor<Path> visitor = new WalkTreeAndCreateLinks(this, source, destination);

        try {
            Files.walkFileTree(source, ImmutableSet.of(FileVisitOption.FOLLOW_LINKS), Integer.MAX_VALUE, visitor);
        } catch (IOException e) {
            logger.error("IOException occurred.  Replicating source path '%s' to destination path '%s' results are dubious.", e);
        }
    }


    /**
     * Will either replicate the file tree, if source is a directory, or create a hard link if a file.
     *
     * @param source The source file or directory
     * @param dest   The destination.  will become a new directory or a hard link to a file.
     */
    public void linkOrReplicate(File source, Path dest) {
        if (source.isDirectory()) {
            this.replicateFileTreeWithHardLinks(source.toPath(), dest, false);
        } else if (source.isFile()) {
            // make a hard link
            try {
                // TODO: distinguish missing parents?
                // TODO: ... Validate.isTrue(new File(dest.toFile().getParent()).exists(), String.format("Parent path of '%s' does not exist", dest));
                this.createLink(dest, source.toPath());
            } catch (IOException e) {
                // Note: there are situations where the link may have already been created.. that's fine.  Let's continue
                logger.error("Unable to create link '{}' on file '{}'", dest, source);
            }
        }
    }

    public void createLink(Path destFsObj, Path srcFsObj) throws IOException {
        trackingUtils.createLink(destFsObj, srcFsObj);
    }

    public Path ensurePath(File path) {
        if (path.exists()) {
            // TODO: need to ensure this path is exactly rwx for owner only
            if (!(path.isDirectory() &&
                  path.canRead() &&
                  path.canWrite() &&
                  path.canExecute())) {
                throw new RuntimeException(String.format("Path='%s' is an unsuitable as "
                                                         + " location as it cannot be managed by the user='%s",
                                                         path,
                                                         org.apache.commons.lang.SystemUtils.USER_NAME));
            } // end if
            return path.toPath();
        } else {
            return createPrivateDirectory(path);
        }
    }

}
