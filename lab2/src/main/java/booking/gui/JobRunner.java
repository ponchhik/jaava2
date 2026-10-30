package booking.gui;

import javafx.beans.property.BooleanProperty;

import java.util.function.Consumer;

// Запуск фоновых задач: блокирует кнопки на время работы, показывает прогресс, поддерживает отмену.
// Результат и ошибки обрабатываются в потоке GUI; ошибка всегда доходит до пользователя (диалог + журнал).
public final class JobRunner {
    private final StatusBar status;
    private final BooleanProperty busy;
    private final EventLog log;
    private final UserDialogs dialogs;
    private final ViewRefresher refresher;
    private BackgroundJob<?> current;

    public JobRunner(StatusBar status, BooleanProperty busy, EventLog log, UserDialogs dialogs, ViewRefresher refresher) {
        this.status = status;
        this.busy = busy;
        this.log = log;
        this.dialogs = dialogs;
        this.refresher = refresher;
        status.cancelButton().setOnAction(e -> {
            if (current != null) current.requestCancel();
        });
        status.cancelButton().disableProperty().bind(busy.not());
    }

    public <R> void run(String title, boolean live, BackgroundJob.Job<R> job, Consumer<R> onSuccess, Runnable onCancel) {
        if (busy.get()) return;
        BackgroundJob<R> task = new BackgroundJob<>(job, status::slowDemo);
        current = task;
        busy.set(true);
        status.setState(title);
        status.bar().progressProperty().bind(task.progressProperty());
        if (live) task.progressProperty().addListener((o, a, b) -> refresher.requestLive());

        task.setOnSucceeded(e -> {
            try {
                if (task.cancelRequested()) onCancel.run();
                else onSuccess.accept(task.getValue());
            } finally {
                finish();
            }
        });
        task.setOnFailed(e -> {
            Throwable error = task.getException();
            log.println("Ошибка: " + error);
            dialogs.error(ErrorMessages.describe(error));
            finish();
        });
        Background.start(task, "job-" + title);
    }

    private void finish() {
        status.bar().progressProperty().unbind();
        status.bar().setProgress(0);
        status.setState(status.idleText());
        current = null;
        busy.set(false);
        refresher.request(null, null);
    }
}
