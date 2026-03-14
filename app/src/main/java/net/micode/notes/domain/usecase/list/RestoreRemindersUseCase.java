package net.micode.notes.domain.usecase.list;

import net.micode.notes.domain.model.ScheduledReminder;
import net.micode.notes.domain.repository.NoteRepository;
import net.micode.notes.domain.service.ReminderScheduler;

import java.util.List;

public final class RestoreRemindersUseCase {
    private final NoteRepository noteRepository;
    private final ReminderScheduler reminderScheduler;

    public RestoreRemindersUseCase(NoteRepository noteRepository,
            ReminderScheduler reminderScheduler) {
        this.noteRepository = noteRepository;
        this.reminderScheduler = reminderScheduler;
    }

    public void restore(long currentTimeMillis) {
        List<ScheduledReminder> reminders = noteRepository.getUpcomingReminders(currentTimeMillis);
        for (ScheduledReminder reminder : reminders) {
            reminderScheduler.updateReminder(reminder.getNoteId(), reminder.getAlertDate(), true);
        }
    }
}