package net.micode.notes.ui;

public final class NoteEditUiEvent {
    public enum Type {
        CLOSE_EDITOR,
        OPEN_NEW_NOTE
    }

    private final long id;
    private final Type type;
    private final boolean setResultOk;
    private final long folderId;

    public NoteEditUiEvent(long id, Type type, boolean setResultOk, long folderId) {
        this.id = id;
        this.type = type;
        this.setResultOk = setResultOk;
        this.folderId = folderId;
    }

    public long getId() {
        return id;
    }

    public Type getType() {
        return type;
    }

    public boolean shouldSetResultOk() {
        return setResultOk;
    }

    public long getFolderId() {
        return folderId;
    }
}