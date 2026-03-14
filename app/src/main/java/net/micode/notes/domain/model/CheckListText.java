package net.micode.notes.domain.model;

import android.text.TextUtils;

public final class CheckListText {
    public static final String CHECKED_PREFIX = String.valueOf('\u221A');
    public static final String UNCHECKED_PREFIX = String.valueOf('\u25A1');

    private CheckListText() {
    }

    public static String stripMarkers(String text) {
        if (text == null) {
            return "";
        }
        return text.replace(CHECKED_PREFIX, "").replace(UNCHECKED_PREFIX, "");
    }

    public static String normalizeForPlainText(String checkListText, boolean hasCheckedItems) {
        if (TextUtils.isEmpty(checkListText)) {
            return "";
        }
        if (hasCheckedItems) {
            return checkListText;
        }
        return checkListText.replace(UNCHECKED_PREFIX + " ", "");
    }
}