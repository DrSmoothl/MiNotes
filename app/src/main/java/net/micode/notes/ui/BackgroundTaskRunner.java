package net.micode.notes.ui;

public interface BackgroundTaskRunner {
    void execute(Runnable task);

    void shutdown();
}