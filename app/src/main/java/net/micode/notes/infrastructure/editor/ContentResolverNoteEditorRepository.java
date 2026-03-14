package net.micode.notes.infrastructure.editor;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;

import net.micode.notes.data.Notes;
import net.micode.notes.data.Notes.CallNote;
import net.micode.notes.data.Notes.NoteColumns;
import net.micode.notes.domain.model.NoteEditorSession;
import net.micode.notes.domain.repository.NoteEditorRepository;

public final class ContentResolverNoteEditorRepository implements NoteEditorRepository {
    private final Context appContext;
    private final ContentResolver contentResolver;

    public ContentResolverNoteEditorRepository(Context context) {
        this.appContext = context.getApplicationContext();
        this.contentResolver = appContext.getContentResolver();
    }

    @Override
    public boolean isVisibleNote(long noteId) {
        Cursor cursor = contentResolver.query(
                ContentUris.withAppendedId(Notes.CONTENT_NOTE_URI, noteId),
                new String[] { NoteColumns.ID },
                NoteColumns.TYPE + "=? AND " + NoteColumns.PARENT_ID + "<>?",
                new String[] { String.valueOf(Notes.TYPE_NOTE), String.valueOf(Notes.ID_TRASH_FOLER) },
                null);
        if (cursor == null) {
            return false;
        }
        try {
            return cursor.moveToFirst();
        } finally {
            cursor.close();
        }
    }

    @Override
    public long findCallRecordNoteId(String phoneNumber, long callDate) {
        Cursor cursor = contentResolver.query(
                Notes.CONTENT_DATA_URI,
                new String[] { CallNote.NOTE_ID },
                CallNote.CALL_DATE + "=? AND " + CallNote.MIME_TYPE + "=? AND PHONE_NUMBERS_EQUAL("
                        + CallNote.PHONE_NUMBER + ", ?)",
                new String[] {
                        String.valueOf(callDate),
                        CallNote.CONTENT_ITEM_TYPE,
                        phoneNumber
                },
                null);
        if (cursor == null) {
            return 0;
        }
        try {
            return cursor.moveToFirst() ? cursor.getLong(0) : 0;
        } finally {
            cursor.close();
        }
    }

    @Override
    public NoteEditorSession loadSession(long noteId) {
        return ContentResolverNoteEditorSession.load(appContext, noteId);
    }

    @Override
    public NoteEditorSession createSession(long folderId, int widgetId, int widgetType,
            int defaultBgColorId) {
        return ContentResolverNoteEditorSession.create(appContext, folderId, widgetId, widgetType,
                defaultBgColorId);
    }

    @Override
    public NoteEditorSession createCallRecordSession(long folderId, int widgetId, int widgetType,
            int defaultBgColorId, String phoneNumber, long callDate) {
        ContentResolverNoteEditorSession session = ContentResolverNoteEditorSession.create(
                appContext, folderId, widgetId, widgetType, defaultBgColorId);
        session.convertToCallNote(phoneNumber, callDate);
        return session;
    }

    @Override
    public boolean deleteNote(long noteId) {
        return contentResolver.delete(
                ContentUris.withAppendedId(Notes.CONTENT_NOTE_URI, noteId),
                null,
                null) > 0;
    }
}