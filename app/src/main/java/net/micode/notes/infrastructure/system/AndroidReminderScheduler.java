package net.micode.notes.infrastructure.system;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import net.micode.notes.data.Notes;
import net.micode.notes.domain.service.ReminderScheduler;
import net.micode.notes.ui.AlarmReceiver;

public final class AndroidReminderScheduler implements ReminderScheduler {
    private final Context appContext;

    public AndroidReminderScheduler(Context context) {
        this.appContext = context.getApplicationContext();
    }

    @Override
    public void updateReminder(long noteId, long date, boolean enabled) {
        Intent intent = new Intent(appContext, AlarmReceiver.class);
        intent.setData(ContentUris.withAppendedId(Notes.CONTENT_NOTE_URI, noteId));
        PendingIntent pendingIntent = PendingIntent.getBroadcast(appContext, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        AlarmManager alarmManager = (AlarmManager) appContext.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) {
            return;
        }
        if (!enabled) {
            alarmManager.cancel(pendingIntent);
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                && !alarmManager.canScheduleExactAlarms()) {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, date, pendingIntent);
            return;
        }
        try {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, date, pendingIntent);
        } catch (SecurityException exception) {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, date, pendingIntent);
        }
    }
}
