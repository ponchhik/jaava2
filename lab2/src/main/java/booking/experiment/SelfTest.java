package booking.experiment;

import booking.ds.ArrayBuf;
import booking.ds.IntervalStore;
import booking.ds.IntervalTree;
import booking.model.Booking;
import booking.service.BookingGenerator;

import java.time.LocalDateTime;
import java.util.Random;

// Самопроверка структуры: случайные операции сверяются с полным перебором по массиву.
// Запуск: gradlew selfTest
public final class SelfTest {
    public static void main(String[] args) {
        Random rnd = new Random(7);
        BookingGenerator gen = new BookingGenerator(11);
        IntervalTree<LocalDateTime, Booking> tree = new IntervalTree<>();
        Booking[] ref = new Booking[6000];     // эталон: обычный массив
        int refSize = 0, nextId = 1, checks = 0;

        for (int step = 0; step < 20000; step++) {
            int op = rnd.nextInt(10);
            if (op < 5 || refSize == 0) {                              // вставка
                if (refSize == ref.length) continue;
                Booking b = gen.next(nextId++);
                tree.add(b);
                ref[refSize++] = b;
            } else if (op < 7) {                                        // удаление конкретной записи
                int i = rnd.nextInt(refSize);
                require(tree.remove(ref[i], null), "remove must succeed");
                ref[i] = ref[--refSize];
            } else if (op < 8) {                                        // удаление по ключу
                Booking probe = ref[rnd.nextInt(refSize)];
                Booking expected = probe;
                for (int i = 0; i < refSize; i++)
                    if (ref[i].low().equals(probe.low()) && ref[i].uid() < expected.uid()) expected = ref[i];
                require(tree.findByKey(probe.low()) == expected, "findByKey must return smallest uid");
                require(tree.removeByKey(probe.low()), "removeByKey must succeed");
                for (int i = 0; i < refSize; i++) if (ref[i] == expected) { ref[i] = ref[--refSize]; break; }
            } else {                                                    // запрос пересечений и конфликт комнаты
                LocalDateTime from = ref[rnd.nextInt(refSize)].low().plusMinutes(rnd.nextInt(240) - 120);
                LocalDateTime to = from.plusMinutes(30 + rnd.nextInt(300));
                int expectedCount = 0;
                for (int i = 0; i < refSize; i++)
                    if (ref[i].low().isBefore(to) && from.isBefore(ref[i].high())) expectedCount++;
                ArrayBuf<Booking> got = tree.findOverlapping(from, to, null);
                require(got.size() == expectedCount, "overlap count " + got.size() + " != " + expectedCount);
                String room = gen.randomRoom();
                boolean expConflict = false;
                for (int i = 0; i < refSize; i++)
                    if (ref[i].getRoom().equals(room) && ref[i].low().isBefore(to) && from.isBefore(ref[i].high())) expConflict = true;
                require((tree.findConflict(from, to, b -> b.getRoom().equals(room), null) != null) == expConflict, "conflict");
                checks++;
            }
            if (step % 500 == 0) {
                IntervalStore.Integrity in = tree.checkIntegrity();
                require(in.ok() && in.reachable() == refSize, "integrity broken: " + in);
            }
        }
        IntervalStore.Integrity in = tree.checkIntegrity();
        require(in.ok() && tree.size() == refSize, "final integrity: " + in);
        System.out.println("OK: размер " + tree.size() + ", высота " + tree.height() + ", запросов проверено " + checks);
    }

    private static void require(boolean cond, String msg) {
        if (!cond) throw new AssertionError(msg);
    }
}
