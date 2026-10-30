package booking.gui;

import booking.ds.Trace;
import booking.model.Booking;
import booking.service.BookingService;
import booking.service.StoreView;

import javafx.concurrent.Task;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.IntSupplier;

// Обновление таблицы и дерева. Срез строится в фоновом потоке, в потоке GUI только применяется готовый результат —
// поэтому при 10^5–10^6 записей интерфейс не подвисает. Запросы объединяются: пока идёт построение, новый запрос
// запоминается и выполняется следом (живые обновления во время генерации не перебивают запрос со следом операции).
public final class ViewRefresher {
    private static final long LIVE_INTERVAL_MS = 200;
    private static final String THREAD_NAME = "view-refresh";

    public interface Sink {
        void show(StoreView view, Booking select);
        void failed(Throwable error);
    }

    private record Request(Trace trace, Booking select, boolean live) { }

    private final BookingService service;
    private final IntSupplier levels;
    private final Sink sink;
    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, THREAD_NAME);
        t.setDaemon(true);
        return t;
    });

    private boolean running;
    private Request pending;
    private long lastLive;

    public ViewRefresher(BookingService service, IntSupplier levels, Sink sink) {
        this.service = service;
        this.levels = levels;
        this.sink = sink;
    }

    // Вызывать из потока GUI
    public void request(Trace trace, Booking select) { submit(new Request(trace, select, false)); }

    // Не чаще раза в LIVE_INTERVAL_MS — для показа процесса генерации
    public void requestLive() {
        long now = System.currentTimeMillis();
        if (now - lastLive < LIVE_INTERVAL_MS) return;
        lastLive = now;
        submit(new Request(null, null, true));
    }

    private void submit(Request request) {
        if (running) {
            if (pending == null || !request.live()) pending = request;
            return;
        }
        start(request);
    }

    private void start(Request request) {
        running = true;
        int depth = levels.getAsInt();
        Task<StoreView> task = new Task<>() {
            @Override protected StoreView call() { return service.view(depth, request.trace()); }
        };
        task.setOnSucceeded(e -> {
            sink.show(task.getValue(), request.select());
            next();
        });
        task.setOnFailed(e -> {
            sink.failed(task.getException());
            next();
        });
        executor.execute(task);
    }

    private void next() {
        running = false;
        Request p = pending;
        pending = null;
        if (p != null) start(p);
    }
}
