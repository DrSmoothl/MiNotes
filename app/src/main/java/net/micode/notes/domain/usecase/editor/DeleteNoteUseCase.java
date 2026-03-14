package net.micode.notes.domain.usecase.editor;

import net.micode.notes.domain.repository.NoteEditorRepository;

public final class DeleteNoteUseCase {
    private final NoteEditorRepository noteEditorRepository;

    public DeleteNoteUseCase(NoteEditorRepository noteEditorRepository) {
        this.noteEditorRepository = noteEditorRepository;
    }

    public boolean delete(long noteId) {
        return noteId > 0 && noteEditorRepository.deleteNote(noteId);
    }
}