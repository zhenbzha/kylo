package com.thinkbiganalytics.projects.services;

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

import com.google.common.cache.Cache;
import com.google.common.collect.Sets;
import com.sun.nio.file.SensitivityWatchEventModifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.OVERFLOW;

public class RecursiWatcherService {

    private static final Logger logger = LoggerFactory.getLogger(RecursiWatcherService.class);

    @Value("${notebooks.users.repository}")
    private File rootFolder;

    @Inject
    private Cache<Path,Boolean> trackedPaths;

    private WatchService watcher;

    private ExecutorService executor;

    private Set<IFileListener> listeners = Sets.newConcurrentHashSet();

    @PostConstruct
    public void init() throws IOException {
        logger.info("Watching root folder: {}", rootFolder);
        watcher = FileSystems.getDefault().newWatchService();
        executor = Executors.newSingleThreadExecutor();
        startRecursiveWatcher();
    }

    @PreDestroy
    public void cleanup() {
        try {
            watcher.close();
        } catch (IOException e) {
            logger.error("Error closing watcher service", e);
        }
        executor.shutdown();
    }

    private void startRecursiveWatcher() throws IOException {
        logger.info("Starting Recursive Watcher");

        final Map<WatchKey, Path> keys = new HashMap<>();

        Consumer<Path> register = p -> {
            if (!p.toFile().exists() || !p.toFile().isDirectory()) {
                throw new RuntimeException("folder " + p + " does not exist or is not a directory");
            }
            try {
                Files.walkFileTree(p, new SimpleFileVisitor<Path>() {
                    @Override
                    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                        logger.info("registering " + dir + " in watcher service");
                        WatchKey watchKey = dir.register(watcher, new WatchEvent.Kind[]{ENTRY_CREATE, ENTRY_DELETE}, SensitivityWatchEventModifier.HIGH);
                        keys.put(watchKey, dir);
                        return FileVisitResult.CONTINUE;
                    }
                });
            } catch (IOException e) {
                throw new RuntimeException("Error registering path " + p);
            }
        };

        register.accept(rootFolder.toPath());

        executor.submit(() -> {
            while (true) {
                logger.info("Loop waiting for keys..");

                final WatchKey key;
                try {
                    logger.info("take a key.");
                    key = watcher.take(); // wait for a key to be available
                } catch (InterruptedException ex) {
                    return;
                }

                logger.info("Key received.");
                final Path dir = keys.get(key);
                if (dir == null) {
                    System.err.println("WatchKey " + key + " not recognized!");
                    continue;
                }

                key.pollEvents().stream()
                    .filter(e -> (e.kind() != OVERFLOW))
                    .forEach(e -> {
                        WatchEvent<Path> event = (WatchEvent<Path>) e;
                        final Path p = event.context();
                        final Path absPath = dir.resolve(p);
                        if (absPath.toFile().isDirectory()) {
                            // TODO: hmm.. might need to delete watch key for deleted directories...
                            register.accept(absPath);
                            // return;  Don't return so that we will process the directory replication
                        }
                        final File f = absPath.toFile();
                        if (e.kind() == ENTRY_CREATE) {
                            fileCreated(f);
                        } else if (e.kind() == ENTRY_DELETE) {
                            fileDeleted(f);
                        } else {
                            throw new UnsupportedOperationException("Don't know what to do with MODIFY");
                        }
                    });

                logger.info("Resetting key.");
                boolean valid = key.reset(); // IMPORTANT: The key must be reset after processed
                if (!valid) {
                    logger.warn("Key '{}' no longer valid.");
                    //break;  ORIGINAL CODE would break here, but there are situations where it is ok that a key is not valid.. just keep going
                }
            }
        });
    }

    public File getRootFolder() {
        return rootFolder;
    }

    public void registerListener(IFileListener listener) {
        this.listeners.add(listener);
    }

    public void fileCreated(File file) {
        logger.debug("fileCreated('{}') called", file);
        String userPerformingFileEvent = rootFolder.toPath().relativize(file.toPath()).getName(0).toString();
        for (IFileListener listener : listeners) {
            try {
                listener.fileCreated(file);
            } catch (Exception e) {
                // Exceptions will kill the executor so just log and continue
                logger.error("The listerner for user '" + userPerformingFileEvent + "'"
                             + " threw an Exception. It was caught and will be swallowed so as not to kill the recursiWatcher's executor.", e);
            }
        }
    } // end method

    public void fileDeleted(File file) {
        logger.debug("fileDeleted('{}') called", file);

        String userPerformingFileEvent = rootFolder.toPath().relativize(file.toPath()).getName(0).toString();
        for (IFileListener listener : listeners) {
            try {
                listener.fileDeleted(file);
            } catch (Exception e) {
                // Exceptions will kill the executor so just log and continue
                logger.error("The listerner for user '" + userPerformingFileEvent + "'"
                             + " threw an Exception. It was caught and will be swallowed so as not to kill the recursiWatcher's executor.", e);
            }
        }
    } // end method

    public interface IFileListener {

        void fileModified(File file);

        void fileDeleted(File file);

        void fileCreated(File file);
    }

    static class PrintListener implements IFileListener {

        @Override
        public void fileCreated(File file) {
            print("created " + file.getAbsolutePath());
        }

        @Override
        public void fileModified(File file) {
            print("modified " + file.getAbsolutePath());
        }

        @Override
        public void fileDeleted(File file) {
            print("deleted " + file.getAbsolutePath());
        }

        void print(String msg) {
            System.err.println(msg);
        }
    }
}
