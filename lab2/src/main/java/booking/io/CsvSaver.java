package booking.io;

import booking.ds.ArrayBuf;
import booking.model.Booking;
import booking.service.TaskContext;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

// Сохранение сначала во временный файл. Отмена/ошибка не портит существующий CSV.
public final class CsvSaver {
    public void save(Path file, ArrayBuf<Booking> bookings, TaskContext ctx) throws IOException {
        Path target = file.toAbsolutePath();
        Path temporary = Files.createTempFile(target.getParent(), ".booking-", ".tmp");
        try {
            try (BufferedWriter writer = Files.newBufferedWriter(temporary)) {
                writer.write("type;id;room;employee;start;end;extra");
                writer.newLine();
                for (int i = 0; i < bookings.size(); i++) {
                    if (ctx.isCancellationRequested()) return;
                    Booking b = bookings.get(i);
                    writer.write(String.join(";",
                            b.csvType(), String.valueOf(b.getId()), b.getRoom(),
                            b.getEmployee(), b.getStart().toString(),
                            b.getEnd().toString(), b.extraCsv()));
                    writer.newLine();
                    ctx.progress(i + 1, bookings.size());
                }
            }
            if (ctx.isCancellationRequested()) return;
            try {
                Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException ex) {
                Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
    }
}
