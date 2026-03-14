package net.micode.notes.ui;

public final class NoteEditViewState {
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

    public NoteEditViewState(boolean existingNote, long modifiedDate, int backgroundColorId,
            boolean hasClockAlert, long alertDate, long folderId, int widgetId, int widgetType,
            int checkListMode, String content, boolean hasContent) {
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
}