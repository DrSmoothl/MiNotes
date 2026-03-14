package net.micode.notes.domain.usecase.editor;

import net.micode.notes.domain.model.AlarmNotePreview;
import net.micode.notes.domain.repository.NoteEditorRepository;

public final class GetAlarmNotePreviewUseCase {
    private final NoteEditorRepository noteEditorRepository;

    public GetAlarmNotePreviewUseCase(NoteEditorRepository noteEditorRepository) {
        this.noteEditorRepository = noteEditorRepository;
    }

    public AlarmNotePreview load(long noteId) {
        if (noteId <= 0 || !noteEditorRepository.isVisibleNote(noteId)) {
            return null;
        }
        return new AlarmNotePreview(noteId, noteEditorRepository.getSnippet(noteId));
    }
}