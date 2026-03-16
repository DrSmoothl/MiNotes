package net.micode.notes.domain.usecase.editor;

import net.micode.notes.domain.model.NoteEditorSession;
import net.micode.notes.domain.repository.NoteEditorRepository;

public final class StartNoteEditorSessionUseCase {
    private final NoteEditorRepository noteEditorRepository;

    public StartNoteEditorSessionUseCase(NoteEditorRepository noteEditorRepository) {
        this.noteEditorRepository = noteEditorRepository;
    }

    public NoteEditorSession openExisting(long noteId) {
        if (!noteEditorRepository.isVisibleNote(noteId)) {
            return null;
        }
        return noteEditorRepository.loadSession(noteId);
    }

    public NoteEditorSession startNew(long folderId, int widgetId, int widgetType,
            int defaultBgColorId) {
        return noteEditorRepository.createSession(folderId, widgetId, widgetType,
                defaultBgColorId);
    }

    public NoteEditorSession startForCallRecord(long folderId, int widgetId, int widgetType,
            int defaultBgColorId, String phoneNumber, long callDate) {
        if (phoneNumber == null || phoneNumber.length() == 0 || callDate == 0) {
            return startNew(folderId, widgetId, widgetType, defaultBgColorId);
        }
        long existingId = noteEditorRepository.findCallRecordNoteId(phoneNumber, callDate);
        if (existingId > 0) {
            return noteEditorRepository.loadSession(existingId);
        }
        return noteEditorRepository.createCallRecordSession(folderId, widgetId, widgetType,
                defaultBgColorId, phoneNumber, callDate);
    }
}