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

import android.app.AlertDialog;
import android.app.SearchManager;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Paint;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.text.style.BackgroundColorSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.PreferenceManager;

import com.google.android.material.appbar.MaterialToolbar;

import net.micode.notes.R;
import net.micode.notes.data.Notes;
import net.micode.notes.data.Notes.TextNote;
import net.micode.notes.domain.model.CheckListText;
import net.micode.notes.domain.model.NoteEditorSession;
import net.micode.notes.domain.usecase.editor.DeleteNoteUseCase;
import net.micode.notes.domain.usecase.editor.StartNoteEditorSessionUseCase;
import net.micode.notes.inject.NotesApplicationGraph;
import net.micode.notes.tool.ResourceParser;
import net.micode.notes.tool.ResourceParser.NoteColorResources;
import net.micode.notes.tool.ResourceParser.TextAppearanceResources;
import net.micode.notes.ui.DateTimePickerDialog.OnDateTimeSetListener;
import net.micode.notes.ui.NoteEditText.OnTextViewChangeListener;

import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class NoteEditActivity extends AppCompatActivity implements OnClickListener,
    OnTextViewChangeListener {
    private static final int[] BACKGROUND_IDS = new int[] {
        ResourceParser.YELLOW,
        ResourceParser.BLUE,
        ResourceParser.WHITE,
        ResourceParser.GREEN,
        ResourceParser.RED
    };

    private static final int[] FONT_SIZE_IDS = new int[] {
        ResourceParser.TEXT_SMALL,
        ResourceParser.TEXT_MEDIUM,
        ResourceParser.TEXT_LARGE,
        ResourceParser.TEXT_SUPER
    };

    private class HeadViewHolder {
        public TextView tvModified;

        public ImageView ivAlertIcon;

        public TextView tvAlertDate;

        public ImageView ibSetBgColor;
    }

    private static final String TAG = "NoteEditActivity";

    private HeadViewHolder mNoteHeaderHolder;

    private View mHeadViewPanel;

    private EditText mNoteEditor;

    private View mNoteEditorPanel;

    private NoteEditorSession mNoteSession;

    private NoteEditViewModel mNoteEditViewModel;

    private SharedPreferences mSharedPrefs;
    private int mFontSizeId;

    private static final String PREFERENCE_FONT_SIZE = "pref_font_size";

    private static final int SHORTCUT_ICON_TITLE_MAX_LEN = 10;

    private LinearLayout mEditTextList;

    private MaterialToolbar mToolbar;

    private TextView mEditorScreenTitle;

    private TextView mEditorScreenSubtitle;

    private String mUserQuery;
    private Pattern mPattern;

    private static final class EditorContentSnapshot {
        private final String text;
        private final boolean hasCheckedItems;

        private EditorContentSnapshot(String text, boolean hasCheckedItems) {
            this.text = text;
            this.hasCheckedItems = hasCheckedItems;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.note_edit);
        initDependencies();
        initResources();
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                handleBackNavigation();
            }
        });

        if (savedInstanceState == null && !initActivityState(getIntent())) {
            finish();
            return;
        }
    }

    private void initDependencies() {
        NotesApplicationGraph graph = new NotesApplicationGraph(this);
        StartNoteEditorSessionUseCase startNoteEditorSessionUseCase =
            graph.startNoteEditorSessionUseCase();
        DeleteNoteUseCase deleteNoteUseCase = graph.deleteNoteUseCase();
        mNoteEditViewModel = new ViewModelProvider(this,
            new NoteEditViewModel.Factory(startNoteEditorSessionUseCase, deleteNoteUseCase,
                    graph.reminderScheduler(), graph.widgetNotifier()))
                .get(NoteEditViewModel.class);
    }

    /**
     * Current activity may be killed when the memory is low. Once it is killed, for another time
     * user load this activity, we should restore the former state
     */
    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        if (savedInstanceState != null && savedInstanceState.containsKey(Intent.EXTRA_UID)) {
            mNoteEditViewModel.restoreExisting(savedInstanceState.getLong(Intent.EXTRA_UID));
            syncSessionFromViewModel();
            if (mNoteSession == null) {
                finish();
                return;
            }
            Log.d(TAG, "Restoring from killed activity");
        }
    }

    private boolean initActivityState(Intent intent) {
        /**
         * If the user specified the {@link Intent#ACTION_VIEW} but not provided with id,
         * then jump to the NotesListActivity
         */
        if (TextUtils.equals(Intent.ACTION_VIEW, intent.getAction())) {
            long noteId = intent.getLongExtra(Intent.EXTRA_UID, 0);
            String userQuery = "";

            /**
             * Starting from the searched result
             */
            if (intent.hasExtra(SearchManager.EXTRA_DATA_KEY)) {
                noteId = Long.parseLong(intent.getStringExtra(SearchManager.EXTRA_DATA_KEY));
                userQuery = intent.getStringExtra(SearchManager.USER_QUERY);
            }

            if (!mNoteEditViewModel.openExisting(noteId, userQuery)) {
                Intent jump = new Intent(this, NotesListActivity.class);
                startActivity(jump);
                showToast(R.string.error_note_not_exist);
                finish();
                return false;
            }
            syncSessionFromViewModel();
            if (mNoteSession == null) {
                Intent jump = new Intent(this, NotesListActivity.class);
                startActivity(jump);
                showToast(R.string.error_note_not_exist);
                finish();
                return false;
            }
            getWindow().setSoftInputMode(
                    WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
        } else if(TextUtils.equals(Intent.ACTION_INSERT_OR_EDIT, intent.getAction())) {
            // New note
            long folderId = intent.getLongExtra(Notes.INTENT_EXTRA_FOLDER_ID, 0);
            int widgetId = intent.getIntExtra(Notes.INTENT_EXTRA_WIDGET_ID,
                    AppWidgetManager.INVALID_APPWIDGET_ID);
            int widgetType = intent.getIntExtra(Notes.INTENT_EXTRA_WIDGET_TYPE,
                    Notes.TYPE_WIDGET_INVALIDE);
            int bgResId = intent.getIntExtra(Notes.INTENT_EXTRA_BACKGROUND_ID,
                    ResourceParser.getDefaultBgId(this));

            // Parse call-record note
            String phoneNumber = intent.getStringExtra(Intent.EXTRA_PHONE_NUMBER);
            long callDate = intent.getLongExtra(Notes.INTENT_EXTRA_CALL_DATE, 0);
            if (callDate != 0 && phoneNumber != null) {
                if (TextUtils.isEmpty(phoneNumber)) {
                    Log.w(TAG, "The call record number is null");
                }
                mNoteEditViewModel.startForCallRecord(folderId, widgetId, widgetType,
                        bgResId, phoneNumber, callDate);
            } else {
                mNoteEditViewModel.startNew(folderId, widgetId, widgetType, bgResId);
            }
            syncSessionFromViewModel();

            getWindow().setSoftInputMode(
            WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        } else {
            Log.e(TAG, "Intent not specified action, should not support");
            finish();
            return false;
        }
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        syncSessionFromViewModel();
        mNoteEditViewModel.refreshState();
        initNoteScreen();
    }

    private void initNoteScreen() {
        NoteEditViewState state = mNoteEditViewModel.getCurrentState();
        if (mNoteSession == null || state == null) {
            return;
        }
        renderEditorContent(state);
        renderViewState(state);
    }

    private void renderEditorContent(NoteEditViewState state) {
        mNoteEditor.setTextAppearance(TextAppearanceResources
            .getTexAppearanceResource(mFontSizeId));
        if (state.getCheckListMode() == TextNote.MODE_CHECK_LIST) {
            switchToListMode(mNoteSession.getContent());
        } else {
            mNoteEditor.setText(getHighlightQueryResult(mNoteSession.getContent(), mUserQuery));
            mNoteEditor.setSelection(mNoteEditor.getText().length());
            mEditTextList.setVisibility(View.GONE);
            mNoteEditor.setVisibility(View.VISIBLE);
        }
    }

    private void showAlertHeader(NoteEditViewState state) {
        if (state.hasClockAlert()) {
            long time = System.currentTimeMillis();
            if (time > state.getAlertDate()) {
                mNoteHeaderHolder.tvAlertDate.setText(R.string.note_alert_expired);
            } else {
                mNoteHeaderHolder.tvAlertDate.setText(DateUtils.getRelativeTimeSpanString(
                        state.getAlertDate(), time, DateUtils.MINUTE_IN_MILLIS));
            }
            mNoteHeaderHolder.tvAlertDate.setVisibility(View.VISIBLE);
            mNoteHeaderHolder.ivAlertIcon.setVisibility(View.VISIBLE);
        } else {
            mNoteHeaderHolder.tvAlertDate.setVisibility(View.GONE);
            mNoteHeaderHolder.ivAlertIcon.setVisibility(View.GONE);
        };
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        initActivityState(intent);
        initNoteScreen();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        /**
         * For new note without note id, we should firstly save it to
         * generate a id. If the editing note is not worth saving, there
         * is no id which is equivalent to create new note
         */
        if (!mNoteSession.existsInDatabase()) {
            saveNote();
        }
        outState.putLong(Intent.EXTRA_UID, mNoteSession.getNoteId());
        Log.d(TAG, "Save working note id: " + mNoteSession.getNoteId() + " onSaveInstanceState");
    }

    private void initResources() {
        mToolbar = (MaterialToolbar) findViewById(R.id.top_app_bar);
        setSupportActionBar(mToolbar);
        mToolbar.setTitle(" ");
        mToolbar.setNavigationIcon(AppCompatResources.getDrawable(this,
                androidx.appcompat.R.drawable.abc_ic_ab_back_material));
        mToolbar.setNavigationOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                handleBackNavigation();
            }
        });
        mEditorScreenTitle = (TextView) findViewById(R.id.editor_screen_title);
        mEditorScreenSubtitle = (TextView) findViewById(R.id.editor_screen_subtitle);
        mHeadViewPanel = findViewById(R.id.note_title);
        mNoteHeaderHolder = new HeadViewHolder();
        mNoteHeaderHolder.tvModified = (TextView) findViewById(R.id.tv_modified_date);
        mNoteHeaderHolder.ivAlertIcon = (ImageView) findViewById(R.id.iv_alert_icon);
        mNoteHeaderHolder.tvAlertDate = (TextView) findViewById(R.id.tv_alert_date);
        mNoteHeaderHolder.ibSetBgColor = (ImageView) findViewById(R.id.btn_set_bg_color);
        mNoteHeaderHolder.ibSetBgColor.setOnClickListener(this);
        mNoteEditor = (EditText) findViewById(R.id.note_edit_view);
        mNoteEditorPanel = findViewById(R.id.sv_note_edit);
        mSharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        mFontSizeId = mSharedPrefs.getInt(PREFERENCE_FONT_SIZE, ResourceParser.BG_DEFAULT_FONT_SIZE);
        if (mFontSizeId >= TextAppearanceResources.getResourcesSize()) {
            mFontSizeId = ResourceParser.BG_DEFAULT_FONT_SIZE;
        }
        mEditTextList = (LinearLayout) findViewById(R.id.note_edit_list);
        mNoteEditViewModel.getViewState().observe(this, this::renderViewState);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (saveNote()) {
            Log.d(TAG, "Note data was saved with length:" + mNoteSession.getContent().length());
        }
    }

    public void onClick(View v) {
        if (v.getId() == R.id.btn_set_bg_color) {
            showBackgroundPickerDialog();
        }
    }

    private void handleBackNavigation() {
        saveNote();
        finish();
    }

    private void updateScreenHeader() {
        renderViewState(mNoteEditViewModel.getCurrentState());
    }

    private void applyEditorColors(NoteEditViewState state) {
        mHeadViewPanel.setBackgroundColor(ContextCompat.getColor(this,
            NoteColorResources.getNoteEditorHeaderColor(state.getBackgroundColorId())));
        mNoteEditorPanel.setBackgroundColor(ContextCompat.getColor(this,
            NoteColorResources.getNoteEditorBackgroundColor(state.getBackgroundColorId())));
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (isFinishing()) {
            return true;
        }
        menu.clear();
        if (mNoteSession.getFolderId() == Notes.ID_CALL_RECORD_FOLDER) {
            getMenuInflater().inflate(R.menu.call_note_edit, menu);
        } else {
            getMenuInflater().inflate(R.menu.note_edit, menu);
        }
        if (mNoteSession.getCheckListMode() == TextNote.MODE_CHECK_LIST) {
            menu.findItem(R.id.menu_list_mode).setTitle(R.string.menu_normal_mode);
        } else {
            menu.findItem(R.id.menu_list_mode).setTitle(R.string.menu_list_mode);
        }
        if (mNoteSession.hasClockAlert()) {
            menu.findItem(R.id.menu_alert).setVisible(false);
        } else {
            menu.findItem(R.id.menu_delete_remind).setVisible(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_new_note:
                createNewNote();
                break;
            case R.id.menu_delete:
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle(getString(R.string.alert_title_delete));
                builder.setIcon(android.R.drawable.ic_dialog_alert);
                builder.setMessage(getString(R.string.alert_message_delete_note));
                builder.setPositiveButton(android.R.string.ok,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                deleteCurrentNote();
                                finish();
                            }
                        });
                builder.setNegativeButton(android.R.string.cancel, null);
                builder.show();
                break;
            case R.id.menu_font_size:
                showFontSizePickerDialog();
                break;
            case R.id.menu_list_mode:
                int oldMode = mNoteSession.getCheckListMode();
                int newMode = oldMode == 0 ? TextNote.MODE_CHECK_LIST : 0;
                EditorContentSnapshot modeSnapshot = collectWorkingText();
                mNoteEditViewModel.changeCheckListMode(modeSnapshot.text,
                        modeSnapshot.hasCheckedItems, newMode);
                syncSessionFromViewModel();
                renderEditorContent(mNoteEditViewModel.getCurrentState());
                break;
            case R.id.menu_share:
                EditorContentSnapshot shareSnapshot = collectWorkingText();
                mNoteEditViewModel.updateWorkingText(shareSnapshot.text);
                syncSessionFromViewModel();
                sendTo(this, mNoteSession.getContent());
                break;
            case R.id.menu_alert:
                setReminder();
                break;
            case R.id.menu_delete_remind:
                EditorContentSnapshot clearReminderSnapshot = collectWorkingText();
                if (!mNoteEditViewModel.applyReminder(clearReminderSnapshot.text, 0, false)) {
                    showToast(R.string.error_note_empty_for_clock);
                }
                syncSessionFromViewModel();
                break;
            default:
                break;
        }
        return true;
    }

    private void showBackgroundPickerDialog() {
        final String[] labels = new String[] {
                getString(R.string.note_color_yellow),
                getString(R.string.note_color_blue),
                getString(R.string.note_color_white),
                getString(R.string.note_color_green),
                getString(R.string.note_color_red)
        };
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle(R.string.menu_note_color)
                .setSingleChoiceItems(labels, getBackgroundSelectionIndex(),
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                mNoteEditViewModel.setBackgroundColor(BACKGROUND_IDS[which]);
                                syncSessionFromViewModel();
                                dialog.dismiss();
                            }
                        })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void showFontSizePickerDialog() {
        final String[] labels = new String[] {
                getString(R.string.menu_font_small),
                getString(R.string.menu_font_normal),
                getString(R.string.menu_font_large),
                getString(R.string.menu_font_super)
        };
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle(R.string.menu_font_size)
                .setSingleChoiceItems(labels, getFontSizeSelectionIndex(),
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                applyFontSize(FONT_SIZE_IDS[which]);
                                dialog.dismiss();
                            }
                        })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private int getBackgroundSelectionIndex() {
        for (int i = 0; i < BACKGROUND_IDS.length; i++) {
            if (BACKGROUND_IDS[i] == mNoteSession.getBgColorId()) {
                return i;
            }
        }
        return 0;
    }

    private int getFontSizeSelectionIndex() {
        for (int i = 0; i < FONT_SIZE_IDS.length; i++) {
            if (FONT_SIZE_IDS[i] == mFontSizeId) {
                return i;
            }
        }
        return ResourceParser.BG_DEFAULT_FONT_SIZE;
    }

    private void applyFontSize(int fontSizeId) {
        mFontSizeId = fontSizeId;
        mSharedPrefs.edit().putInt(PREFERENCE_FONT_SIZE, mFontSizeId).commit();
        if (mNoteSession.getCheckListMode() == TextNote.MODE_CHECK_LIST) {
            EditorContentSnapshot contentSnapshot = collectWorkingText();
            switchToListMode(contentSnapshot.text);
        } else {
            mNoteEditor.setTextAppearance(
                    TextAppearanceResources.getTexAppearanceResource(mFontSizeId));
        }
    }

    private void setReminder() {
        DateTimePickerDialog d = new DateTimePickerDialog(this, System.currentTimeMillis());
        d.setOnDateTimeSetListener(new OnDateTimeSetListener() {
            public void OnDateTimeSet(AlertDialog dialog, long date) {
                EditorContentSnapshot reminderSnapshot = collectWorkingText();
                if (!mNoteEditViewModel.applyReminder(reminderSnapshot.text, date, true)) {
                    showToast(R.string.error_note_empty_for_clock);
                }
                syncSessionFromViewModel();
            }
        });
        d.show();
    }

    /**
     * Share note to apps that support {@link Intent#ACTION_SEND} action
     * and {@text/plain} type
     */
    private void sendTo(Context context, String info) {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.putExtra(Intent.EXTRA_TEXT, info);
        intent.setType("text/plain");
        context.startActivity(intent);
    }

    private void createNewNote() {
        // Firstly, save current editing notes
        saveNote();

        // For safety, start a new NoteEditActivity
        finish();
        Intent intent = new Intent(this, NoteEditActivity.class);
        intent.setAction(Intent.ACTION_INSERT_OR_EDIT);
        intent.putExtra(Notes.INTENT_EXTRA_FOLDER_ID, mNoteSession.getFolderId());
        startActivity(intent);
    }

    private void deleteCurrentNote() {
        if (!mNoteEditViewModel.deleteCurrent()) {
            Log.e(TAG, "Delete Note error");
        }
        syncSessionFromViewModel();
    }

    public void onEditTextDelete(int index, String text) {
        int childCount = mEditTextList.getChildCount();
        if (childCount == 1) {
            return;
        }

        for (int i = index + 1; i < childCount; i++) {
            ((NoteEditText) mEditTextList.getChildAt(i).findViewById(R.id.et_edit_text))
                    .setIndex(i - 1);
        }

        mEditTextList.removeViewAt(index);
        NoteEditText edit = null;
        if(index == 0) {
            edit = (NoteEditText) mEditTextList.getChildAt(0).findViewById(
                    R.id.et_edit_text);
        } else {
            edit = (NoteEditText) mEditTextList.getChildAt(index - 1).findViewById(
                    R.id.et_edit_text);
        }
        int length = edit.length();
        edit.append(text);
        edit.requestFocus();
        edit.setSelection(length);
    }

    public void onEditTextEnter(int index, String text) {
        /**
         * Should not happen, check for debug
         */
        if(index > mEditTextList.getChildCount()) {
            Log.e(TAG, "Index out of mEditTextList boundrary, should not happen");
        }

        View view = getListItem(text, index);
        mEditTextList.addView(view, index);
        NoteEditText edit = (NoteEditText) view.findViewById(R.id.et_edit_text);
        edit.requestFocus();
        edit.setSelection(0);
        for (int i = index + 1; i < mEditTextList.getChildCount(); i++) {
            ((NoteEditText) mEditTextList.getChildAt(i).findViewById(R.id.et_edit_text))
                    .setIndex(i);
        }
    }

    private void switchToListMode(String text) {
        mEditTextList.removeAllViews();
        String[] items = text.split("\n");
        int index = 0;
        for (String item : items) {
            if(!TextUtils.isEmpty(item)) {
                mEditTextList.addView(getListItem(item, index));
                index++;
            }
        }
        mEditTextList.addView(getListItem("", index));
        mEditTextList.getChildAt(index).findViewById(R.id.et_edit_text).requestFocus();

        mNoteEditor.setVisibility(View.GONE);
        mEditTextList.setVisibility(View.VISIBLE);
    }

    private Spannable getHighlightQueryResult(String fullText, String userQuery) {
        SpannableString spannable = new SpannableString(fullText == null ? "" : fullText);
        if (!TextUtils.isEmpty(userQuery)) {
            mPattern = Pattern.compile(userQuery);
            Matcher m = mPattern.matcher(fullText);
            int start = 0;
            while (m.find(start)) {
                spannable.setSpan(
                    new BackgroundColorSpan(ContextCompat.getColor(this,
                        R.color.user_query_highlight)), m.start(), m.end(),
                        Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
                start = m.end();
            }
        }
        return spannable;
    }

    private View getListItem(String item, int index) {
        View view = LayoutInflater.from(this).inflate(R.layout.note_edit_list_item, null);
        final NoteEditText edit = (NoteEditText) view.findViewById(R.id.et_edit_text);
        edit.setTextAppearance(TextAppearanceResources.getTexAppearanceResource(mFontSizeId));
        CheckBox cb = ((CheckBox) view.findViewById(R.id.cb_edit_item));
        cb.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    edit.setPaintFlags(edit.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                } else {
                    edit.setPaintFlags(Paint.ANTI_ALIAS_FLAG | Paint.DEV_KERN_TEXT_FLAG);
                }
            }
        });

        if (item.startsWith(CheckListText.CHECKED_PREFIX)) {
            cb.setChecked(true);
            edit.setPaintFlags(edit.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            item = item.substring(CheckListText.CHECKED_PREFIX.length(), item.length()).trim();
        } else if (item.startsWith(CheckListText.UNCHECKED_PREFIX)) {
            cb.setChecked(false);
            edit.setPaintFlags(Paint.ANTI_ALIAS_FLAG | Paint.DEV_KERN_TEXT_FLAG);
            item = item.substring(CheckListText.UNCHECKED_PREFIX.length(), item.length()).trim();
        }

        edit.setOnTextViewChangeListener(this);
        edit.setIndex(index);
        edit.setText(getHighlightQueryResult(item, mUserQuery));
        return view;
    }

    public void onTextChange(int index, boolean hasText) {
        if (index >= mEditTextList.getChildCount()) {
            Log.e(TAG, "Wrong index, should not happen");
            return;
        }
        if(hasText) {
            mEditTextList.getChildAt(index).findViewById(R.id.cb_edit_item).setVisibility(View.VISIBLE);
        } else {
            mEditTextList.getChildAt(index).findViewById(R.id.cb_edit_item).setVisibility(View.GONE);
        }
    }

    private EditorContentSnapshot collectWorkingText() {
        boolean hasCheckedItems = false;
        if (mNoteSession.getCheckListMode() == TextNote.MODE_CHECK_LIST) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < mEditTextList.getChildCount(); i++) {
                View view = mEditTextList.getChildAt(i);
                NoteEditText edit = (NoteEditText) view.findViewById(R.id.et_edit_text);
                if (!TextUtils.isEmpty(edit.getText())) {
                    if (((CheckBox) view.findViewById(R.id.cb_edit_item)).isChecked()) {
                        sb.append(CheckListText.CHECKED_PREFIX).append(" ")
                                .append(edit.getText()).append("\n");
                        hasCheckedItems = true;
                    } else {
                        sb.append(CheckListText.UNCHECKED_PREFIX).append(" ")
                                .append(edit.getText()).append("\n");
                    }
                }
            }
            return new EditorContentSnapshot(sb.toString(), hasCheckedItems);
        }
        return new EditorContentSnapshot(mNoteEditor.getText().toString(), false);
    }

    private boolean saveNote() {
        EditorContentSnapshot contentSnapshot = collectWorkingText();
        boolean saved = mNoteEditViewModel.save(contentSnapshot.text);
        syncSessionFromViewModel();
        if (saved) {
            /**
             * There are two modes from List view to edit view, open one note,
             * create/edit a node. Opening node requires to the original
             * position in the list when back from edit view, while creating a
             * new node requires to the top of the list. This code
             * {@link #RESULT_OK} is used to identify the create/edit state
             */
            setResult(RESULT_OK);
        }
        return saved;
    }

    private void renderViewState(NoteEditViewState state) {
        if (state == null) {
            return;
        }
        if (mEditorScreenTitle != null) {
            mEditorScreenTitle.setText(state.isExistingNote()
                    ? R.string.notes_editor_existing : R.string.notes_editor_new);
        }
        if (mEditorScreenSubtitle != null) {
            mEditorScreenSubtitle.setText(R.string.notes_editor_hint);
        }
        if (mNoteHeaderHolder != null && mNoteHeaderHolder.tvModified != null) {
            mNoteHeaderHolder.tvModified.setText(DateUtils.formatDateTime(this,
                    state.getModifiedDate(), DateUtils.FORMAT_SHOW_DATE
                            | DateUtils.FORMAT_NUMERIC_DATE | DateUtils.FORMAT_SHOW_TIME
                            | DateUtils.FORMAT_SHOW_YEAR));
        }
        applyEditorColors(state);
        showAlertHeader(state);
        invalidateOptionsMenu();
    }

    private void syncSessionFromViewModel() {
        mNoteSession = mNoteEditViewModel.getNoteSession();
        mUserQuery = mNoteEditViewModel.getUserQuery();
    }

    private void showToast(int resId) {
        showToast(resId, Toast.LENGTH_SHORT);
    }

    private void showToast(int resId, int duration) {
        Toast.makeText(this, resId, duration).show();
    }
}
