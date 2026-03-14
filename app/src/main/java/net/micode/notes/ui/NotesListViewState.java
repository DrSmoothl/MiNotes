package net.micode.notes.ui;

import net.micode.notes.data.Notes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class NotesListViewState {
    public enum ScreenMode {
        ROOT,
        FOLDER,
        CALL_RECORD
    }

    private final long currentFolderId;
    private final ScreenMode mode;
    private final String currentFolderName;
    private final List<NoteItemData> items;

    public NotesListViewState(long currentFolderId, ScreenMode mode, String currentFolderName,
            List<NoteItemData> items) {
        this.currentFolderId = currentFolderId;
        this.mode = mode;
        this.currentFolderName = currentFolderName;
        this.items = Collections.unmodifiableList(new ArrayList<NoteItemData>(items));
    }

    public static NotesListViewState root(List<NoteItemData> items) {
        return new NotesListViewState(Notes.ID_ROOT_FOLDER, ScreenMode.ROOT, null, items);
    }

    public long getCurrentFolderId() {
        return currentFolderId;
    }

    public ScreenMode getMode() {
        return mode;
    }

    public String getCurrentFolderName() {
        return currentFolderName;
    }

    public List<NoteItemData> getItems() {
        return items;
    }

    public int getItemCount() {
        return items.size();
    }

    public boolean isRootMode() {
        return mode == ScreenMode.ROOT;
    }

    public boolean isCallRecordMode() {
        return mode == ScreenMode.CALL_RECORD;
    }

    public NotesListViewState withCurrentFolderName(String name) {
        return new NotesListViewState(currentFolderId, mode, name, items);
    }
}