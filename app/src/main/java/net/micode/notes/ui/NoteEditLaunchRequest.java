package net.micode.notes.ui;

public final class NoteEditLaunchRequest {
    public enum Action {
        OPEN_EXISTING,
        CREATE_OR_EDIT,
        INVALID
    }

    private final Action action;
    private final long noteId;
    private final String userQuery;
    private final long folderId;
    private final int widgetId;
    private final int widgetType;
    private final int backgroundColorId;
    private final String phoneNumber;
    private final long callDate;

    private NoteEditLaunchRequest(Action action, long noteId, String userQuery, long folderId,
            int widgetId, int widgetType, int backgroundColorId, String phoneNumber,
            long callDate) {
        this.action = action;
        this.noteId = noteId;
        this.userQuery = userQuery == null ? "" : userQuery;
        this.folderId = folderId;
        this.widgetId = widgetId;
        this.widgetType = widgetType;
        this.backgroundColorId = backgroundColorId;
        this.phoneNumber = phoneNumber;
        this.callDate = callDate;
    }

    public static NoteEditLaunchRequest invalid() {
        return new NoteEditLaunchRequest(Action.INVALID, 0L, "", 0L, 0, 0, 0, null, 0L);
    }

    public static NoteEditLaunchRequest openExisting(long noteId, String userQuery) {
        return new NoteEditLaunchRequest(Action.OPEN_EXISTING, noteId, userQuery,
                0L, 0, 0, 0, null, 0L);
    }

    public static NoteEditLaunchRequest createOrEdit(long folderId, int widgetId, int widgetType,
            int backgroundColorId, String phoneNumber, long callDate) {
        return new NoteEditLaunchRequest(Action.CREATE_OR_EDIT, 0L, "", folderId,
                widgetId, widgetType, backgroundColorId, phoneNumber, callDate);
    }

    public Action getAction() {
        return action;
    }

    public long getNoteId() {
        return noteId;
    }

    public String getUserQuery() {
        return userQuery;
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

    public int getBackgroundColorId() {
        return backgroundColorId;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public long getCallDate() {
        return callDate;
    }

    public boolean isValid() {
        return action != Action.INVALID;
    }

    public boolean isExistingNoteRequest() {
        return action == Action.OPEN_EXISTING;
    }

    public boolean shouldShowKeyboard() {
        return action == Action.CREATE_OR_EDIT;
    }

    public boolean shouldHideKeyboard() {
        return action == Action.OPEN_EXISTING;
    }

    public boolean hasCallRecordSource() {
        return callDate != 0L && phoneNumber != null;
    }
}