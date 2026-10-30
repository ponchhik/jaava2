package booking.gui;

import booking.experiment.RaceDemo;

import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.TextArea;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import javafx.stage.Window;

// Демонстрация гонки: многопоточная запись в дерево без защиты и под synchronized (окно не блокирует главное)
public final class RaceDialog extends Stage {
    private static final int MIN_THREADS = 2, MAX_THREADS = 32, DEFAULT_THREADS = 8;
    private static final int MIN_INSERTS = 100, MAX_INSERTS = 100_000, DEFAULT_INSERTS = 5000, INSERTS_STEP = 500;
    private static final int MIN_ROUNDS = 1, MAX_ROUNDS = 50, DEFAULT_ROUNDS = 5;
    private static final int OUTPUT_ROWS = 18, OUTPUT_COLUMNS = 70;
    private static final double MONO_SIZE = 12, GAP = 8;

    private final Spinner<Integer> threads = new Spinner<>(MIN_THREADS, MAX_THREADS, DEFAULT_THREADS);
    private final Spinner<Integer> perThread = new Spinner<>(MIN_INSERTS, MAX_INSERTS, DEFAULT_INSERTS, INSERTS_STEP);
    private final Spinner<Integer> rounds = new Spinner<>(MIN_ROUNDS, MAX_ROUNDS, DEFAULT_ROUNDS);
    private final TextArea out = new TextArea();
    private final Button run = new Button("Запустить");

    public RaceDialog(Window owner) {
        initOwner(owner);
        setTitle("Демонстрация гонки потоков");

        out.setEditable(false);
        out.setFont(Font.font("Monospaced", MONO_SIZE));
        out.setPrefRowCount(OUTPUT_ROWS);
        out.setPrefColumnCount(OUTPUT_COLUMNS);
        out.setText("Нажмите «Запустить»: сначала запись в дерево без защиты, затем под synchronized.\n");
        run.setOnAction(e -> start());

        HBox top = new HBox(GAP, new Label("Потоков:"), threads, new Label("Вставок на поток:"), perThread,
                new Label("Повторов:"), rounds, run);
        top.setPadding(new Insets(GAP));
        BorderPane root = new BorderPane(out);
        root.setTop(top);
        setScene(new Scene(root));
    }

    private void start() {
        run.setDisable(true);
        out.setText("Идёт запуск потоков…\n");
        int t = threads.getValue(), p = perThread.getValue(), r = rounds.getValue();
        Task<String> task = new Task<>() {
            @Override protected String call() throws Exception {
                return RaceDemo.run(false, t, p, r).format() + "\n" + RaceDemo.run(true, t, p, r).format();
            }
        };
        task.setOnSucceeded(e -> finish(task.getValue()));
        task.setOnFailed(e -> finish("Ошибка: " + task.getException()));
        Background.start(task, "race-demo");
    }

    private void finish(String text) {
        out.setText(text);
        run.setDisable(false);
    }
}
