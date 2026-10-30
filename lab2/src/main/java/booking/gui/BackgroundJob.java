package booking.gui;

import booking.service.TaskContext;

import javafx.concurrent.Task;

import java.util.function.BooleanSupplier;

// Фоновая работа на javafx.concurrent.Task: call() идёт в фоновом потоке, обработчики setOnSucceeded/setOnFailed — в потоке GUI.
// Отмена кооперативная: Task.cancel() не используется (он сообщает «отменено» сразу, а поток ещё работает);
// вместо этого выставляется флаг, работа сама останавливается, и только потом приходит результат.
public final class BackgroundJob<R> extends Task<R> implements TaskContext {
    private static final int SLOW_EVERY_MASK = 15;
    private static final long SLOW_SLEEP_MS = 2;

    public interface Job<R> { R run(TaskContext ctx) throws Exception; }

    private final Job<R> job;
    private final BooleanSupplier slowDemo;
    private volatile boolean cancelRequested;

    public BackgroundJob(Job<R> job, BooleanSupplier slowDemo) {
        this.job = job;
        this.slowDemo = slowDemo;
    }

    @Override protected R call() throws Exception { return job.run(this); }

    @Override
    public void progress(int done, int total) {
        if (total > 0) updateProgress(done, total);
        if (slowDemo.getAsBoolean() && (done & SLOW_EVERY_MASK) == 0) {    // демо-замедление, чтобы успеть нажать «Отмена»
            try {
                Thread.sleep(SLOW_SLEEP_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    public void requestCancel() { cancelRequested = true; }

    public boolean cancelRequested() { return cancelRequested; }

    @Override public boolean isCancellationRequested() { return cancelRequested; }
}
