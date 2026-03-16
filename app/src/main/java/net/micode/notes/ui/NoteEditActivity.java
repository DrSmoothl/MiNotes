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
import android.text.Editable;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.TextWatcher;
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
import net.micode.notes.domain.model.CheckListDocument;
import net.micode.notes.domain.model.CheckListItem;
import net.micode.notes.domain.model.CheckListText;
import net.micode.notes.domain.model.NoteEditorSession;
import net.micode.notes.domain.usecase.editor.DeleteNoteUseCase;
import net.micode.notes.domain.usecase.editor.StartNoteEditorSessionUseCase;
import net.micode.notes.inject.NotesApplicationGraph;
import net.micode.notes.tool.ResourceParser;
import net.micode.notes.tool.ResourceParser.NoteColorResources;
import net.micode.notes.tool.ResourceParser.TextAppearanceResources;
import net.micode.notes.ui.DateTimePickerDialog.OnDateTimeSetListener;
import net.micode.notes.ui.NoteEditText.DeleteRequest;
import net.micode.notes.ui.NoteEditText.OnTextViewChangeListener;
import net.micode.notes.ui.NoteEditText.SplitRequest;

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
    private boolean mRenderingEditorContent;

    private static final class EditorContentSnapshot {
        private final String text;
        private final boolean hasCheckedItems;

        private EditorContentSnapshot(String text, boolean hasCheckedItems) {
            this.text = text;
            this.hasCheckedItems = hasCheckedItems;
        }
    }

    private static final class CheckListFocusRequest {
        private final int index;
        private final int selection;

        private CheckListFocusRequest(int index, int selection) {
            this.index = index;
            this.selection = selection;
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
        NoteEditLaunchRequest request = buildLaunchRequest(intent);
        if (!request.isValid()) {
            Log.e(TAG, "Intent not specified action, should not support");
            finish();
            return false;
        }
        if (!mNoteEditViewModel.launch(request)) {
            if (request.isExistingNoteRequest()) {
                redirectToNotesList();
                showToast(R.string.error_note_not_exist);
            } else {
                finish();
            }
            return false;
        }
        syncSessionFromViewModel();
        if (mNoteSession == null) {
            if (request.isExistingNoteRequest()) {
                redirectToNotesList();
                showToast(R.string.error_note_not_exist);
            } else {
                finish();
            }
            return false;
        }
        if (request.shouldHideKeyboard()) {
            getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
        } else if (request.shouldShowKeyboard()) {
            getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        }
        return true;
    }

    private NoteEditLaunchRequest buildLaunchRequest(Intent intent) {
        if (intent == null) {
            return NoteEditLaunchRequest.invalid();
        }
        if (TextUtils.equals(Intent.ACTION_VIEW, intent.getAction())) {
            long noteId = intent.getLongExtra(Intent.EXTRA_UID, 0);
            String userQuery = "";
            if (intent.hasExtra(SearchManager.EXTRA_DATA_KEY)) {
                String searchDataKey = intent.getStringExtra(SearchManager.EXTRA_DATA_KEY);
                if (!TextUtils.isEmpty(searchDataKey)) {
                    noteId = Long.parseLong(searchDataKey);
                }
                userQuery = intent.getStringExtra(SearchManager.USER_QUERY);
            }
            return NoteEditLaunchRequest.openExisting(noteId, userQuery);
        }
        if (TextUtils.equals(Intent.ACTION_INSERT_OR_EDIT, intent.getAction())) {
            String phoneNumber = intent.getStringExtra(Intent.EXTRA_PHONE_NUMBER);
            long callDate = intent.getLongExtra(Notes.INTENT_EXTRA_CALL_DATE, 0);
            if (callDate != 0L && TextUtils.isEmpty(phoneNumber)) {
                Log.w(TAG, "The call record number is null");
            }
            return NoteEditLaunchRequest.createOrEdit(
                    intent.getLongExtra(Notes.INTENT_EXTRA_FOLDER_ID, 0),
                    intent.getIntExtra(Notes.INTENT_EXTRA_WIDGET_ID,
                            AppWidgetManager.INVALID_APPWIDGET_ID),
                    intent.getIntExtra(Notes.INTENT_EXTRA_WIDGET_TYPE,
                            Notes.TYPE_WIDGET_INVALIDE),
                    intent.getIntExtra(Notes.INTENT_EXTRA_BACKGROUND_ID,
                            ResourceParser.getDefaultBgId(this)),
                    phoneNumber,
                    callDate);
        }
        return NoteEditLaunchRequest.invalid();
    }

    private void redirectToNotesList() {
        Intent jump = new Intent(this, NotesListActivity.class);
        startActivity(jump);
        finish();
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
        mRenderingEditorContent = true;
        try {
            mNoteEditor.setTextAppearance(TextAppearanceResources
                .getTexAppearanceResource(mFontSizeId));
            if (state.getCheckListMode() == TextNote.MODE_CHECK_LIST) {
                renderCheckListDocument(CheckListDocument.fromText(state.getContent()), null);
            } else {
                mNoteEditor.setText(getHighlightQueryResult(state.getContent(), mUserQuery));
                mNoteEditor.setSelection(mNoteEditor.getText().length());
                mEditTextList.setVisibility(View.GONE);
                mNoteEditor.setVisibility(View.VISIBLE);
            }
        } finally {
            mRenderingEditorContent = false;
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
        NoteEditViewState state = mNoteEditViewModel.getCurrentState();
        /**
         * For new note without note id, we should firstly save it to
         * generate a id. If the editing note is not worth saving, there
         * is no id which is equivalent to create new note
         */
        if (state != null && !state.isExistingNote()) {
            saveCurrentEditorContent();
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
        mNoteEditor.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                NoteEditViewState state = mNoteEditViewModel.getCurrentState();
                if (mRenderingEditorContent || state == null
                        || state.getCheckListMode() == TextNote.MODE_CHECK_LIST) {
                    return;
                }
                mNoteEditViewModel.updateWorkingText(s == null ? "" : s.toString());
                syncSessionFromViewModel();
            }
        });
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
        if (saveCurrentEditorContent()) {
            Log.d(TAG, "Note data was saved with length:" + mNoteSession.getContent().length());
        }
    }

    public void onClick(View v) {
        if (v.getId() == R.id.btn_set_bg_color) {
            showBackgroundPickerDialog();
        }
    }

    private void handleBackNavigation() {
        mNoteEditViewModel.requestClose(pushCurrentEditorContent().text);
        syncSessionFromViewModel();
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
        NoteEditViewState state = mNoteEditViewModel.getCurrentState();
        if (state == null) {
            return true;
        }
        menu.clear();
        if (state.usesCallRecordMenu()) {
            getMenuInflater().inflate(R.menu.call_note_edit, menu);
        } else {
            getMenuInflater().inflate(R.menu.note_edit, menu);
        }
        if (state.isCheckListMode()) {
            menu.findItem(R.id.menu_list_mode).setTitle(R.string.menu_normal_mode);
        } else {
            menu.findItem(R.id.menu_list_mode).setTitle(R.string.menu_list_mode);
        }
        if (state.shouldShowAddReminderAction()) {
            menu.findItem(R.id.menu_delete_remind).setVisible(false);
        } else if (state.shouldShowClearReminderAction()) {
            menu.findItem(R.id.menu_alert).setVisible(false);
        }
        MenuItem shareItem = menu.findItem(R.id.menu_share);
        if (shareItem != null) {
            shareItem.setEnabled(state.canShare());
        }
        MenuItem deleteItem = menu.findItem(R.id.menu_delete);
        if (deleteItem != null) {
            deleteItem.setEnabled(state.canDelete());
        }
        MenuItem alertItem = menu.findItem(R.id.menu_alert);
        if (alertItem != null) {
            alertItem.setEnabled(state.canSetReminder());
        }
        MenuItem listModeItem = menu.findItem(R.id.menu_list_mode);
        if (listModeItem != null) {
            listModeItem.setEnabled(state.canToggleListMode());
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        NoteEditViewState state = mNoteEditViewModel.getCurrentState();
        if (state == null) {
            return true;
        }
        switch (item.getItemId()) {
            case R.id.menu_new_note:
                createNewNote();
                break;
            case R.id.menu_delete:
                showDeleteNoteConfirmation();
                break;
            case R.id.menu_font_size:
                showFontSizePickerDialog();
                break;
            case R.id.menu_list_mode:
                if (!state.canToggleListMode()) {
                    break;
                }
                toggleListMode(state);
                break;
            case R.id.menu_share:
                if (!state.canShare()) {
                    break;
                }
                shareCurrentNote();
                break;
            case R.id.menu_alert:
                if (!state.canSetReminder()) {
                    break;
                }
                setReminder();
                break;
            case R.id.menu_delete_remind:
                clearReminder();
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
        NoteEditViewState state = mNoteEditViewModel.getCurrentState();
        int backgroundId = state == null ? ResourceParser.getDefaultBgId(this)
                : state.getBackgroundColorId();
        for (int i = 0; i < BACKGROUND_IDS.length; i++) {
            if (BACKGROUND_IDS[i] == backgroundId) {
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
        NoteEditViewState state = mNoteEditViewModel.getCurrentState();
        if (state != null && state.isCheckListMode()) {
            renderCheckListDocument(CheckListDocument.fromText(pushCurrentEditorContent().text), null);
        } else {
            mNoteEditor.setTextAppearance(
                    TextAppearanceResources.getTexAppearanceResource(mFontSizeId));
        }
    }

    private void setReminder() {
        DateTimePickerDialog d = new DateTimePickerDialog(this, System.currentTimeMillis());
        d.setOnDateTimeSetListener(new OnDateTimeSetListener() {
            public void OnDateTimeSet(AlertDialog dialog, long date) {
                if (!applyReminderFromCurrentContent(date, true)) {
                    showToast(R.string.error_note_empty_for_clock);
                }
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
        mNoteEditViewModel.requestCreateNew(pushCurrentEditorContent().text);
        syncSessionFromViewModel();
    }

    private void deleteCurrentNote() {
        mNoteEditViewModel.requestDeleteAndClose();
        syncSessionFromViewModel();
    }

    private void showDeleteNoteConfirmation() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.alert_title_delete));
        builder.setIcon(android.R.drawable.ic_dialog_alert);
        builder.setMessage(getString(R.string.alert_message_delete_note));
        builder.setPositiveButton(android.R.string.ok,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        deleteCurrentNote();
                    }
                });
        builder.setNegativeButton(android.R.string.cancel, null);
        builder.show();
    }

    private void toggleListMode(NoteEditViewState state) {
        int oldMode = state.getCheckListMode();
        int newMode = oldMode == 0 ? TextNote.MODE_CHECK_LIST : 0;
        EditorContentSnapshot modeSnapshot = pushCurrentEditorContent();
        mNoteEditViewModel.changeCheckListMode(modeSnapshot.text,
                modeSnapshot.hasCheckedItems, newMode);
        syncSessionFromViewModel();
        renderEditorContent(mNoteEditViewModel.getCurrentState());
    }

    private void shareCurrentNote() {
        pushCurrentEditorContent();
        sendTo(this, mNoteSession.getContent());
    }

    private void clearReminder() {
        if (!applyReminderFromCurrentContent(0, false)) {
            showToast(R.string.error_note_empty_for_clock);
        }
    }

    private EditorContentSnapshot pushCurrentEditorContent() {
        EditorContentSnapshot snapshot = collectWorkingText();
        mNoteEditViewModel.updateWorkingText(snapshot.text);
        syncSessionFromViewModel();
        return snapshot;
    }

    private boolean applyReminderFromCurrentContent(long alertDate, boolean enabled) {
        boolean applied = mNoteEditViewModel.applyReminder(pushCurrentEditorContent().text,
                alertDate, enabled);
        syncSessionFromViewModel();
        return applied;
    }

    public void onDeleteRequested(DeleteRequest request) {
        int childCount = mEditTextList.getChildCount();
        if (childCount == 1) {
            return;
        }
        CheckListDocument document = collectCheckListDocument();
        int focusIndex = Math.max(0, request.getIndex() - 1);
        int previousLength = document.getItems().get(focusIndex).getText().length();
        CheckListDocument updatedDocument = document.mergeIntoPrevious(request.getIndex());
        mNoteEditViewModel.updateWorkingText(updatedDocument.toText());
        syncSessionFromViewModel();
        renderCheckListDocument(updatedDocument, new CheckListFocusRequest(focusIndex, previousLength));
    }

    public void onSplitRequested(SplitRequest request) {
        /**
         * Should not happen, check for debug
         */
        if(request.getIndex() > mEditTextList.getChildCount()) {
            Log.e(TAG, "Index out of mEditTextList boundrary, should not happen");
        }

        CheckListDocument updatedDocument = collectCheckListDocument()
                .insertUncheckedItem(request.getIndex(), request.getTrailingText());
        mNoteEditViewModel.updateWorkingText(updatedDocument.toText());
        syncSessionFromViewModel();
        renderCheckListDocument(updatedDocument, new CheckListFocusRequest(request.getIndex(), 0));
    }

    private void renderCheckListDocument(CheckListDocument document,
            CheckListFocusRequest focusRequest) {
        mRenderingEditorContent = true;
        try {
            mEditTextList.removeAllViews();
            int index = 0;
            for (CheckListItem item : document.getItems()) {
                mEditTextList.addView(getListItem(item, index));
                index++;
            }
            mEditTextList.addView(getListItem(new CheckListItem(false, ""), index));
            applyCheckListFocus(focusRequest, index);

            mNoteEditor.setVisibility(View.GONE);
            mEditTextList.setVisibility(View.VISIBLE);
        } finally {
            mRenderingEditorContent = false;
        }
    }

    private void applyCheckListFocus(CheckListFocusRequest focusRequest, int fallbackIndex) {
        int targetIndex = focusRequest == null ? fallbackIndex : focusRequest.index;
        int selection = focusRequest == null ? 0 : focusRequest.selection;
        NoteEditText target = (NoteEditText) mEditTextList.getChildAt(targetIndex)
                .findViewById(R.id.et_edit_text);
        target.requestFocus();
        target.setSelection(Math.min(selection, target.length()));
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

    private View getListItem(CheckListItem item, int index) {
        View view = LayoutInflater.from(this).inflate(R.layout.note_edit_list_item, null);
        final NoteEditText edit = (NoteEditText) view.findViewById(R.id.et_edit_text);
        edit.setTextAppearance(TextAppearanceResources.getTexAppearanceResource(mFontSizeId));
        CheckBox cb = ((CheckBox) view.findViewById(R.id.cb_edit_item));

        String itemText = item.getText();
        if (itemText.startsWith(CheckListText.CHECKED_PREFIX)) {
            cb.setChecked(true);
            edit.setPaintFlags(edit.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            itemText = itemText.substring(CheckListText.CHECKED_PREFIX.length(), itemText.length()).trim();
        } else if (itemText.startsWith(CheckListText.UNCHECKED_PREFIX)) {
            cb.setChecked(false);
            edit.setPaintFlags(Paint.ANTI_ALIAS_FLAG | Paint.DEV_KERN_TEXT_FLAG);
            itemText = itemText.substring(CheckListText.UNCHECKED_PREFIX.length(), itemText.length()).trim();
        } else if (item.isChecked()) {
            cb.setChecked(true);
            edit.setPaintFlags(edit.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
        }

        cb.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                applyCheckedStyle(edit, isChecked);
                syncCheckListStateFromViews();
            }
        });

        edit.setOnTextViewChangeListener(this);
        edit.setIndex(index);
        edit.setText(getHighlightQueryResult(itemText, mUserQuery));
        edit.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (mRenderingEditorContent) {
                    return;
                }
                syncCheckListStateFromViews();
            }
        });
        return view;
    }

    public void onTextPresenceChanged(int index, boolean hasText) {
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

    private void applyCheckedStyle(NoteEditText edit, boolean isChecked) {
        if (isChecked) {
            edit.setPaintFlags(edit.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
        } else {
            edit.setPaintFlags(Paint.ANTI_ALIAS_FLAG | Paint.DEV_KERN_TEXT_FLAG);
        }
    }

    private void syncCheckListStateFromViews() {
        NoteEditViewState state = mNoteEditViewModel.getCurrentState();
        if (state == null || state.getCheckListMode() != TextNote.MODE_CHECK_LIST) {
            return;
        }
        CheckListDocument document = collectCheckListDocument();
        mNoteEditViewModel.updateWorkingText(document.toText());
        syncSessionFromViewModel();
    }

    private EditorContentSnapshot collectWorkingText() {
        boolean hasCheckedItems = false;
        NoteEditViewState state = mNoteEditViewModel.getCurrentState();
        if (state != null && state.getCheckListMode() == TextNote.MODE_CHECK_LIST) {
            CheckListDocument document = collectCheckListDocument();
            return new EditorContentSnapshot(document.toText(), document.hasCheckedItems());
        }
        return new EditorContentSnapshot(mNoteEditor.getText().toString(), false);
    }

    private CheckListDocument collectCheckListDocument() {
        java.util.ArrayList<CheckListItem> items = new java.util.ArrayList<CheckListItem>();
        for (int i = 0; i < mEditTextList.getChildCount(); i++) {
            View view = mEditTextList.getChildAt(i);
            NoteEditText edit = (NoteEditText) view.findViewById(R.id.et_edit_text);
            if (TextUtils.isEmpty(edit.getText())) {
                continue;
            }
            boolean checked = ((CheckBox) view.findViewById(R.id.cb_edit_item)).isChecked();
            items.add(new CheckListItem(checked, edit.getText().toString()));
        }
        return new CheckListDocument(items);
    }

    private boolean saveCurrentEditorContent() {
        boolean saved = mNoteEditViewModel.save(pushCurrentEditorContent().text);
        syncSessionFromViewModel();
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
        handlePendingAction(state);
    }

    private void handlePendingAction(NoteEditViewState state) {
        if (state.getPendingAction() == NoteEditViewState.PendingAction.NONE) {
            return;
        }
        long actionId = state.getPendingActionId();
        if (state.pendingActionSetsResultOk()) {
            setResult(RESULT_OK);
        }
        switch (state.getPendingAction()) {
            case OPEN_NEW_NOTE:
                openPendingNewNote(actionId, state.getPendingActionFolderId());
                return;
            case CLOSE_EDITOR:
                closeFromPendingAction(actionId);
                return;
            case NONE:
            default:
                return;
        }
    }

    private void openPendingNewNote(long actionId, long folderId) {
        mNoteEditViewModel.markPendingActionHandled(actionId);
        Intent intent = new Intent(this, NoteEditActivity.class);
        intent.setAction(Intent.ACTION_INSERT_OR_EDIT);
        intent.putExtra(Notes.INTENT_EXTRA_FOLDER_ID, folderId);
        startActivity(intent);
        finish();
    }

    private void closeFromPendingAction(long actionId) {
        mNoteEditViewModel.markPendingActionHandled(actionId);
        finish();
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
