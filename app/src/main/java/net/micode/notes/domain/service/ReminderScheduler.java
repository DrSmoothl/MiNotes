package net.micode.notes.domain.service;

public interface ReminderScheduler {
    void updateReminder(long noteId, long date, boolean enabled);
}