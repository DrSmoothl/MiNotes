package net.micode.notes.ui;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;

public final class ExecutorBackgroundTaskRunner implements BackgroundTaskRunner {
    private final Executor executor;

    public ExecutorBackgroundTaskRunner(Executor executor) {
        this.executor = executor;
    }

    @Override
    public void execute(Runnable task) {
        executor.execute(task);
    }

    @Override
    public void shutdown() {
        if (executor instanceof ExecutorService) {
            ((ExecutorService) executor).shutdown();
        }
    }
}