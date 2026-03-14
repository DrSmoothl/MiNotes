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
import android.view.View;
import android.view.ViewGroup;
import androidx.recyclerview.widget.RecyclerView;

import net.micode.notes.data.Notes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;


public class NotesListAdapter extends RecyclerView.Adapter<NotesListAdapter.NoteViewHolder> {
    public interface NoteItemListener {
        void onItemClick(NoteItemData item, int position);

        boolean onItemLongClick(View view, NoteItemData item, int position);
    }

    private final Context mContext;
    private final NoteItemListener mListener;
    private final ArrayList<NoteItemData> mItems;
    private HashMap<Integer, Boolean> mSelectedIndex;
    private int mNotesCount;
    private boolean mChoiceMode;

    public static class AppWidgetAttribute {
        public int widgetId;
        public int widgetType;
    };

    public NotesListAdapter(Context context, NoteItemListener listener) {
        mSelectedIndex = new HashMap<Integer, Boolean>();
        mContext = context;
        mListener = listener;
        mItems = new ArrayList<NoteItemData>();
        mNotesCount = 0;
        setHasStableIds(true);
    }

    @Override
    public NoteViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new NoteViewHolder(new NotesListItem(parent.getContext()));
    }

    @Override
    public void onBindViewHolder(NoteViewHolder holder, int position) {
        final NoteItemData itemData = mItems.get(position);
        holder.itemView.bind(mContext, itemData, mChoiceMode, isSelectedItem(position));
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mListener.onItemClick(itemData, holder.getBindingAdapterPosition());
            }
        });
        holder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                return mListener.onItemLongClick(view, itemData, holder.getBindingAdapterPosition());
            }
        });
    }

    @Override
    public long getItemId(int position) {
        return mItems.get(position).getId();
    }

    @Override
    public int getItemCount() {
        return mItems.size();
    }

    public void submitList(List<NoteItemData> items) {
        mItems.clear();
        mSelectedIndex.clear();
        if (items != null) {
            mItems.addAll(items);
        }
        calcNotesCount();
        notifyDataSetChanged();
    }

    public void setCheckedItem(final int position, final boolean checked) {
        mSelectedIndex.put(position, checked);
        notifyDataSetChanged();
    }

    public boolean isInChoiceMode() {
        return mChoiceMode;
    }

    public void setChoiceMode(boolean mode) {
        mSelectedIndex.clear();
        mChoiceMode = mode;
    }

    public void selectAll(boolean checked) {
        for (int i = 0; i < getItemCount(); i++) {
            if (mItems.get(i).getType() == Notes.TYPE_NOTE) {
                setCheckedItem(i, checked);
            }
        }
    }

    public HashSet<Long> getSelectedItemIds() {
        HashSet<Long> itemSet = new HashSet<Long>();
        for (Integer position : mSelectedIndex.keySet()) {
            if (mSelectedIndex.get(position) == true) {
                Long id = mItems.get(position).getId();
                if (id == Notes.ID_ROOT_FOLDER) {
                } else {
                    itemSet.add(id);
                }
            }
        }

        return itemSet;
    }

    public HashSet<AppWidgetAttribute> getSelectedWidget() {
        HashSet<AppWidgetAttribute> itemSet = new HashSet<AppWidgetAttribute>();
        for (Integer position : mSelectedIndex.keySet()) {
            if (mSelectedIndex.get(position) == true) {
                NoteItemData item = mItems.get(position);
                AppWidgetAttribute widget = new AppWidgetAttribute();
                widget.widgetId = item.getWidgetId();
                widget.widgetType = item.getWidgetType();
                itemSet.add(widget);
            }
        }
        return itemSet;
    }

    public int getSelectedCount() {
        int count = 0;
        for (Boolean selected : mSelectedIndex.values()) {
            if (selected == true) {
                count++;
            }
        }
        return count;
    }

    public boolean isAllSelected() {
        int checkedCount = getSelectedCount();
        return (checkedCount != 0 && checkedCount == mNotesCount);
    }

    public boolean isSelectedItem(final int position) {
        Boolean selected = mSelectedIndex.get(position);
        return selected != null && selected;
    }

    public NoteItemData getItem(int position) {
        return mItems.get(position);
    }

    public static final class NoteViewHolder extends RecyclerView.ViewHolder {
        final NotesListItem itemView;

        public NoteViewHolder(NotesListItem itemView) {
            super(itemView);
            this.itemView = itemView;
        }
    }

    private void calcNotesCount() {
        mNotesCount = 0;
        for (NoteItemData item : mItems) {
            if (item.getType() == Notes.TYPE_NOTE) {
                mNotesCount++;
            }
        }
    }
}
