/*
 * Copyright (c) 2010-2011, The MiCode Open Source Community (www.micode.net)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.micode.notes.ui;

import android.app.Dialog;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.HapticFeedbackConstants;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;
import android.view.ContextThemeWrapper;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.appcompat.view.ActionMode;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import net.micode.notes.R;
import net.micode.notes.data.Notes;
import net.micode.notes.domain.model.ExportedTextFile;
import net.micode.notes.domain.model.FolderDestination;
import net.micode.notes.domain.model.WidgetBinding;
import net.micode.notes.domain.service.WidgetNotifier;
import net.micode.notes.inject.NotesApplicationGraph;
import net.micode.notes.tool.ResourceParser;
import net.micode.notes.ui.NotesListAdapter.AppWidgetAttribute;

import java.util.HashSet;
import java.util.List;

public class NotesListActivity extends AppCompatActivity implements OnClickListener,
    NotesListAdapter.NoteItemListener {
    private static final int MENU_FOLDER_DELETE = 0;

    private static final int MENU_FOLDER_VIEW = 1;

    private static final int MENU_FOLDER_CHANGE_NAME = 2;

    private NotesListAdapter mNotesListAdapter;

    private RecyclerView mNotesListView;

    private View mAddNewNote;

    private MaterialToolbar mToolbar;

    private TextView mHeaderTitle;

    private TextView mHeaderSubtitle;

    private ModeCallback mModeCallBack;

    private WidgetNotifier mWidgetNotifier;

    private NotesListViewModel mListViewModel;

    private static final String TAG = "NotesListActivity";

    public static final int NOTES_LISTVIEW_SCROLL_RATE = 30;

    private NoteItemData mFocusNoteDataItem;
    private HashSet<AppWidgetAttribute> mPendingDeletedWidgets;
    private Dialog mFolderNameDialog;
    private EditText mFolderNameEditText;

    private final ActivityResultLauncher<Intent> mNoteEditorLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == RESULT_OK) {
                            startAsyncNotesListQuery();
                        }
                    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.note_list);
        initResources();
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                handleBackNavigation();
            }
        });

        mListViewModel.initializeIntroduction(ResourceParser.RED);
    }

    @Override
    protected void onStart() {
        super.onStart();
        mListViewModel.refresh();
    }

    private void initResources() {
        NotesApplicationGraph graph = new NotesApplicationGraph(this);
        mWidgetNotifier = graph.widgetNotifier();
        mListViewModel = new ViewModelProvider(this,
            new NotesListViewModel.Factory(graph.loadNotesUseCase(), graph.folderManagementUseCase(),
                    graph.deleteNotesUseCase(), graph.exportNotesUseCase(),
                    graph.initializeIntroductionNoteUseCase()))
                .get(NotesListViewModel.class);
        mToolbar = (MaterialToolbar) findViewById(R.id.top_app_bar);
        setSupportActionBar(mToolbar);
        EdgeToEdgeInsets.applyTopInset(this, mToolbar);
        mToolbar.setNavigationOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                handleBackNavigation();
            }
        });
        mNotesListView = (RecyclerView) findViewById(R.id.notes_list);
        EdgeToEdgeInsets.applyBottomInsetToPadding(this, mNotesListView);
        mNotesListView.setLayoutManager(new LinearLayoutManager(this));
        mNotesListAdapter = new NotesListAdapter(this, this);
        mNotesListView.setAdapter(mNotesListAdapter);
        mHeaderTitle = (TextView) findViewById(R.id.header_title);
        mHeaderSubtitle = (TextView) findViewById(R.id.header_subtitle);
        findViewById(R.id.button_search_notes).setOnClickListener(this);
        findViewById(R.id.button_settings).setOnClickListener(this);
        mAddNewNote = findViewById(R.id.btn_new_note);
        EdgeToEdgeInsets.applyBottomInsetToMargin(this, mAddNewNote);
        mAddNewNote.setOnClickListener(this);
        mModeCallBack = new ModeCallback();
        mListViewModel.getViewState().observe(this, this::renderViewState);
    }

    private class ModeCallback implements ActionMode.Callback {
        private ActionMode mActionMode;
        private MenuItem mMoveMenu;

        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            getMenuInflater().inflate(R.menu.note_list_options, menu);
            mMoveMenu = menu.findItem(R.id.move);
            renderMoveActionVisibility(getSelectionUiState());
            mActionMode = mode;
            enterSelectionMode();
            renderActionMode(mode, menu, getSelectionUiState());
            return true;
        }

        private void renderActionMode(ActionMode mode, Menu menu,
                NotesListViewModel.SelectionUiState selectionUiState) {
            renderMoveActionVisibility(selectionUiState);
            String format = getResources().getString(R.string.menu_select_title,
                selectionUiState.getSelectedCount());
            mode.setTitle(format);
            MenuItem item = menu.findItem(R.id.action_select_all);
            if (item != null) {
                item.setTitle(selectionUiState.shouldUseDeselectAllLabel()
                        ? R.string.menu_deselect_all : R.string.menu_select_all);
            }
        }

        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            renderActionMode(mode, menu, getSelectionUiState());
            return true;
        }

        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            NotesListViewModel.SelectionUiState selectionUiState = getSelectionUiState();
            if (selectionUiState.shouldFinishActionMode()) {
                Toast.makeText(NotesListActivity.this, getString(R.string.menu_select_none),
                        Toast.LENGTH_SHORT).show();
                return true;
            }

            switch (item.getItemId()) {
                case R.id.delete:
                    requestDeleteSelectedNotesConfirmation(selectionUiState);
                    return true;
                case R.id.move:
                    requestMoveSelectedNotes();
                    return true;
                case R.id.action_select_all:
                    mNotesListAdapter.selectAll(selectionUiState.shouldSelectAllOnToggle());
                    renderSelectionState();
                    return true;
                default:
                    return false;
            }
        }

        public void onDestroyActionMode(ActionMode mode) {
            exitSelectionMode();
            mActionMode = null;
        }

        public void finishActionMode() {
            if (mActionMode != null) {
                mActionMode.finish();
            }
        }

        public void toggleSelection(int position) {
            mNotesListAdapter.toggleSelection(position);
            if (mActionMode == null) {
                return;
            }
            NotesListViewModel.SelectionUiState selectionUiState = getSelectionUiState();
            if (selectionUiState.shouldFinishActionMode()) {
                finishActionMode();
            } else {
                renderActionMode(mActionMode, mActionMode.getMenu(), selectionUiState);
            }
        }

        private void renderSelectionState() {
            if (mActionMode == null) {
                return;
            }
            renderActionMode(mActionMode, mActionMode.getMenu(), getSelectionUiState());
        }

        private void renderMoveActionVisibility(NotesListViewModel.SelectionUiState selectionUiState) {
            if (mMoveMenu != null) {
                mMoveMenu.setVisible(selectionUiState.shouldShowMoveAction());
            }
        }

        private void enterSelectionMode() {
            mNotesListAdapter.setChoiceMode(true);
            mAddNewNote.setVisibility(View.GONE);
        }

        private void exitSelectionMode() {
            mNotesListAdapter.setChoiceMode(false);
            mAddNewNote.setVisibility(View.VISIBLE);
        }

        private NotesListViewModel.SelectionUiState getSelectionUiState() {
            NotesListAdapter.SelectionSnapshot selection = mNotesListAdapter.getSelectionSnapshot();
            return mListViewModel.buildSelectionUiState(mFocusNoteDataItem,
                    selection.getSelectedCount(), selection.isAllSelected());
        }
    }

    private void startAsyncNotesListQuery() {
        mListViewModel.refresh();
    }

    private void showFolderListMenu(final List<FolderDestination> folders) {
        final NotesListAdapter.SelectionSnapshot selection = mNotesListAdapter.getSelectionSnapshot();
        AlertDialog.Builder builder = new AlertDialog.Builder(NotesListActivity.this);
        builder.setTitle(R.string.menu_title_select_folder);
        final String[] names = new String[folders.size()];
        for (int i = 0; i < folders.size(); i++) {
            names[i] = folders.get(i).getName();
        }
        builder.setItems(names, new DialogInterface.OnClickListener() {

            public void onClick(DialogInterface dialog, int which) {
                moveSelectedNotes(selection, folders.get(which));
            }
        });
        builder.show();
    }

    private void createNewNote() {
        mListViewModel.requestCreateNewNote();
    }

    private void batchDelete() {
        NotesListAdapter.SelectionSnapshot selection = mNotesListAdapter.getSelectionSnapshot();
        mPendingDeletedWidgets = selection.getSelectedWidgets();
        deleteSelectedNotes(selection);
    }

    private void requestDeleteSelectedNotesConfirmation(
            NotesListViewModel.SelectionUiState selectionUiState) {
        mListViewModel.requestDeleteNotesConfirmation(selectionUiState.getSelectedCount());
    }

    private void requestMoveSelectedNotes() {
        mListViewModel.requestMoveDestinations();
    }

    private void moveSelectedNotes(NotesListAdapter.SelectionSnapshot selection,
            FolderDestination destination) {
        mListViewModel.moveNotes(selection.getSelectedItemIds(), destination.getId(),
                destination.getName(), selection.getSelectedCount());
    }

    private void deleteSelectedNotes(NotesListAdapter.SelectionSnapshot selection) {
        mListViewModel.deleteNotes(selection.getSelectedItemIds());
    }

    private void openNode(NoteItemData data) {
        mListViewModel.requestOpenExistingNote(data.getId());
    }

    private void openFolder(NoteItemData data) {
        mListViewModel.openFolder(data);
    }

    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_new_note:
                createNewNote();
                break;
            case R.id.button_search_notes:
                openSearchScreen();
                break;
            case R.id.button_settings:
                startPreferenceActivity();
                break;
            default:
                break;
        }
    }

    private void showSoftInput(View view) {
        InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (inputMethodManager != null) {
            view.requestFocus();
            inputMethodManager.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT);
        }
    }

    private void hideSoftInput(View view) {
        InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    private void showCreateOrModifyFolderDialog(final boolean create, final long folderId,
            String initialName) {
        final MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_edit_text, null);
        final EditText etName = (EditText) view.findViewById(R.id.et_foler_name);
        mFolderNameEditText = etName;
        showSoftInput(etName);
        if (!create) {
            if (!TextUtils.isEmpty(initialName)) {
                etName.setText(initialName);
                builder.setTitle(getString(R.string.menu_folder_change_name));
            } else {
                Log.e(TAG, "Missing folder dialog initial name");
                return;
            }
        } else {
            etName.setText("");
            builder.setTitle(this.getString(R.string.menu_create_folder));
        }

        builder.setPositiveButton(android.R.string.ok, null);
        builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                hideSoftInput(etName);
                clearFolderDialogState();
            }
        });

        final AlertDialog dialog = builder.setView(view).show();
        mFolderNameDialog = dialog;
        final Button positive = (Button)dialog.findViewById(android.R.id.button1);
        positive.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                hideSoftInput(etName);
                String name = etName.getText().toString();
                if (!create) {
                    if (!TextUtils.isEmpty(name)) {
                        mListViewModel.renameFolder(folderId, name);
                    }
                } else if (!TextUtils.isEmpty(name)) {
                    mListViewModel.createFolder(name);
                }
            }
        });

        if (TextUtils.isEmpty(etName.getText())) {
            positive.setEnabled(false);
        }
        /**
         * When the name edit text is null, disable the positive button
         */
        etName.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // TODO Auto-generated method stub

            }

            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (TextUtils.isEmpty(etName.getText())) {
                    positive.setEnabled(false);
                } else {
                    positive.setEnabled(true);
                }
            }

            public void afterTextChanged(Editable s) {
                // TODO Auto-generated method stub

            }
        });
    }

    private void handleBackNavigation() {
        if (!mListViewModel.navigateUp()) {
            finish();
        }
    }

    private void updateWidget(int appWidgetId, int appWidgetType) {
        mWidgetNotifier.refresh(appWidgetId, appWidgetType);
        setResult(RESULT_OK);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.clear();
        NotesListViewState.ScreenMode mode = mListViewModel.getCurrentState().getMode();
        if (mode == NotesListViewState.ScreenMode.ROOT) {
            getMenuInflater().inflate(R.menu.note_list, menu);
        } else if (mode == NotesListViewState.ScreenMode.FOLDER) {
            getMenuInflater().inflate(R.menu.sub_folder, menu);
        } else if (mode == NotesListViewState.ScreenMode.CALL_RECORD) {
            getMenuInflater().inflate(R.menu.call_record_folder, menu);
        } else {
            Log.e(TAG, "Wrong state:" + mode);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_new_folder: {
                mListViewModel.requestCreateFolderDialog();
                break;
            }
            case R.id.menu_export_text: {
                mListViewModel.requestExport();
                break;
            }
            case R.id.menu_setting: {
                startPreferenceActivity();
                break;
            }
            case R.id.menu_new_note: {
                createNewNote();
                break;
            }
            case R.id.menu_search:
                openSearchScreen();
                break;
            default:
                break;
        }
        return true;
    }

    @Override
    public boolean onSearchRequested() {
        openSearchScreen();
        return true;
    }

    private void openSearchScreen() {
        startActivity(new Intent(this, SearchNotesActivity.class));
    }

    private void startPreferenceActivity() {
        Intent intent = new Intent(this, NotesPreferenceActivity.class);
        startActivity(intent);
    }

    private void renderViewState(NotesListViewState state) {
        mNotesListAdapter.submitList(state.getItems());
        mAddNewNote.setVisibility(state.isCallRecordMode() ? View.GONE : View.VISIBLE);
        renderTopBar(state);
        invalidateOptionsMenu();
        handlePendingAction(state);
    }

    private void handlePendingAction(NotesListViewState state) {
        if (!state.hasPendingAction()) {
            return;
        }
        long actionId = state.getPendingActionId();
        switch (state.getPendingAction()) {
            case INTRODUCTION_INIT_FAILED:
                markPendingActionHandled(actionId);
                Log.e(TAG, "Initialize introduction note error");
                return;
            case OPEN_NOTE_EDITOR:
                markPendingActionHandled(actionId);
                launchNoteEditor(state);
                return;
            case SHOW_FOLDER_NAME_DIALOG:
                markPendingActionHandled(actionId);
                showCreateOrModifyFolderDialog(state.isPendingFolderDialogCreateMode(),
                        state.getPendingFolderDialogFolderId(),
                        state.getPendingFolderDialogInitialName());
                return;
            case CONFIRM_DELETE_FOLDER:
                markPendingActionHandled(actionId);
                showDeleteFolderConfirmation(state);
                return;
            case CONFIRM_DELETE_NOTES:
                markPendingActionHandled(actionId);
                showDeleteNotesConfirmation(state.getPendingAffectedCount());
                return;
            case SHOW_MOVE_DESTINATIONS:
                markPendingActionHandled(actionId);
                if (state.getPendingFolderDestinations().isEmpty()) {
                    Log.e(TAG, "Query folder failed");
                    return;
                }
                showFolderListMenu(state.getPendingFolderDestinations());
                return;
            case SHOW_EXPORT_RESULT:
                markPendingActionHandled(actionId);
                showExportResult(state.getPendingExportedFile());
                return;
            case DELETE_COMPLETED:
                markPendingActionHandled(actionId);
                if (!state.isPendingOperationSucceeded()) {
                    Log.e(TAG, "Delete notes error, should not happens");
                    return;
                }
                refreshPendingDeletedWidgets();
                finishSelectionMode();
                return;
            case MOVE_COMPLETED:
                markPendingActionHandled(actionId);
                if (!state.isPendingOperationSucceeded()) {
                    Log.e(TAG, "Move notes error, should not happen");
                    return;
                }
                Toast.makeText(this,
                        getString(R.string.format_move_notes_to_folder,
                                state.getPendingAffectedCount(),
                                state.getPendingDestinationFolderName()),
                        Toast.LENGTH_SHORT).show();
                finishSelectionMode();
                return;
            case FOLDER_NAME_CONFLICT:
                markPendingActionHandled(actionId);
                if (mFolderNameEditText != null) {
                    mFolderNameEditText.requestFocus();
                    mFolderNameEditText.setSelection(0, mFolderNameEditText.length());
                    showSoftInput(mFolderNameEditText);
                }
                Toast.makeText(this,
                        getString(R.string.folder_exist, state.getPendingDestinationFolderName()),
                        Toast.LENGTH_LONG).show();
                return;
            case FOLDER_CREATED:
                markPendingActionHandled(actionId);
                if (!state.isPendingOperationSucceeded()) {
                    Log.e(TAG, "Create folder failed");
                    return;
                }
                dismissFolderNameDialog();
                return;
            case FOLDER_RENAMED:
                markPendingActionHandled(actionId);
                if (!state.isPendingOperationSucceeded()) {
                    Log.e(TAG, "Rename folder failed");
                    return;
                }
                dismissFolderNameDialog();
                return;
            case FOLDER_DELETED:
                markPendingActionHandled(actionId);
                if (!state.isPendingOperationSucceeded()) {
                    Log.e(TAG, "Delete folder failed");
                    return;
                }
                refreshWidgets(state.getPendingWidgetBindings());
                return;
            default:
                return;
        }
    }

    private void markPendingActionHandled(long actionId) {
        mListViewModel.markPendingActionHandled(actionId);
    }

    private void finishSelectionMode() {
        mModeCallBack.finishActionMode();
    }

    private void launchNoteEditor(NotesListViewState state) {
        Intent intent = new Intent(this, NoteEditActivity.class);
        if (state.isPendingEditorCreateMode()) {
            intent.setAction(Intent.ACTION_INSERT_OR_EDIT);
            intent.putExtra(Notes.INTENT_EXTRA_FOLDER_ID, state.getPendingEditorFolderId());
        } else {
            intent.setAction(Intent.ACTION_VIEW);
            intent.putExtra(Intent.EXTRA_UID, state.getPendingEditorNoteId());
        }
        mNoteEditorLauncher.launch(intent);
    }

    private void showDeleteFolderConfirmation(NotesListViewState state) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.alert_title_delete));
        builder.setIcon(android.R.drawable.ic_dialog_alert);
        builder.setMessage(getString(R.string.alert_message_delete_folder));
        builder.setPositiveButton(android.R.string.ok,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        mListViewModel.deleteFolder(state.getPendingDeleteFolderId());
                    }
                });
        builder.setNegativeButton(android.R.string.cancel, null);
        builder.show();
    }

    private void showDeleteNotesConfirmation(int selectedCount) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.alert_title_delete));
        builder.setIcon(android.R.drawable.ic_dialog_alert);
        builder.setMessage(getString(R.string.alert_message_delete_notes, selectedCount));
        builder.setPositiveButton(android.R.string.ok,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        batchDelete();
                    }
                });
        builder.setNegativeButton(android.R.string.cancel, null);
        builder.show();
    }

    private void refreshPendingDeletedWidgets() {
        if (mPendingDeletedWidgets == null || mPendingDeletedWidgets.isEmpty()) {
            return;
        }
        for (AppWidgetAttribute widget : mPendingDeletedWidgets) {
            if (widget.widgetId != AppWidgetManager.INVALID_APPWIDGET_ID
                    && widget.widgetType != Notes.TYPE_WIDGET_INVALIDE) {
                updateWidget(widget.widgetId, widget.widgetType);
            }
        }
        mPendingDeletedWidgets = null;
    }

    private void dismissFolderNameDialog() {
        if (mFolderNameDialog != null && mFolderNameDialog.isShowing()) {
            mFolderNameDialog.dismiss();
        }
        clearFolderDialogState();
    }

    private void clearFolderDialogState() {
        mFolderNameDialog = null;
        mFolderNameEditText = null;
    }

    private void refreshWidgets(List<WidgetBinding> widgetBindings) {
        if (widgetBindings == null || widgetBindings.isEmpty()) {
            return;
        }
        for (WidgetBinding widget : widgetBindings) {
            if (widget.getWidgetId() != AppWidgetManager.INVALID_APPWIDGET_ID
                    && widget.getWidgetType() != Notes.TYPE_WIDGET_INVALIDE) {
                updateWidget(widget.getWidgetId(), widget.getWidgetType());
            }
        }
    }

    private void showExportResult(ExportedTextFile result) {
        if (result == null) {
            return;
        }
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
        if (result.getState() == ExportedTextFile.ExportState.STORAGE_UNAVAILABLE) {
            builder.setTitle(getString(R.string.failed_sdcard_export));
            builder.setMessage(getString(R.string.error_sdcard_unmounted));
        } else if (result.getState() == ExportedTextFile.ExportState.SUCCESS) {
            builder.setTitle(getString(R.string.success_sdcard_export));
            builder.setMessage(getString(R.string.format_exported_file_location,
                    result.getFileName(), result.getDirectory()));
        } else {
            builder.setTitle(getString(R.string.failed_sdcard_export));
            builder.setMessage(getString(R.string.error_sdcard_export));
        }
        builder.setPositiveButton(android.R.string.ok, null);
        builder.show();
    }

    private void renderTopBar(NotesListViewState state) {
        if (state.isRootMode()) {
            mToolbar.setTitle(" ");
            mToolbar.setNavigationIcon(null);
            if (mHeaderTitle != null) {
                mHeaderTitle.setText(R.string.notes_home_headline);
            }
            if (mHeaderSubtitle != null) {
                mHeaderSubtitle.setText(getString(R.string.notes_home_summary) + " "
                        + getString(R.string.notes_item_count, state.getItemCount()));
            }
            return;
        }

        mToolbar.setNavigationIcon(AppCompatResources.getDrawable(this,
                androidx.appcompat.R.drawable.abc_ic_ab_back_material));
        mToolbar.setTitle(" ");
        if (state.isCallRecordMode()) {
            if (mHeaderTitle != null) {
                mHeaderTitle.setText(R.string.call_record_folder_name);
            }
            if (mHeaderSubtitle != null) {
                mHeaderSubtitle.setText(getString(R.string.notes_call_summary) + " "
                        + getString(R.string.notes_item_count, state.getItemCount()));
            }
        } else {
            if (mHeaderTitle != null) {
                mHeaderTitle.setText(state.getCurrentFolderName());
            }
            if (mHeaderSubtitle != null) {
                mHeaderSubtitle.setText(getString(R.string.notes_folder_summary) + " "
                        + getString(R.string.notes_item_count, state.getItemCount()));
            }
        }
    }

    private void showFolderMenu(View anchor, NoteItemData item) {
        PopupMenu menu = new PopupMenu(new ContextThemeWrapper(this,
            R.style.ThemeOverlay_Notes_ToolbarPopup), anchor);
        menu.getMenu().add(0, MENU_FOLDER_VIEW, 0, R.string.menu_folder_view);
        menu.getMenu().add(0, MENU_FOLDER_DELETE, 1, R.string.menu_folder_delete);
        menu.getMenu().add(0, MENU_FOLDER_CHANGE_NAME, 2, R.string.menu_folder_change_name);
        menu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                switch (menuItem.getItemId()) {
                    case MENU_FOLDER_VIEW:
                        openFolder(item);
                        return true;
                    case MENU_FOLDER_DELETE:
                        mListViewModel.requestDeleteFolderConfirmation(item);
                        return true;
                    case MENU_FOLDER_CHANGE_NAME:
                        mListViewModel.requestRenameFolderDialog(item);
                        return true;
                    default:
                        return false;
                }
            }
        });
        menu.show();
    }

    private boolean beginSelectionMode(View view, int position) {
        ActionMode actionMode = startSupportActionMode(mModeCallBack);
        if (actionMode == null) {
            Log.e(TAG, "startActionMode fails");
            return false;
        }
        mModeCallBack.toggleSelection(position);
        view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
        return true;
    }

    private boolean handleSelectionModeItemClick(NoteItemData item, int position) {
        if (!mNotesListAdapter.isInChoiceMode()) {
            return false;
        }
        if (item.getType() == Notes.TYPE_NOTE) {
            mModeCallBack.toggleSelection(position);
        }
        return true;
    }

    private void handleRegularItemClick(NoteItemData item) {
        switch (mListViewModel.resolveItemClickAction(item)) {
            case OPEN_FOLDER:
                openFolder(item);
                break;
            case OPEN_NOTE:
                openNode(item);
                break;
            case NONE:
            default:
                Log.e(TAG, "Unsupported list item click for current screen state");
                break;
        }
    }

    private boolean handleItemLongClickAction(View view, NoteItemData item, int position) {
        switch (mListViewModel.resolveItemLongClickAction(item,
                mNotesListAdapter.isInChoiceMode())) {
            case START_SELECTION:
                return beginSelectionMode(view, position);
            case SHOW_FOLDER_MENU:
                showFolderMenu(view, item);
                return true;
            case NONE:
            default:
                return false;
        }
    }

    @Override
    public void onItemClick(NoteItemData item, int position) {
        if (position == RecyclerView.NO_POSITION) {
            return;
        }
        if (handleSelectionModeItemClick(item, position)) {
            return;
        }
        handleRegularItemClick(item);
    }

    @Override
    public boolean onItemLongClick(View view, NoteItemData item, int position) {
        if (position == RecyclerView.NO_POSITION) {
            return false;
        }
        mFocusNoteDataItem = item;
        return handleItemLongClickAction(view, item, position);
    }
}
