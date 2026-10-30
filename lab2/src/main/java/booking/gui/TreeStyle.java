package booking.gui;

import booking.ds.Trace;

import javafx.scene.paint.Color;

// Цвета и подписи подсветки узлов дерева (общие для холста и легенды)
public final class TreeStyle {
    public static final Color VISITED = Color.rgb(255, 224, 138);
    public static final Color MATCH = Color.rgb(143, 214, 148);
    public static final Color PRUNED = Color.rgb(244, 166, 166);
    public static final Color INSERTED = Color.rgb(156, 201, 255);
    public static final Color PLAIN = Color.WHITE;

    private TreeStyle() { }

    public static Color fill(Trace.Kind kind) {
        if (kind == null) return PLAIN;
        return switch (kind) {
            case VISITED -> VISITED;
            case MATCH -> MATCH;
            case PRUNED -> PRUNED;
            case INSERTED -> INSERTED;
        };
    }

    public static String legend(Trace.Kind kind) {
        return switch (kind) {
            case VISITED -> "посещён";
            case MATCH -> "найден";
            case PRUNED -> "отсечён по max_end";
            case INSERTED -> "вставлен";
        };
    }
}
