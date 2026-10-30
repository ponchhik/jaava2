package booking.gui;

import booking.model.Booking;
import booking.model.RecurringBooking;
import booking.model.SingleBooking;

import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.stage.Window;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.time.format.TextStyle;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

// Диалог добавления / изменения брони. Некорректные данные не закрывают диалог.
public final class BookingDialog extends Dialog<Booking> {
    private static final Locale RU = Locale.forLanguageTag("ru");
    private static final int FIELD_COLUMNS = 20;
    private static final double GAP = 8;

    private final ComboBox<BookingKind> kindBox = new ComboBox<>();
    private final TextField roomField = new TextField();
    private final TextField employeeField = new TextField();
    private final TextField startField = new TextField();
    private final TextField endField = new TextField();
    private final TextField commentField = new TextField();
    private final EnumMap<DayOfWeek, CheckBox> dayBoxes = new EnumMap<>(DayOfWeek.class);

    private final int id;
    private Booking built;

    public BookingDialog(Window owner, Booking existing, int newId) {
        initOwner(owner);
        setTitle(existing == null ? "Добавить бронь" : "Изменить бронь");
        this.id = existing == null ? newId : existing.getId();

        kindBox.getItems().addAll(BookingKind.values());
        kindBox.setValue(BookingKind.SINGLE);
        for (TextField f : new TextField[]{roomField, employeeField, startField, endField, commentField}) {
            f.setPrefColumnCount(FIELD_COLUMNS);
        }
        HBox days = new HBox(GAP);
        for (DayOfWeek d : DayOfWeek.values()) {
            CheckBox box = new CheckBox(d.getDisplayName(TextStyle.SHORT, RU));
            dayBoxes.put(d, box);
            days.getChildren().add(box);
        }

        GridPane form = new GridPane();
        form.setHgap(GAP);
        form.setVgap(GAP);
        form.setPadding(new Insets(GAP));
        addRow(form, 0, "Тип:", kindBox);
        addRow(form, 1, "Комната:", roomField);
        addRow(form, 2, "Сотрудник:", employeeField);
        addRow(form, 3, "Начало (" + Formats.DATE_HINT + "):", startField);
        addRow(form, 4, "Конец (" + Formats.DATE_HINT + "):", endField);
        addRow(form, 5, "Комментарий (обычная):", commentField);
        addRow(form, 6, "Дни недели (регулярная):", days);
        getDialogPane().setContent(form);
        getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        kindBox.valueProperty().addListener((o, a, b) -> updateEnabled());
        if (existing != null) {
            fill(existing);
        } else {
            LocalDateTime next = LocalDateTime.now().withMinute(0).withSecond(0).withNano(0).plusHours(1);
            startField.setText(next.format(Formats.DATE_TIME));
            endField.setText(next.plusHours(1).format(Formats.DATE_TIME));
        }
        updateEnabled();

        // Проверка при нажатии OK: при ошибках событие гасится, и диалог остаётся открытым
        getDialogPane().lookupButton(ButtonType.OK).addEventFilter(ActionEvent.ACTION, e -> {
            if (!tryBuild()) e.consume();
        });
        setResultConverter(button -> button == ButtonType.OK ? built : null);
    }

    private static void addRow(GridPane form, int row, String label, javafx.scene.Node control) {
        form.add(new Label(label), 0, row);
        form.add(control, 1, row);
    }

    private void fill(Booking b) {
        roomField.setText(b.getRoom());
        employeeField.setText(b.getEmployee());
        startField.setText(b.getStart().format(Formats.DATE_TIME));
        endField.setText(b.getEnd().format(Formats.DATE_TIME));
        if (b instanceof RecurringBooking r) {
            kindBox.setValue(BookingKind.RECURRING);
            for (DayOfWeek d : r.getDays()) dayBoxes.get(d).setSelected(true);
        } else if (b instanceof SingleBooking s) {
            kindBox.setValue(BookingKind.SINGLE);
            commentField.setText(s.getComment());
        }
        kindBox.setDisable(true);       // тип существующей брони менять нельзя
    }

    private void updateEnabled() {
        boolean recurring = kindBox.getValue() == BookingKind.RECURRING;
        commentField.setDisable(recurring);
        dayBoxes.values().forEach(box -> box.setDisable(!recurring));
    }

    // true — бронь собрана и корректна; иначе показано сообщение
    private boolean tryBuild() {
        LocalDateTime start;
        LocalDateTime end;
        try {
            start = Formats.parse(startField.getText());
            end = Formats.parse(endField.getText());
        } catch (DateTimeParseException ex) {
            showError("Неверный формат даты. Ожидается: " + Formats.DATE_HINT + "\nНапример: 2026-10-05 09:30");
            return false;
        }
        String room = roomField.getText().trim();
        String employee = employeeField.getText().trim();
        Booking candidate;
        List<String> errors;
        if (kindBox.getValue() == BookingKind.RECURRING) {
            Set<DayOfWeek> days = EnumSet.noneOf(DayOfWeek.class);
            dayBoxes.forEach((day, box) -> { if (box.isSelected()) days.add(day); });
            RecurringBooking r = new RecurringBooking(id, room, employee, start, end, days);
            errors = r.validate();
            candidate = r;
        } else {
            SingleBooking s = new SingleBooking(id, room, employee, start, end, commentField.getText().trim());
            errors = s.validate();
            candidate = s;
        }
        if (!errors.isEmpty()) {
            showError(String.join("\n", errors));
            return false;
        }
        built = candidate;
        return true;
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message, ButtonType.OK);
        alert.initOwner(getDialogPane().getScene().getWindow());
        alert.setTitle("Ошибка");
        alert.setHeaderText(null);
        alert.showAndWait();
    }
}
