package net.micode.notes.domain.usecase.list;

import net.micode.notes.domain.repository.NoteRepository;

public final class ClearWidgetBindingsUseCase {
    private final NoteRepository noteRepository;

    public ClearWidgetBindingsUseCase(NoteRepository noteRepository) {
        this.noteRepository = noteRepository;
    }

    public void clear(int[] widgetIds) {
        noteRepository.clearWidgetBindings(widgetIds);
    }
}