package net.micode.notes.domain.usecase.startup;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import net.micode.notes.data.Notes;
import net.micode.notes.domain.model.NoteEditorSession;
import net.micode.notes.domain.repository.IntroductionRepository;
import net.micode.notes.domain.repository.NoteEditorRepository;
import net.micode.notes.domain.usecase.editor.StartNoteEditorSessionUseCase;

import org.junit.Test;

public final class InitializeIntroductionNoteUseCaseTest {
    @Test
    public void initializeIfNeeded_createsIntroductionAndMarksPreference() {
        FakeIntroductionRepository introductionRepository = new FakeIntroductionRepository();
        FakeNoteEditorSession noteEditorSession = new FakeNoteEditorSession();
        InitializeIntroductionNoteUseCase useCase = new InitializeIntroductionNoteUseCase(
                introductionRepository,
                new StartNoteEditorSessionUseCase(new FakeNoteEditorRepository(noteEditorSession)));

        InitializeIntroductionNoteUseCase.Result result = useCase.initializeIfNeeded(3);

        assertEquals(InitializeIntroductionNoteUseCase.Result.CREATED, result);
        assertTrue(introductionRepository.markedCreated);
        assertEquals("welcome", noteEditorSession.content);
        assertEquals(Notes.ID_ROOT_FOLDER, noteEditorSession.folderId);
    }

    @Test
    public void initializeIfNeeded_skipsWhenAlreadyCreated() {
        FakeIntroductionRepository introductionRepository = new FakeIntroductionRepository();
        introductionRepository.introductionCreated = true;
        InitializeIntroductionNoteUseCase useCase = new InitializeIntroductionNoteUseCase(
                introductionRepository,
                new StartNoteEditorSessionUseCase(new FakeNoteEditorRepository(
                        new FakeNoteEditorSession())));

        InitializeIntroductionNoteUseCase.Result result = useCase.initializeIfNeeded(3);

        assertEquals(InitializeIntroductionNoteUseCase.Result.SKIPPED, result);
    }

    private static final class FakeIntroductionRepository implements IntroductionRepository {
        private boolean introductionCreated;
        private boolean markedCreated;

        @Override
        public boolean isIntroductionCreated() {
            return introductionCreated;
        }

        @Override
        public String loadIntroductionText() {
            return "welcome";
        }

        @Override
        public void markIntroductionCreated() {
            markedCreated = true;
            introductionCreated = true;
        }
    }

    private static final class FakeNoteEditorRepository implements NoteEditorRepository {
        private final FakeNoteEditorSession noteEditorSession;

        private FakeNoteEditorRepository(FakeNoteEditorSession noteEditorSession) {
            this.noteEditorSession = noteEditorSession;
        }

        @Override
        public boolean isVisibleNote(long noteId) {
            return true;
        }

        @Override
        public String getSnippet(long noteId) {
            return "";
        }

        @Override
        public long findCallRecordNoteId(String phoneNumber, long callDate) {
            return 0;
        }

        @Override
        public NoteEditorSession loadSession(long noteId) {
            return noteEditorSession;
        }

        @Override
        public NoteEditorSession createSession(long folderId, int widgetId, int widgetType,
                int defaultBgColorId) {
            noteEditorSession.folderId = folderId;
            noteEditorSession.widgetId = widgetId;
            noteEditorSession.widgetType = widgetType;
            noteEditorSession.backgroundColorId = defaultBgColorId;
            return noteEditorSession;
        }

        @Override
        public NoteEditorSession createCallRecordSession(long folderId, int widgetId,
                int widgetType, int defaultBgColorId, String phoneNumber, long callDate) {
            return createSession(folderId, widgetId, widgetType, defaultBgColorId);
        }

        @Override
        public boolean deleteNote(long noteId) {
            return true;
        }
    }

    private static final class FakeNoteEditorSession implements NoteEditorSession {
        private long folderId;
        private int widgetId;
        private int widgetType;
        private int backgroundColorId;
        private String content = "";

        @Override
        public boolean existsInDatabase() {
            return false;
        }

        @Override
        public boolean save() {
            return true;
        }

        @Override
        public void markDeleted(boolean deleted) {
        }

        @Override
        public void setWorkingText(String text) {
            content = text;
        }

        @Override
        public void setAlertDate(long date, boolean set) {
        }

        @Override
        public void setBgColorId(int id) {
            backgroundColorId = id;
        }

        @Override
        public void setCheckListMode(int mode) {
        }

        @Override
        public long getNoteId() {
            return 0;
        }

        @Override
        public long getFolderId() {
            return folderId;
        }

        @Override
        public long getAlertDate() {
            return 0;
        }

        @Override
        public long getModifiedDate() {
            return 0;
        }

        @Override
        public int getBgColorId() {
            return backgroundColorId;
        }

        @Override
        public int getCheckListMode() {
            return 0;
        }

        @Override
        public int getWidgetId() {
            return widgetId;
        }

        @Override
        public int getWidgetType() {
            return widgetType;
        }

        @Override
        public String getContent() {
            return content;
        }

        @Override
        public boolean hasClockAlert() {
            return false;
        }
    }
}