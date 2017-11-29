package sandbox.watchers;

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

import java.io.File;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;


/**
 * @implNote https://stackoverflow.com/a/27737069/154461
 */
public class FileWatcherThread extends Thread {

    private final File file;
    private AtomicBoolean stop = new AtomicBoolean(false);

    private IFileListener listener;

    public FileWatcherThread(File file) {
        this.file = file;
    }

    public boolean isStopped() {
        return stop.get();
    }

    public void stopThread() {
        stop.set(true);
    }

    public File getFile() {
        return file;
    }

    public void fileCreated(File file) {
        if (listener != null) {
            listener.fileCreated(file);
        }
    }

    public void fileModified(File file) {
        if (listener != null) {
            listener.fileModified(file);
        }
    }

    public void fileDeleted(File file) {
        if (listener != null) {
            listener.fileDeleted(file);
        }
    }

    @Override
    public void run() {
        try (WatchService watcher = FileSystems.getDefault().newWatchService()) {
            Path path = file.toPath().getParent();
            path.register(watcher, StandardWatchEventKinds.ENTRY_CREATE,
                          StandardWatchEventKinds.ENTRY_MODIFY,
                          StandardWatchEventKinds.ENTRY_DELETE);
            while (!isStopped()) {
                WatchKey key;
                try {
                    key = watcher.poll(25, TimeUnit.MILLISECONDS);
                } catch (InterruptedException e) {
                    return;
                }
                if (key == null) {
                    Thread.yield();
                    continue;
                }

                for (WatchEvent<?> event : key.pollEvents()) {
                    WatchEvent.Kind<?> kind = event.kind();

                    @SuppressWarnings("unchecked")
                    WatchEvent<Path> ev = (WatchEvent<Path>) event;
                    Path filename = ev.context();

                    if (filename.toString().equals(file.getName())) {
                        if (kind == StandardWatchEventKinds.ENTRY_MODIFY) {
                            fileCreated(file);
                        } else if (kind == StandardWatchEventKinds.ENTRY_CREATE) {
                            fileModified(file);
                        } else if (kind == StandardWatchEventKinds.ENTRY_DELETE) {
                            fileDeleted(file);
                        } else {
                            Thread.yield();
                            continue;
                        }
                    }

                    boolean valid = key.reset();
                    if (!valid) {
                        break;
                    }
                }
                Thread.yield();
            }
        } catch (Throwable e) {
            // Log or rethrow the error
        }
    }

    public void registerListener(IFileListener listener) {
        this.listener = listener;
    }

    interface IFileListener {
        void fileModified(File file);

        void fileDeleted(File file);

        void fileCreated(File file);
    }

    static class FileListener implements IFileListener {

        @Override
        public void fileCreated(File file) {
            print("modified " + file.getAbsolutePath());
        }

        @Override
        public void fileModified(File file) {
            print("modified " + file.getAbsolutePath());
        }

        @Override
        public void fileDeleted(File file) {
            print("modified " + file.getAbsolutePath());
        }

        void print(String msg) {
            System.err.println(msg);
        }
    }

    public static void main(String... args) {
        FileWatcherThread fw = new FileWatcherThread(new File("/tmp/watchme"));
        fw.registerListener(new FileListener());
        fw.start();
    }

}


