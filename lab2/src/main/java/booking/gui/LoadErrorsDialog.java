package booking.gui;

import booking.ds.ArrayBuf;
import booking.io.CsvLoadException;
import booking.io.LoadErrorCode;

import javafx.geometry.Insets;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.stage.Window;

// Сводка по кодам ошибок и таблица пропущенных строк
public final class LoadErrorsDialog extends Dialog<Void> {
    private static final int TABLE_LIMIT = 5000;    // при 10^5 битых строк таблица бесполезна
    private static final double WIDTH = 720, HEIGHT = 420, GAP = 8;

    public LoadErrorsDialog(Window owner, int loaded, ArrayBuf<CsvLoadException> errors) {
        initOwner(owner);
        setTitle("Загрузка завершена с ошибками");
        setResizable(true);

        int[] counts = new int[LoadErrorCode.values().length];
        for (int i = 0; i < errors.size(); i++) counts[errors.get(i).getCode().ordinal()]++;
        StringBuilder text = new StringBuilder("Загружено записей: " + loaded + "\nПропущено строк: " + errors.size());
        for (LoadErrorCode code : LoadErrorCode.values()) {
            if (counts[code.ordinal()] > 0) {
                text.append("\n  • ").append(code).append(" — ").append(code.getTitle())
                        .append(": ").append(counts[code.ordinal()]);
            }
        }

        TableView<CsvLoadException> table = new TableView<>(new ArrayBufList<>(errors, TABLE_LIMIT));
        table.getColumns().add(column("Строка", 70, e -> String.valueOf(e.getLineNo())));
        table.getColumns().add(column("Код ошибки", 170, e -> e.getCode().name()));
        table.getColumns().add(column("Описание", 420, CsvLoadException::getMessage));
        VBox.setVgrow(table, Priority.ALWAYS);

        VBox box = new VBox(GAP, new Label(text.toString()), table);
        box.setPadding(new Insets(GAP));
        getDialogPane().setContent(box);
        getDialogPane().setPrefSize(WIDTH, HEIGHT);
        getDialogPane().getButtonTypes().add(ButtonType.OK);
    }

    private static TableColumn<CsvLoadException, String> column(String title, double width,
                                                               java.util.function.Function<CsvLoadException, String> text) {
        TableColumn<CsvLoadException, String> c = new TableColumn<>(title);
        c.setCellValueFactory(cell -> new ReadOnlyStringWrapper(text.apply(cell.getValue())));
        c.setPrefWidth(width);
        c.setSortable(false);
        return c;
    }
}
