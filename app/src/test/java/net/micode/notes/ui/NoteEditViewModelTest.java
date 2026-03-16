package net.micode.notes.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import net.micode.notes.data.Notes;
import net.micode.notes.domain.model.NoteEditorSession;
import net.micode.notes.domain.repository.NoteEditorRepository;
import net.micode.notes.domain.service.ReminderScheduler;
import net.micode.notes.domain.service.WidgetNotifier;
import net.micode.notes.domain.usecase.editor.DeleteNoteUseCase;
import net.micode.notes.domain.usecase.editor.StartNoteEditorSessionUseCase;

import org.junit.Rule;
import org.junit.Test;

public final class NoteEditViewModelTest {
    @Rule
    public final InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    @Test
    public void requestCreateNew_publishesOpenNewActionInState() {
        FakeNoteEditorSession session = new FakeNoteEditorSession();
        session.folderId = 99L;
        FakeNoteEditorRepository repository = new FakeNoteEditorRepository(session);
        NoteEditViewModel viewModel = new NoteEditViewModel(
                new StartNoteEditorSessionUseCase(repository),
                new DeleteNoteUseCase(repository),
                new FakeReminderScheduler(),
                new FakeWidgetNotifier());

        viewModel.startNew(99L, 0, Notes.TYPE_WIDGET_INVALIDE, 0);
        viewModel.requestCreateNew("hello");

        NoteEditViewState state = viewModel.getCurrentState();
        assertEquals(NoteEditViewState.PendingAction.OPEN_NEW_NOTE, state.getPendingAction());
        assertTrue(state.pendingActionSetsResultOk());
        assertEquals(99L, state.getPendingActionFolderId());
        assertEquals("hello", state.getContent());
    }

    @Test
    public void launch_openExistingUsesLaunchRequestQuery() {
        FakeNoteEditorSession session = new FakeNoteEditorSession();
        FakeNoteEditorRepository repository = new FakeNoteEditorRepository(session);
        NoteEditViewModel viewModel = new NoteEditViewModel(
                new StartNoteEditorSessionUseCase(repository),
                new DeleteNoteUseCase(repository),
                new FakeReminderScheduler(),
                new FakeWidgetNotifier());

        boolean launched = viewModel.launch(NoteEditLaunchRequest.openExisting(42L, "needle"));

        assertTrue(launched);
        assertEquals(42L, viewModel.getNoteSession().getNoteId());
        assertEquals("needle", viewModel.getUserQuery());
        assertTrue(viewModel.getCurrentState().isExistingNote());
    }

    @Test
    public void launch_createOrEditStartsCallRecordSessionWhenCallMetadataExists() {
        FakeNoteEditorSession session = new FakeNoteEditorSession();
        FakeNoteEditorRepository repository = new FakeNoteEditorRepository(session);
        NoteEditViewModel viewModel = new NoteEditViewModel(
                new StartNoteEditorSessionUseCase(repository),
                new DeleteNoteUseCase(repository),
                new FakeReminderScheduler(),
                new FakeWidgetNotifier());

        boolean launched = viewModel.launch(NoteEditLaunchRequest.createOrEdit(
                7L, 9, Notes.TYPE_WIDGET_2X, 3, "10086", 12345L));

        assertTrue(launched);
        assertEquals(7L, viewModel.getNoteSession().getFolderId());
        assertEquals(9, viewModel.getNoteSession().getWidgetId());
        assertEquals(Notes.TYPE_WIDGET_2X, viewModel.getNoteSession().getWidgetType());
        assertEquals(3, viewModel.getNoteSession().getBgColorId());
        assertEquals("", viewModel.getUserQuery());
        assertTrue(repository.createCallRecordSessionInvoked);
    }

    @Test
    public void markPendingActionHandled_clearsPendingAction() {
        FakeNoteEditorSession session = new FakeNoteEditorSession();
        session.folderId = 7L;
        FakeNoteEditorRepository repository = new FakeNoteEditorRepository(session);
        NoteEditViewModel viewModel = new NoteEditViewModel(
                new StartNoteEditorSessionUseCase(repository),
                new DeleteNoteUseCase(repository),
                new FakeReminderScheduler(),
                new FakeWidgetNotifier());

        viewModel.startNew(7L, 0, Notes.TYPE_WIDGET_INVALIDE, 0);
        viewModel.requestClose("draft");
        long actionId = viewModel.getCurrentState().getPendingActionId();

        viewModel.markPendingActionHandled(actionId);

        assertEquals(NoteEditViewState.PendingAction.NONE,
                viewModel.getCurrentState().getPendingAction());
        assertFalse(viewModel.getCurrentState().pendingActionSetsResultOk());
    }

    private static final class FakeNoteEditorRepository implements NoteEditorRepository {
        private final FakeNoteEditorSession session;
        private boolean createCallRecordSessionInvoked;

        private FakeNoteEditorRepository(FakeNoteEditorSession session) {
            this.session = session;
        }

        @Override
        public boolean isVisibleNote(long noteId) {
            return true;
        }

        @Override
        public String getSnippet(long noteId) {
            return session.content;
        }

        @Override
        public long findCallRecordNoteId(String phoneNumber, long callDate) {
            return 0;
        }

        @Override
        public NoteEditorSession loadSession(long noteId) {
            session.noteId = noteId;
            session.existsInDatabase = noteId > 0;
            return session;
        }

        @Override
        public NoteEditorSession createSession(long folderId, int widgetId, int widgetType,
                int defaultBgColorId) {
            session.folderId = folderId;
            session.widgetId = widgetId;
            session.widgetType = widgetType;
            session.bgColorId = defaultBgColorId;
            return session;
        }

        @Override
        public NoteEditorSession createCallRecordSession(long folderId, int widgetId,
                int widgetType, int defaultBgColorId, String phoneNumber, long callDate) {
            createCallRecordSessionInvoked = true;
            return createSession(folderId, widgetId, widgetType, defaultBgColorId);
        }

        @Override
        public boolean deleteNote(long noteId) {
            session.deleted = true;
            return true;
        }
    }

    private static final class FakeNoteEditorSession implements NoteEditorSession {
        private boolean existsInDatabase;
        private boolean deleted;
        private long noteId;
        private long folderId;
        private long alertDate;
        private long modifiedDate;
        private int bgColorId;
        private int checkListMode;
        private int widgetId;
        private int widgetType = Notes.TYPE_WIDGET_INVALIDE;
        private String content = "";

        @Override
        public boolean existsInDatabase() {
            return existsInDatabase;
        }

        @Override
        public boolean save() {
            existsInDatabase = true;
            if (noteId == 0L) {
                noteId = 1L;
            }
            modifiedDate++;
            return !deleted;
        }

        @Override
        public void markDeleted(boolean deleted) {
            this.deleted = deleted;
        }

        @Override
        public void setWorkingText(String text) {
            content = text;
        }

        @Override
        public void setAlertDate(long date, boolean set) {
            alertDate = set ? date : 0L;
        }

        @Override
        public void setBgColorId(int id) {
            bgColorId = id;
        }

        @Override
        public void setCheckListMode(int mode) {
            checkListMode = mode;
        }

        @Override
        public long getNoteId() {
            return noteId;
        }

        @Override
        public long getFolderId() {
            return folderId;
        }

        @Override
        public long getAlertDate() {
            return alertDate;
        }

        @Override
        public long getModifiedDate() {
            return modifiedDate;
        }

        @Override
        public int getBgColorId() {
            return bgColorId;
        }

        @Override
        public int getCheckListMode() {
            return checkListMode;
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
            return alertDate > 0;
        }
    }

    private static final class FakeReminderScheduler implements ReminderScheduler {
        @Override
        public void updateReminder(long noteId, long date, boolean enabled) {
        }
    }

    private static final class FakeWidgetNotifier implements WidgetNotifier {
        @Override
        public void refresh(int widgetId, int widgetType) {
        }
    }
}