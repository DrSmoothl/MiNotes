package net.micode.notes.domain.usecase.list;

import net.micode.notes.domain.repository.NoteRepository;

import java.util.Set;

public final class DeleteNotesUseCase {
    private final NoteRepository noteRepository;

    public DeleteNotesUseCase(NoteRepository noteRepository) {
        this.noteRepository = noteRepository;
    }

    public boolean delete(Set<Long> ids) {
        return noteRepository.deleteNotes(ids);
    }
}