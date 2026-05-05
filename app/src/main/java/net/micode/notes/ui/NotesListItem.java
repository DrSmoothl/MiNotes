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

import android.content.Context;
import android.text.format.DateUtils;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.google.android.material.card.MaterialCardView;

import net.micode.notes.R;
import net.micode.notes.data.Notes;
import net.micode.notes.tool.ResourceParser;
import net.micode.notes.tool.ResourceParser.NoteColorResources;
import net.micode.notes.tool.TextSnippetFormatter;


public class NotesListItem extends LinearLayout {
    private MaterialCardView mCardView;
    private ImageView mAlert;
    private TextView mTitle;
    private TextView mTime;
    private TextView mCallName;
    private NoteItemData mItemData;
    private CheckBox mCheckBox;

    public NotesListItem(Context context) {
        super(context);
        inflate(context, R.layout.note_item, this);
        mCardView = (MaterialCardView) findViewById(R.id.note_item);
        mAlert = (ImageView) findViewById(R.id.iv_alert_icon);
        mTitle = (TextView) findViewById(R.id.tv_title);
        mTime = (TextView) findViewById(R.id.tv_time);
        mCallName = (TextView) findViewById(R.id.tv_name);
        mCheckBox = (CheckBox) findViewById(android.R.id.checkbox);
    }

    public void bind(Context context, NoteItemData data, boolean choiceMode, boolean checked) {
        if (choiceMode && data.getType() == Notes.TYPE_NOTE) {
            mCheckBox.setVisibility(View.VISIBLE);
            mCheckBox.setChecked(checked);
        } else {
            mCheckBox.setVisibility(View.GONE);
        }

        mItemData = data;
        if (data.getId() == Notes.ID_CALL_RECORD_FOLDER) {
            mCallName.setVisibility(View.GONE);
            mAlert.setVisibility(View.VISIBLE);
            mTitle.setTextAppearance(R.style.TextAppearancePrimaryItem);
            mTitle.setText(context.getString(R.string.format_folder_title,
                    context.getString(R.string.call_record_folder_name),
                    context.getString(R.string.format_folder_files_count, data.getNotesCount())));
            mAlert.setImageResource(R.drawable.call_record);
            mAlert.setContentDescription(context.getString(R.string.call_record_folder_name));
        } else if (data.getParentId() == Notes.ID_CALL_RECORD_FOLDER) {
            mCallName.setVisibility(View.VISIBLE);
            mCallName.setText(data.getCallName());
            mTitle.setTextAppearance(R.style.TextAppearanceSecondaryItem);
            mTitle.setText(TextSnippetFormatter.format(data.getSnippet()));
            if (data.hasAlert()) {
                mAlert.setImageResource(R.drawable.clock);
                mAlert.setContentDescription(
                        context.getString(R.string.note_alert_icon_description));
                mAlert.setVisibility(View.VISIBLE);
            } else {
                mAlert.setContentDescription(null);
                mAlert.setVisibility(View.GONE);
            }
        } else {
            mCallName.setVisibility(View.GONE);
            mTitle.setTextAppearance(R.style.TextAppearancePrimaryItem);

            if (data.getType() == Notes.TYPE_FOLDER) {
                mTitle.setText(context.getString(R.string.format_folder_title,
                        data.getSnippet(),
                        context.getString(R.string.format_folder_files_count,
                                data.getNotesCount())));
                mAlert.setContentDescription(null);
                mAlert.setVisibility(View.GONE);
            } else {
                mTitle.setText(TextSnippetFormatter.format(data.getSnippet()));
                if (data.hasAlert()) {
                    mAlert.setImageResource(R.drawable.clock);
                    mAlert.setContentDescription(
                            context.getString(R.string.note_alert_icon_description));
                    mAlert.setVisibility(View.VISIBLE);
                } else {
                    mAlert.setContentDescription(null);
                    mAlert.setVisibility(View.GONE);
                }
            }
        }
        mTime.setText(DateUtils.getRelativeTimeSpanString(data.getModifiedDate()));

        applyCardStyle(data);
    }

    private void applyCardStyle(NoteItemData data) {
        mCardView.setStrokeColor(ContextCompat.getColor(getContext(), R.color.notes_outline_soft));
        if (data.getId() == Notes.ID_CALL_RECORD_FOLDER) {
            mCardView.setCardBackgroundColor(ContextCompat.getColor(getContext(),
                    R.color.notes_call_card));
            return;
        }

        if (data.getType() == Notes.TYPE_FOLDER || data.getType() == Notes.TYPE_SYSTEM) {
            mCardView.setCardBackgroundColor(ContextCompat.getColor(getContext(),
                    R.color.notes_folder_card));
            return;
        }

        switch (data.getBgColorId()) {
            case ResourceParser.BLUE:
        mCardView.setCardBackgroundColor(ContextCompat.getColor(getContext(),
            NoteColorResources.getNoteCardBackgroundColor(ResourceParser.BLUE)));
                break;
            case ResourceParser.WHITE:
        mCardView.setCardBackgroundColor(ContextCompat.getColor(getContext(),
            NoteColorResources.getNoteCardBackgroundColor(ResourceParser.WHITE)));
                break;
            case ResourceParser.GREEN:
        mCardView.setCardBackgroundColor(ContextCompat.getColor(getContext(),
            NoteColorResources.getNoteCardBackgroundColor(ResourceParser.GREEN)));
                break;
            case ResourceParser.RED:
        mCardView.setCardBackgroundColor(ContextCompat.getColor(getContext(),
            NoteColorResources.getNoteCardBackgroundColor(ResourceParser.RED)));
                break;
            case ResourceParser.YELLOW:
            default:
        mCardView.setCardBackgroundColor(ContextCompat.getColor(getContext(),
            NoteColorResources.getNoteCardBackgroundColor(ResourceParser.YELLOW)));
                break;
        }
    }

    public NoteItemData getItemData() {
        return mItemData;
    }
}
