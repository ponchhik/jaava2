package booking.gui;

import javafx.geometry.Insets;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

// Параметры генерации набора данных: сколько броней и без конфликтов ли
public final class GenerateDialog extends Dialog<GenerateDialog.Request> {
    private static final int MIN_COUNT = 1, MAX_COUNT = 1_000_000, DEFAULT_COUNT = 10_000, COUNT_STEP = 1000;
    private static final double GAP = 8;

    public record Request(int count, boolean conflictFree) { }

    public GenerateDialog(Window owner) {
        initOwner(owner);
        setTitle("Генерация набора данных");
        Spinner<Integer> count = new Spinner<>(MIN_COUNT, MAX_COUNT, DEFAULT_COUNT, COUNT_STEP);
        count.setEditable(true);
        CheckBox noConflict = new CheckBox("Без конфликтов комнат (медленнее)");
        VBox box = new VBox(GAP, new Label("Сколько броней сгенерировать:"), count, noConflict);
        box.setPadding(new Insets(GAP));
        getDialogPane().setContent(box);
        getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        setResultConverter(b -> {
            if (b != ButtonType.OK) return null;
            count.commitValue();            // значение, набранное вручную, но не подтверждённое Enter
            return new Request(count.getValue(), noConflict.isSelected());
        });
    }
}
