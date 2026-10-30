package booking.gui;

import booking.ds.Trace;
import booking.experiment.Benchmark;
import booking.io.CsvLoader;
import booking.model.Booking;
import booking.model.Editable;
import booking.service.BookingConflictException;
import booking.service.BookingService;
import booking.service.StoreView;

import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

// Связывает элементы интерфейса с сервисом. Предметной логики здесь нет: правила (конфликты, валидация, хранение) —
// в BookingService и модели, отрисовка — в TreeCanvas/BookingTable, фоновые задачи — в JobRunner, обновление видов — в ViewRefresher.
public final class MainController implements QueryPane.ResultSink {
    private static final int MIN_LEVELS = 2, MAX_LEVELS = 9, DEFAULT_LEVELS = 5;
    private static final int BENCH_SMALL = 10_000, BENCH_LARGE = 100_000;
    private static final double GAP = 4, DIVIDER = 0.42;
    private static final String TREE_TITLE = "Дерево интервалов (узел: #id комната / начало–конец / max_end)";

    private final BookingService service;
    private final Stage stage;
    private final UserDialogs dialogs;
    private final BooleanProperty busy = new SimpleBooleanProperty();

    private final BookingTable table = new BookingTable();
    private final TreeCanvas tree = new TreeCanvas();
    private final EventLog log = new EventLog();
    private final StatusBar status = new StatusBar();
    private final Label info = new Label(" ");
    private final Spinner<Integer> levels = new Spinner<>(MIN_LEVELS, MAX_LEVELS, DEFAULT_LEVELS);
    private final QueryPane queryPane;
    private final ViewRefresher refresher;
    private final JobRunner runner;

    private final Button loadButton = new Button("Загрузить из CSV");
    private final Button saveButton = new Button("Сохранить в CSV");
    private final Button addButton = new Button("Добавить");
    private final Button editButton = new Button("Изменить");
    private final Button deleteButton = new Button("Удалить");
    private final Button generateButton = new Button("Сгенерировать…");
    private final Button benchButton = new Button("Замеры");
    private final Button raceButton = new Button("Гонка потоков");

    public MainController(BookingService service, Stage stage) {
        this.service = service;
        this.stage = stage;
        this.dialogs = new UserDialogs(stage);
        this.queryPane = new QueryPane(service, this);
        this.refresher = new ViewRefresher(service, levels::getValue, new ViewRefresher.Sink() {
            @Override public void show(StoreView view, Booking select) { showView(view, select); }
            @Override public void failed(Throwable error) { reportFailure(error); }
        });
        this.runner = new JobRunner(status, busy, log, dialogs, refresher);
        bindActions();
        bindAvailability();
    }

    public Parent root() {
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(GAP));
        root.setTop(toolbar());
        root.setCenter(content());
        root.setBottom(status);
        return root;
    }

    // Первое заполнение видов
    public void start() { refresher.request(null, null); }

    // ---------- сборка интерфейса ----------
    private Node toolbar() {
        HBox bar = new HBox(GAP, loadButton, saveButton, addButton, editButton, deleteButton,
                generateButton, benchButton, raceButton);
        bar.setPadding(new Insets(GAP));
        return bar;
    }

    private Node content() {
        VBox bottom = new VBox(GAP, queryPane, log);
        BorderPane left = new BorderPane(table);
        left.setBottom(bottom);

        HBox head = new HBox(GAP * 2, new Label("Уровней:"), levels);
        head.getChildren().addAll(new Legend());
        head.setPadding(new Insets(GAP));
        BorderPane treeBox = new BorderPane(tree);
        treeBox.setTop(head);
        treeBox.setBottom(info);
        TitledPane right = new TitledPane(TREE_TITLE, treeBox);
        right.setCollapsible(false);
        right.setMaxHeight(Double.MAX_VALUE);

        SplitPane split = new SplitPane(left, right);
        split.setOrientation(Orientation.HORIZONTAL);
        split.setDividerPositions(DIVIDER);
        return split;
    }

    private void bindActions() {
        loadButton.setOnAction(e -> onLoad());
        saveButton.setOnAction(e -> onSave());
        addButton.setOnAction(e -> onAdd());
        editButton.setOnAction(e -> onEdit());
        deleteButton.setOnAction(e -> onDelete());
        generateButton.setOnAction(e -> onGenerate());
        benchButton.setOnAction(e -> onBenchmark());
        raceButton.setOnAction(e -> new RaceDialog(stage).show());
        levels.valueProperty().addListener((o, a, b) -> refresher.request(null, null));
    }

    // Доступность кнопок задаётся привязками, а не ручным «updateButtons»
    private void bindAvailability() {
        var notEditable = Bindings.createBooleanBinding(() -> !(table.selected() instanceof Editable), table.selectedProperty());
        var nothingSelected = table.selectedProperty().isNull();       // «Изменить» только для Editable-записей
        for (Button b : new Button[]{loadButton, saveButton, addButton, generateButton, benchButton}) {
            b.disableProperty().bind(busy);
        }
        editButton.disableProperty().bind(busy.or(notEditable));
        deleteButton.disableProperty().bind(busy.or(nothingSelected));
        queryPane.bindDisable(busy);
    }

    // ---------- показ результата ----------
    private void showView(StoreView view, Booking select) {
        table.setRows(view.rows());
        if (select != null) table.select(select);
        tree.setSnapshot(view.tree());
        info.setText(describe(view));
    }

    private static String describe(StoreView view) {
        var snap = view.tree();
        String text = " Броней: " + snap.total() + ", высота дерева: " + snap.height()
                + ", показано узлов: " + snap.count() + " (уровни 1–" + snap.levels() + ")";
        Trace t = view.trace();
        if (t == null) return text;
        return text + ", в подсветке: посещено " + t.count(Trace.Kind.VISITED) + ", отсечено " + t.count(Trace.Kind.PRUNED);
    }

    private void reportFailure(Throwable error) {
        log.println("Ошибка обновления: " + error);
        dialogs.error(ErrorMessages.describe(error));
    }

    @Override
    public void show(Trace trace, String message, Booking select) {
        refresher.request(trace, select);
        log.println(message);
    }

    @Override
    public void error(String message) { dialogs.error(message); }

    // ---------- файловые операции и фоновые задачи ----------
    private void onLoad() {
        dialogs.chooseFileToOpen().ifPresent(path -> runner.run("Загрузка", false,
                ctx -> service.load(path, ctx), this::onLoaded,
                () -> log.println("Загрузка отменена, данные не изменены")));
    }

    private void onLoaded(CsvLoader.LoadResult r) {
        if (r.errors().isEmpty()) {
            log.println("Загружено записей: " + r.loaded());
            dialogs.info("Загрузка завершена", "Загружено записей: " + r.loaded());
        } else {
            log.println("Загружено " + r.loaded() + ", пропущено строк: " + r.errors().size());
            new LoadErrorsDialog(stage, r.loaded(), r.errors()).showAndWait();
        }
    }

    private void onSave() {
        dialogs.chooseFileToSave().ifPresent(path -> runner.run("Сохранение", false,
                ctx -> service.save(path, ctx), n -> log.println("Сохранено записей: " + n),
                () -> log.println("Сохранение отменено, исходный файл не изменён")));
    }

    private void onGenerate() {
        new GenerateDialog(stage).showAndWait().ifPresent(request -> {
            int before = service.size();
            runner.run("Генерация", true, ctx -> service.generate(request.count(), request.conflictFree(), ctx),
                    added -> log.println("Сгенерировано броней: " + added),
                    () -> log.println("Генерация отменена, добавлено: " + (service.size() - before)));
        });
    }

    private void onBenchmark() {
        runner.run("Замеры", false, ctx -> Benchmark.run(new int[]{BENCH_SMALL, BENCH_LARGE}, ctx),
                rows -> new BenchmarkDialog(stage, rows).showAndWait(),
                () -> log.println("Замеры отменены"));
    }

    // ---------- правка данных ----------
    // Через «Добавить» создаются только редактируемые типы
    private void onAdd() {
        new BookingDialog(stage, null, service.nextId()).showAndWait().ifPresent(created -> {
            Trace t = new Trace();
            try {
                service.add(created, t);
                show(t, "Добавлено: " + created + ", посещено узлов: " + t.count(Trace.Kind.VISITED), created);
            } catch (BookingConflictException ex) {
                dialogs.error(ex.getMessage());
            }
        });
    }

    private void onEdit() {
        Booking old = table.selected();
        if (!(old instanceof Editable)) {
            dialogs.error("Эту запись нельзя изменять");
            return;
        }
        new BookingDialog(stage, old, old.getId()).showAndWait().ifPresent(changed -> {
            Trace t = new Trace();
            try {
                service.replace(old, changed, t);
                show(t, "Изменено: " + changed, changed);
            } catch (BookingConflictException ex) {
                dialogs.error(ex.getMessage());
            }
        });
    }

    private void onDelete() {
        Booking b = table.selected();
        if (b == null) return;
        Trace t = new Trace();
        boolean ok = service.remove(b, t);
        show(t, ok ? "Удалено: " + b : "Не удалось удалить: " + b, null);
    }
}
