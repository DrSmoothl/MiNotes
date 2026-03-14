package net.micode.notes.domain.usecase.list;

import net.micode.notes.domain.model.WidgetNoteState;
import net.micode.notes.domain.repository.NoteRepository;

public final class GetWidgetNoteStateUseCase {
    private final NoteRepository noteRepository;

    public GetWidgetNoteStateUseCase(NoteRepository noteRepository) {
        this.noteRepository = noteRepository;
    }

    public WidgetNoteState load(int widgetId, int defaultBackgroundId, String emptySnippet) {
        WidgetNoteState state = noteRepository.getWidgetNoteState(widgetId);
        if (state == null || !state.hasBoundNote()) {
            return new WidgetNoteState(0L, defaultBackgroundId, emptySnippet);
        }
        return state;
    }
}