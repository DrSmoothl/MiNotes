package net.micode.notes.ui;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import net.micode.notes.domain.model.NoteEditorSession;
import net.micode.notes.domain.usecase.editor.DeleteNoteUseCase;
import net.micode.notes.domain.usecase.editor.StartNoteEditorSessionUseCase;

public final class NoteEditViewModel extends ViewModel {
    public static final class Factory implements ViewModelProvider.Factory {
        private final StartNoteEditorSessionUseCase startNoteEditorSessionUseCase;
        private final DeleteNoteUseCase deleteNoteUseCase;

        public Factory(StartNoteEditorSessionUseCase startNoteEditorSessionUseCase,
                DeleteNoteUseCase deleteNoteUseCase) {
            this.startNoteEditorSessionUseCase = startNoteEditorSessionUseCase;
            this.deleteNoteUseCase = deleteNoteUseCase;
        }

        @NonNull
        @Override
        @SuppressWarnings("unchecked")
        public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
            if (modelClass.isAssignableFrom(NoteEditViewModel.class)) {
                return (T) new NoteEditViewModel(startNoteEditorSessionUseCase, deleteNoteUseCase);
            }
            throw new IllegalArgumentException("Unknown ViewModel class: " + modelClass.getName());
        }
    }

    private final StartNoteEditorSessionUseCase startNoteEditorSessionUseCase;
    private final DeleteNoteUseCase deleteNoteUseCase;
    private final MutableLiveData<NoteEditViewState> viewState = new MutableLiveData<NoteEditViewState>();

    private NoteEditorSession noteSession;
    private String userQuery = "";

    public NoteEditViewModel(StartNoteEditorSessionUseCase startNoteEditorSessionUseCase,
            DeleteNoteUseCase deleteNoteUseCase) {
        this.startNoteEditorSessionUseCase = startNoteEditorSessionUseCase;
        this.deleteNoteUseCase = deleteNoteUseCase;
    }

    public LiveData<NoteEditViewState> getViewState() {
        return viewState;
    }

    public NoteEditViewState getCurrentState() {
        return viewState.getValue();
    }

    public NoteEditorSession getNoteSession() {
        return noteSession;
    }

    public String getUserQuery() {
        return userQuery;
    }

    public boolean openExisting(long noteId, String query) {
        this.userQuery = query == null ? "" : query;
        noteSession = startNoteEditorSessionUseCase.openExisting(noteId);
        publishState();
        return noteSession != null;
    }

    public void restoreExisting(long noteId) {
        noteSession = startNoteEditorSessionUseCase.openExisting(noteId);
        publishState();
    }

    public void startNew(long folderId, int widgetId, int widgetType, int backgroundColorId) {
        this.userQuery = "";
        noteSession = startNoteEditorSessionUseCase.startNew(folderId, widgetId, widgetType,
                backgroundColorId);
        publishState();
    }

    public void startForCallRecord(long folderId, int widgetId, int widgetType,
            int backgroundColorId, String phoneNumber, long callDate) {
        this.userQuery = "";
        noteSession = startNoteEditorSessionUseCase.startForCallRecord(folderId, widgetId,
                widgetType, backgroundColorId, phoneNumber, callDate);
        publishState();
    }

    public void setBackgroundColor(int backgroundColorId) {
        if (noteSession == null) {
            return;
        }
        noteSession.setBgColorId(backgroundColorId);
        publishState();
    }

    public void setCheckListMode(int checkListMode) {
        if (noteSession == null) {
            return;
        }
        noteSession.setCheckListMode(checkListMode);
        publishState();
    }

    public void setAlertDate(long alertDate, boolean set) {
        if (noteSession == null) {
            return;
        }
        noteSession.setAlertDate(alertDate, set);
        publishState();
    }

    public boolean save() {
        if (noteSession == null) {
            return false;
        }
        boolean saved = noteSession.save();
        publishState();
        return saved;
    }

    public boolean deleteCurrent() {
        if (noteSession == null) {
            return false;
        }
        boolean deleted = true;
        if (noteSession.existsInDatabase()) {
            deleted = deleteNoteUseCase.delete(noteSession.getNoteId());
        }
        noteSession.markDeleted(true);
        publishState();
        return deleted;
    }

    public void refreshState() {
        publishState();
    }

    private void publishState() {
        if (noteSession == null) {
            viewState.setValue(null);
            return;
        }
        viewState.setValue(new NoteEditViewState(
                noteSession.existsInDatabase(),
                noteSession.getModifiedDate(),
                noteSession.getBgColorId(),
                noteSession.hasClockAlert(),
                noteSession.getAlertDate(),
                noteSession.getFolderId(),
                noteSession.getWidgetId(),
                noteSession.getWidgetType(),
                noteSession.getCheckListMode()));
    }
}