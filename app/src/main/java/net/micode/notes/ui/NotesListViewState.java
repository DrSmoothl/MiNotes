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
        INTRODUCTION_INIT_FAILED,
        OPEN_NOTE_EDITOR,
        SHOW_FOLDER_NAME_DIALOG,
        CONFIRM_DELETE_FOLDER,
        CONFIRM_DELETE_NOTES,
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
    private final long pendingEditorNoteId;
    private final long pendingEditorFolderId;
    private final boolean pendingEditorCreateMode;
    private final boolean pendingFolderDialogCreateMode;
    private final long pendingFolderDialogFolderId;
    private final String pendingFolderDialogInitialName;
    private final long pendingDeleteFolderId;
    private final String pendingDeleteFolderName;

    public static final class Builder {
        private long currentFolderId;
        private ScreenMode mode;
        private String currentFolderName;
        private List<NoteItemData> items = Collections.emptyList();
        private boolean hasUserFolders;
        private long pendingActionId;
        private PendingAction pendingAction = PendingAction.NONE;
        private List<FolderDestination> pendingFolderDestinations = Collections.emptyList();
        private ExportedTextFile pendingExportedFile;
        private boolean pendingOperationSucceeded;
        private int pendingAffectedCount;
        private String pendingDestinationFolderName;
        private List<WidgetBinding> pendingWidgetBindings = Collections.emptyList();
        private long pendingEditorNoteId;
        private long pendingEditorFolderId;
        private boolean pendingEditorCreateMode;
        private boolean pendingFolderDialogCreateMode;
        private long pendingFolderDialogFolderId;
        private String pendingFolderDialogInitialName;
        private long pendingDeleteFolderId;
        private String pendingDeleteFolderName;

        private Builder() {
        }

        private Builder(NotesListViewState state) {
            currentFolderId = state.currentFolderId;
            mode = state.mode;
            currentFolderName = state.currentFolderName;
            items = state.items;
            hasUserFolders = state.hasUserFolders;
            pendingActionId = state.pendingActionId;
            pendingAction = state.pendingAction;
            pendingFolderDestinations = state.pendingFolderDestinations;
            pendingExportedFile = state.pendingExportedFile;
            pendingOperationSucceeded = state.pendingOperationSucceeded;
            pendingAffectedCount = state.pendingAffectedCount;
            pendingDestinationFolderName = state.pendingDestinationFolderName;
            pendingWidgetBindings = state.pendingWidgetBindings;
            pendingEditorNoteId = state.pendingEditorNoteId;
            pendingEditorFolderId = state.pendingEditorFolderId;
            pendingEditorCreateMode = state.pendingEditorCreateMode;
            pendingFolderDialogCreateMode = state.pendingFolderDialogCreateMode;
            pendingFolderDialogFolderId = state.pendingFolderDialogFolderId;
            pendingFolderDialogInitialName = state.pendingFolderDialogInitialName;
            pendingDeleteFolderId = state.pendingDeleteFolderId;
            pendingDeleteFolderName = state.pendingDeleteFolderName;
        }

        public Builder setCurrentFolderId(long currentFolderId) {
            this.currentFolderId = currentFolderId;
            return this;
        }

        public Builder setMode(ScreenMode mode) {
            this.mode = mode;
            return this;
        }

        public Builder setCurrentFolderName(String currentFolderName) {
            this.currentFolderName = currentFolderName;
            return this;
        }

        public Builder setItems(List<NoteItemData> items) {
            this.items = items;
            return this;
        }

        public Builder setHasUserFolders(boolean hasUserFolders) {
            this.hasUserFolders = hasUserFolders;
            return this;
        }

        public Builder setPendingActionId(long pendingActionId) {
            this.pendingActionId = pendingActionId;
            return this;
        }

        public Builder setPendingAction(PendingAction pendingAction) {
            this.pendingAction = pendingAction;
            return this;
        }

        public Builder setPendingFolderDestinations(List<FolderDestination> pendingFolderDestinations) {
            this.pendingFolderDestinations = pendingFolderDestinations;
            return this;
        }

        public Builder setPendingExportedFile(ExportedTextFile pendingExportedFile) {
            this.pendingExportedFile = pendingExportedFile;
            return this;
        }

        public Builder setPendingOperationSucceeded(boolean pendingOperationSucceeded) {
            this.pendingOperationSucceeded = pendingOperationSucceeded;
            return this;
        }

        public Builder setPendingAffectedCount(int pendingAffectedCount) {
            this.pendingAffectedCount = pendingAffectedCount;
            return this;
        }

        public Builder setPendingDestinationFolderName(String pendingDestinationFolderName) {
            this.pendingDestinationFolderName = pendingDestinationFolderName;
            return this;
        }

        public Builder setPendingWidgetBindings(List<WidgetBinding> pendingWidgetBindings) {
            this.pendingWidgetBindings = pendingWidgetBindings;
            return this;
        }

        public Builder setPendingEditorNoteId(long pendingEditorNoteId) {
            this.pendingEditorNoteId = pendingEditorNoteId;
            return this;
        }

        public Builder setPendingEditorFolderId(long pendingEditorFolderId) {
            this.pendingEditorFolderId = pendingEditorFolderId;
            return this;
        }

        public Builder setPendingEditorCreateMode(boolean pendingEditorCreateMode) {
            this.pendingEditorCreateMode = pendingEditorCreateMode;
            return this;
        }

        public Builder setPendingFolderDialogCreateMode(boolean pendingFolderDialogCreateMode) {
            this.pendingFolderDialogCreateMode = pendingFolderDialogCreateMode;
            return this;
        }

        public Builder setPendingFolderDialogFolderId(long pendingFolderDialogFolderId) {
            this.pendingFolderDialogFolderId = pendingFolderDialogFolderId;
            return this;
        }

        public Builder setPendingFolderDialogInitialName(String pendingFolderDialogInitialName) {
            this.pendingFolderDialogInitialName = pendingFolderDialogInitialName;
            return this;
        }

        public Builder setPendingDeleteFolderId(long pendingDeleteFolderId) {
            this.pendingDeleteFolderId = pendingDeleteFolderId;
            return this;
        }

        public Builder setPendingDeleteFolderName(String pendingDeleteFolderName) {
            this.pendingDeleteFolderName = pendingDeleteFolderName;
            return this;
        }

        public Builder clearPendingState() {
            pendingActionId = 0L;
            pendingAction = PendingAction.NONE;
            pendingFolderDestinations = Collections.emptyList();
            pendingExportedFile = null;
            pendingOperationSucceeded = false;
            pendingAffectedCount = 0;
            pendingDestinationFolderName = null;
            pendingWidgetBindings = Collections.emptyList();
            pendingEditorNoteId = 0L;
            pendingEditorFolderId = 0L;
            pendingEditorCreateMode = false;
            pendingFolderDialogCreateMode = false;
            pendingFolderDialogFolderId = 0L;
            pendingFolderDialogInitialName = null;
            pendingDeleteFolderId = 0L;
            pendingDeleteFolderName = null;
            return this;
        }

        public NotesListViewState build() {
            return new NotesListViewState(this);
        }
    }

    public NotesListViewState(long currentFolderId, ScreenMode mode, String currentFolderName,
            List<NoteItemData> items) {
        this(newBuilder()
                .setCurrentFolderId(currentFolderId)
                .setMode(mode)
                .setCurrentFolderName(currentFolderName)
                .setItems(items));
    }

    public NotesListViewState(long currentFolderId, ScreenMode mode, String currentFolderName,
            List<NoteItemData> items, boolean hasUserFolders) {
        this(newBuilder()
                .setCurrentFolderId(currentFolderId)
                .setMode(mode)
                .setCurrentFolderName(currentFolderName)
                .setItems(items)
                .setHasUserFolders(hasUserFolders));
    }

    public NotesListViewState(long currentFolderId, ScreenMode mode, String currentFolderName,
            List<NoteItemData> items, boolean hasUserFolders, long pendingActionId,
            PendingAction pendingAction,
            List<FolderDestination> pendingFolderDestinations,
            ExportedTextFile pendingExportedFile, boolean pendingOperationSucceeded,
            int pendingAffectedCount, String pendingDestinationFolderName,
            List<WidgetBinding> pendingWidgetBindings, long pendingEditorNoteId,
            long pendingEditorFolderId, boolean pendingEditorCreateMode,
            boolean pendingFolderDialogCreateMode, long pendingFolderDialogFolderId,
            String pendingFolderDialogInitialName, long pendingDeleteFolderId,
            String pendingDeleteFolderName) {
            this(newBuilder()
                .setCurrentFolderId(currentFolderId)
                .setMode(mode)
                .setCurrentFolderName(currentFolderName)
                .setItems(items)
                .setHasUserFolders(hasUserFolders)
                .setPendingActionId(pendingActionId)
                .setPendingAction(pendingAction)
                .setPendingFolderDestinations(pendingFolderDestinations)
                .setPendingExportedFile(pendingExportedFile)
                .setPendingOperationSucceeded(pendingOperationSucceeded)
                .setPendingAffectedCount(pendingAffectedCount)
                .setPendingDestinationFolderName(pendingDestinationFolderName)
                .setPendingWidgetBindings(pendingWidgetBindings)
                .setPendingEditorNoteId(pendingEditorNoteId)
                .setPendingEditorFolderId(pendingEditorFolderId)
                .setPendingEditorCreateMode(pendingEditorCreateMode)
                .setPendingFolderDialogCreateMode(pendingFolderDialogCreateMode)
                .setPendingFolderDialogFolderId(pendingFolderDialogFolderId)
                .setPendingFolderDialogInitialName(pendingFolderDialogInitialName)
                .setPendingDeleteFolderId(pendingDeleteFolderId)
                .setPendingDeleteFolderName(pendingDeleteFolderName));
            }

            private NotesListViewState(Builder builder) {
            currentFolderId = builder.currentFolderId;
            mode = builder.mode;
            currentFolderName = builder.currentFolderName;
            items = Collections.unmodifiableList(new ArrayList<NoteItemData>(builder.items));
            hasUserFolders = builder.hasUserFolders;
            pendingActionId = builder.pendingActionId;
            pendingAction = builder.pendingAction;
            pendingFolderDestinations = Collections.unmodifiableList(
                new ArrayList<FolderDestination>(builder.pendingFolderDestinations));
            pendingExportedFile = builder.pendingExportedFile;
            pendingOperationSucceeded = builder.pendingOperationSucceeded;
            pendingAffectedCount = builder.pendingAffectedCount;
            pendingDestinationFolderName = builder.pendingDestinationFolderName;
            pendingWidgetBindings = Collections.unmodifiableList(
                new ArrayList<WidgetBinding>(builder.pendingWidgetBindings));
            pendingEditorNoteId = builder.pendingEditorNoteId;
            pendingEditorFolderId = builder.pendingEditorFolderId;
            pendingEditorCreateMode = builder.pendingEditorCreateMode;
            pendingFolderDialogCreateMode = builder.pendingFolderDialogCreateMode;
            pendingFolderDialogFolderId = builder.pendingFolderDialogFolderId;
            pendingFolderDialogInitialName = builder.pendingFolderDialogInitialName;
            pendingDeleteFolderId = builder.pendingDeleteFolderId;
            pendingDeleteFolderName = builder.pendingDeleteFolderName;
    }

    public static NotesListViewState root(List<NoteItemData> items) {
        return new NotesListViewState(Notes.ID_ROOT_FOLDER, ScreenMode.ROOT, null, items);
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public Builder buildUpon() {
        return new Builder(this);
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

    public boolean hasPendingAction() {
        return pendingAction != PendingAction.NONE;
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

    public long getPendingEditorNoteId() {
        return pendingEditorNoteId;
    }

    public long getPendingEditorFolderId() {
        return pendingEditorFolderId;
    }

    public boolean isPendingEditorCreateMode() {
        return pendingEditorCreateMode;
    }

    public boolean isPendingFolderDialogCreateMode() {
        return pendingFolderDialogCreateMode;
    }

    public long getPendingFolderDialogFolderId() {
        return pendingFolderDialogFolderId;
    }

    public String getPendingFolderDialogInitialName() {
        return pendingFolderDialogInitialName;
    }

    public long getPendingDeleteFolderId() {
        return pendingDeleteFolderId;
    }

    public String getPendingDeleteFolderName() {
        return pendingDeleteFolderName;
    }

    public boolean isRootMode() {
        return mode == ScreenMode.ROOT;
    }

    public boolean isCallRecordMode() {
        return mode == ScreenMode.CALL_RECORD;
    }

    public boolean shouldOpenNoteEditorAfterHandling() {
        return pendingAction == PendingAction.OPEN_NOTE_EDITOR;
    }

    public boolean shouldShowFolderNameDialogAfterHandling() {
        return pendingAction == PendingAction.SHOW_FOLDER_NAME_DIALOG;
    }

    public boolean shouldConfirmDeleteFolderAfterHandling() {
        return pendingAction == PendingAction.CONFIRM_DELETE_FOLDER;
    }

    public boolean shouldConfirmDeleteNotesAfterHandling() {
        return pendingAction == PendingAction.CONFIRM_DELETE_NOTES;
    }

    public boolean shouldShowMoveDestinationsAfterHandling() {
        return pendingAction == PendingAction.SHOW_MOVE_DESTINATIONS;
    }

    public boolean shouldShowExportResultAfterHandling() {
        return pendingAction == PendingAction.SHOW_EXPORT_RESULT;
    }

    public boolean shouldFinishSelectionModeAfterHandling() {
        return pendingAction == PendingAction.DELETE_COMPLETED
                || pendingAction == PendingAction.MOVE_COMPLETED;
    }

    public NotesListViewState withCurrentFolderName(String name) {
        return buildUpon()
            .setCurrentFolderName(name)
            .build();
    }

    public NotesListViewState withPendingAction(long actionId, PendingAction action,
            List<FolderDestination> folderDestinations, ExportedTextFile exportedFile,
            boolean operationSucceeded, int affectedCount, String destinationFolderName,
            List<WidgetBinding> widgetBindings) {
        return buildUpon()
            .setPendingActionId(actionId)
            .setPendingAction(action)
            .setPendingFolderDestinations(folderDestinations)
            .setPendingExportedFile(exportedFile)
            .setPendingOperationSucceeded(operationSucceeded)
            .setPendingAffectedCount(affectedCount)
            .setPendingDestinationFolderName(destinationFolderName)
            .setPendingWidgetBindings(widgetBindings)
            .build();
    }

    public NotesListViewState withEditorNavigation(long actionId, long noteId, long folderId,
            boolean createMode) {
        return buildUpon()
            .clearPendingState()
            .setPendingActionId(actionId)
            .setPendingAction(PendingAction.OPEN_NOTE_EDITOR)
            .setPendingOperationSucceeded(true)
            .setPendingEditorNoteId(noteId)
            .setPendingEditorFolderId(folderId)
            .setPendingEditorCreateMode(createMode)
            .build();
    }

    public NotesListViewState withFolderNameDialog(long actionId, boolean createMode,
            long folderId, String initialName) {
        return buildUpon()
            .clearPendingState()
            .setPendingActionId(actionId)
            .setPendingAction(PendingAction.SHOW_FOLDER_NAME_DIALOG)
            .setPendingOperationSucceeded(true)
            .setPendingFolderDialogCreateMode(createMode)
            .setPendingFolderDialogFolderId(folderId)
            .setPendingFolderDialogInitialName(initialName)
            .build();
    }

        public NotesListViewState withDeleteFolderConfirmation(long actionId, long folderId,
            String folderName) {
        return buildUpon()
            .clearPendingState()
            .setPendingActionId(actionId)
            .setPendingAction(PendingAction.CONFIRM_DELETE_FOLDER)
            .setPendingOperationSucceeded(true)
            .setPendingDeleteFolderId(folderId)
            .setPendingDeleteFolderName(folderName)
            .build();
        }

        public NotesListViewState withDeleteNotesConfirmation(long actionId, int selectedCount) {
        return buildUpon()
            .clearPendingState()
            .setPendingActionId(actionId)
            .setPendingAction(PendingAction.CONFIRM_DELETE_NOTES)
            .setPendingOperationSucceeded(true)
            .setPendingAffectedCount(selectedCount)
            .build();
        }

    public NotesListViewState withoutPendingAction() {
        return buildUpon()
            .clearPendingState()
            .build();
    }
}