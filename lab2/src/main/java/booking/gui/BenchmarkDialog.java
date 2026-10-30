package booking.gui;

import booking.ds.ArrayBuf;
import booking.experiment.Benchmark;

import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

import java.util.function.Function;

// Таблица замеров: своё дерево / своё + synchronized / java.util.TreeMap (только для эксперимента)
public final class BenchmarkDialog extends Dialog<Void> {
    private static final double WIDTH = 860, HEIGHT = 340, GAP = 8;
    private static final String SEPARATOR = "\t";

    private record Col(String title, double width, Function<Benchmark.Row, String> text) { }

    private static final Col[] COLUMNS = {
            new Col("N", 70, r -> String.valueOf(r.n())),
            new Col("Операция", 280, Benchmark.Row::operation),
            new Col("Операций", 90, r -> String.valueOf(r.ops())),
            new Col("Своё дерево, мс", 120, r -> String.format("%.1f", r.ownMs())),
            new Col("Своё + synchronized, мс", 160, r -> String.format("%.1f", r.syncMs())),
            new Col("TreeMap, мс", 100, r -> String.format("%.1f", r.treeMapMs()))
    };

    public BenchmarkDialog(Window owner, ArrayBuf<Benchmark.Row> rows) {
        initOwner(owner);
        setTitle("Замеры времени");
        setResizable(true);

        TableView<Benchmark.Row> table = new TableView<>(new ArrayBufList<>(rows));
        for (Col col : COLUMNS) {
            TableColumn<Benchmark.Row, String> c = new TableColumn<>(col.title());
            c.setCellValueFactory(cell -> new ReadOnlyStringWrapper(col.text().apply(cell.getValue())));
            c.setPrefWidth(col.width());
            c.setSortable(false);
            table.getColumns().add(c);
        }
        VBox.setVgrow(table, Priority.ALWAYS);

        Button copy = new Button("Копировать в буфер (для Word)");
        copy.setOnAction(e -> {
            ClipboardContent content = new ClipboardContent();
            content.putString(toTsv(rows));
            Clipboard.getSystemClipboard().setContent(content);
        });

        VBox box = new VBox(GAP, table, copy);
        box.setPadding(new Insets(GAP));
        getDialogPane().setContent(box);
        getDialogPane().setPrefSize(WIDTH, HEIGHT);
        getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
    }

    private static String toTsv(ArrayBuf<Benchmark.Row> rows) {
        StringBuilder sb = new StringBuilder();
        for (int c = 0; c < COLUMNS.length; c++) sb.append(c > 0 ? SEPARATOR : "").append(COLUMNS[c].title());
        sb.append('\n');
        for (int i = 0; i < rows.size(); i++) {
            for (int c = 0; c < COLUMNS.length; c++) {
                sb.append(c > 0 ? SEPARATOR : "").append(COLUMNS[c].text().apply(rows.get(i)));
            }
            sb.append('\n');
        }
        return sb.toString();
    }
}
