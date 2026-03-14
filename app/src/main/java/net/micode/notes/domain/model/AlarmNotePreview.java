package net.micode.notes.domain.model;

public final class AlarmNotePreview {
    private final long noteId;
    private final String snippet;

    public AlarmNotePreview(long noteId, String snippet) {
        this.noteId = noteId;
        this.snippet = snippet;
    }

    public long getNoteId() {
        return noteId;
    }

    public String getSnippet() {
        return snippet;
    }
}