package net.micode.notes.domain.model;

public final class CheckListItem {
    private final boolean checked;
    private final String text;

    public CheckListItem(boolean checked, String text) {
        this.checked = checked;
        this.text = text == null ? "" : text;
    }

    public boolean isChecked() {
        return checked;
    }

    public String getText() {
        return text;
    }
}