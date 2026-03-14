package net.micode.notes.domain.model;

public interface NoteEditorSession {
    boolean existsInDatabase();

    boolean save();

    void markDeleted(boolean deleted);

    void setWorkingText(String text);

    void setAlertDate(long date, boolean set);

    void setBgColorId(int id);

    void setCheckListMode(int mode);

    long getNoteId();

    long getFolderId();

    long getAlertDate();

    long getModifiedDate();

    int getBgColorId();

    int getCheckListMode();

    int getWidgetId();

    int getWidgetType();

    String getContent();

    boolean hasClockAlert();
}