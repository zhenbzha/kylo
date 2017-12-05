package com.thinkbiganalytics.projects.utils.tracking;

import com.google.common.cache.Cache;
import com.thinkbiganalytics.projects.exceptions.NotebookIoException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.util.Collection;

import javax.inject.Inject;

public class TrackingUtils {
    private static final Logger logger = LoggerFactory.getLogger(TrackingUtils.class);

    @Inject
    Cache<Path, Boolean> trackedPaths;

    /**
     * Creates a directory by creating all nonexistent parent directories first.
     * Unlike the {@link #createDirectory createDirectory} method, an exception
     * is not thrown if the directory could not be created because it already
     * exists.
     *
     * <p> The {@code attrs} parameter is optional {@link FileAttribute
     * file-attributes} to set atomically when creating the nonexistent
     * directories. Each file attribute is identified by its {@link
     * FileAttribute#name name}. If more than one attribute of the same name is
     * included in the array then all but the last occurrence is ignored.
     *
     * <p> If this method fails, then it may do so after creating some, but not
     * all, of the parent directories.
     *
     * @param dir   the directory to create
     * @param attrs an optional list of file attributes to set atomically when
     *              creating the directory
     * @return the directory
     * @throws UnsupportedOperationException if the array contains an attribute that cannot be set atomically
     *                                       when creating the directory
     * @throws FileAlreadyExistsException    if {@code dir} exists but is not a directory <i>(optional specific
     *                                       exception)</i>
     * @throws IOException                   if an I/O error occurs
     * @throws SecurityException             in the case of the default provider, and a security manager is
     *                                       installed, the {@link SecurityManager#checkWrite(String) checkWrite}
     *                                       method is invoked prior to attempting to create a directory and
     *                                       its {@link SecurityManager#checkRead(String) checkRead} is
     *                                       invoked for each parent directory that is checked. If {@code
     *                                       dir} is not an absolute path then its {@link Path#toAbsolutePath
     *                                       toAbsolutePath} may need to be invoked to get its absolute path.
     *                                       This may invoke the security manager's {@link
     *                                       SecurityManager#checkPropertyAccess(String) checkPropertyAccess}
     *                                       method to check access to the system property {@code user.dir}
     */
    private Path createTrackedDirectories(Path dir, FileAttribute<?>... attrs) throws IOException {
        // attempt to create the directory
        try {
            createAndCheckIsDirectory(dir, attrs);
            return dir;
        } catch (FileAlreadyExistsException x) {
            // file exists and is not a directory
            throw x;
        } catch (IOException x) {
            // parent may not exist or other reason
        }
        SecurityException se = null;
        try {
            dir = dir.toAbsolutePath();
        } catch (SecurityException x) {
            // don't have permission to get absolute path
            se = x;
        }
        // find a decendent that exists
        Path parent = dir.getParent();
        while (parent != null) {
            try {
                parent.getFileSystem().provider().checkAccess(parent);
                break;
            } catch (NoSuchFileException x) {
                // does not exist
            }
            parent = parent.getParent();
        }
        if (parent == null) {
            // unable to find existing parent
            if (se == null) {
                throw new FileSystemException(dir.toString(), null,
                                              "Unable to determine if root directory exists");
            } else {
                throw se;
            }
        }

        // create directories
        Path child = parent;
        for (Path name : parent.relativize(dir)) {
            child = child.resolve(name);
            createAndCheckIsDirectory(child, attrs);
        }
        return dir;
    }

    /**
     * Used by createDirectories to attempt to create a directory. A no-op
     * if the directory already exists.
     */
    private void createAndCheckIsDirectory(Path dir,
                                                  FileAttribute<?>... attrs)
        throws IOException {
        try {
            Files.createDirectory(dir, attrs);
            trackedPaths.put(dir, true);
        } catch (FileAlreadyExistsException x) {
            if (!Files.isDirectory(dir, LinkOption.NOFOLLOW_LINKS)) {
                throw x;
            }
        }
    }

    /**
     *
     * @param dir
     * @return
     *
     * @see org.apache.commons.io.FileUtils.createDirectories
     */
    public Path createDirectories(Path dir) {
        try {
            return createTrackedDirectories(dir);
        } catch(IOException e ) {
            // log and transform error
            logger.error("Fatal error trying to create directory", e);
            throw new NotebookIoException(e);
        }
    }


    /**
     * Lists files in a directory, asserting that the supplied directory satisfies exists and is a directory
     *
     * @param directory The directory to list
     * @return The files in the directory, never null.
     * @throws IOException if an I/O error occurs
     * @see org.apache.commons.io.FileUtils.verifiedListFiles
     */
    private static File[] verifiedListFiles(File directory) throws IOException {
        if (!directory.exists()) {
            final String message = directory + " does not exist";
            throw new IllegalArgumentException(message);
        }

        if (!directory.isDirectory()) {
            final String message = directory + " is not a directory";
            throw new IllegalArgumentException(message);
        }

        final File[] files = directory.listFiles();
        if (files == null) {  // null if security restricted
            throw new IOException("Failed to list contents of " + directory);
        }
        return files;
    }

    /**
     * Cleans a directory without deleting it.
     *
     * @param directory directory to clean
     * @throws IOException              in case cleaning is unsuccessful
     * @throws IllegalArgumentException if {@code directory} does not exist or is not a directory
     * @see org.apache.commons.io.FileUtils.cleanDirectory
     */
    public void cleanDirectory(final File directory) throws IOException {
        final File[] files = verifiedListFiles(directory);

        IOException exception = null;
        for (final File file : files) {
            try {
                forceDelete(file);
            } catch (final IOException ioe) {
                exception = ioe;
            }
        }

        if (null != exception) {
            throw exception;
        }
    }

    /**
     * Deletes a file. If file is a directory, delete it and all sub-directories.
     * <p>
     * The difference between File.delete() and this method are:
     * <ul>
     * <li>A directory to be deleted does not have to be empty.</li>
     * <li>You get exceptions when a file or directory cannot be deleted.
     * (java.io.File methods returns a boolean)</li>
     * </ul>
     *
     * @param file file or directory to delete, must not be {@code null}
     * @throws NullPointerException  if the directory is {@code null}
     * @throws FileNotFoundException if the file was not found
     * @throws IOException           in case deletion is unsuccessful
     * @see org.apache.commons.io.FileUtils.forceDelete
     */
    public void forceDelete(final File file) throws IOException {
        if (file.isDirectory()) {
            deleteDirectory(file);
        } else {
            if (file.delete()) {
                trackedPaths.put(file.toPath(), true);
            } else {
                if (! file.exists() ) {
                    throw new FileNotFoundException("File does not exist: " + file);
                }
                final String message =
                    "Unable to delete file: " + file;
                throw new IOException(message);
            }
        }
    }

    //-----------------------------------------------------------------------
    /**
     * Deletes a directory recursively.
     *
     * @param directory directory to delete
     * @throws IOException              in case deletion is unsuccessful
     * @throws IllegalArgumentException if {@code directory} does not exist or is not a directory
     * @see org.apache.commons.io.FileUtils.deleteDirectory
     */
    public void deleteDirectory(final File directory) throws IOException {
        if (!directory.exists()) {
            return;
        }

        if (!org.apache.commons.io.FileUtils.isSymlink(directory)) {
            cleanDirectory(directory);
        }

        if (!directory.delete()) {
            final String message =
                "Unable to delete directory " + directory + ".";
            throw new IOException(message);
        }

        trackedPaths.put(directory.toPath(), true);
    }

    public void createLink(Path destFsObj, Path srcFsObj) throws IOException {
        // TODO: check impl only creates one object needing tracking
        Files.createLink(destFsObj, srcFsObj);
        trackedPaths.put(destFsObj, true);
    }

}

