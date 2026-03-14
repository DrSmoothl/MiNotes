package net.micode.notes.infrastructure.contentresolver;

import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.text.TextUtils;
import android.util.Log;

import net.micode.notes.data.Notes;
import net.micode.notes.data.Notes.CallNote;
import net.micode.notes.data.Notes.NoteColumns;
import net.micode.notes.domain.model.CheckListText;
import net.micode.notes.domain.model.FolderDestination;
import net.micode.notes.domain.model.NoteListItem;
import net.micode.notes.domain.model.ScheduledReminder;
import net.micode.notes.domain.model.WidgetBinding;
import net.micode.notes.domain.model.WidgetNoteState;
import net.micode.notes.domain.repository.NoteRepository;
import net.micode.notes.domain.service.ContactNameResolver;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class ContentResolverNoteRepository implements NoteRepository {
    private static final String TAG = "ContentResolverNoteRepo";

    private static final String[] NOTE_PROJECTION = new String[] {
            NoteColumns.ID,
            NoteColumns.ALERTED_DATE,
            NoteColumns.BG_COLOR_ID,
            NoteColumns.CREATED_DATE,
            NoteColumns.HAS_ATTACHMENT,
            NoteColumns.MODIFIED_DATE,
            NoteColumns.NOTES_COUNT,
            NoteColumns.PARENT_ID,
            NoteColumns.SNIPPET,
            NoteColumns.TYPE,
            NoteColumns.WIDGET_ID,
            NoteColumns.WIDGET_TYPE,
    };

    private static final String[] FOLDER_PROJECTION = new String[] {
            NoteColumns.ID,
            NoteColumns.SNIPPET
    };

    private static final String[] WIDGET_PROJECTION = new String[] {
            NoteColumns.WIDGET_ID,
            NoteColumns.WIDGET_TYPE
    };

        private static final String[] REMINDER_PROJECTION = new String[] {
            NoteColumns.ID,
            NoteColumns.ALERTED_DATE,
        };

        private static final String[] WIDGET_NOTE_PROJECTION = new String[] {
            NoteColumns.ID,
            NoteColumns.BG_COLOR_ID,
            NoteColumns.SNIPPET,
        };

    private static final String ROOT_FOLDER_SELECTION = "(" + NoteColumns.TYPE + "<>"
            + Notes.TYPE_SYSTEM + " AND " + NoteColumns.PARENT_ID + "=?) OR ("
            + NoteColumns.ID + "=" + Notes.ID_CALL_RECORD_FOLDER + " AND "
            + NoteColumns.NOTES_COUNT + ">0)";

    private static final String NORMAL_SELECTION = NoteColumns.PARENT_ID + "=?";

    private final Context appContext;
    private final ContentResolver contentResolver;
    private final ContactNameResolver contactNameResolver;

    public ContentResolverNoteRepository(Context context, ContentResolver contentResolver,
            ContactNameResolver contactNameResolver) {
        this.appContext = context.getApplicationContext();
        this.contentResolver = contentResolver;
        this.contactNameResolver = contactNameResolver;
    }

    @Override
    public List<NoteListItem> getNotes(long folderId) {
        String selection = folderId == Notes.ID_ROOT_FOLDER ? ROOT_FOLDER_SELECTION
                : NORMAL_SELECTION;
        Cursor cursor = contentResolver.query(Notes.CONTENT_NOTE_URI,
                NOTE_PROJECTION,
                selection,
                new String[] { String.valueOf(folderId) },
                NoteColumns.TYPE + " DESC," + NoteColumns.MODIFIED_DATE + " DESC");
        ArrayList<NoteListItem> items = new ArrayList<NoteListItem>();
        if (cursor == null) {
            return items;
        }
        try {
            if (cursor.moveToFirst()) {
                do {
                    items.add(mapNote(cursor));
                } while (cursor.moveToNext());
            }
        } finally {
            cursor.close();
        }
        return items;
    }

    @Override
    public List<FolderDestination> getFolderDestinations(long currentFolderId, boolean includeRoot) {
        String selection = NoteColumns.TYPE + "=? AND " + NoteColumns.PARENT_ID + "<>? AND "
                + NoteColumns.ID + "<>?";
        if (includeRoot) {
            selection = "(" + selection + ") OR (" + NoteColumns.ID + "="
                    + Notes.ID_ROOT_FOLDER + ")";
        }

        Cursor cursor = contentResolver.query(Notes.CONTENT_NOTE_URI,
                FOLDER_PROJECTION,
                selection,
                new String[] {
                        String.valueOf(Notes.TYPE_FOLDER),
                        String.valueOf(Notes.ID_TRASH_FOLER),
                        String.valueOf(currentFolderId)
                },
                NoteColumns.MODIFIED_DATE + " DESC");
        ArrayList<FolderDestination> folders = new ArrayList<FolderDestination>();
        if (cursor == null) {
            return folders;
        }
        try {
            if (cursor.moveToFirst()) {
                do {
                    long folderId = cursor.getLong(0);
                    String folderName = folderId == Notes.ID_ROOT_FOLDER
                            ? appContext.getString(net.micode.notes.R.string.menu_move_parent_folder)
                            : cursor.getString(1);
                    folders.add(new FolderDestination(folderId, folderName));
                } while (cursor.moveToNext());
            }
        } finally {
            cursor.close();
        }
        return folders;
    }

    @Override
    public int getUserFolderCount() {
        Cursor cursor = contentResolver.query(Notes.CONTENT_NOTE_URI,
                new String[] { "COUNT(*)" },
                NoteColumns.TYPE + "=? AND " + NoteColumns.PARENT_ID + "<>?",
                new String[] {
                        String.valueOf(Notes.TYPE_FOLDER),
                        String.valueOf(Notes.ID_TRASH_FOLER)
                },
                null);
        if (cursor == null) {
            return 0;
        }
        try {
            return cursor.moveToFirst() ? cursor.getInt(0) : 0;
        } finally {
            cursor.close();
        }
    }

    @Override
    public boolean isVisibleFolderName(String name) {
        Cursor cursor = contentResolver.query(Notes.CONTENT_NOTE_URI,
                null,
                NoteColumns.TYPE + "=" + Notes.TYPE_FOLDER + " AND " + NoteColumns.PARENT_ID
                        + "<>" + Notes.ID_TRASH_FOLER + " AND " + NoteColumns.SNIPPET + "=?",
                new String[] { name },
                null);
        if (cursor == null) {
            return false;
        }
        try {
            return cursor.getCount() > 0;
        } finally {
            cursor.close();
        }
    }

    @Override
    public long createFolder(String name) {
        ContentValues values = new ContentValues();
        values.put(NoteColumns.SNIPPET, name);
        values.put(NoteColumns.TYPE, Notes.TYPE_FOLDER);
        values.put(NoteColumns.LOCAL_MODIFIED, 1);
        android.net.Uri uri = contentResolver.insert(Notes.CONTENT_NOTE_URI, values);
        if (uri == null || uri.getPathSegments().size() < 2) {
            return 0;
        }
        try {
            return Long.parseLong(uri.getPathSegments().get(1));
        } catch (NumberFormatException exception) {
            Log.e(TAG, "Failed to parse new folder id", exception);
            return 0;
        }
    }

    @Override
    public boolean renameFolder(long folderId, String name) {
        ContentValues values = new ContentValues();
        values.put(NoteColumns.SNIPPET, name);
        values.put(NoteColumns.TYPE, Notes.TYPE_FOLDER);
        values.put(NoteColumns.LOCAL_MODIFIED, 1);
        return contentResolver.update(Notes.CONTENT_NOTE_URI, values,
                NoteColumns.ID + "=?", new String[] { String.valueOf(folderId) }) > 0;
    }

    @Override
    public boolean deleteNotes(Set<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return true;
        }
        ArrayList<ContentProviderOperation> operations = new ArrayList<ContentProviderOperation>();
        for (Long id : ids) {
            if (id == null || id.longValue() == Notes.ID_ROOT_FOLDER) {
                continue;
            }
            operations.add(ContentProviderOperation.newDelete(
                    ContentUris.withAppendedId(Notes.CONTENT_NOTE_URI, id.longValue())).build());
        }
        return applyBatch(operations);
    }

    @Override
    public boolean moveNotes(Set<Long> ids, long folderId) {
        if (ids == null || ids.isEmpty()) {
            return true;
        }
        ArrayList<ContentProviderOperation> operations = new ArrayList<ContentProviderOperation>();
        for (Long id : ids) {
            if (id == null) {
                continue;
            }
            ContentProviderOperation.Builder builder = ContentProviderOperation.newUpdate(
                    ContentUris.withAppendedId(Notes.CONTENT_NOTE_URI, id.longValue()));
            builder.withValue(NoteColumns.PARENT_ID, folderId);
            builder.withValue(NoteColumns.LOCAL_MODIFIED, 1);
            operations.add(builder.build());
        }
        return applyBatch(operations);
    }

    @Override
    public Set<WidgetBinding> getWidgetsForFolder(long folderId) {
        Cursor cursor = contentResolver.query(Notes.CONTENT_NOTE_URI,
                WIDGET_PROJECTION,
                NoteColumns.PARENT_ID + "=?",
                new String[] { String.valueOf(folderId) },
                null);
        if (cursor == null) {
            return Collections.emptySet();
        }
        HashSet<WidgetBinding> widgets = new HashSet<WidgetBinding>();
        try {
            if (cursor.moveToFirst()) {
                do {
                    widgets.add(new WidgetBinding(cursor.getInt(0), cursor.getInt(1)));
                } while (cursor.moveToNext());
            }
        } finally {
            cursor.close();
        }
        return widgets;
    }

    @Override
    public List<ScheduledReminder> getUpcomingReminders(long currentTimeMillis) {
        Cursor cursor = contentResolver.query(Notes.CONTENT_NOTE_URI,
                REMINDER_PROJECTION,
                NoteColumns.ALERTED_DATE + ">? AND " + NoteColumns.TYPE + "=?",
                new String[] {
                        String.valueOf(currentTimeMillis),
                        String.valueOf(Notes.TYPE_NOTE)
                },
                null);
        ArrayList<ScheduledReminder> reminders = new ArrayList<ScheduledReminder>();
        if (cursor == null) {
            return reminders;
        }
        try {
            while (cursor.moveToNext()) {
                reminders.add(new ScheduledReminder(cursor.getLong(0), cursor.getLong(1)));
            }
        } finally {
            cursor.close();
        }
        return reminders;
    }

    @Override
    public WidgetNoteState getWidgetNoteState(int widgetId) {
        Cursor cursor = contentResolver.query(Notes.CONTENT_NOTE_URI,
                WIDGET_NOTE_PROJECTION,
                NoteColumns.WIDGET_ID + "=? AND " + NoteColumns.PARENT_ID + "<>?",
                new String[] { String.valueOf(widgetId), String.valueOf(Notes.ID_TRASH_FOLER) },
                null);
        if (cursor == null) {
            return null;
        }
        try {
            if (!cursor.moveToFirst()) {
                return null;
            }
            if (cursor.getCount() > 1) {
                Log.e(TAG, "Multiple notes with same widget id: " + widgetId);
            }
            return new WidgetNoteState(cursor.getLong(0), cursor.getInt(1), cursor.getString(2));
        } finally {
            cursor.close();
        }
    }

    @Override
    public void clearWidgetBindings(int[] widgetIds) {
        if (widgetIds == null || widgetIds.length == 0) {
            return;
        }
        ContentValues values = new ContentValues();
        values.put(NoteColumns.WIDGET_ID, android.appwidget.AppWidgetManager.INVALID_APPWIDGET_ID);
        for (int widgetId : widgetIds) {
            contentResolver.update(Notes.CONTENT_NOTE_URI,
                    values,
                    NoteColumns.WIDGET_ID + "=?",
                    new String[] { String.valueOf(widgetId) });
        }
    }

    private boolean applyBatch(ArrayList<ContentProviderOperation> operations) {
        if (operations.isEmpty()) {
            return true;
        }
        try {
            ContentProviderResult[] results = contentResolver.applyBatch(Notes.AUTHORITY, operations);
            return results != null && results.length > 0 && results[0] != null;
        } catch (Exception exception) {
            Log.e(TAG, "Batch operation failed", exception);
            return false;
        }
    }

    private NoteListItem mapNote(Cursor cursor) {
        long noteId = cursor.getLong(0);
        long parentId = cursor.getLong(7);
        String snippet = sanitizeSnippet(cursor.getString(8));
        String phoneNumber = "";
        String callName = "";
        if (parentId == Notes.ID_CALL_RECORD_FOLDER) {
            phoneNumber = getCallNumberByNoteId(noteId);
            if (!TextUtils.isEmpty(phoneNumber)) {
                callName = contactNameResolver.resolve(phoneNumber);
                if (callName == null) {
                    callName = phoneNumber;
                }
            }
        }
        return new NoteListItem(
                noteId,
                cursor.getLong(1),
                cursor.getInt(2),
                cursor.getLong(3),
                cursor.getInt(4) > 0,
                cursor.getLong(5),
                cursor.getInt(6),
                parentId,
                snippet,
                cursor.getInt(9),
                cursor.getInt(10),
                cursor.getInt(11),
                callName,
                phoneNumber);
    }

    private String sanitizeSnippet(String snippet) {
        if (snippet == null) {
            return "";
        }
        return CheckListText.stripMarkers(snippet);
    }

    private String getCallNumberByNoteId(long noteId) {
        Cursor cursor = contentResolver.query(Notes.CONTENT_DATA_URI,
                new String[] { CallNote.PHONE_NUMBER },
                CallNote.NOTE_ID + "=? AND " + CallNote.MIME_TYPE + "=?",
                new String[] { String.valueOf(noteId), CallNote.CONTENT_ITEM_TYPE },
                null);
        if (cursor == null) {
            return "";
        }
        try {
            return cursor.moveToFirst() ? cursor.getString(0) : "";
        } finally {
            cursor.close();
        }
    }
}