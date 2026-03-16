package net.micode.notes.ui;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public interface BackgroundTaskRunner {
    static BackgroundTaskRunner fromExecutor(Executor executor) {
        return new ExecutorBackgroundTaskRunner(executor);
    }

    static BackgroundTaskRunner immediate() {
        return new BackgroundTaskRunner() {
            @Override
            public void execute(Runnable task) {
                task.run();
            }

            @Override
            public void shutdown() {
            }
        };
    }

    static BackgroundTaskRunner singleThreaded() {
        return fromExecutor(Executors.newSingleThreadExecutor());
    }

    void execute(Runnable task);

    void shutdown();
}