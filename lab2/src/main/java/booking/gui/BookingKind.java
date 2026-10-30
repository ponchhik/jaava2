package booking.gui;

// Редактируемые виды броней, которые можно создать через «Добавить»
// (прошедшая бронь сюда намеренно не входит — она появляется только из файла)
public enum BookingKind {
    SINGLE("Обычная"),
    RECURRING("Регулярная");

    private final String title;

    BookingKind(String title) { this.title = title; }

    @Override public String toString() { return title; }
}
