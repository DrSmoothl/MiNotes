package net.micode.notes.domain.model;

public final class ExportedTextFile {
    public enum ExportState {
        STORAGE_UNAVAILABLE,
        SUCCESS,
        FAILURE
    }

    private final ExportState state;
    private final String fileName;
    private final String directory;

    public ExportedTextFile(ExportState state, String fileName, String directory) {
        this.state = state;
        this.fileName = fileName == null ? "" : fileName;
        this.directory = directory == null ? "" : directory;
    }

    public ExportState getState() {
        return state;
    }

    public String getFileName() {
        return fileName;
    }

    public String getDirectory() {
        return directory;
    }
}