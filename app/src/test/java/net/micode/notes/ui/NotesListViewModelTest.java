package net.micode.notes.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import net.micode.notes.data.Notes;
import net.micode.notes.domain.model.ExportedTextFile;
import net.micode.notes.domain.model.FolderDestination;
import net.micode.notes.domain.model.NoteListItem;
import net.micode.notes.domain.model.ScheduledReminder;
import net.micode.notes.domain.model.WidgetBinding;
import net.micode.notes.domain.model.WidgetNoteState;
import net.micode.notes.domain.repository.BackupRepository;
import net.micode.notes.domain.repository.NoteRepository;
import net.micode.notes.domain.usecase.list.DeleteNotesUseCase;
import net.micode.notes.domain.usecase.list.ExportNotesUseCase;
import net.micode.notes.domain.usecase.list.FolderManagementUseCase;
import net.micode.notes.domain.usecase.list.LoadNotesUseCase;

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
            return false;
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
}