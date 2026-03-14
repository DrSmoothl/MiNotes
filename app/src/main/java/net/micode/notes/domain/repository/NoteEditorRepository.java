package net.micode.notes.domain.repository;

import net.micode.notes.domain.model.NoteEditorSession;

public interface NoteEditorRepository {
    boolean isVisibleNote(long noteId);

    String getSnippet(long noteId);

    long findCallRecordNoteId(String phoneNumber, long callDate);

    NoteEditorSession loadSession(long noteId);

    NoteEditorSession createSession(long folderId, int widgetId, int widgetType,
            int defaultBgColorId);

    NoteEditorSession createCallRecordSession(long folderId, int widgetId, int widgetType,
            int defaultBgColorId, String phoneNumber, long callDate);

    boolean deleteNote(long noteId);
}