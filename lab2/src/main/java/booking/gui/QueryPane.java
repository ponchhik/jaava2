package booking.gui;

import booking.ds.ArrayBuf;
import booking.ds.Trace;
import booking.model.Booking;
import booking.service.BookingService;

import javafx.beans.value.ObservableValue;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;

// Панель операций над деревом: поиск/удаление по ключу и дополнительные операции варианта.
// Только читает поля ввода и вызывает BookingService; результат отдаёт через ResultSink.
public final class QueryPane extends VBox {
    private static final String DEFAULT_START = "2026-10-05 10:00";
    private static final String DEFAULT_END = "2026-10-05 11:30";
    private static final String DEFAULT_ROOM = "Альфа";
    private static final int DATE_COLUMNS = 14, ROOM_COLUMNS = 8;
    private static final int PREVIEW_LIMIT = 8;
    private static final double SPACING = 4;
    private static final String NO_ROOM_INTERVAL_HINT = "(интервал — из строки выше)";
    private static final String BAD_INTERVAL = "Конец интервала должен быть позже начала";

    public interface ResultSink {
        void show(Trace trace, String message, Booking select);
        void error(String message);
    }

    private final BookingService service;
    private final ResultSink sink;
    private final TextField keyField = field(DEFAULT_START, DATE_COLUMNS);
    private final TextField fromField = field(DEFAULT_START, DATE_COLUMNS);
    private final TextField toField = field(DEFAULT_END, DATE_COLUMNS);
    private final TextField roomField = field(DEFAULT_ROOM, ROOM_COLUMNS);
    private final Button find = new Button("Найти по началу");
    private final Button remove = new Button("Удалить по началу");
    private final Button overlap = new Button("Пересекающиеся");
    private final Button conflict = new Button("Конфликт комнаты");

    public QueryPane(BookingService service, ResultSink sink) {
        super(SPACING);
        this.service = service;
        this.sink = sink;
        setPadding(new Insets(SPACING));

        find.setOnAction(e -> onFind());
        remove.setOnAction(e -> onRemove());
        overlap.setOnAction(e -> onOverlap());
        conflict.setOnAction(e -> onConflict());

        getChildren().addAll(
                new Label("Операции над деревом (формат даты: " + Formats.DATE_HINT + ")"),
                row(new Label("Начало (ключ):"), keyField, find, remove),
                row(new Label("Интервал с:"), fromField, new Label("по:"), toField, overlap),
                row(new Label("Комната:"), roomField, conflict, new Label(NO_ROOM_INTERVAL_HINT)));
    }

    public void bindDisable(ObservableValue<Boolean> disabled) {
        for (Button b : new Button[]{find, remove, overlap, conflict}) b.disableProperty().bind(disabled);
    }

    private static TextField field(String text, int columns) {
        TextField f = new TextField(text);
        f.setPrefColumnCount(columns);
        return f;
    }

    private static HBox row(Node... parts) {
        HBox box = new HBox(SPACING, parts);
        box.setFillHeight(true);
        return box;
    }

    // null — формат неверный (пользователь уже получил сообщение)
    private LocalDateTime parse(TextField f) {
        try {
            return Formats.parse(f.getText());
        } catch (DateTimeParseException ex) {
            sink.error(Formats.badDateMessage(f.getText()));
            return null;
        }
    }

    private String stats(Trace t) {
        return "посещено узлов: " + t.count(Trace.Kind.VISITED) + " из " + service.size()
                + (t.count(Trace.Kind.PRUNED) > 0 ? ", отсечено поддеревьев: " + t.count(Trace.Kind.PRUNED) : "");
    }

    private void onFind() {
        LocalDateTime key = parse(keyField);
        if (key == null) return;
        Trace t = new Trace();
        Booking b = service.findByStart(key, t);
        String label = key.format(Formats.DATE_TIME);
        sink.show(t, b == null ? "Поиск по началу " + label + ": не найдено, " + stats(t)
                : "Найдено: " + b + ", " + stats(t), b);
    }

    private void onRemove() {
        LocalDateTime key = parse(keyField);
        if (key == null) return;
        Trace t = new Trace();
        boolean ok = service.removeByStart(key, t);
        String label = key.format(Formats.DATE_TIME);
        sink.show(t, ok ? "Удалена бронь с началом " + label + ", " + stats(t)
                : "Удаление: брони с таким началом нет, " + stats(t), null);
    }

    private void onOverlap() {
        LocalDateTime from = parse(fromField), to = parse(toField);
        if (from == null || to == null) return;
        if (!to.isAfter(from)) { sink.error(BAD_INTERVAL); return; }
        Trace t = new Trace();
        ArrayBuf<Booking> r = service.findOverlapping(from, to, t);
        StringBuilder sb = new StringBuilder("Пересекаются с [" + from.format(Formats.DATE_TIME) + "; "
                + to.format(Formats.DATE_TIME) + "): " + r.size() + " шт., " + stats(t));
        for (int i = 0; i < Math.min(r.size(), PREVIEW_LIMIT); i++) sb.append("\n    ").append(r.get(i));
        if (r.size() > PREVIEW_LIMIT) sb.append("\n    … и ещё ").append(r.size() - PREVIEW_LIMIT);
        sink.show(t, sb.toString(), r.isEmpty() ? null : r.get(0));
    }

    private void onConflict() {
        LocalDateTime from = parse(fromField), to = parse(toField);
        if (from == null || to == null) return;
        if (!to.isAfter(from)) { sink.error(BAD_INTERVAL); return; }
        String room = roomField.getText().trim();
        Trace t = new Trace();
        Booking c = service.findConflict(room, from, to, -1, t);
        sink.show(t, c == null ? "Комната «" + room + "» свободна, " + stats(t)
                : "Конфликт комнаты «" + room + "» с " + c + ", " + stats(t), c);
    }
}
