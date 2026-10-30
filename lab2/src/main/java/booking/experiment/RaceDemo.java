package booking.experiment;

import booking.ds.IntervalStore;
import booking.ds.IntervalTree;
import booking.ds.SynchronizedIntervalStore;
import booking.model.Booking;
import booking.service.BookingGenerator;

import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;

// Демонстрация гонки потоков: многопоточная запись в дерево без защиты и с synchronized
public final class RaceDemo {

    public record Result(String title, int threads, int perThread, int rounds, int expected, int declaredSize,
                         int reachable, int orderErrors, int maxEndErrors, int exceptions, int corruptedRounds) {
        public String format() {
            return title + "\n"
                    + "  потоков: " + threads + ", вставок на поток: " + perThread + ", повторов: " + rounds + "\n"
                    + "  ожидалось узлов (сумма по повторам): " + expected + "\n"
                    + "  счётчик size (сумма): " + declaredSize + "  → потеряно приращений: " + (expected - declaredSize) + "\n"
                    + "  реально достижимых узлов: " + reachable + "  → потеряно вставок: " + (expected - reachable) + "\n"
                    + "  нарушений порядка ключей: " + orderErrors + ", неверных maxEnd: " + maxEndErrors + "\n"
                    + "  исключений в потоках: " + exceptions + "\n"
                    + "  повторов с повреждённой структурой: " + corruptedRounds + " из " + rounds + "\n";
        }
    }

    public static Result run(boolean safe, int threads, int perThread, int rounds) throws InterruptedException {
        int expected = 0, declared = 0, reachable = 0, order = 0, maxErr = 0, corrupted = 0;
        int[] exceptions = {0};
        for (int round = 0; round < rounds; round++) {
            IntervalStore<LocalDateTime, Booking> store = safe
                    ? new SynchronizedIntervalStore<>(new IntervalTree<LocalDateTime, Booking>())
                    : new IntervalTree<>();
            Booking[][] data = new Booking[threads][perThread];          // данные готовим заранее — гонка только на вставке
            BookingGenerator gen = new BookingGenerator(100 + round);
            for (int t = 0; t < threads; t++)
                for (int i = 0; i < perThread; i++) data[t][i] = gen.next(t * perThread + i + 1);

            CountDownLatch ready = new CountDownLatch(threads), go = new CountDownLatch(1);
            Thread[] pool = new Thread[threads];
            for (int t = 0; t < threads; t++) {
                final Booking[] mine = data[t];
                pool[t] = new Thread(() -> {
                    ready.countDown();
                    try {
                        go.await();
                        for (Booking b : mine) store.add(b);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } catch (Throwable ex) {
                        synchronized (exceptions) { exceptions[0]++; }
                    }
                }, "writer-" + t);
                pool[t].start();
            }
            ready.await();
            go.countDown();
            for (Thread th : pool) th.join();

            IntervalStore.Integrity in = store.checkIntegrity();
            expected += threads * perThread;
            declared += in.declaredSize();
            reachable += in.reachable();
            order += in.orderErrors();
            maxErr += in.maxEndErrors();
            if (!in.ok() || in.reachable() != threads * perThread) corrupted++;
        }
        return new Result(safe ? "С защитой (synchronized)" : "БЕЗ защиты (гонка)", threads, perThread, rounds,
                expected, declared, reachable, order, maxErr, exceptions[0], corrupted);
    }

    public static void main(String[] args) throws InterruptedException {
        System.out.println(run(false, 8, 5000, 5).format());
        System.out.println(run(true, 8, 5000, 5).format());
    }
}
