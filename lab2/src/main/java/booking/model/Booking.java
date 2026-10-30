package booking.model;

import booking.ds.Interval;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public abstract class Booking implements Interval<LocalDateTime> {
    private final int id;
    private String room;
    private String employee;
    private LocalDateTime start;
    private LocalDateTime end;

    protected Booking(int id, String room, String employee, LocalDateTime start, LocalDateTime end) {
        this.id = id;
        this.room = room;
        this.employee = employee;
        this.start = start;
        this.end = end;
    }

    public int getId() { return id; }
    public String getRoom() { return room; }
    public String getEmployee() { return employee; }
    public LocalDateTime getStart() { return start; }
    public LocalDateTime getEnd() { return end; }

    public void setRoom(String room) { this.room = room; }
    public void setEmployee(String employee) { this.employee = employee; }
    public void setStart(LocalDateTime start) { this.start = start; }
    public void setEnd(LocalDateTime end) { this.end = end; }

    // Контракт Interval для дерева: ключ — начало, uid — id брони
    @Override public LocalDateTime low() { return start; }
    @Override public LocalDateTime high() { return end; }
    @Override public int uid() { return id; }

    public abstract String csvType();

    //Название типа для таблицы в GUI
    public abstract String typeTitle();

    //Значение дополнительного поля для записи в CSV
    public abstract String extraCsv();

    //Значение дополнительного поля для показа в таблице
    public abstract String extraText();

    //Пересекается ли эта бронь с другой в одной и той же комнате
    public boolean overlaps(Booking other) {
        return room.equalsIgnoreCase(other.room)
                && start.isBefore(other.end)
                && other.start.isBefore(end);
    }

    // Пустой список = ошибок нет
    public List<String> validateCommon() {
        List<String> errors = new ArrayList<>();
        if (isBlank(room)) errors.add("Не указана комната");
        if (isBlank(employee)) errors.add("Не указан сотрудник");
        if (hasBadChar(room) || hasBadChar(employee)) {
            errors.add("В тексте нельзя использовать символ «;» и переводы строк");
        }
        if (start == null || end == null) {
            errors.add("Не указано время начала или конца");
        } else if (!end.isAfter(start)) {
            errors.add("Конец брони должен быть позже начала");
        }
        return errors;
    }

    protected static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    protected static boolean hasBadChar(String s) {
        return s != null && (s.contains(";") || s.contains("\n") || s.contains("\r"));
    }

    @Override
    public String toString() {
        return "#" + id + " " + room + " " + start + " – " + end + " (" + employee + ")";
    }
}
