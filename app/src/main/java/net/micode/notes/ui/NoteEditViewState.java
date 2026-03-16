package net.micode.notes.ui;

public final class NoteEditViewState {
    public enum PendingAction {
        NONE,
        CLOSE_EDITOR,
        OPEN_NEW_NOTE
    }

    private final boolean existingNote;
    private final long modifiedDate;
    private final int backgroundColorId;
    private final boolean hasClockAlert;
    private final long alertDate;
    private final long folderId;
    private final int widgetId;
    private final int widgetType;
    private final int checkListMode;
    private final String content;
    private final boolean hasContent;
    private final boolean canShare;
    private final boolean canDelete;
    private final boolean canSetReminder;
    private final boolean canToggleListMode;
    private final long pendingActionId;
    private final PendingAction pendingAction;
    private final boolean pendingActionSetsResultOk;
    private final long pendingActionFolderId;

    public static final class Builder {
        private boolean existingNote;
        private long modifiedDate;
        private int backgroundColorId;
        private boolean hasClockAlert;
        private long alertDate;
        private long folderId;
        private int widgetId;
        private int widgetType;
        private int checkListMode;
        private String content = "";
        private boolean hasContent;
        private boolean canShare;
        private boolean canDelete;
        private boolean canSetReminder;
        private boolean canToggleListMode;
        private long pendingActionId;
        private PendingAction pendingAction = PendingAction.NONE;
        private boolean pendingActionSetsResultOk;
        private long pendingActionFolderId;

        private Builder() {
        }

        private Builder(NoteEditViewState state) {
            existingNote = state.existingNote;
            modifiedDate = state.modifiedDate;
            backgroundColorId = state.backgroundColorId;
            hasClockAlert = state.hasClockAlert;
            alertDate = state.alertDate;
            folderId = state.folderId;
            widgetId = state.widgetId;
            widgetType = state.widgetType;
            checkListMode = state.checkListMode;
            content = state.content;
            hasContent = state.hasContent;
            canShare = state.canShare;
            canDelete = state.canDelete;
            canSetReminder = state.canSetReminder;
            canToggleListMode = state.canToggleListMode;
            pendingActionId = state.pendingActionId;
            pendingAction = state.pendingAction;
            pendingActionSetsResultOk = state.pendingActionSetsResultOk;
            pendingActionFolderId = state.pendingActionFolderId;
        }

        public Builder setExistingNote(boolean existingNote) {
            this.existingNote = existingNote;
            return this;
        }

        public Builder setModifiedDate(long modifiedDate) {
            this.modifiedDate = modifiedDate;
            return this;
        }

        public Builder setBackgroundColorId(int backgroundColorId) {
            this.backgroundColorId = backgroundColorId;
            return this;
        }

        public Builder setHasClockAlert(boolean hasClockAlert) {
            this.hasClockAlert = hasClockAlert;
            return this;
        }

        public Builder setAlertDate(long alertDate) {
            this.alertDate = alertDate;
            return this;
        }

        public Builder setFolderId(long folderId) {
            this.folderId = folderId;
            return this;
        }

        public Builder setWidgetId(int widgetId) {
            this.widgetId = widgetId;
            return this;
        }

        public Builder setWidgetType(int widgetType) {
            this.widgetType = widgetType;
            return this;
        }

        public Builder setCheckListMode(int checkListMode) {
            this.checkListMode = checkListMode;
            return this;
        }

        public Builder setContent(String content) {
            this.content = content == null ? "" : content;
            return this;
        }

        public Builder setHasContent(boolean hasContent) {
            this.hasContent = hasContent;
            return this;
        }

        public Builder setCanShare(boolean canShare) {
            this.canShare = canShare;
            return this;
        }

        public Builder setCanDelete(boolean canDelete) {
            this.canDelete = canDelete;
            return this;
        }

        public Builder setCanSetReminder(boolean canSetReminder) {
            this.canSetReminder = canSetReminder;
            return this;
        }

        public Builder setCanToggleListMode(boolean canToggleListMode) {
            this.canToggleListMode = canToggleListMode;
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

        public Builder setPendingActionSetsResultOk(boolean pendingActionSetsResultOk) {
            this.pendingActionSetsResultOk = pendingActionSetsResultOk;
            return this;
        }

        public Builder setPendingActionFolderId(long pendingActionFolderId) {
            this.pendingActionFolderId = pendingActionFolderId;
            return this;
        }

        public Builder clearPendingAction() {
            pendingActionId = 0L;
            pendingAction = PendingAction.NONE;
            pendingActionSetsResultOk = false;
            pendingActionFolderId = 0L;
            return this;
        }

        public NoteEditViewState build() {
            return new NoteEditViewState(this);
        }
    }

    public NoteEditViewState(boolean existingNote, long modifiedDate, int backgroundColorId,
            boolean hasClockAlert, long alertDate, long folderId, int widgetId, int widgetType,
            int checkListMode, String content, boolean hasContent, boolean canShare,
            boolean canDelete, boolean canSetReminder, boolean canToggleListMode,
            long pendingActionId, PendingAction pendingAction,
            boolean pendingActionSetsResultOk, long pendingActionFolderId) {
        this(newBuilder()
                .setExistingNote(existingNote)
                .setModifiedDate(modifiedDate)
                .setBackgroundColorId(backgroundColorId)
                .setHasClockAlert(hasClockAlert)
                .setAlertDate(alertDate)
                .setFolderId(folderId)
                .setWidgetId(widgetId)
                .setWidgetType(widgetType)
                .setCheckListMode(checkListMode)
                .setContent(content)
                .setHasContent(hasContent)
                .setCanShare(canShare)
                .setCanDelete(canDelete)
                .setCanSetReminder(canSetReminder)
                .setCanToggleListMode(canToggleListMode)
                .setPendingActionId(pendingActionId)
                .setPendingAction(pendingAction)
                .setPendingActionSetsResultOk(pendingActionSetsResultOk)
                .setPendingActionFolderId(pendingActionFolderId));
    }

    private NoteEditViewState(Builder builder) {
        existingNote = builder.existingNote;
        modifiedDate = builder.modifiedDate;
        backgroundColorId = builder.backgroundColorId;
        hasClockAlert = builder.hasClockAlert;
        alertDate = builder.alertDate;
        folderId = builder.folderId;
        widgetId = builder.widgetId;
        widgetType = builder.widgetType;
        checkListMode = builder.checkListMode;
        content = builder.content;
        hasContent = builder.hasContent;
        canShare = builder.canShare;
        canDelete = builder.canDelete;
        canSetReminder = builder.canSetReminder;
        canToggleListMode = builder.canToggleListMode;
        pendingActionId = builder.pendingActionId;
        pendingAction = builder.pendingAction;
        pendingActionSetsResultOk = builder.pendingActionSetsResultOk;
        pendingActionFolderId = builder.pendingActionFolderId;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public Builder buildUpon() {
        return new Builder(this);
    }

    public boolean isExistingNote() {
        return existingNote;
    }

    public long getModifiedDate() {
        return modifiedDate;
    }

    public int getBackgroundColorId() {
        return backgroundColorId;
    }

    public boolean hasClockAlert() {
        return hasClockAlert;
    }

    public long getAlertDate() {
        return alertDate;
    }

    public long getFolderId() {
        return folderId;
    }

    public int getWidgetId() {
        return widgetId;
    }

    public int getWidgetType() {
        return widgetType;
    }

    public int getCheckListMode() {
        return checkListMode;
    }

    public boolean isCheckListMode() {
        return checkListMode == net.micode.notes.data.Notes.TextNote.MODE_CHECK_LIST;
    }

    public String getContent() {
        return content;
    }

    public boolean hasContent() {
        return hasContent;
    }

    public boolean canShare() {
        return canShare;
    }

    public boolean canDelete() {
        return canDelete;
    }

    public boolean canSetReminder() {
        return canSetReminder;
    }

    public boolean canToggleListMode() {
        return canToggleListMode;
    }

    public boolean shouldShowAddReminderAction() {
        return !hasClockAlert;
    }

    public boolean shouldShowClearReminderAction() {
        return hasClockAlert;
    }

    public boolean usesCallRecordMenu() {
        return folderId == net.micode.notes.data.Notes.ID_CALL_RECORD_FOLDER;
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

    public boolean pendingActionSetsResultOk() {
        return pendingActionSetsResultOk;
    }

    public long getPendingActionFolderId() {
        return pendingActionFolderId;
    }

    public boolean shouldOpenNewNoteAfterHandling() {
        return pendingAction == PendingAction.OPEN_NEW_NOTE;
    }

    public boolean shouldCloseEditorAfterHandling() {
        return pendingAction == PendingAction.CLOSE_EDITOR;
    }

    public NoteEditViewState withPendingAction(long actionId, PendingAction action,
            boolean setResultOk, long folderId) {
        return buildUpon()
                .setPendingActionId(actionId)
                .setPendingAction(action)
                .setPendingActionSetsResultOk(setResultOk)
                .setPendingActionFolderId(folderId)
                .build();
    }

    public NoteEditViewState withoutPendingAction() {
        return buildUpon()
                .clearPendingAction()
                .build();
    }
}