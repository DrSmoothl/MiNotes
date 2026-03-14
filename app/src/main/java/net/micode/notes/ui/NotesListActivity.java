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
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.appcompat.view.ActionMode;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;

import net.micode.notes.R;
import net.micode.notes.data.Notes;
import net.micode.notes.domain.model.ExportedTextFile;
import net.micode.notes.domain.model.FolderDestination;
import net.micode.notes.domain.model.NoteEditorSession;
import net.micode.notes.domain.model.NoteListItem;
import net.micode.notes.domain.model.WidgetBinding;
import net.micode.notes.domain.service.WidgetNotifier;
import net.micode.notes.domain.usecase.editor.StartNoteEditorSessionUseCase;
import net.micode.notes.domain.usecase.list.DeleteNotesUseCase;
import net.micode.notes.domain.usecase.list.ExportNotesUseCase;
import net.micode.notes.domain.usecase.list.FolderManagementUseCase;
import net.micode.notes.domain.usecase.list.LoadNotesUseCase;
import net.micode.notes.inject.NotesApplicationGraph;
import net.micode.notes.tool.ResourceParser;
import net.micode.notes.ui.NotesListAdapter.AppWidgetAttribute;

import java.util.ArrayList;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class NotesListActivity extends AppCompatActivity implements OnClickListener,
    NotesListAdapter.NoteItemListener {
    private static final int MENU_FOLDER_DELETE = 0;

    private static final int MENU_FOLDER_VIEW = 1;

    private static final int MENU_FOLDER_CHANGE_NAME = 2;

    private static final String PREFERENCE_ADD_INTRODUCTION = "net.micode.notes.introduction";

    private enum ListEditState {
        NOTE_LIST, SUB_FOLDER, CALL_RECORD_FOLDER
    };

    private ListEditState mState;

    private NotesListAdapter mNotesListAdapter;

    private RecyclerView mNotesListView;

    private View mAddNewNote;

    private MaterialToolbar mToolbar;

    private TextView mHeaderTitle;

    private TextView mHeaderSubtitle;

    private String mCurrentFolderName;

    private long mCurrentFolderId;

    private ModeCallback mModeCallBack;

    private LoadNotesUseCase mLoadNotesUseCase;

    private FolderManagementUseCase mFolderManagementUseCase;

    private DeleteNotesUseCase mDeleteNotesUseCase;

    private ExportNotesUseCase mExportNotesUseCase;

    private WidgetNotifier mWidgetNotifier;

    private StartNoteEditorSessionUseCase mStartNoteEditorSessionUseCase;

    private static final String TAG = "NotesListActivity";

    public static final int NOTES_LISTVIEW_SCROLL_RATE = 30;

    private NoteItemData mFocusNoteDataItem;

    private final ExecutorService mBackgroundExecutor = Executors.newSingleThreadExecutor();

    private final Handler mMainHandler = new Handler(Looper.getMainLooper());

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

        /**
         * Insert an introduction when user firstly use this application
         */
        setAppInfoFromRawRes();
    }

    private void setAppInfoFromRawRes() {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        if (!sp.getBoolean(PREFERENCE_ADD_INTRODUCTION, false)) {
            StringBuilder sb = new StringBuilder();
            InputStream in = null;
            try {
                 in = getResources().openRawResource(R.raw.introduction);
                if (in != null) {
                    InputStreamReader isr = new InputStreamReader(in);
                    BufferedReader br = new BufferedReader(isr);
                    char [] buf = new char[1024];
                    int len = 0;
                    while ((len = br.read(buf)) > 0) {
                        sb.append(buf, 0, len);
                    }
                } else {
                    Log.e(TAG, "Read introduction file error");
                    return;
                }
            } catch (IOException e) {
                e.printStackTrace();
                return;
            } finally {
                if(in != null) {
                    try {
                        in.close();
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
            }

            NoteEditorSession note = mStartNoteEditorSessionUseCase.startNew(
                    Notes.ID_ROOT_FOLDER,
                    AppWidgetManager.INVALID_APPWIDGET_ID,
                    Notes.TYPE_WIDGET_INVALIDE,
                    ResourceParser.RED);
            note.setWorkingText(sb.toString());
            if (note.save()) {
                sp.edit().putBoolean(PREFERENCE_ADD_INTRODUCTION, true).commit();
            } else {
                Log.e(TAG, "Save introduction note error");
                return;
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        startAsyncNotesListQuery();
    }

    private void initResources() {
        NotesApplicationGraph graph = new NotesApplicationGraph(this);
        mLoadNotesUseCase = graph.loadNotesUseCase();
        mFolderManagementUseCase = graph.folderManagementUseCase();
        mDeleteNotesUseCase = graph.deleteNotesUseCase();
        mExportNotesUseCase = graph.exportNotesUseCase();
        mWidgetNotifier = graph.widgetNotifier();
        mStartNoteEditorSessionUseCase = graph.startNoteEditorSessionUseCase();
        mCurrentFolderId = Notes.ID_ROOT_FOLDER;
        mToolbar = (MaterialToolbar) findViewById(R.id.top_app_bar);
        setSupportActionBar(mToolbar);
        mToolbar.setNavigationOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                handleBackNavigation();
            }
        });
        mNotesListView = (RecyclerView) findViewById(R.id.notes_list);
        mNotesListView.setLayoutManager(new LinearLayoutManager(this));
        mNotesListAdapter = new NotesListAdapter(this, this);
        mNotesListView.setAdapter(mNotesListAdapter);
        mHeaderTitle = (TextView) findViewById(R.id.header_title);
        mHeaderSubtitle = (TextView) findViewById(R.id.header_subtitle);
        findViewById(R.id.button_search_notes).setOnClickListener(this);
        findViewById(R.id.button_settings).setOnClickListener(this);
        mAddNewNote = findViewById(R.id.btn_new_note);
        mAddNewNote.setOnClickListener(this);
        mState = ListEditState.NOTE_LIST;
        mModeCallBack = new ModeCallback();
        updateTopBar();
    }

    private class ModeCallback implements ActionMode.Callback {
        private ActionMode mActionMode;
        private MenuItem mMoveMenu;

        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            getMenuInflater().inflate(R.menu.note_list_options, menu);
            mMoveMenu = menu.findItem(R.id.move);
            if (mFocusNoteDataItem.getParentId() == Notes.ID_CALL_RECORD_FOLDER
                    || mFolderManagementUseCase.getUserFolderCount() == 0) {
                mMoveMenu.setVisible(false);
            } else {
                mMoveMenu.setVisible(true);
            }
            mActionMode = mode;
            mNotesListAdapter.setChoiceMode(true);
            mAddNewNote.setVisibility(View.GONE);
            updateMenu(mode, menu);
            return true;
        }

        private void updateMenu(ActionMode mode, Menu menu) {
            int selectedCount = mNotesListAdapter.getSelectedCount();
            String format = getResources().getString(R.string.menu_select_title, selectedCount);
            mode.setTitle(format);
            MenuItem item = menu.findItem(R.id.action_select_all);
            if (item != null) {
                item.setTitle(mNotesListAdapter.isAllSelected()
                        ? R.string.menu_deselect_all : R.string.menu_select_all);
            }
        }

        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            updateMenu(mode, menu);
            return true;
        }

        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            if (mNotesListAdapter.getSelectedCount() == 0) {
                Toast.makeText(NotesListActivity.this, getString(R.string.menu_select_none),
                        Toast.LENGTH_SHORT).show();
                return true;
            }

            switch (item.getItemId()) {
                case R.id.delete:
                    AlertDialog.Builder builder = new AlertDialog.Builder(NotesListActivity.this);
                    builder.setTitle(getString(R.string.alert_title_delete));
                    builder.setIcon(android.R.drawable.ic_dialog_alert);
                    builder.setMessage(getString(R.string.alert_message_delete_notes,
                            mNotesListAdapter.getSelectedCount()));
                    builder.setPositiveButton(android.R.string.ok,
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    batchDelete();
                                }
                            });
                    builder.setNegativeButton(android.R.string.cancel, null);
                    builder.show();
                    return true;
                case R.id.move:
                    startQueryDestinationFolders();
                    return true;
                case R.id.action_select_all:
                    mNotesListAdapter.selectAll(!mNotesListAdapter.isAllSelected());
                    updateMenu(mode, mode.getMenu());
                    return true;
                default:
                    return false;
            }
        }

        public void onDestroyActionMode(ActionMode mode) {
            mNotesListAdapter.setChoiceMode(false);
            mAddNewNote.setVisibility(View.VISIBLE);
            mActionMode = null;
        }

        public void finishActionMode() {
            if (mActionMode != null) {
                mActionMode.finish();
            }
        }

        public void toggleSelection(int position) {
            mNotesListAdapter.setCheckedItem(position, !mNotesListAdapter.isSelectedItem(position));
            if (mActionMode == null) {
                return;
            }
            if (mNotesListAdapter.getSelectedCount() == 0) {
                finishActionMode();
            } else {
                updateMenu(mActionMode, mActionMode.getMenu());
            }
        }
    }

    private void startAsyncNotesListQuery() {
        mBackgroundExecutor.execute(new Runnable() {
            @Override
            public void run() {
                final List<NoteListItem> items = mLoadNotesUseCase.load(mCurrentFolderId);
                mMainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        mNotesListAdapter.submitList(mapToUiItems(items));
                        updateTopBar(items.size());
                    }
                });
            }
        });
    }

    private ArrayList<NoteItemData> mapToUiItems(List<NoteListItem> items) {
        ArrayList<NoteItemData> uiItems = new ArrayList<NoteItemData>();
        if (items == null) {
            return uiItems;
        }
        for (NoteListItem item : items) {
            uiItems.add(new NoteItemData(item));
        }
        return uiItems;
    }

    private void showFolderListMenu(final List<FolderDestination> folders) {
        AlertDialog.Builder builder = new AlertDialog.Builder(NotesListActivity.this);
        builder.setTitle(R.string.menu_title_select_folder);
        final String[] names = new String[folders.size()];
        for (int i = 0; i < folders.size(); i++) {
            names[i] = folders.get(i).getName();
        }
        builder.setItems(names, new DialogInterface.OnClickListener() {

            public void onClick(DialogInterface dialog, int which) {
                mFolderManagementUseCase.moveNotes(mNotesListAdapter.getSelectedItemIds(),
                        folders.get(which).getId());
                Toast.makeText(
                        NotesListActivity.this,
                        getString(R.string.format_move_notes_to_folder,
                                mNotesListAdapter.getSelectedCount(),
                                folders.get(which).getName()),
                        Toast.LENGTH_SHORT).show();
                startAsyncNotesListQuery();
                mModeCallBack.finishActionMode();
            }
        });
        builder.show();
    }

    private void createNewNote() {
        Intent intent = new Intent(this, NoteEditActivity.class);
        intent.setAction(Intent.ACTION_INSERT_OR_EDIT);
        intent.putExtra(Notes.INTENT_EXTRA_FOLDER_ID, mCurrentFolderId);
        mNoteEditorLauncher.launch(intent);
    }

    private void batchDelete() {
        mBackgroundExecutor.execute(new Runnable() {
            @Override
            public void run() {
                final HashSet<AppWidgetAttribute> widgets = mNotesListAdapter.getSelectedWidget();
                if (!mDeleteNotesUseCase.delete(mNotesListAdapter.getSelectedItemIds())) {
                    Log.e(TAG, "Delete notes error, should not happens");
                }
                mMainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (widgets != null) {
                            for (AppWidgetAttribute widget : widgets) {
                                if (widget.widgetId != AppWidgetManager.INVALID_APPWIDGET_ID
                                        && widget.widgetType != Notes.TYPE_WIDGET_INVALIDE) {
                                    updateWidget(widget.widgetId, widget.widgetType);
                                }
                            }
                        }
                        mModeCallBack.finishActionMode();
                    }
                });
            }
        });
    }

    private void deleteFolder(long folderId) {
        if (folderId == Notes.ID_ROOT_FOLDER) {
            Log.e(TAG, "Wrong folder id, should not happen " + folderId);
            return;
        }

        java.util.Set<WidgetBinding> widgets = mFolderManagementUseCase.deleteFolder(folderId);
        if (widgets != null) {
            for (WidgetBinding widget : widgets) {
                if (widget.getWidgetId() != AppWidgetManager.INVALID_APPWIDGET_ID
                        && widget.getWidgetType() != Notes.TYPE_WIDGET_INVALIDE) {
                    updateWidget(widget.getWidgetId(), widget.getWidgetType());
                }
            }
        }
    }

    private void openNode(NoteItemData data) {
        Intent intent = new Intent(this, NoteEditActivity.class);
        intent.setAction(Intent.ACTION_VIEW);
        intent.putExtra(Intent.EXTRA_UID, data.getId());
        mNoteEditorLauncher.launch(intent);
    }

    private void openFolder(NoteItemData data) {
        mCurrentFolderId = data.getId();
        startAsyncNotesListQuery();
        if (data.getId() == Notes.ID_CALL_RECORD_FOLDER) {
            mState = ListEditState.CALL_RECORD_FOLDER;
            mAddNewNote.setVisibility(View.GONE);
            mCurrentFolderName = getString(R.string.call_record_folder_name);
        } else {
            mState = ListEditState.SUB_FOLDER;
            mCurrentFolderName = data.getSnippet();
        }
        updateTopBar(data);
        invalidateOptionsMenu();
    }

    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_new_note:
                createNewNote();
                break;
            case R.id.button_search_notes:
                onSearchRequested();
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

    private void showCreateOrModifyFolderDialog(final boolean create) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_edit_text, null);
        final EditText etName = (EditText) view.findViewById(R.id.et_foler_name);
        showSoftInput(etName);
        if (!create) {
            if (mFocusNoteDataItem != null) {
                etName.setText(mFocusNoteDataItem.getSnippet());
                builder.setTitle(getString(R.string.menu_folder_change_name));
            } else {
                Log.e(TAG, "The long click data item is null");
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
            }
        });

        final Dialog dialog = builder.setView(view).show();
        final Button positive = (Button)dialog.findViewById(android.R.id.button1);
        positive.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                hideSoftInput(etName);
                String name = etName.getText().toString();
                if (mFolderManagementUseCase.folderNameExists(name)) {
                    Toast.makeText(NotesListActivity.this, getString(R.string.folder_exist, name),
                            Toast.LENGTH_LONG).show();
                    etName.setSelection(0, etName.length());
                    return;
                }
                if (!create) {
                    if (!TextUtils.isEmpty(name)) {
                        mFolderManagementUseCase.renameFolder(mFocusNoteDataItem.getId(), name);
                        if (mFocusNoteDataItem.getId() == mCurrentFolderId) {
                            mCurrentFolderName = name;
                            updateTopBar();
                        }
                    }
                } else if (!TextUtils.isEmpty(name)) {
                    mFolderManagementUseCase.createFolder(name);
                }
                startAsyncNotesListQuery();
                dialog.dismiss();
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
        switch (mState) {
            case SUB_FOLDER:
                mCurrentFolderId = Notes.ID_ROOT_FOLDER;
                mState = ListEditState.NOTE_LIST;
                mCurrentFolderName = null;
                startAsyncNotesListQuery();
                updateTopBar();
                invalidateOptionsMenu();
                break;
            case CALL_RECORD_FOLDER:
                mCurrentFolderId = Notes.ID_ROOT_FOLDER;
                mState = ListEditState.NOTE_LIST;
                mCurrentFolderName = null;
                mAddNewNote.setVisibility(View.VISIBLE);
                startAsyncNotesListQuery();
                updateTopBar();
                invalidateOptionsMenu();
                break;
            case NOTE_LIST:
                finish();
                break;
            default:
                break;
        }
    }

    private void updateWidget(int appWidgetId, int appWidgetType) {
        mWidgetNotifier.refresh(appWidgetId, appWidgetType);
        setResult(RESULT_OK);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.clear();
        if (mState == ListEditState.NOTE_LIST) {
            getMenuInflater().inflate(R.menu.note_list, menu);
        } else if (mState == ListEditState.SUB_FOLDER) {
            getMenuInflater().inflate(R.menu.sub_folder, menu);
        } else if (mState == ListEditState.CALL_RECORD_FOLDER) {
            getMenuInflater().inflate(R.menu.call_record_folder, menu);
        } else {
            Log.e(TAG, "Wrong state:" + mState);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_new_folder: {
                showCreateOrModifyFolderDialog(true);
                break;
            }
            case R.id.menu_export_text: {
                exportNoteToText();
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
                onSearchRequested();
                break;
            default:
                break;
        }
        return true;
    }

    @Override
    public boolean onSearchRequested() {
        startSearch(null, false, null /* appData */, false);
        return true;
    }

    private void exportNoteToText() {
        mBackgroundExecutor.execute(new Runnable() {
            @Override
            public void run() {
                final ExportedTextFile result = mExportNotesUseCase.exportToText();
                mMainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (result.getState() == ExportedTextFile.ExportState.STORAGE_UNAVAILABLE) {
                            AlertDialog.Builder builder = new AlertDialog.Builder(NotesListActivity.this);
                            builder.setTitle(NotesListActivity.this
                                    .getString(R.string.failed_sdcard_export));
                            builder.setMessage(NotesListActivity.this
                                    .getString(R.string.error_sdcard_unmounted));
                            builder.setPositiveButton(android.R.string.ok, null);
                            builder.show();
                            } else if (result.getState() == ExportedTextFile.ExportState.SUCCESS) {
                            AlertDialog.Builder builder = new AlertDialog.Builder(NotesListActivity.this);
                            builder.setTitle(NotesListActivity.this
                                    .getString(R.string.success_sdcard_export));
                            builder.setMessage(NotesListActivity.this.getString(
                                    R.string.format_exported_file_location,
                                    result.getFileName(), result.getDirectory()));
                            builder.setPositiveButton(android.R.string.ok, null);
                            builder.show();
                            } else {
                            AlertDialog.Builder builder = new AlertDialog.Builder(NotesListActivity.this);
                            builder.setTitle(NotesListActivity.this
                                    .getString(R.string.failed_sdcard_export));
                            builder.setMessage(NotesListActivity.this
                                    .getString(R.string.error_sdcard_export));
                            builder.setPositiveButton(android.R.string.ok, null);
                            builder.show();
                        }
                    }
                });
            }
        });
    }

    @Override
    protected void onDestroy() {
        mBackgroundExecutor.shutdown();
        super.onDestroy();
    }

    private void startPreferenceActivity() {
        Intent intent = new Intent(this, NotesPreferenceActivity.class);
        startActivity(intent);
    }

    private void startQueryDestinationFolders() {
        mBackgroundExecutor.execute(new Runnable() {
            @Override
            public void run() {
            final List<FolderDestination> folders = mFolderManagementUseCase
                .getFolderDestinations(mCurrentFolderId,
                    mState != ListEditState.NOTE_LIST);
                mMainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (folders.isEmpty()) {
                            Log.e(TAG, "Query folder failed");
                            return;
                        }
                        showFolderListMenu(folders);
                    }
                });
            }
        });
    }

    private void updateTopBar() {
        updateTopBar(null);
    }

    private void updateTopBar(NoteItemData currentFolder) {
        updateTopBar(currentFolder, mNotesListAdapter == null ? 0 : mNotesListAdapter.getItemCount());
    }

    private void updateTopBar(int itemCount) {
        updateTopBar(null, itemCount);
    }

    private void updateTopBar(NoteItemData currentFolder, int itemCount) {
        if (currentFolder != null) {
            mCurrentFolderName = currentFolder.getSnippet();
        }
        if (mState == ListEditState.NOTE_LIST) {
            mToolbar.setTitle(" ");
            mToolbar.setNavigationIcon(null);
            if (mHeaderTitle != null) {
                mHeaderTitle.setText(R.string.notes_home_headline);
            }
            if (mHeaderSubtitle != null) {
                mHeaderSubtitle.setText(getString(R.string.notes_home_summary) + " "
                        + getString(R.string.notes_item_count, itemCount));
            }
            return;
        }

        mToolbar.setNavigationIcon(AppCompatResources.getDrawable(this,
                androidx.appcompat.R.drawable.abc_ic_ab_back_material));
        mToolbar.setTitle(" ");
        if (mState == ListEditState.CALL_RECORD_FOLDER) {
            if (mHeaderTitle != null) {
                mHeaderTitle.setText(R.string.call_record_folder_name);
            }
            if (mHeaderSubtitle != null) {
                mHeaderSubtitle.setText(getString(R.string.notes_call_summary) + " "
                        + getString(R.string.notes_item_count, itemCount));
            }
        } else {
            if (mHeaderTitle != null) {
                mHeaderTitle.setText(mCurrentFolderName);
            }
            if (mHeaderSubtitle != null) {
                mHeaderSubtitle.setText(getString(R.string.notes_folder_summary) + " "
                        + getString(R.string.notes_item_count, itemCount));
            }
        }
    }

    private void showFolderMenu(View anchor, NoteItemData item) {
        PopupMenu menu = new PopupMenu(this, anchor);
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
                        AlertDialog.Builder builder = new AlertDialog.Builder(NotesListActivity.this);
                        builder.setTitle(getString(R.string.alert_title_delete));
                        builder.setIcon(android.R.drawable.ic_dialog_alert);
                        builder.setMessage(getString(R.string.alert_message_delete_folder));
                        builder.setPositiveButton(android.R.string.ok,
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        deleteFolder(item.getId());
                                        startAsyncNotesListQuery();
                                    }
                                });
                        builder.setNegativeButton(android.R.string.cancel, null);
                        builder.show();
                        return true;
                    case MENU_FOLDER_CHANGE_NAME:
                        mFocusNoteDataItem = item;
                        showCreateOrModifyFolderDialog(false);
                        return true;
                    default:
                        return false;
                }
            }
        });
        menu.show();
    }

    @Override
    public void onItemClick(NoteItemData item, int position) {
        if (position == RecyclerView.NO_POSITION) {
            return;
        }
        if (mNotesListAdapter.isInChoiceMode()) {
            if (item.getType() == Notes.TYPE_NOTE) {
                mModeCallBack.toggleSelection(position);
            }
            return;
        }

        switch (mState) {
            case NOTE_LIST:
                if (item.getType() == Notes.TYPE_FOLDER || item.getType() == Notes.TYPE_SYSTEM) {
                    openFolder(item);
                } else if (item.getType() == Notes.TYPE_NOTE) {
                    openNode(item);
                } else {
                    Log.e(TAG, "Wrong note type in NOTE_LIST");
                }
                break;
            case SUB_FOLDER:
            case CALL_RECORD_FOLDER:
                if (item.getType() == Notes.TYPE_NOTE) {
                    openNode(item);
                } else {
                    Log.e(TAG, "Wrong note type in SUB_FOLDER");
                }
                break;
            default:
                break;
        }
    }

    @Override
    public boolean onItemLongClick(View view, NoteItemData item, int position) {
        if (position == RecyclerView.NO_POSITION) {
            return false;
        }
        mFocusNoteDataItem = item;
        if (item.getType() == Notes.TYPE_NOTE && !mNotesListAdapter.isInChoiceMode()) {
            if (startSupportActionMode(mModeCallBack) != null) {
                mModeCallBack.toggleSelection(position);
                view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
                return true;
            }
            Log.e(TAG, "startActionMode fails");
            return false;
        }
        if (item.getType() == Notes.TYPE_FOLDER) {
            showFolderMenu(view, item);
            return true;
        }
        return false;
    }
}
