package net.micode.notes.infrastructure.editor;

import android.appwidget.AppWidgetManager;
import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.net.Uri;
import android.os.RemoteException;
import android.text.TextUtils;
import android.util.Log;

import net.micode.notes.data.Notes;
import net.micode.notes.data.Notes.CallNote;
import net.micode.notes.data.Notes.DataColumns;
import net.micode.notes.data.Notes.DataConstants;
import net.micode.notes.data.Notes.NoteColumns;
import net.micode.notes.data.Notes.TextNote;
import net.micode.notes.domain.model.NoteEditorSession;

import java.util.ArrayList;

public final class ContentResolverNoteEditorSession implements NoteEditorSession {
    private static final String TAG = "ContentResolverEditorSession";

    private static final String[] DATA_PROJECTION = new String[] {
            DataColumns.ID,
            DataColumns.CONTENT,
            DataColumns.MIME_TYPE,
            DataColumns.DATA1,
            DataColumns.DATA2,
            DataColumns.DATA3,
            DataColumns.DATA4,
    };

    private static final String[] NOTE_PROJECTION = new String[] {
            NoteColumns.PARENT_ID,
            NoteColumns.ALERTED_DATE,
            NoteColumns.BG_COLOR_ID,
            NoteColumns.WIDGET_ID,
            NoteColumns.WIDGET_TYPE,
            NoteColumns.MODIFIED_DATE,
    };

    private final ContentResolver contentResolver;
    private final ContentValues noteDiffValues = new ContentValues();
    private final ContentValues textDataValues = new ContentValues();
    private final ContentValues callDataValues = new ContentValues();

    private long noteId;
    private long folderId;
    private String content;
    private int mode;
    private long alertDate;
    private long modifiedDate;
    private int bgColorId;
    private int widgetId;
    private int widgetType;
    private boolean deleted;
    private long textDataId;
    private long callDataId;

    private ContentResolverNoteEditorSession(Context context) {
        this.contentResolver = context.getApplicationContext().getContentResolver();
        this.content = "";
        this.mode = 0;
        this.alertDate = 0;
        this.modifiedDate = System.currentTimeMillis();
        this.widgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
        this.widgetType = Notes.TYPE_WIDGET_INVALIDE;
    }

    public static ContentResolverNoteEditorSession create(Context context, long folderId,
            int widgetId, int widgetType, int defaultBgColorId) {
        ContentResolverNoteEditorSession session = new ContentResolverNoteEditorSession(context);
        session.folderId = folderId;
        session.setBgColorId(defaultBgColorId);
        session.setWidgetId(widgetId);
        session.setWidgetType(widgetType);
        return session;
    }

    public static ContentResolverNoteEditorSession load(Context context, long noteId) {
        ContentResolverNoteEditorSession session = new ContentResolverNoteEditorSession(context);
        session.noteId = noteId;
        session.loadNote();
        session.loadNoteData();
        return session;
    }

    public void convertToCallNote(String phoneNumber, long callDate) {
        setCallData(CallNote.CALL_DATE, String.valueOf(callDate));
        setCallData(CallNote.PHONE_NUMBER, phoneNumber);
        setNoteValue(NoteColumns.PARENT_ID, String.valueOf(Notes.ID_CALL_RECORD_FOLDER));
        folderId = Notes.ID_CALL_RECORD_FOLDER;
    }

    @Override
    public boolean existsInDatabase() {
        return noteId > 0;
    }

    @Override
    public boolean save() {
        if (!isWorthSaving()) {
            return false;
        }

        if (!existsInDatabase()) {
            noteId = createNoteRow();
            if (noteId == 0) {
                Log.e(TAG, "Create new note failed");
                return false;
            }
        }

        return persistNoteValues() && persistDataValues();
    }

    @Override
    public void markDeleted(boolean deleted) {
        this.deleted = deleted;
    }

    @Override
    public void setWorkingText(String text) {
        if (!TextUtils.equals(content, text)) {
            content = text;
            setTextData(DataColumns.CONTENT, content);
        }
    }

    @Override
    public void setAlertDate(long date, boolean set) {
        if (date != alertDate) {
            alertDate = date;
            setNoteValue(NoteColumns.ALERTED_DATE, String.valueOf(alertDate));
        }
    }

    @Override
    public void setBgColorId(int id) {
        if (id != bgColorId) {
            bgColorId = id;
            setNoteValue(NoteColumns.BG_COLOR_ID, String.valueOf(bgColorId));
        }
    }

    @Override
    public void setCheckListMode(int mode) {
        if (this.mode != mode) {
            this.mode = mode;
            setTextData(TextNote.MODE, String.valueOf(mode));
        }
    }

    @Override
    public long getNoteId() {
        return noteId;
    }

    @Override
    public long getFolderId() {
        return folderId;
    }

    @Override
    public long getAlertDate() {
        return alertDate;
    }

    @Override
    public long getModifiedDate() {
        return modifiedDate;
    }

    @Override
    public int getBgColorId() {
        return bgColorId;
    }

    @Override
    public int getCheckListMode() {
        return mode;
    }

    @Override
    public int getWidgetId() {
        return widgetId;
    }

    @Override
    public int getWidgetType() {
        return widgetType;
    }

    @Override
    public String getContent() {
        return content;
    }

    @Override
    public boolean hasClockAlert() {
        return alertDate > 0;
    }

    public void setWidgetType(int type) {
        if (type != widgetType) {
            widgetType = type;
            setNoteValue(NoteColumns.WIDGET_TYPE, String.valueOf(type));
        }
    }

    public void setWidgetId(int id) {
        if (id != widgetId) {
            widgetId = id;
            setNoteValue(NoteColumns.WIDGET_ID, String.valueOf(id));
        }
    }

    private void loadNote() {
        Cursor cursor = contentResolver.query(
                ContentUris.withAppendedId(Notes.CONTENT_NOTE_URI, noteId),
                NOTE_PROJECTION,
                null,
                null,
                null);
        if (cursor == null) {
            throw new IllegalArgumentException("Unable to find note with id " + noteId);
        }
        try {
            if (cursor.moveToFirst()) {
                folderId = cursor.getLong(0);
                alertDate = cursor.getLong(1);
                bgColorId = cursor.getInt(2);
                widgetId = cursor.getInt(3);
                widgetType = cursor.getInt(4);
                modifiedDate = cursor.getLong(5);
            }
        } finally {
            cursor.close();
        }
    }

    private void loadNoteData() {
        Cursor cursor = contentResolver.query(
                Notes.CONTENT_DATA_URI,
                DATA_PROJECTION,
                DataColumns.NOTE_ID + "=?",
                new String[] { String.valueOf(noteId) },
                null);
        if (cursor == null) {
            throw new IllegalArgumentException("Unable to find note data with id " + noteId);
        }
        try {
            while (cursor.moveToNext()) {
                String type = cursor.getString(2);
                if (DataConstants.NOTE.equals(type)) {
                    textDataId = cursor.getLong(0);
                    content = cursor.getString(1);
                    mode = cursor.getInt(3);
                } else if (DataConstants.CALL_NOTE.equals(type)) {
                    callDataId = cursor.getLong(0);
                }
            }
        } finally {
            cursor.close();
        }
    }

    private boolean isWorthSaving() {
        return !deleted
                && !(!existsInDatabase() && TextUtils.isEmpty(content))
                && !(existsInDatabase() && !isDirty());
    }

    private boolean isDirty() {
        return noteDiffValues.size() > 0 || textDataValues.size() > 0 || callDataValues.size() > 0;
    }

    private long createNoteRow() {
        ContentValues values = new ContentValues();
        long createdTime = System.currentTimeMillis();
        values.put(NoteColumns.CREATED_DATE, createdTime);
        values.put(NoteColumns.MODIFIED_DATE, createdTime);
        values.put(NoteColumns.TYPE, Notes.TYPE_NOTE);
        values.put(NoteColumns.LOCAL_MODIFIED, 1);
        values.put(NoteColumns.PARENT_ID, folderId);
        values.putAll(noteDiffValues);
        Uri uri = contentResolver.insert(Notes.CONTENT_NOTE_URI, values);
        noteDiffValues.clear();
        if (uri == null || uri.getPathSegments().size() < 2) {
            return 0;
        }
        try {
            return Long.parseLong(uri.getPathSegments().get(1));
        } catch (NumberFormatException exception) {
            Log.e(TAG, "Failed to parse note id", exception);
            return 0;
        }
    }

    private boolean persistNoteValues() {
        if (noteDiffValues.size() == 0) {
            return true;
        }
        int updated = contentResolver.update(
                ContentUris.withAppendedId(Notes.CONTENT_NOTE_URI, noteId),
                noteDiffValues,
                null,
                null);
        noteDiffValues.clear();
        return updated > 0;
    }

    private boolean persistDataValues() {
        boolean inserted = false;
        ArrayList<ContentProviderOperation> operations = new ArrayList<ContentProviderOperation>();

        if (textDataValues.size() > 0) {
            textDataValues.put(DataColumns.NOTE_ID, noteId);
            if (textDataId == 0) {
                textDataValues.put(DataColumns.MIME_TYPE, TextNote.CONTENT_ITEM_TYPE);
                Uri uri = contentResolver.insert(Notes.CONTENT_DATA_URI, textDataValues);
                if (uri == null || uri.getPathSegments().size() < 2) {
                    return false;
                }
                inserted = true;
                try {
                    textDataId = Long.parseLong(uri.getPathSegments().get(1));
                } catch (NumberFormatException exception) {
                    Log.e(TAG, "Failed to parse text data id", exception);
                    return false;
                }
            } else {
                ContentProviderOperation.Builder builder = ContentProviderOperation.newUpdate(
                        ContentUris.withAppendedId(Notes.CONTENT_DATA_URI, textDataId));
                builder.withValues(textDataValues);
                operations.add(builder.build());
            }
            textDataValues.clear();
        }

        if (callDataValues.size() > 0) {
            callDataValues.put(DataColumns.NOTE_ID, noteId);
            if (callDataId == 0) {
                callDataValues.put(DataColumns.MIME_TYPE, CallNote.CONTENT_ITEM_TYPE);
                Uri uri = contentResolver.insert(Notes.CONTENT_DATA_URI, callDataValues);
                if (uri == null || uri.getPathSegments().size() < 2) {
                    return false;
                }
                inserted = true;
                try {
                    callDataId = Long.parseLong(uri.getPathSegments().get(1));
                } catch (NumberFormatException exception) {
                    Log.e(TAG, "Failed to parse call data id", exception);
                    return false;
                }
            } else {
                ContentProviderOperation.Builder builder = ContentProviderOperation.newUpdate(
                        ContentUris.withAppendedId(Notes.CONTENT_DATA_URI, callDataId));
                builder.withValues(callDataValues);
                operations.add(builder.build());
            }
            callDataValues.clear();
        }

        if (operations.isEmpty()) {
            return true;
        }
        try {
            ContentProviderResult[] results = contentResolver.applyBatch(Notes.AUTHORITY, operations);
            return results != null && results.length > 0;
        } catch (RemoteException exception) {
            Log.e(TAG, "Persist data batch failed", exception);
            return false;
        } catch (OperationApplicationException exception) {
            Log.e(TAG, "Persist data batch failed", exception);
            return false;
        }
    }

    private void setNoteValue(String key, String value) {
        noteDiffValues.put(key, value);
        long now = System.currentTimeMillis();
        noteDiffValues.put(NoteColumns.LOCAL_MODIFIED, 1);
        noteDiffValues.put(NoteColumns.MODIFIED_DATE, now);
        modifiedDate = now;
    }

    private void setTextData(String key, String value) {
        textDataValues.put(key, value);
        long now = System.currentTimeMillis();
        noteDiffValues.put(NoteColumns.LOCAL_MODIFIED, 1);
        noteDiffValues.put(NoteColumns.MODIFIED_DATE, now);
        modifiedDate = now;
    }

    private void setCallData(String key, String value) {
        callDataValues.put(key, value);
        long now = System.currentTimeMillis();
        noteDiffValues.put(NoteColumns.LOCAL_MODIFIED, 1);
        noteDiffValues.put(NoteColumns.MODIFIED_DATE, now);
        modifiedDate = now;
    }
}