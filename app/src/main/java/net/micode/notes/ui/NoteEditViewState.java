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

    public NoteEditViewState(boolean existingNote, long modifiedDate, int backgroundColorId,
            boolean hasClockAlert, long alertDate, long folderId, int widgetId, int widgetType,
            int checkListMode, String content, boolean hasContent, boolean canShare,
            boolean canDelete, boolean canSetReminder, boolean canToggleListMode,
            long pendingActionId, PendingAction pendingAction,
            boolean pendingActionSetsResultOk, long pendingActionFolderId) {
        this.existingNote = existingNote;
        this.modifiedDate = modifiedDate;
        this.backgroundColorId = backgroundColorId;
        this.hasClockAlert = hasClockAlert;
        this.alertDate = alertDate;
        this.folderId = folderId;
        this.widgetId = widgetId;
        this.widgetType = widgetType;
        this.checkListMode = checkListMode;
        this.content = content == null ? "" : content;
        this.hasContent = hasContent;
        this.canShare = canShare;
        this.canDelete = canDelete;
        this.canSetReminder = canSetReminder;
        this.canToggleListMode = canToggleListMode;
        this.pendingActionId = pendingActionId;
        this.pendingAction = pendingAction;
        this.pendingActionSetsResultOk = pendingActionSetsResultOk;
        this.pendingActionFolderId = pendingActionFolderId;
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

    public long getPendingActionId() {
        return pendingActionId;
    }

    public PendingAction getPendingAction() {
        return pendingAction;
    }

    public boolean pendingActionSetsResultOk() {
        return pendingActionSetsResultOk;
    }

    public long getPendingActionFolderId() {
        return pendingActionFolderId;
    }
}