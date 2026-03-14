package net.micode.notes.ui;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import net.micode.notes.data.Notes;
import net.micode.notes.domain.model.NoteListItem;
import net.micode.notes.domain.usecase.list.LoadNotesUseCase;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class NotesListViewModel extends ViewModel {
    public static final class Factory implements ViewModelProvider.Factory {
        private final LoadNotesUseCase loadNotesUseCase;

        public Factory(LoadNotesUseCase loadNotesUseCase) {
            this.loadNotesUseCase = loadNotesUseCase;
        }

        @NonNull
        @Override
        @SuppressWarnings("unchecked")
        public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
            if (modelClass.isAssignableFrom(NotesListViewModel.class)) {
                return (T) new NotesListViewModel(loadNotesUseCase);
            }
            throw new IllegalArgumentException("Unknown ViewModel class: " + modelClass.getName());
        }
    }

    private final LoadNotesUseCase loadNotesUseCase;
    private final ExecutorService backgroundExecutor = Executors.newSingleThreadExecutor();
    private final MutableLiveData<NotesListViewState> viewState =
            new MutableLiveData<NotesListViewState>(NotesListViewState.root(
                    Collections.<NoteItemData>emptyList()));

    private NotesListViewState currentState = NotesListViewState.root(
            Collections.<NoteItemData>emptyList());

    public NotesListViewModel(LoadNotesUseCase loadNotesUseCase) {
        this.loadNotesUseCase = loadNotesUseCase;
    }

    public LiveData<NotesListViewState> getViewState() {
        return viewState;
    }

    public NotesListViewState getCurrentState() {
        return currentState;
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

    public void renameCurrentFolder(String name) {
        currentState = currentState.withCurrentFolderName(name);
        viewState.setValue(currentState);
    }

    @Override
    protected void onCleared() {
        backgroundExecutor.shutdown();
    }

    private void loadState(final long folderId, final NotesListViewState.ScreenMode mode,
            final String folderName) {
        backgroundExecutor.execute(new Runnable() {
            @Override
            public void run() {
                List<NoteListItem> items = loadNotesUseCase.load(folderId);
                ArrayList<NoteItemData> uiItems = new ArrayList<NoteItemData>();
                if (items != null) {
                    for (NoteListItem item : items) {
                        uiItems.add(new NoteItemData(item));
                    }
                }
                currentState = new NotesListViewState(folderId, mode, folderName, uiItems);
                viewState.postValue(currentState);
            }
        });
    }
}