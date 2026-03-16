package net.micode.notes.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import net.micode.notes.data.Notes;
import net.micode.notes.domain.model.ExportedTextFile;
import net.micode.notes.domain.model.FolderDestination;
import net.micode.notes.domain.model.NoteEditorSession;
import net.micode.notes.domain.model.NoteListItem;
import net.micode.notes.domain.model.ScheduledReminder;
import net.micode.notes.domain.model.WidgetBinding;
import net.micode.notes.domain.model.WidgetNoteState;
import net.micode.notes.domain.repository.BackupRepository;
import net.micode.notes.domain.repository.IntroductionRepository;
import net.micode.notes.domain.repository.NoteEditorRepository;
import net.micode.notes.domain.repository.NoteRepository;
import net.micode.notes.domain.usecase.editor.StartNoteEditorSessionUseCase;
import net.micode.notes.domain.usecase.list.DeleteNotesUseCase;
import net.micode.notes.domain.usecase.list.ExportNotesUseCase;
import net.micode.notes.domain.usecase.list.FolderManagementUseCase;
import net.micode.notes.domain.usecase.list.LoadNotesUseCase;
import net.micode.notes.domain.usecase.startup.InitializeIntroductionNoteUseCase;

import org.junit.Rule;
import org.junit.Test;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executor;

public final class NotesListViewModelTest {
    @Rule
    public final InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    @Test
    public void openFolder_updatesFolderStateSynchronously() {
        FakeNoteRepository repository = new FakeNoteRepository(
                Collections.singletonList(new NoteListItem(11L, 0L, 0, 0L,
                        false, 0L, 0, 2L, "Hello", Notes.TYPE_NOTE, 0, 0, "", "")));
        repository.userFolderCount = 1;
        NotesListViewModel viewModel = new NotesListViewModel(new LoadNotesUseCase(repository),
                new FolderManagementUseCase(repository), new DeleteNotesUseCase(repository),
                new ExportNotesUseCase(new FakeBackupRepository()), new DirectExecutor());

        NoteItemData folder = new NoteItemData(new NoteListItem(2L, 0L, 0, 0L, false, 0L,
                1, Notes.ID_ROOT_FOLDER, "Projects", Notes.TYPE_FOLDER, 0, 0, "", ""));

        viewModel.openFolder(folder);

        assertEquals(2L, viewModel.getCurrentState().getCurrentFolderId());
        assertEquals(NotesListViewState.ScreenMode.FOLDER, viewModel.getCurrentState().getMode());
        assertEquals("Projects", viewModel.getCurrentState().getCurrentFolderName());
        assertEquals(1, viewModel.getCurrentState().getItemCount());
        assertTrue(viewModel.getCurrentState().hasUserFolders());
    }

    @Test
    public void navigateUp_fromRootReturnsFalse() {
        NotesListViewModel viewModel = new NotesListViewModel(new LoadNotesUseCase(
                new FakeNoteRepository(Collections.<NoteListItem>emptyList())), new Executor() {
            @Override
            public void execute(Runnable command) {
                command.run();
            }
        });

        assertFalse(viewModel.navigateUp());
        assertTrue(viewModel.getCurrentState().isRootMode());
    }

    @Test
    public void requestMoveDestinations_publishesFolderListAction() {
        FakeNoteRepository repository = new FakeNoteRepository(Collections.<NoteListItem>emptyList());
        repository.folderDestinations = Collections.singletonList(new FolderDestination(5L,
                "Archive"));
        repository.userFolderCount = 1;
        NotesListViewModel viewModel = new NotesListViewModel(new LoadNotesUseCase(repository),
                new FolderManagementUseCase(repository), new DeleteNotesUseCase(repository),
                new ExportNotesUseCase(new FakeBackupRepository()), new DirectExecutor());

        viewModel.requestMoveDestinations();

        assertEquals(NotesListViewState.PendingAction.SHOW_MOVE_DESTINATIONS,
                viewModel.getCurrentState().getPendingAction());
        assertEquals(1, viewModel.getCurrentState().getPendingFolderDestinations().size());
        assertEquals("Archive",
                viewModel.getCurrentState().getPendingFolderDestinations().get(0).getName());
    }

        @Test
        public void resolveItemLongClickAction_routesByItemTypeAndChoiceMode() {
        FakeNoteRepository repository = new FakeNoteRepository(Collections.<NoteListItem>emptyList());
        NotesListViewModel viewModel = new NotesListViewModel(new LoadNotesUseCase(repository),
            new FolderManagementUseCase(repository), new DeleteNotesUseCase(repository),
            new ExportNotesUseCase(new FakeBackupRepository()), new DirectExecutor());
        NoteItemData note = new NoteItemData(new NoteListItem(3L, 0L, 0, 0L, false, 0L,
            0, Notes.ID_ROOT_FOLDER, "Draft", Notes.TYPE_NOTE, 0, 0, "", ""));
        NoteItemData folder = new NoteItemData(new NoteListItem(2L, 0L, 0, 0L, false, 0L,
            1, Notes.ID_ROOT_FOLDER, "Projects", Notes.TYPE_FOLDER, 0, 0, "", ""));

        assertEquals(NotesListViewModel.ItemLongClickAction.START_SELECTION,
            viewModel.resolveItemLongClickAction(note, false));
        assertEquals(NotesListViewModel.ItemLongClickAction.SHOW_FOLDER_MENU,
            viewModel.resolveItemLongClickAction(folder, false));
        assertEquals(NotesListViewModel.ItemLongClickAction.NONE,
            viewModel.resolveItemLongClickAction(note, true));
        }

        @Test
        public void shouldShowMoveAction_dependsOnFolderAvailabilityAndParentFolder() {
        FakeNoteRepository repository = new FakeNoteRepository(Collections.<NoteListItem>emptyList());
        repository.userFolderCount = 1;
        NotesListViewModel viewModel = new NotesListViewModel(new LoadNotesUseCase(repository),
            new FolderManagementUseCase(repository), new DeleteNotesUseCase(repository),
            new ExportNotesUseCase(new FakeBackupRepository()), new DirectExecutor());
        NoteItemData normalNote = new NoteItemData(new NoteListItem(3L, 0L, 0, 0L, false, 0L,
            0, Notes.ID_ROOT_FOLDER, "Draft", Notes.TYPE_NOTE, 0, 0, "", ""));
        NoteItemData callRecordNote = new NoteItemData(new NoteListItem(4L, 0L, 0, 0L, false, 0L,
            0, Notes.ID_CALL_RECORD_FOLDER, "Call", Notes.TYPE_NOTE, 0, 0, "", ""));

        assertTrue(viewModel.shouldShowMoveAction(normalNote));
        assertFalse(viewModel.shouldShowMoveAction(callRecordNote));
        assertFalse(new NotesListViewModel(new LoadNotesUseCase(new FakeNoteRepository(
            Collections.<NoteListItem>emptyList())), new DirectExecutor())
            .shouldShowMoveAction(normalNote));
        }

        @Test
        public void resolveItemClickAction_inRootModeRoutesFolderAndNote() {
        FakeNoteRepository repository = new FakeNoteRepository(Collections.<NoteListItem>emptyList());
        NotesListViewModel viewModel = new NotesListViewModel(new LoadNotesUseCase(repository),
            new FolderManagementUseCase(repository), new DeleteNotesUseCase(repository),
            new ExportNotesUseCase(new FakeBackupRepository()), new DirectExecutor());

        NoteItemData folder = new NoteItemData(new NoteListItem(2L, 0L, 0, 0L, false, 0L,
            1, Notes.ID_ROOT_FOLDER, "Projects", Notes.TYPE_FOLDER, 0, 0, "", ""));
        NoteItemData note = new NoteItemData(new NoteListItem(3L, 0L, 0, 0L, false, 0L,
            0, Notes.ID_ROOT_FOLDER, "Draft", Notes.TYPE_NOTE, 0, 0, "", ""));

        assertEquals(NotesListViewModel.ItemClickAction.OPEN_FOLDER,
            viewModel.resolveItemClickAction(folder));
        assertEquals(NotesListViewModel.ItemClickAction.OPEN_NOTE,
            viewModel.resolveItemClickAction(note));
        }

        @Test
        public void resolveItemClickAction_inFolderModeRejectsFolderItem() {
        FakeNoteRepository repository = new FakeNoteRepository(Collections.<NoteListItem>emptyList());
        NotesListViewModel viewModel = new NotesListViewModel(new LoadNotesUseCase(repository),
            new FolderManagementUseCase(repository), new DeleteNotesUseCase(repository),
            new ExportNotesUseCase(new FakeBackupRepository()), new DirectExecutor());
        NoteItemData currentFolder = new NoteItemData(new NoteListItem(9L, 0L, 0, 0L, false, 0L,
            0, Notes.ID_ROOT_FOLDER, "Work", Notes.TYPE_FOLDER, 0, 0, "", ""));
        NoteItemData nestedFolder = new NoteItemData(new NoteListItem(10L, 0L, 0, 0L, false, 0L,
            0, 9L, "Nested", Notes.TYPE_FOLDER, 0, 0, "", ""));
        NoteItemData note = new NoteItemData(new NoteListItem(11L, 0L, 0, 0L, false, 0L,
            0, 9L, "Item", Notes.TYPE_NOTE, 0, 0, "", ""));

        viewModel.openFolder(currentFolder);

        assertEquals(NotesListViewModel.ItemClickAction.NONE,
            viewModel.resolveItemClickAction(nestedFolder));
        assertEquals(NotesListViewModel.ItemClickAction.OPEN_NOTE,
            viewModel.resolveItemClickAction(note));
        }

        @Test
        public void requestCreateNewNote_publishesEditorNavigationForCurrentFolder() {
        FakeNoteRepository repository = new FakeNoteRepository(Collections.<NoteListItem>emptyList());
        NotesListViewModel viewModel = new NotesListViewModel(new LoadNotesUseCase(repository),
            new FolderManagementUseCase(repository), new DeleteNotesUseCase(repository),
            new ExportNotesUseCase(new FakeBackupRepository()), new DirectExecutor());
        NoteItemData currentFolder = new NoteItemData(new NoteListItem(9L, 0L, 0, 0L, false, 0L,
            0, Notes.ID_ROOT_FOLDER, "Work", Notes.TYPE_FOLDER, 0, 0, "", ""));

        viewModel.openFolder(currentFolder);
        viewModel.requestCreateNewNote();

        assertEquals(NotesListViewState.PendingAction.OPEN_NOTE_EDITOR,
            viewModel.getCurrentState().getPendingAction());
        assertTrue(viewModel.getCurrentState().isPendingEditorCreateMode());
        assertEquals(9L, viewModel.getCurrentState().getPendingEditorFolderId());
        }

        @Test
        public void requestOpenExistingNote_publishesEditorNavigationForNoteId() {
        FakeNoteRepository repository = new FakeNoteRepository(Collections.<NoteListItem>emptyList());
        NotesListViewModel viewModel = new NotesListViewModel(new LoadNotesUseCase(repository),
            new FolderManagementUseCase(repository), new DeleteNotesUseCase(repository),
            new ExportNotesUseCase(new FakeBackupRepository()), new DirectExecutor());

        viewModel.requestOpenExistingNote(42L);

        assertEquals(NotesListViewState.PendingAction.OPEN_NOTE_EDITOR,
            viewModel.getCurrentState().getPendingAction());
        assertFalse(viewModel.getCurrentState().isPendingEditorCreateMode());
        assertEquals(42L, viewModel.getCurrentState().getPendingEditorNoteId());
        }

        @Test
        public void requestCreateFolderDialog_publishesDialogRequest() {
        FakeNoteRepository repository = new FakeNoteRepository(Collections.<NoteListItem>emptyList());
        NotesListViewModel viewModel = new NotesListViewModel(new LoadNotesUseCase(repository),
            new FolderManagementUseCase(repository), new DeleteNotesUseCase(repository),
            new ExportNotesUseCase(new FakeBackupRepository()), new DirectExecutor());

        viewModel.requestCreateFolderDialog();

        assertEquals(NotesListViewState.PendingAction.SHOW_FOLDER_NAME_DIALOG,
            viewModel.getCurrentState().getPendingAction());
        assertTrue(viewModel.getCurrentState().isPendingFolderDialogCreateMode());
        assertEquals(0L, viewModel.getCurrentState().getPendingFolderDialogFolderId());
        }

        @Test
        public void requestRenameFolderDialog_publishesDialogRequestWithInitialName() {
        FakeNoteRepository repository = new FakeNoteRepository(Collections.<NoteListItem>emptyList());
        NotesListViewModel viewModel = new NotesListViewModel(new LoadNotesUseCase(repository),
            new FolderManagementUseCase(repository), new DeleteNotesUseCase(repository),
            new ExportNotesUseCase(new FakeBackupRepository()), new DirectExecutor());
        NoteItemData folder = new NoteItemData(new NoteListItem(12L, 0L, 0, 0L, false, 0L,
            0, Notes.ID_ROOT_FOLDER, "Archive", Notes.TYPE_FOLDER, 0, 0, "", ""));

        viewModel.requestRenameFolderDialog(folder);

        assertEquals(NotesListViewState.PendingAction.SHOW_FOLDER_NAME_DIALOG,
            viewModel.getCurrentState().getPendingAction());
        assertFalse(viewModel.getCurrentState().isPendingFolderDialogCreateMode());
        assertEquals(12L, viewModel.getCurrentState().getPendingFolderDialogFolderId());
        assertEquals("Archive", viewModel.getCurrentState().getPendingFolderDialogInitialName());
        }

        @Test
        public void requestDeleteFolderConfirmation_publishesConfirmationAction() {
        FakeNoteRepository repository = new FakeNoteRepository(Collections.<NoteListItem>emptyList());
        NotesListViewModel viewModel = new NotesListViewModel(new LoadNotesUseCase(repository),
            new FolderManagementUseCase(repository), new DeleteNotesUseCase(repository),
            new ExportNotesUseCase(new FakeBackupRepository()), new DirectExecutor());
        NoteItemData folder = new NoteItemData(new NoteListItem(13L, 0L, 0, 0L, false, 0L,
            0, Notes.ID_ROOT_FOLDER, "Projects", Notes.TYPE_FOLDER, 0, 0, "", ""));

        viewModel.requestDeleteFolderConfirmation(folder);

        assertEquals(NotesListViewState.PendingAction.CONFIRM_DELETE_FOLDER,
            viewModel.getCurrentState().getPendingAction());
        assertEquals(13L, viewModel.getCurrentState().getPendingDeleteFolderId());
        assertEquals("Projects", viewModel.getCurrentState().getPendingDeleteFolderName());
        }

    @Test
    public void requestDeleteNotesConfirmation_publishesSelectionCount() {
        FakeNoteRepository repository = new FakeNoteRepository(Collections.<NoteListItem>emptyList());
        NotesListViewModel viewModel = new NotesListViewModel(new LoadNotesUseCase(repository),
                new FolderManagementUseCase(repository), new DeleteNotesUseCase(repository),
                new ExportNotesUseCase(new FakeBackupRepository()), new DirectExecutor());

        viewModel.requestDeleteNotesConfirmation(3);

        assertEquals(NotesListViewState.PendingAction.CONFIRM_DELETE_NOTES,
                viewModel.getCurrentState().getPendingAction());
        assertEquals(3, viewModel.getCurrentState().getPendingAffectedCount());
    }

    @Test
    public void requestExport_publishesExportResultAction() {
        FakeNoteRepository repository = new FakeNoteRepository(Collections.<NoteListItem>emptyList());
        NotesListViewModel viewModel = new NotesListViewModel(new LoadNotesUseCase(repository),
                new FolderManagementUseCase(repository), new DeleteNotesUseCase(repository),
                new ExportNotesUseCase(new FakeBackupRepository()), new DirectExecutor());

        viewModel.requestExport();

        assertEquals(NotesListViewState.PendingAction.SHOW_EXPORT_RESULT,
                viewModel.getCurrentState().getPendingAction());
        assertEquals(ExportedTextFile.ExportState.SUCCESS,
                viewModel.getCurrentState().getPendingExportedFile().getState());
    }

        @Test
        public void createFolder_withDuplicateNamePublishesConflictAction() {
        FakeNoteRepository repository = new FakeNoteRepository(Collections.<NoteListItem>emptyList());
        repository.folderNameExists = true;
        repository.userFolderCount = 1;
        NotesListViewModel viewModel = new NotesListViewModel(new LoadNotesUseCase(repository),
            new FolderManagementUseCase(repository), new DeleteNotesUseCase(repository),
            new ExportNotesUseCase(new FakeBackupRepository()), new DirectExecutor());

        viewModel.createFolder("Archive");

        assertEquals(NotesListViewState.PendingAction.FOLDER_NAME_CONFLICT,
            viewModel.getCurrentState().getPendingAction());
        assertEquals("Archive", viewModel.getCurrentState().getPendingDestinationFolderName());
        }

    @Test
    public void renameFolder_updatesCurrentFolderNameAndPublishesResult() {
        FakeNoteRepository repository = new FakeNoteRepository(Collections.<NoteListItem>emptyList());
        repository.userFolderCount = 1;
        NotesListViewModel viewModel = new NotesListViewModel(new LoadNotesUseCase(repository),
                new FolderManagementUseCase(repository), new DeleteNotesUseCase(repository),
                new ExportNotesUseCase(new FakeBackupRepository()), new DirectExecutor());
        NoteItemData folder = new NoteItemData(new NoteListItem(9L, 0L, 0, 0L, false, 0L,
                0, Notes.ID_ROOT_FOLDER, "Old", Notes.TYPE_FOLDER, 0, 0, "", ""));

        viewModel.openFolder(folder);
        viewModel.renameFolder(9L, "Renamed");

        assertEquals(NotesListViewState.PendingAction.FOLDER_RENAMED,
                viewModel.getCurrentState().getPendingAction());
        assertEquals("Renamed", viewModel.getCurrentState().getCurrentFolderName());
        assertTrue(repository.renameFolderCalled);
    }

    @Test
    public void deleteFolder_publishesWidgetBindingsForRefresh() {
        FakeNoteRepository repository = new FakeNoteRepository(Collections.<NoteListItem>emptyList());
        repository.widgets = Collections.singleton(new WidgetBinding(12, Notes.TYPE_WIDGET_2X));
        repository.userFolderCount = 1;
        NotesListViewModel viewModel = new NotesListViewModel(new LoadNotesUseCase(repository),
                new FolderManagementUseCase(repository), new DeleteNotesUseCase(repository),
                new ExportNotesUseCase(new FakeBackupRepository()), new DirectExecutor());

        viewModel.deleteFolder(10L);

        assertEquals(NotesListViewState.PendingAction.FOLDER_DELETED,
                viewModel.getCurrentState().getPendingAction());
        assertEquals(1, viewModel.getCurrentState().getPendingWidgetBindings().size());
        assertTrue(repository.deleteNotesCalled);
    }

        @Test
        public void initializeIntroduction_failurePublishesPendingAction() {
        FakeNoteRepository repository = new FakeNoteRepository(Collections.<NoteListItem>emptyList());
        NotesListViewModel viewModel = new NotesListViewModel(new LoadNotesUseCase(repository),
            new FolderManagementUseCase(repository), new DeleteNotesUseCase(repository),
            new ExportNotesUseCase(new FakeBackupRepository()),
            new InitializeIntroductionNoteUseCase(new FakeIntroductionRepository(false, ""),
                new StartNoteEditorSessionUseCase(new FakeNoteEditorRepository())),
            new DirectExecutor());

        viewModel.initializeIntroduction(3);

        assertEquals(NotesListViewState.PendingAction.INTRODUCTION_INIT_FAILED,
            viewModel.getCurrentState().getPendingAction());
        assertFalse(viewModel.getCurrentState().hasUserFolders());
        viewModel.markPendingActionHandled(viewModel.getCurrentState().getPendingActionId());
        assertFalse(viewModel.getCurrentState().hasUserFolders());
        }

    private static final class DirectExecutor implements Executor {
        @Override
        public void execute(Runnable command) {
            command.run();
        }
    }

    private static final class FakeNoteRepository implements NoteRepository {
        private final List<NoteListItem> notes;
        private List<FolderDestination> folderDestinations = Collections.emptyList();
        private Set<WidgetBinding> widgets = Collections.emptySet();
        private int userFolderCount;
        private boolean folderNameExists;
        private boolean renameFolderCalled;
        private boolean deleteNotesCalled;

        private FakeNoteRepository(List<NoteListItem> notes) {
            this.notes = notes;
        }

        @Override
        public List<NoteListItem> getNotes(long folderId) {
            return notes;
        }

        @Override
        public List<FolderDestination> getFolderDestinations(long currentFolderId,
                boolean includeRoot) {
            return folderDestinations;
        }

        @Override
        public int getUserFolderCount() {
            return userFolderCount;
        }

        @Override
        public boolean isVisibleFolderName(String name) {
            return folderNameExists;
        }

        @Override
        public long createFolder(String name) {
            return 0;
        }

        @Override
        public boolean renameFolder(long folderId, String name) {
            renameFolderCalled = true;
            return true;
        }

        @Override
        public boolean deleteNotes(Set<Long> ids) {
            deleteNotesCalled = true;
            return true;
        }

        @Override
        public boolean moveNotes(Set<Long> ids, long folderId) {
            return false;
        }

        @Override
        public Set<WidgetBinding> getWidgetsForFolder(long folderId) {
            return widgets;
        }

        @Override
        public List<ScheduledReminder> getUpcomingReminders(long currentTimeMillis) {
            return Collections.emptyList();
        }

        @Override
        public WidgetNoteState getWidgetNoteState(int widgetId) {
            return null;
        }

        @Override
        public void clearWidgetBindings(int[] widgetIds) {
        }
    }

    private static final class FakeBackupRepository implements BackupRepository {
        @Override
        public ExportedTextFile exportToText() {
            return new ExportedTextFile(ExportedTextFile.ExportState.SUCCESS, "notes.txt",
                    "/tmp");
        }
    }

    private static final class FakeIntroductionRepository implements IntroductionRepository {
        private final boolean introductionCreated;
        private final String introductionText;

        private FakeIntroductionRepository(boolean introductionCreated, String introductionText) {
            this.introductionCreated = introductionCreated;
            this.introductionText = introductionText;
        }

        @Override
        public boolean isIntroductionCreated() {
            return introductionCreated;
        }

        @Override
        public String loadIntroductionText() {
            return introductionText;
        }

        @Override
        public void markIntroductionCreated() {
        }
    }

    private static final class FakeNoteEditorRepository implements NoteEditorRepository {
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
            return new FakeNoteEditorSession();
        }

        @Override
        public NoteEditorSession createSession(long folderId, int widgetId, int widgetType,
                int defaultBgColorId) {
            return new FakeNoteEditorSession();
        }

        @Override
        public NoteEditorSession createCallRecordSession(long folderId, int widgetId,
                int widgetType, int defaultBgColorId, String phoneNumber, long callDate) {
            return new FakeNoteEditorSession();
        }

        @Override
        public boolean deleteNote(long noteId) {
            return true;
        }
    }

    private static final class FakeNoteEditorSession implements NoteEditorSession {
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
        }

        @Override
        public void setAlertDate(long date, boolean set) {
        }

        @Override
        public void setBgColorId(int id) {
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
            return 0;
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
            return 0;
        }

        @Override
        public int getCheckListMode() {
            return 0;
        }

        @Override
        public int getWidgetId() {
            return 0;
        }

        @Override
        public int getWidgetType() {
            return 0;
        }

        @Override
        public String getContent() {
            return "";
        }

        @Override
        public boolean hasClockAlert() {
            return false;
        }
    }
}