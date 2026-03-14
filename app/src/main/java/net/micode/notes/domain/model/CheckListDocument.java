package net.micode.notes.domain.model;

import android.text.TextUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class CheckListDocument {
    private final List<CheckListItem> items;

    public CheckListDocument(List<CheckListItem> items) {
        this.items = Collections.unmodifiableList(new ArrayList<CheckListItem>(items));
    }

    public static CheckListDocument fromText(String text) {
        ArrayList<CheckListItem> items = new ArrayList<CheckListItem>();
        if (!TextUtils.isEmpty(text)) {
            String[] lines = text.split("\n");
            for (String line : lines) {
                if (TextUtils.isEmpty(line)) {
                    continue;
                }
                items.add(parseLine(line));
            }
        }
        return new CheckListDocument(items);
    }

    public List<CheckListItem> getItems() {
        return items;
    }

    public CheckListDocument insertUncheckedItem(int index, String text) {
        ArrayList<CheckListItem> updatedItems = new ArrayList<CheckListItem>(items);
        int safeIndex = Math.max(0, Math.min(index, updatedItems.size()));
        updatedItems.add(safeIndex, new CheckListItem(false, text));
        return new CheckListDocument(updatedItems);
    }

    public CheckListDocument mergeIntoPrevious(int index) {
        if (index <= 0 || index >= items.size()) {
            return this;
        }
        ArrayList<CheckListItem> updatedItems = new ArrayList<CheckListItem>(items);
        CheckListItem currentItem = updatedItems.remove(index);
        CheckListItem previousItem = updatedItems.get(index - 1);
        updatedItems.set(index - 1, new CheckListItem(previousItem.isChecked(),
                previousItem.getText() + currentItem.getText()));
        return new CheckListDocument(updatedItems);
    }

    public boolean hasCheckedItems() {
        for (CheckListItem item : items) {
            if (item.isChecked()) {
                return true;
            }
        }
        return false;
    }

    public String toText() {
        StringBuilder builder = new StringBuilder();
        for (CheckListItem item : items) {
            if (TextUtils.isEmpty(item.getText())) {
                continue;
            }
            builder.append(item.isChecked() ? CheckListText.CHECKED_PREFIX
                    : CheckListText.UNCHECKED_PREFIX)
                    .append(" ")
                    .append(item.getText())
                    .append("\n");
        }
        return builder.toString();
    }

    private static CheckListItem parseLine(String line) {
        if (line.startsWith(CheckListText.CHECKED_PREFIX)) {
            return new CheckListItem(true,
                    line.substring(CheckListText.CHECKED_PREFIX.length()).trim());
        }
        if (line.startsWith(CheckListText.UNCHECKED_PREFIX)) {
            return new CheckListItem(false,
                    line.substring(CheckListText.UNCHECKED_PREFIX.length()).trim());
        }
        return new CheckListItem(false, line);
    }
}