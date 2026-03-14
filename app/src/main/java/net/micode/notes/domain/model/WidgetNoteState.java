package net.micode.notes.domain.model;

public final class WidgetNoteState {
    private final long noteId;
    private final int backgroundColorId;
    private final String snippet;

    public WidgetNoteState(long noteId, int backgroundColorId, String snippet) {
        this.noteId = noteId;
        this.backgroundColorId = backgroundColorId;
        this.snippet = snippet == null ? "" : snippet;
    }

    public long getNoteId() {
        return noteId;
    }

    public int getBackgroundColorId() {
        return backgroundColorId;
    }

    public String getSnippet() {
        return snippet;
    }

    public boolean hasBoundNote() {
        return noteId > 0;
    }
}