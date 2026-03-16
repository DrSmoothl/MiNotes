package net.micode.notes.domain.usecase.startup;

import net.micode.notes.data.Notes;
import net.micode.notes.domain.model.NoteEditorSession;
import net.micode.notes.domain.repository.IntroductionRepository;
import net.micode.notes.domain.usecase.editor.StartNoteEditorSessionUseCase;

public final class InitializeIntroductionNoteUseCase {
    private static final int INVALID_WIDGET_ID = -1;

    public enum Result {
        SKIPPED,
        CREATED,
        FAILED
    }

    private final IntroductionRepository introductionRepository;
    private final StartNoteEditorSessionUseCase startNoteEditorSessionUseCase;

    public InitializeIntroductionNoteUseCase(IntroductionRepository introductionRepository,
            StartNoteEditorSessionUseCase startNoteEditorSessionUseCase) {
        this.introductionRepository = introductionRepository;
        this.startNoteEditorSessionUseCase = startNoteEditorSessionUseCase;
    }

    public Result initializeIfNeeded(int defaultBackgroundColorId) {
        if (introductionRepository.isIntroductionCreated()) {
            return Result.SKIPPED;
        }
        String introductionText = introductionRepository.loadIntroductionText();
        if (introductionText == null || introductionText.length() == 0) {
            return Result.FAILED;
        }

        NoteEditorSession noteSession = startNoteEditorSessionUseCase.startNew(
                Notes.ID_ROOT_FOLDER,
                INVALID_WIDGET_ID,
                Notes.TYPE_WIDGET_INVALIDE,
                defaultBackgroundColorId);
        noteSession.setWorkingText(introductionText);
        if (!noteSession.save()) {
            return Result.FAILED;
        }

        introductionRepository.markIntroductionCreated();
        return Result.CREATED;
    }
}