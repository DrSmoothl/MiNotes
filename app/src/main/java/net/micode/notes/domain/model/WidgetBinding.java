package net.micode.notes.domain.model;

public final class WidgetBinding {
    private final int widgetId;
    private final int widgetType;

    public WidgetBinding(int widgetId, int widgetType) {
        this.widgetId = widgetId;
        this.widgetType = widgetType;
    }

    public int getWidgetId() {
        return widgetId;
    }

    public int getWidgetType() {
        return widgetType;
    }
}