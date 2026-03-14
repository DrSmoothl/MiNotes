package net.micode.notes.domain.usecase.list;

import net.micode.notes.domain.model.NoteListItem;
import net.micode.notes.domain.repository.NoteRepository;

import java.util.List;

public final class LoadNotesUseCase {
    private final NoteRepository noteRepository;

    public LoadNotesUseCase(NoteRepository noteRepository) {
        this.noteRepository = noteRepository;
    }

    public List<NoteListItem> load(long folderId) {
        return noteRepository.getNotes(folderId);
    }
}