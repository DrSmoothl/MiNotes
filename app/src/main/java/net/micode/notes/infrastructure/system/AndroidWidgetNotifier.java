package net.micode.notes.infrastructure.system;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import net.micode.notes.data.Notes;
import net.micode.notes.domain.service.WidgetNotifier;
import net.micode.notes.widget.NoteWidgetProvider_2x;
import net.micode.notes.widget.NoteWidgetProvider_4x;

public final class AndroidWidgetNotifier implements WidgetNotifier {
    private static final String TAG = "AndroidWidgetNotifier";

    private final Context appContext;

    public AndroidWidgetNotifier(Context context) {
        this.appContext = context.getApplicationContext();
    }

    @Override
    public void refresh(int widgetId, int widgetType) {
        Intent intent = new Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        if (widgetType == Notes.TYPE_WIDGET_2X) {
            intent.setClass(appContext, NoteWidgetProvider_2x.class);
        } else if (widgetType == Notes.TYPE_WIDGET_4X) {
            intent.setClass(appContext, NoteWidgetProvider_4x.class);
        } else {
            Log.e(TAG, "Unsupported widget type: " + widgetType);
            return;
        }
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, new int[] { widgetId });
        appContext.sendBroadcast(intent);
    }
}