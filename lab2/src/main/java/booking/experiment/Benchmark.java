package booking.experiment;

import booking.ds.ArrayBuf;
import booking.ds.IntervalStore;
import booking.ds.IntervalTree;
import booking.ds.SynchronizedIntervalStore;
import booking.model.Booking;
import booking.service.BookingGenerator;
import booking.service.TaskContext;

import java.time.LocalDateTime;
import java.util.Random;
import java.util.TreeMap;

// Замеры времени. java.util.TreeMap используется ТОЛЬКО здесь, как эталон для сравнения;
// в основном коде приложения его нет.
public final class Benchmark {
    private static final int QUERIES = 1000;       // число запросов для дополнительных операций

    public record Row(String operation, int n, int ops, double ownMs, double syncMs, double treeMapMs) { }

    private record Key(LocalDateTime start, int id) implements Comparable<Key> {
        @Override public int compareTo(Key o) {
            int c = start.compareTo(o.start);
            return c != 0 ? c : Integer.compare(id, o.id);
        }
    }

    public static ArrayBuf<Row> run(int[] sizes, TaskContext ctx) {
        measure(10_000, TaskContext.NONE);                           // прогрев JIT, результат отбрасывается
        ArrayBuf<Row> out = new ArrayBuf<>();
        for (int i = 0; i < sizes.length; i++) {
            if (ctx.isCancellationRequested()) break;
            ArrayBuf<Row> part = measure(sizes[i], ctx);
            for (int j = 0; j < part.size(); j++) out.add(part.get(j));
            ctx.progress(i + 1, sizes.length);
        }
        return out;
    }

    private static ArrayBuf<Row> measure(int n, TaskContext ctx) {
        BookingGenerator gen = new BookingGenerator(42);
        Booking[] data = new Booking[n];
        for (int i = 0; i < n; i++) data[i] = gen.next(i + 1);
        Random rnd = new Random(5);
        LocalDateTime[] from = new LocalDateTime[QUERIES], to = new LocalDateTime[QUERIES];
        String[] room = new String[QUERIES];
        for (int i = 0; i < QUERIES; i++) {
            from[i] = data[rnd.nextInt(n)].low().plusMinutes(rnd.nextInt(120));
            to[i] = from[i].plusMinutes(120);
            room[i] = gen.randomRoom();
        }

        IntervalTree<LocalDateTime, Booking> own = new IntervalTree<>();
        SynchronizedIntervalStore<LocalDateTime, Booking> sync = new SynchronizedIntervalStore<>(new IntervalTree<LocalDateTime, Booking>());
        TreeMap<Key, Booking> tm = new TreeMap<>();
        Key[] probes = new Key[n];
        for (int i = 0; i < n; i++) probes[i] = new Key(data[i].low(), Integer.MIN_VALUE);

        double[][] t = new double[5][3];
        long s;

        s = System.nanoTime(); for (Booking b : data) own.add(b);  t[0][0] = ms(s);
        s = System.nanoTime(); for (Booking b : data) sync.add(b); t[0][1] = ms(s);
        s = System.nanoTime(); for (Booking b : data) tm.put(new Key(b.low(), b.uid()), b); t[0][2] = ms(s);
        ctx.progress(1, 5);

        int sink = 0;
        s = System.nanoTime(); for (Booking b : data) sink += own.findByKey(b.low()) != null ? 1 : 0;  t[1][0] = ms(s);
        s = System.nanoTime(); for (Booking b : data) sink += sync.findByKey(b.low()) != null ? 1 : 0; t[1][1] = ms(s);
        s = System.nanoTime(); for (Key k : probes) sink += tm.ceilingEntry(k) != null ? 1 : 0;        t[1][2] = ms(s);
        ctx.progress(2, 5);

        s = System.nanoTime(); for (int i = 0; i < QUERIES; i++) sink += own.findOverlapping(from[i], to[i], null).size();  t[2][0] = ms(s);
        s = System.nanoTime(); for (int i = 0; i < QUERIES; i++) sink += sync.findOverlapping(from[i], to[i], null).size(); t[2][1] = ms(s);
        s = System.nanoTime();
        for (int i = 0; i < QUERIES; i++) {            // у TreeMap нет интервального запроса: перебор всех с началом < to
            for (Booking b : tm.headMap(new Key(to[i], Integer.MIN_VALUE)).values()) if (from[i].isBefore(b.high())) sink++;
        }
        t[2][2] = ms(s);
        ctx.progress(3, 5);

        s = System.nanoTime();
        for (int i = 0; i < QUERIES; i++) { String r = room[i]; sink += own.findConflict(from[i], to[i], b -> b.getRoom().equals(r), null) != null ? 1 : 0; }
        t[3][0] = ms(s);
        s = System.nanoTime();
        for (int i = 0; i < QUERIES; i++) { String r = room[i]; sink += sync.findConflict(from[i], to[i], b -> b.getRoom().equals(r), null) != null ? 1 : 0; }
        t[3][1] = ms(s);
        s = System.nanoTime();
        for (int i = 0; i < QUERIES; i++) {
            for (Booking b : tm.headMap(new Key(to[i], Integer.MIN_VALUE)).values())
                if (b.getRoom().equals(room[i]) && from[i].isBefore(b.high())) { sink++; break; }
        }
        t[3][2] = ms(s);
        ctx.progress(4, 5);

        s = System.nanoTime(); for (Booking b : data) own.removeByKey(b.low());  t[4][0] = ms(s);
        s = System.nanoTime(); for (Booking b : data) sync.removeByKey(b.low()); t[4][1] = ms(s);
        s = System.nanoTime(); for (Booking b : data) tm.remove(new Key(b.low(), b.uid())); t[4][2] = ms(s);
        ctx.progress(5, 5);
        if (sink == 42) System.out.print("");      // чтобы JIT не выкинул вычисления

        String[] names = {"Вставка", "Поиск по началу", "Пересекающиеся с интервалом (запросы)",
                "Проверка конфликта комнаты (запросы)", "Удаление по началу"};
        int[] ops = {n, n, QUERIES, QUERIES, n};
        ArrayBuf<Row> rows = new ArrayBuf<>();
        for (int i = 0; i < 5; i++) rows.add(new Row(names[i], n, ops[i], t[i][0], t[i][1], t[i][2]));
        return rows;
    }

    private static double ms(long startNanos) { return (System.nanoTime() - startNanos) / 1e6; }

    public static void main(String[] args) {
        ArrayBuf<Row> r = run(new int[]{10_000, 100_000}, TaskContext.NONE);
        System.out.println("| N | Операция | Операций | Своё дерево, мс | Своё + synchronized, мс | TreeMap, мс |");
        for (int i = 0; i < r.size(); i++) {
            Row x = r.get(i);
            System.out.printf("| %d | %s | %d | %.1f | %.1f | %.1f |%n", x.n(), x.operation(), x.ops(), x.ownMs(), x.syncMs(), x.treeMapMs());
        }
    }
}
