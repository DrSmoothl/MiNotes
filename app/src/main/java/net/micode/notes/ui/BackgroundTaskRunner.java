package net.micode.notes.ui;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public interface BackgroundTaskRunner {
    static BackgroundTaskRunner fromExecutor(Executor executor) {
        return new ExecutorBackgroundTaskRunner(executor);
    }

    static BackgroundTaskRunner singleThreaded() {
        return fromExecutor(Executors.newSingleThreadExecutor());
    }

    void execute(Runnable task);

    void shutdown();
}