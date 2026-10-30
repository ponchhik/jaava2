package booking.service;

import booking.ds.ArrayBuf;
import booking.ds.IntervalTree;
import booking.ds.SynchronizedIntervalStore;
import booking.ds.Trace;
import booking.ds.TreeSnapshot;
import booking.io.CsvFileException;
import booking.io.CsvLoader;
import booking.io.CsvSaver;
import booking.model.Booking;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicInteger;

// Прикладной слой: хранилище броней + предметные операции. GUI работает только через него
// (само хранилище наружу не отдаётся). Контракт «BookStore» из методички здесь — IntervalStore<LocalDateTime, Booking>.
public final class BookingService {
    private static final int MAX_ATTEMPTS_FACTOR = 30;

    private final SynchronizedIntervalStore<LocalDateTime, Booking> store =
            new SynchronizedIntervalStore<>(new IntervalTree<LocalDateTime, Booking>());
    private final AtomicInteger idSeq = new AtomicInteger();

    public int size() { return store.size(); }
    public int nextId() { return idSeq.incrementAndGet(); }

    // Конфликт: другая бронь той же комнаты, пересекающаяся по времени
    public Booking findConflict(String room, LocalDateTime from, LocalDateTime to, int ignoreId, Trace trace) {
        return store.findConflict(from, to,
                other -> other.getId() != ignoreId && other.getRoom().equalsIgnoreCase(room), trace);
    }

    public Booking findConflict(Booking candidate, Trace trace) {
        return findConflict(candidate.getRoom(), candidate.getStart(), candidate.getEnd(), candidate.getId(), trace);
    }

    // Проверка конфликта и вставка — одной критической секцией, чтобы между ними никто не вклинился
    public void add(Booking booking, Trace trace) throws BookingConflictException {
        synchronized (store) {
            failOnConflict(booking);
            store.add(booking, trace);
        }
    }

    // Ключ (начало) мог измениться: удалить старую и вставить новую под одной блокировкой
    public void replace(Booking old, Booking changed, Trace trace) throws BookingConflictException {
        synchronized (store) {
            failOnConflict(changed);
            store.replace(old, changed, trace);
        }
    }

    public boolean remove(Booking booking, Trace trace) { return store.remove(booking, trace); }

    public Booking findByStart(LocalDateTime start, Trace trace) { return store.findByKey(start, trace); }

    public boolean removeByStart(LocalDateTime start, Trace trace) { return store.removeByKey(start, trace); }

    public ArrayBuf<Booking> findOverlapping(LocalDateTime from, LocalDateTime to, Trace trace) {
        return store.findOverlapping(from, to, trace);
    }

    // Срез для показа: таблица и дерево берутся под одной блокировкой, подсветка накладывается уже на копию
    public StoreView view(int levels, Trace trace) {
        ArrayBuf<Booking> rows;
        TreeSnapshot<LocalDateTime, Booking> tree;
        synchronized (store) {
            rows = store.toBuf(false);
            tree = store.snapshot(levels);
        }
        tree.applyTrace(trace);
        return new StoreView(rows, tree, trace);
    }

    private void failOnConflict(Booking candidate) throws BookingConflictException {
        Booking other = findConflict(candidate, null);
        if (other != null) throw new BookingConflictException(candidate, other);
    }

    // Генерация count броней (в фоне). Возвращает, сколько реально добавлено.
    public int generate(int count, boolean conflictFree, TaskContext ctx) {
        BookingGenerator gen = new BookingGenerator(System.nanoTime());
        int added = 0;
        long attempts = 0, max = (long) count * MAX_ATTEMPTS_FACTOR;
        while (added < count && attempts++ < max && !ctx.isCancellationRequested()) {
            Booking b = gen.next(nextId());
            if (conflictFree && findConflict(b, null) != null) continue;
            store.add(b);
            ctx.progress(++added, count);
        }
        return added;
    }

    // Загрузка в новое дерево; старое заменяется только при успехе (отмена ничего не портит)
    public CsvLoader.LoadResult load(Path file, TaskContext ctx) throws CsvFileException {
        CsvLoader.LoadResult r = new CsvLoader().load(file, ctx);
        if (!r.cancelled()) {
            store.swapTree(r.tree());
            idSeq.set(r.maxId());
        }
        return r;
    }

    // Сохранение в симметричном порядке «корень первым» — повторная загрузка даёт то же дерево
    public int save(Path file, TaskContext ctx) throws IOException {
        var items = store.toBuf(true);
        new CsvSaver().save(file, items, ctx);
        return items.size();
    }
}
