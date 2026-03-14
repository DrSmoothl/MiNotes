package net.micode.notes.tool;

public final class TextSnippetFormatter {
    private TextSnippetFormatter() {
    }

    public static String format(String snippet) {
        if (snippet == null) {
            return null;
        }
        String normalized = snippet.trim();
        int index = normalized.indexOf('\n');
        if (index != -1) {
            normalized = normalized.substring(0, index);
        }
        return normalized;
    }
}