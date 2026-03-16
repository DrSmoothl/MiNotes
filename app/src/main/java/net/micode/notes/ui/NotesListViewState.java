package net.micode.notes.ui;

import net.micode.notes.data.Notes;
import net.micode.notes.domain.model.ExportedTextFile;
import net.micode.notes.domain.model.FolderDestination;
import net.micode.notes.domain.model.WidgetBinding;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class NotesListViewState {
    public enum PendingAction {
        NONE,
        SHOW_MOVE_DESTINATIONS,
        SHOW_EXPORT_RESULT,
        DELETE_COMPLETED,
        MOVE_COMPLETED,
        FOLDER_NAME_CONFLICT,
        FOLDER_CREATED,
        FOLDER_RENAMED,
        FOLDER_DELETED
    }

    public enum ScreenMode {
        ROOT,
        FOLDER,
        CALL_RECORD
    }

    private final long currentFolderId;
    private final ScreenMode mode;
    private final String currentFolderName;
    private final List<NoteItemData> items;
    private final boolean hasUserFolders;
    private final long pendingActionId;
    private final PendingAction pendingAction;
    private final List<FolderDestination> pendingFolderDestinations;
    private final ExportedTextFile pendingExportedFile;
    private final boolean pendingOperationSucceeded;
    private final int pendingAffectedCount;
    private final String pendingDestinationFolderName;
    private final List<WidgetBinding> pendingWidgetBindings;

    public NotesListViewState(long currentFolderId, ScreenMode mode, String currentFolderName,
            List<NoteItemData> items) {
        this(currentFolderId, mode, currentFolderName, items, false, 0L, PendingAction.NONE,
                Collections.<FolderDestination>emptyList(), null, false, 0, null,
                Collections.<WidgetBinding>emptyList());
    }

        public NotesListViewState(long currentFolderId, ScreenMode mode, String currentFolderName,
            List<NoteItemData> items, boolean hasUserFolders) {
        this(currentFolderId, mode, currentFolderName, items, hasUserFolders, 0L,
            PendingAction.NONE, Collections.<FolderDestination>emptyList(), null, false, 0,
            null, Collections.<WidgetBinding>emptyList());
        }

    public NotesListViewState(long currentFolderId, ScreenMode mode, String currentFolderName,
            List<NoteItemData> items, boolean hasUserFolders, long pendingActionId,
            PendingAction pendingAction,
            List<FolderDestination> pendingFolderDestinations,
            ExportedTextFile pendingExportedFile, boolean pendingOperationSucceeded,
            int pendingAffectedCount, String pendingDestinationFolderName,
            List<WidgetBinding> pendingWidgetBindings) {
        this.currentFolderId = currentFolderId;
        this.mode = mode;
        this.currentFolderName = currentFolderName;
        this.items = Collections.unmodifiableList(new ArrayList<NoteItemData>(items));
        this.hasUserFolders = hasUserFolders;
        this.pendingActionId = pendingActionId;
        this.pendingAction = pendingAction;
        this.pendingFolderDestinations = Collections.unmodifiableList(
                new ArrayList<FolderDestination>(pendingFolderDestinations));
        this.pendingExportedFile = pendingExportedFile;
        this.pendingOperationSucceeded = pendingOperationSucceeded;
        this.pendingAffectedCount = pendingAffectedCount;
        this.pendingDestinationFolderName = pendingDestinationFolderName;
        this.pendingWidgetBindings = Collections.unmodifiableList(
            new ArrayList<WidgetBinding>(pendingWidgetBindings));
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

    public boolean hasUserFolders() {
        return hasUserFolders;
    }

    public long getPendingActionId() {
        return pendingActionId;
    }

    public PendingAction getPendingAction() {
        return pendingAction;
    }

    public List<FolderDestination> getPendingFolderDestinations() {
        return pendingFolderDestinations;
    }

    public ExportedTextFile getPendingExportedFile() {
        return pendingExportedFile;
    }

    public boolean isPendingOperationSucceeded() {
        return pendingOperationSucceeded;
    }

    public int getPendingAffectedCount() {
        return pendingAffectedCount;
    }

    public String getPendingDestinationFolderName() {
        return pendingDestinationFolderName;
    }

    public List<WidgetBinding> getPendingWidgetBindings() {
        return pendingWidgetBindings;
    }

    public boolean isRootMode() {
        return mode == ScreenMode.ROOT;
    }

    public boolean isCallRecordMode() {
        return mode == ScreenMode.CALL_RECORD;
    }

    public NotesListViewState withCurrentFolderName(String name) {
        return new NotesListViewState(currentFolderId, mode, name, items, hasUserFolders,
            pendingActionId,
                pendingAction, pendingFolderDestinations, pendingExportedFile,
                pendingOperationSucceeded, pendingAffectedCount, pendingDestinationFolderName,
                pendingWidgetBindings);
    }

    public NotesListViewState withPendingAction(long actionId, PendingAction action,
            List<FolderDestination> folderDestinations, ExportedTextFile exportedFile,
            boolean operationSucceeded, int affectedCount, String destinationFolderName,
            List<WidgetBinding> widgetBindings) {
        return new NotesListViewState(currentFolderId, mode, currentFolderName, items,
            hasUserFolders, actionId, action, folderDestinations, exportedFile,
            operationSucceeded, affectedCount, destinationFolderName, widgetBindings);
    }

    public NotesListViewState withoutPendingAction() {
        return new NotesListViewState(currentFolderId, mode, currentFolderName, items);
    }
}