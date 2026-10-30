package booking.model;

import java.time.LocalDateTime;

//Прошедшая бронь — только для чтения. Создаётся только при загрузке из файла.
public final class PastBooking extends Booking {

    public PastBooking(int id, String room, String employee, LocalDateTime start, LocalDateTime end) {
        super(id, room, employee, start, end);
    }

    private static UnsupportedOperationException readOnly() {
        return new UnsupportedOperationException("Прошедшая бронь доступна только для чтения");
    }

    @Override public void setRoom(String room) { throw readOnly(); }
    @Override public void setEmployee(String employee) { throw readOnly(); }
    @Override public void setStart(LocalDateTime start) { throw readOnly(); }
    @Override public void setEnd(LocalDateTime end) { throw readOnly(); }

    @Override public String csvType() { return "PAST"; }
    @Override public String typeTitle() { return "Прошедшая (только чтение)"; }
    @Override public String extraCsv() { return ""; }
    @Override public String extraText() { return ""; }
}
