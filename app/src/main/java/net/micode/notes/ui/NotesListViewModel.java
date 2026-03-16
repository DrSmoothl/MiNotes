package net.micode.notes.ui;

import androidx.annotation.NonNull;
import androidx.lifecycle.FlowLiveDataConversions;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import net.micode.notes.data.Notes;
import net.micode.notes.domain.model.ExportedTextFile;
import net.micode.notes.domain.model.FolderDestination;
import net.micode.notes.domain.model.NoteListItem;
import net.micode.notes.domain.model.WidgetBinding;
import net.micode.notes.domain.usecase.list.DeleteNotesUseCase;
import net.micode.notes.domain.usecase.list.ExportNotesUseCase;
import net.micode.notes.domain.usecase.list.FolderManagementUseCase;
import net.micode.notes.domain.usecase.list.LoadNotesUseCase;
import net.micode.notes.domain.usecase.startup.InitializeIntroductionNoteUseCase;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import kotlinx.coroutines.flow.MutableStateFlow;
import kotlinx.coroutines.flow.StateFlow;
import kotlinx.coroutines.flow.StateFlowKt;

public final class NotesListViewModel extends ViewModel {
    public enum ItemClickAction {
        NONE,
        OPEN_NOTE,
        OPEN_FOLDER
    }

    public static final class Factory implements ViewModelProvider.Factory {
        private final LoadNotesUseCase loadNotesUseCase;
        private final FolderManagementUseCase folderManagementUseCase;
        private final DeleteNotesUseCase deleteNotesUseCase;
        private final ExportNotesUseCase exportNotesUseCase;
        private final InitializeIntroductionNoteUseCase initializeIntroductionNoteUseCase;
        private final Executor backgroundExecutor;

        public Factory(LoadNotesUseCase loadNotesUseCase,
                FolderManagementUseCase folderManagementUseCase,
                DeleteNotesUseCase deleteNotesUseCase,
                ExportNotesUseCase exportNotesUseCase,
                InitializeIntroductionNoteUseCase initializeIntroductionNoteUseCase) {
            this(loadNotesUseCase, folderManagementUseCase, deleteNotesUseCase,
                    exportNotesUseCase, initializeIntroductionNoteUseCase,
                    Executors.newSingleThreadExecutor());
        }

        public Factory(LoadNotesUseCase loadNotesUseCase,
                FolderManagementUseCase folderManagementUseCase,
                DeleteNotesUseCase deleteNotesUseCase,
                ExportNotesUseCase exportNotesUseCase,
                InitializeIntroductionNoteUseCase initializeIntroductionNoteUseCase,
                Executor backgroundExecutor) {
            this.loadNotesUseCase = loadNotesUseCase;
            this.folderManagementUseCase = folderManagementUseCase;
            this.deleteNotesUseCase = deleteNotesUseCase;
            this.exportNotesUseCase = exportNotesUseCase;
            this.initializeIntroductionNoteUseCase = initializeIntroductionNoteUseCase;
            this.backgroundExecutor = backgroundExecutor;
        }

        @NonNull
        @Override
        @SuppressWarnings("unchecked")
        public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
            if (modelClass.isAssignableFrom(NotesListViewModel.class)) {
                return (T) new NotesListViewModel(loadNotesUseCase, folderManagementUseCase,
                        deleteNotesUseCase, exportNotesUseCase,
                        initializeIntroductionNoteUseCase, backgroundExecutor);
            }
            throw new IllegalArgumentException("Unknown ViewModel class: " + modelClass.getName());
        }
    }

    private final LoadNotesUseCase loadNotesUseCase;
    private final FolderManagementUseCase folderManagementUseCase;
    private final DeleteNotesUseCase deleteNotesUseCase;
    private final ExportNotesUseCase exportNotesUseCase;
    private final InitializeIntroductionNoteUseCase initializeIntroductionNoteUseCase;
    private final Executor backgroundExecutor;
    private final MutableStateFlow<NotesListViewState> viewStateFlow =
            StateFlowKt.MutableStateFlow(NotesListViewState.root(
                Collections.<NoteItemData>emptyList()));
    private final LiveData<NotesListViewState> viewState =
            FlowLiveDataConversions.asLiveData(viewStateFlow);

    private NotesListViewState currentState = NotesListViewState.root(
            Collections.<NoteItemData>emptyList());
    private long nextPendingActionId = 1L;

    public NotesListViewModel(LoadNotesUseCase loadNotesUseCase) {
        this(loadNotesUseCase, null, null, null, null, Executors.newSingleThreadExecutor());
    }

    public NotesListViewModel(LoadNotesUseCase loadNotesUseCase, Executor backgroundExecutor) {
        this(loadNotesUseCase, null, null, null, null, backgroundExecutor);
    }

    public NotesListViewModel(LoadNotesUseCase loadNotesUseCase,
            FolderManagementUseCase folderManagementUseCase,
            DeleteNotesUseCase deleteNotesUseCase,
            ExportNotesUseCase exportNotesUseCase,
            Executor backgroundExecutor) {
        this(loadNotesUseCase, folderManagementUseCase, deleteNotesUseCase, exportNotesUseCase,
            null, backgroundExecutor);
        }

        public NotesListViewModel(LoadNotesUseCase loadNotesUseCase,
            FolderManagementUseCase folderManagementUseCase,
            DeleteNotesUseCase deleteNotesUseCase,
            ExportNotesUseCase exportNotesUseCase,
            InitializeIntroductionNoteUseCase initializeIntroductionNoteUseCase,
            Executor backgroundExecutor) {
        this.loadNotesUseCase = loadNotesUseCase;
        this.folderManagementUseCase = folderManagementUseCase;
        this.deleteNotesUseCase = deleteNotesUseCase;
        this.exportNotesUseCase = exportNotesUseCase;
        this.initializeIntroductionNoteUseCase = initializeIntroductionNoteUseCase;
        this.backgroundExecutor = backgroundExecutor;
    }

    public LiveData<NotesListViewState> getViewState() {
        return viewState;
    }

    public StateFlow<NotesListViewState> getViewStateFlow() {
        return viewStateFlow;
    }

    public NotesListViewState getCurrentState() {
        return currentState;
    }

    public ItemClickAction resolveItemClickAction(NoteItemData item) {
        if (item == null) {
            return ItemClickAction.NONE;
        }
        if (currentState.isRootMode()) {
            if (item.getType() == Notes.TYPE_FOLDER || item.getType() == Notes.TYPE_SYSTEM) {
                return ItemClickAction.OPEN_FOLDER;
            }
            if (item.getType() == Notes.TYPE_NOTE) {
                return ItemClickAction.OPEN_NOTE;
            }
            return ItemClickAction.NONE;
        }
        if (item.getType() == Notes.TYPE_NOTE) {
            return ItemClickAction.OPEN_NOTE;
        }
        return ItemClickAction.NONE;
    }

    public void initializeIntroduction(int defaultBackgroundColorId) {
        if (initializeIntroductionNoteUseCase == null) {
            return;
        }
        InitializeIntroductionNoteUseCase.Result result =
                initializeIntroductionNoteUseCase.initializeIfNeeded(defaultBackgroundColorId);
        if (result == InitializeIntroductionNoteUseCase.Result.FAILED) {
            publishPendingAction(currentState, NotesListViewState.PendingAction.INTRODUCTION_INIT_FAILED,
                    Collections.<FolderDestination>emptyList(), null, false, 0, null,
                    Collections.<WidgetBinding>emptyList());
        }
    }

    public void refresh() {
        loadState(currentState.getCurrentFolderId(), currentState.getMode(),
                currentState.getCurrentFolderName());
    }

    public void openFolder(NoteItemData item) {
        NotesListViewState.ScreenMode mode = item.getId() == Notes.ID_CALL_RECORD_FOLDER
                ? NotesListViewState.ScreenMode.CALL_RECORD
                : NotesListViewState.ScreenMode.FOLDER;
        String folderName = mode == NotesListViewState.ScreenMode.CALL_RECORD
                ? null : item.getSnippet();
        loadState(item.getId(), mode, folderName);
    }

    public boolean navigateUp() {
        if (currentState.isRootMode()) {
            return false;
        }
        loadState(Notes.ID_ROOT_FOLDER, NotesListViewState.ScreenMode.ROOT, null);
        return true;
    }

    public void createFolder(final String name) {
        if (folderManagementUseCase == null) {
            return;
        }
        final NotesListViewState stateSnapshot = currentState;
        backgroundExecutor.execute(new Runnable() {
            @Override
            public void run() {
                if (folderManagementUseCase.folderNameExists(name)) {
                    publishPendingAction(stateSnapshot, stateSnapshot.getItems(),
                            NotesListViewState.PendingAction.FOLDER_NAME_CONFLICT,
                            Collections.<FolderDestination>emptyList(), null, false,
                            0, name, Collections.<WidgetBinding>emptyList());
                    return;
                }
                long folderId = folderManagementUseCase.createFolder(name);
                List<NoteItemData> items = loadItems(stateSnapshot.getCurrentFolderId());
                publishPendingAction(stateSnapshot, items,
                        NotesListViewState.PendingAction.FOLDER_CREATED,
                        Collections.<FolderDestination>emptyList(), null, folderId > 0L,
                        0, name, Collections.<WidgetBinding>emptyList());
            }
        });
    }

    public void renameFolder(final long folderId, final String name) {
        if (folderManagementUseCase == null) {
            return;
        }
        final NotesListViewState stateSnapshot = currentState;
        backgroundExecutor.execute(new Runnable() {
            @Override
            public void run() {
                if (folderManagementUseCase.folderNameExists(name)) {
                    publishPendingAction(stateSnapshot, stateSnapshot.getItems(),
                            NotesListViewState.PendingAction.FOLDER_NAME_CONFLICT,
                            Collections.<FolderDestination>emptyList(), null, false,
                            0, name, Collections.<WidgetBinding>emptyList());
                    return;
                }
                boolean renamed = folderManagementUseCase.renameFolder(folderId, name);
                List<NoteItemData> items = loadItems(stateSnapshot.getCurrentFolderId());
                NotesListViewState updatedState = new NotesListViewState(
                        stateSnapshot.getCurrentFolderId(), stateSnapshot.getMode(),
                        stateSnapshot.getCurrentFolderName(), items,
                        hasUserFolders());
                if (renamed && folderId == stateSnapshot.getCurrentFolderId()) {
                    updatedState = updatedState.withCurrentFolderName(name);
                }
                publishPendingAction(updatedState,
                        NotesListViewState.PendingAction.FOLDER_RENAMED,
                        Collections.<FolderDestination>emptyList(), null, renamed,
                        0, name, Collections.<WidgetBinding>emptyList());
            }
        });
    }

    public void deleteFolder(final long folderId) {
        if (folderManagementUseCase == null) {
            return;
        }
        final NotesListViewState stateSnapshot = currentState;
        backgroundExecutor.execute(new Runnable() {
            @Override
            public void run() {
                List<WidgetBinding> bindings = new ArrayList<WidgetBinding>(
                        folderManagementUseCase.deleteFolder(folderId));
                List<NoteItemData> items = loadItems(stateSnapshot.getCurrentFolderId());
                publishPendingAction(stateSnapshot, items,
                        NotesListViewState.PendingAction.FOLDER_DELETED,
                        Collections.<FolderDestination>emptyList(), null, true,
                        0, null, bindings);
            }
        });
    }

    public void requestMoveDestinations() {
        if (folderManagementUseCase == null) {
            return;
        }
        final NotesListViewState stateSnapshot = currentState;
        backgroundExecutor.execute(new Runnable() {
            @Override
            public void run() {
                List<FolderDestination> folders = folderManagementUseCase.getFolderDestinations(
                        stateSnapshot.getCurrentFolderId(), !stateSnapshot.isRootMode());
                publishPendingAction(stateSnapshot, stateSnapshot.getItems(),
                        NotesListViewState.PendingAction.SHOW_MOVE_DESTINATIONS, folders,
                        null, !folders.isEmpty(), 0, null,
                        Collections.<WidgetBinding>emptyList());
            }
        });
    }

    public void requestExport() {
        if (exportNotesUseCase == null) {
            return;
        }
        final NotesListViewState stateSnapshot = currentState;
        backgroundExecutor.execute(new Runnable() {
            @Override
            public void run() {
                ExportedTextFile result = exportNotesUseCase.exportToText();
                publishPendingAction(stateSnapshot, stateSnapshot.getItems(),
                        NotesListViewState.PendingAction.SHOW_EXPORT_RESULT,
                        Collections.<FolderDestination>emptyList(), result,
                        result != null && result.getState() == ExportedTextFile.ExportState.SUCCESS,
                        0, null, Collections.<WidgetBinding>emptyList());
            }
        });
    }

    public void moveNotes(final Set<Long> noteIds, final long folderId,
            final String folderName, final int affectedCount) {
        if (folderManagementUseCase == null) {
            return;
        }
        final NotesListViewState stateSnapshot = currentState;
        backgroundExecutor.execute(new Runnable() {
            @Override
            public void run() {
                boolean moved = folderManagementUseCase.moveNotes(noteIds, folderId);
                List<NoteItemData> items = loadItems(stateSnapshot.getCurrentFolderId());
                publishPendingAction(stateSnapshot, items,
                        NotesListViewState.PendingAction.MOVE_COMPLETED,
                        Collections.<FolderDestination>emptyList(), null, moved,
                        affectedCount, folderName,
                        Collections.<WidgetBinding>emptyList());
            }
        });
    }

    public void deleteNotes(final Set<Long> noteIds) {
        if (deleteNotesUseCase == null) {
            return;
        }
        final NotesListViewState stateSnapshot = currentState;
        backgroundExecutor.execute(new Runnable() {
            @Override
            public void run() {
                boolean deleted = deleteNotesUseCase.delete(noteIds);
                List<NoteItemData> items = loadItems(stateSnapshot.getCurrentFolderId());
                publishPendingAction(stateSnapshot, items,
                        NotesListViewState.PendingAction.DELETE_COMPLETED,
                        Collections.<FolderDestination>emptyList(), null, deleted, 0, null,
                        Collections.<WidgetBinding>emptyList());
            }
        });
    }

    public void markPendingActionHandled(long actionId) {
        if (currentState.getPendingActionId() != actionId) {
            return;
        }
        currentState = currentState.withoutPendingAction();
        viewStateFlow.setValue(currentState);
    }

    @Override
    protected void onCleared() {
        if (backgroundExecutor instanceof ExecutorService) {
            ((ExecutorService) backgroundExecutor).shutdown();
        }
    }

    private void loadState(final long folderId, final NotesListViewState.ScreenMode mode,
            final String folderName) {
        backgroundExecutor.execute(new Runnable() {
            @Override
            public void run() {
                List<NoteItemData> uiItems = loadItems(folderId);
                currentState = new NotesListViewState(folderId, mode, folderName, uiItems,
                        hasUserFolders());
                viewStateFlow.setValue(currentState);
            }
        });
    }

    private boolean hasUserFolders() {
        return folderManagementUseCase != null && folderManagementUseCase.getUserFolderCount() > 0;
    }

    private List<NoteItemData> loadItems(long folderId) {
        List<NoteListItem> items = loadNotesUseCase.load(folderId);
        ArrayList<NoteItemData> uiItems = new ArrayList<NoteItemData>();
        if (items != null) {
            for (NoteListItem item : items) {
                uiItems.add(new NoteItemData(item));
            }
        }
        return uiItems;
    }

    private void publishPendingAction(NotesListViewState stateSnapshot, List<NoteItemData> items,
            NotesListViewState.PendingAction action,
            List<FolderDestination> folderDestinations, ExportedTextFile exportedFile,
            boolean operationSucceeded, int affectedCount, String destinationFolderName) {
        publishPendingAction(stateSnapshot, items, action, folderDestinations, exportedFile,
                operationSucceeded, affectedCount, destinationFolderName,
                Collections.<WidgetBinding>emptyList());
    }

    private void publishPendingAction(NotesListViewState stateSnapshot, List<NoteItemData> items,
            NotesListViewState.PendingAction action,
            List<FolderDestination> folderDestinations, ExportedTextFile exportedFile,
            boolean operationSucceeded, int affectedCount, String destinationFolderName,
            List<WidgetBinding> widgetBindings) {
        currentState = new NotesListViewState(stateSnapshot.getCurrentFolderId(),
                stateSnapshot.getMode(), stateSnapshot.getCurrentFolderName(), items,
                hasUserFolders(), nextPendingActionId++, action, folderDestinations,
                exportedFile, operationSucceeded, affectedCount, destinationFolderName,
                widgetBindings);
        viewStateFlow.setValue(currentState);
    }

    private void publishPendingAction(NotesListViewState state,
            NotesListViewState.PendingAction action,
            List<FolderDestination> folderDestinations, ExportedTextFile exportedFile,
            boolean operationSucceeded, int affectedCount, String destinationFolderName,
            List<WidgetBinding> widgetBindings) {
        currentState = new NotesListViewState(state.getCurrentFolderId(), state.getMode(),
                state.getCurrentFolderName(), state.getItems(), hasUserFolders(),
                nextPendingActionId++, action, folderDestinations, exportedFile,
                operationSucceeded, affectedCount, destinationFolderName, widgetBindings);
        viewStateFlow.setValue(currentState);
    }
}