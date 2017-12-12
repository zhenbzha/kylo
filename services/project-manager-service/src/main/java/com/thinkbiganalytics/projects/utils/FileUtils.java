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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Set;

public class FileUtils {

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

}
