package net.micode.notes.domain.model;

public final class ScheduledReminder {
    private final long noteId;
    private final long alertDate;

    public ScheduledReminder(long noteId, long alertDate) {
        this.noteId = noteId;
        this.alertDate = alertDate;
    }

    public long getNoteId() {
        return noteId;
    }

    public long getAlertDate() {
        return alertDate;
    }
}