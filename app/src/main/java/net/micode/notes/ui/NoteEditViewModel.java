package net.micode.notes.ui;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import android.appwidget.AppWidgetManager;

import net.micode.notes.data.Notes;
import net.micode.notes.domain.model.CheckListText;
import net.micode.notes.domain.model.NoteEditorSession;
import net.micode.notes.domain.service.ReminderScheduler;
import net.micode.notes.domain.service.WidgetNotifier;
import net.micode.notes.domain.usecase.editor.DeleteNoteUseCase;
import net.micode.notes.domain.usecase.editor.StartNoteEditorSessionUseCase;

public final class NoteEditViewModel extends ViewModel {
    public static final class Factory implements ViewModelProvider.Factory {
        private final StartNoteEditorSessionUseCase startNoteEditorSessionUseCase;
        private final DeleteNoteUseCase deleteNoteUseCase;
        private final ReminderScheduler reminderScheduler;
        private final WidgetNotifier widgetNotifier;

        public Factory(StartNoteEditorSessionUseCase startNoteEditorSessionUseCase,
                DeleteNoteUseCase deleteNoteUseCase,
                ReminderScheduler reminderScheduler,
                WidgetNotifier widgetNotifier) {
            this.startNoteEditorSessionUseCase = startNoteEditorSessionUseCase;
            this.deleteNoteUseCase = deleteNoteUseCase;
            this.reminderScheduler = reminderScheduler;
            this.widgetNotifier = widgetNotifier;
        }

        @NonNull
        @Override
        @SuppressWarnings("unchecked")
        public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
            if (modelClass.isAssignableFrom(NoteEditViewModel.class)) {
                return (T) new NoteEditViewModel(startNoteEditorSessionUseCase, deleteNoteUseCase,
                        reminderScheduler, widgetNotifier);
            }
            throw new IllegalArgumentException("Unknown ViewModel class: " + modelClass.getName());
        }
    }

    private final StartNoteEditorSessionUseCase startNoteEditorSessionUseCase;
    private final DeleteNoteUseCase deleteNoteUseCase;
    private final ReminderScheduler reminderScheduler;
    private final WidgetNotifier widgetNotifier;
    private final MutableLiveData<NoteEditViewState> viewState = new MutableLiveData<NoteEditViewState>();

    private NoteEditorSession noteSession;
    private String userQuery = "";
    private long nextPendingActionId = 1L;
    private NoteEditViewState.PendingAction pendingAction = NoteEditViewState.PendingAction.NONE;
    private boolean pendingActionSetsResultOk;
    private long pendingActionFolderId;

    public NoteEditViewModel(StartNoteEditorSessionUseCase startNoteEditorSessionUseCase,
            DeleteNoteUseCase deleteNoteUseCase,
            ReminderScheduler reminderScheduler,
            WidgetNotifier widgetNotifier) {
        this.startNoteEditorSessionUseCase = startNoteEditorSessionUseCase;
        this.deleteNoteUseCase = deleteNoteUseCase;
        this.reminderScheduler = reminderScheduler;
        this.widgetNotifier = widgetNotifier;
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

    public void updateWorkingText(String workingText) {
        if (noteSession == null) {
            return;
        }
        noteSession.setWorkingText(workingText == null ? "" : workingText);
        publishState();
    }

    public boolean changeCheckListMode(String workingText, boolean hasCheckedItems, int newMode) {
        if (noteSession == null) {
            return false;
        }
        String updatedText = newMode == Notes.TextNote.MODE_CHECK_LIST
                ? workingText
                : CheckListText.normalizeForPlainText(workingText, hasCheckedItems);
        noteSession.setWorkingText(updatedText == null ? "" : updatedText);
        noteSession.setCheckListMode(newMode);
        publishState();
        return true;
    }

    public void setAlertDate(long alertDate, boolean set) {
        if (noteSession == null) {
            return;
        }
        noteSession.setAlertDate(alertDate, set);
        publishState();
    }

    public boolean applyReminder(String workingText, long alertDate, boolean set) {
        if (noteSession == null) {
            return false;
        }
        noteSession.setWorkingText(workingText == null ? "" : workingText);
        noteSession.setAlertDate(alertDate, set);
        if (!noteSession.existsInDatabase() && !saveInternal()) {
            publishState();
            return false;
        }
        if (noteSession.getNoteId() <= 0) {
            publishState();
            return false;
        }
        reminderScheduler.updateReminder(noteSession.getNoteId(), alertDate, set);
        publishState();
        return true;
    }

    public boolean save(String workingText) {
        if (noteSession == null) {
            return false;
        }
        noteSession.setWorkingText(workingText == null ? "" : workingText);
        boolean saved = saveInternal();
        publishState();
        return saved;
    }

    public void requestClose(String workingText) {
        boolean saved = save(workingText);
        long folderId = noteSession == null ? 0L : noteSession.getFolderId();
        dispatchPendingAction(NoteEditViewState.PendingAction.CLOSE_EDITOR, saved, folderId);
    }

    public void requestCreateNew(String workingText) {
        boolean saved = save(workingText);
        long folderId = noteSession == null ? 0L : noteSession.getFolderId();
        dispatchPendingAction(NoteEditViewState.PendingAction.OPEN_NEW_NOTE, saved, folderId);
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
        refreshWidgetIfNeeded();
        publishState();
        return deleted;
    }

    public void requestDeleteAndClose() {
        deleteCurrent();
        long folderId = noteSession == null ? 0L : noteSession.getFolderId();
        dispatchPendingAction(NoteEditViewState.PendingAction.CLOSE_EDITOR, false, folderId);
    }

    public void markPendingActionHandled(long actionId) {
        NoteEditViewState state = viewState.getValue();
        if (state == null || state.getPendingActionId() != actionId) {
            return;
        }
        pendingAction = NoteEditViewState.PendingAction.NONE;
        pendingActionSetsResultOk = false;
        pendingActionFolderId = 0L;
        publishState();
    }

    public void refreshState() {
        publishState();
    }

    private boolean saveInternal() {
        boolean saved = noteSession.save();
        if (saved) {
            refreshWidgetIfNeeded();
        }
        return saved;
    }

    private void refreshWidgetIfNeeded() {
        if (noteSession.getWidgetId() == AppWidgetManager.INVALID_APPWIDGET_ID
                || noteSession.getWidgetType() == Notes.TYPE_WIDGET_INVALIDE) {
            return;
        }
        widgetNotifier.refresh(noteSession.getWidgetId(), noteSession.getWidgetType());
    }

    private void dispatchPendingAction(NoteEditViewState.PendingAction action,
            boolean setResultOk, long folderId) {
        pendingAction = action;
        pendingActionSetsResultOk = setResultOk;
        pendingActionFolderId = folderId;
        nextPendingActionId++;
        publishState();
    }

    private void publishState() {
        if (noteSession == null) {
            viewState.setValue(null);
            return;
        }
        String content = noteSession.getContent();
        boolean hasContent = content != null && content.trim().length() > 0;
        viewState.setValue(new NoteEditViewState(
                noteSession.existsInDatabase(),
                noteSession.getModifiedDate(),
                noteSession.getBgColorId(),
                noteSession.hasClockAlert(),
                noteSession.getAlertDate(),
                noteSession.getFolderId(),
                noteSession.getWidgetId(),
                noteSession.getWidgetType(),
                noteSession.getCheckListMode(),
                content,
                hasContent,
                hasContent,
                noteSession.existsInDatabase() || hasContent,
                hasContent,
                true,
                pendingAction == NoteEditViewState.PendingAction.NONE ? 0L : nextPendingActionId - 1,
                pendingAction,
                pendingActionSetsResultOk,
                pendingActionFolderId));
    }
}