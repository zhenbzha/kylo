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

import org.apache.commons.lang3.SystemUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Set;

import javax.annotation.Nonnull;

public class FileUtils {

    private static final Logger logger = LoggerFactory.getLogger(FileUtils.class);

    private static final boolean isPosix =
        FileSystems.getDefault().supportedFileAttributeViews().contains("posix");

    /**
     * @implNote Assumes failure to create the directory as required is a fatal un-recoverable error and throws RuntimeException()
     */
    public static Path createPrivateDirectory(File directory) {
        try {
            Path theDir = Files.createDirectories(directory.toPath());

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
                Files.setPosixFilePermissions(theDir, set);
            } else {
                // TODO: implemention for ACL based system needed
            }
            return theDir;
        } catch (IOException e) {
            throw new RuntimeException(String.format("Unable to create directory '%s'", directory));
        }
    }


    public static void replicateFileTreeWithHardLinks(Path source, Path destination, boolean failOnCollisions) {
        FileVisitor<Path> visitor = new WalkTreeAndCreateLinks(source, destination, failOnCollisions);

        try {
            Files.walkFileTree(source, ImmutableSet.of(FileVisitOption.FOLLOW_LINKS), Integer.MAX_VALUE, visitor);
        } catch (IOException e) {
            logger.error("IOException occurred.  Replicating source path '%s' to destination path '%s' results are dubious.", e);
        }
    }

    public static String readFile(String path, Charset encoding) throws IOException {
        byte[] encoded = Files.readAllBytes(Paths.get(path));
        return new String(encoded, encoding);
    }

    public static String readFile(String path) throws IOException {
        byte[] encoded = Files.readAllBytes(Paths.get(path));
        return new String(encoded, StandardCharsets.UTF_8);
    }

    public static void writeFile(String path, String content) throws IOException {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(path))) {
            bw.write(content);
        }
    }

    /**
     * Will either replicate the file tree, if source is a directory, or create a hard link if a file.
     *
     * @param source    The source file or directory
     * @param dest      The destination.  will become a new directory or a hard link to a file.
     */
    public static void linkOrReplicate(File source, Path dest) {
        if( source.isDirectory() ) {
            FileUtils.replicateFileTreeWithHardLinks(source.toPath(), dest, false);
        } else if( source.isFile() ) {
            // make a hard link
            try {
                // TODO: distinguish missing parents?
                // TODO: ... Validate.isTrue(new File(dest.toFile().getParent()).exists(), String.format("Parent path of '%s' does not exist", dest));

                Files.createLink(dest, source.toPath());
            } catch (IOException e) {
                // Note: there are situations where the link may have already been created.. that's fine.  Let's continue
                logger.error("Unable to create link '{}' on file '{}'", dest, source);
            }
        }
    }


    static class WalkTreeAndCreateLinks implements FileVisitor<Path> {

        private static final Logger logger = LoggerFactory.getLogger(WalkTreeAndCreateLinks.class);

        private boolean failOnCollisions;
        private Path srcPath;
        private Path destPath;

        WalkTreeAndCreateLinks(Path srcPath, Path destPath, boolean failOnCollisions) {
            this.srcPath = srcPath;
            this.destPath = destPath;
            this.failOnCollisions = failOnCollisions;
        }

        @Override
        public FileVisitResult preVisitDirectory(@Nonnull Path dir,
                                                 BasicFileAttributes attrs) {
            Path pathRelative = srcPath.relativize(dir);
            Path destSubdir = destPath.resolve(pathRelative);

            if (!destSubdir.toFile().exists()) {
                createPrivateDirectory(destSubdir.toFile());
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
                    Files.createLink(destFile, file);
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
}
