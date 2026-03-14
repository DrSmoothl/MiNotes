package net.micode.notes.domain.model;

import android.text.TextUtils;

import net.micode.notes.data.Notes;

public final class NoteListItem {
    private final long id;
    private final long alertDate;
    private final int bgColorId;
    private final long createdDate;
    private final boolean hasAttachment;
    private final long modifiedDate;
    private final int notesCount;
    private final long parentId;
    private final String snippet;
    private final int type;
    private final int widgetId;
    private final int widgetType;
    private final String callName;
    private final String phoneNumber;

    public NoteListItem(long id, long alertDate, int bgColorId, long createdDate,
            boolean hasAttachment, long modifiedDate, int notesCount, long parentId,
            String snippet, int type, int widgetId, int widgetType, String callName,
            String phoneNumber) {
        this.id = id;
        this.alertDate = alertDate;
        this.bgColorId = bgColorId;
        this.createdDate = createdDate;
        this.hasAttachment = hasAttachment;
        this.modifiedDate = modifiedDate;
        this.notesCount = notesCount;
        this.parentId = parentId;
        this.snippet = snippet == null ? "" : snippet;
        this.type = type;
        this.widgetId = widgetId;
        this.widgetType = widgetType;
        this.callName = callName == null ? "" : callName;
        this.phoneNumber = phoneNumber == null ? "" : phoneNumber;
    }

    public long getId() {
        return id;
    }

    public long getAlertDate() {
        return alertDate;
    }

    public int getBgColorId() {
        return bgColorId;
    }

    public long getCreatedDate() {
        return createdDate;
    }

    public boolean hasAttachment() {
        return hasAttachment;
    }

    public long getModifiedDate() {
        return modifiedDate;
    }

    public int getNotesCount() {
        return notesCount;
    }

    public long getParentId() {
        return parentId;
    }

    public String getSnippet() {
        return snippet;
    }

    public int getType() {
        return type;
    }

    public int getWidgetId() {
        return widgetId;
    }

    public int getWidgetType() {
        return widgetType;
    }

    public String getCallName() {
        return callName;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public boolean hasAlert() {
        return alertDate > 0;
    }

    public boolean isCallRecord() {
        return parentId == Notes.ID_CALL_RECORD_FOLDER && !TextUtils.isEmpty(phoneNumber);
    }
}