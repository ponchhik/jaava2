package booking.experiment;

import booking.ds.IntervalTree;
import booking.io.CsvLoader;
import booking.io.CsvSaver;
import booking.io.LoadErrorCode;
import booking.model.Booking;
import booking.model.SingleBooking;
import booking.service.TaskContext;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicInteger;

// Небольшие независимые проверки по одному поведению. Не заменяют стресс-тест SelfTest.
public final class FocusedTests {
    private static Booking booking(int id, int startHour, int endHour, String room) {
        LocalDateTime day = LocalDateTime.of(2026, 10, 5, 0, 0);
        return new SingleBooking(id, room, "Тест", day.plusHours(startHour), day.plusHours(endHour), "");
    }

    private static void check(boolean ok, String msg) {
        if (!ok) throw new AssertionError(msg);
    }

    private static void testInsertFind() {
        var t = new IntervalTree<LocalDateTime, Booking>();
        Booking b = booking(1, 9, 10, "Альфа");
        t.add(b);
        check(t.size() == 1 && t.findByKey(b.low()) == b, "вставка и поиск");
        check(t.checkIntegrity().ok(), "целостность после вставки");
    }

    private static void testRemove() {
        var t = new IntervalTree<LocalDateTime, Booking>();
        Booking b = booking(1, 9, 10, "Альфа");
        t.add(b);
        check(t.remove(b, null), "удаление существующей записи");
        check(t.size() == 0 && t.findByKey(b.low()) == null, "дерево должно опустеть");
    }

    private static void testOverlapBoundaries() {
        var t = new IntervalTree<LocalDateTime, Booking>();
        Booking b = booking(1, 9, 10, "Альфа");
        t.add(b);
        check(t.findOverlapping(b.high(), b.high().plusHours(1), null).isEmpty(), "касание границы не конфликт");
        check(t.findOverlapping(b.low().plusMinutes(30), b.high().plusHours(1), null).size() == 1,
                "пересечение найдено");
    }

    private static void testConflictByRoom() {
        var t = new IntervalTree<LocalDateTime, Booking>();
        Booking b = booking(1, 9, 10, "Альфа");
        t.add(b);
        check(t.findConflict(b.low(), b.high(), x -> x.getRoom().equals("Бета"), null) == null,
                "другая комната не конфликтует");
        check(t.findConflict(b.low(), b.high(), x -> x.getRoom().equals("Альфа"), null) == b,
                "та же комната конфликтует");
    }

    private static void testLoadInvalidCsv() throws Exception {
        Path tmp = Files.createTempFile("booking-invalid", ".csv");
        try {
            Files.writeString(tmp, "type;id;room;employee;start;end;extra\n"
                    + "SINGLE;1;Альфа;Тест;2026-10-05T09:00;2026-10-05T10:00;\n"
                    + "SINGLE;1;Альфа;Тест;2026-10-05T10:00;2026-10-05T11:00;\n");
            var loaded = new CsvLoader().load(tmp, TaskContext.NONE);
            check(loaded.loaded() == 1 && loaded.errors().size() == 1, "одна строка отклонена");
            check(loaded.errors().get(0).getCode() == LoadErrorCode.DUPLICATE_ID, "код ошибки дубликата");
        } finally {
            Files.deleteIfExists(tmp);
        }
    }

    private static void testCancelledSavePreservesFile() throws Exception {
        Path tmp = Files.createTempFile("booking-save", ".csv");
        try {
            Files.writeString(tmp, "Исходное содержимое");
            var items = new booking.ds.ArrayBuf<Booking>();
            items.add(booking(1, 9, 10, "Альфа"));
            AtomicInteger progressCount = new AtomicInteger();
            TaskContext cancelDuringWrite = new TaskContext() {
                public void progress(int done, int total) { progressCount.incrementAndGet(); }
                public boolean isCancellationRequested() { return progressCount.get() > 0; }
            };
            new CsvSaver().save(tmp, items, cancelDuringWrite);
            check(Files.readString(tmp).equals("Исходное содержимое"), "отмена не должна перезаписывать CSV");
        } finally {
            Files.deleteIfExists(tmp);
        }
    }

    public static void main(String[] args) throws Exception {
        testInsertFind();
        testRemove();
        testOverlapBoundaries();
        testConflictByRoom();
        testLoadInvalidCsv();
        testCancelledSavePreservesFile();
        System.out.println("OK: 6 focused tests (insert, remove, overlap, conflict, CSV validation, save cancellation)");
    }
}
