package booking.gui;

import booking.model.Booking;

// Колонки таблицы броней: заголовок, ширина и способ получить текст из брони
public enum BookingColumn {
    ID("ID", 60) {
        @Override public String text(Booking b) { return String.valueOf(b.getId()); }
    },
    TYPE("Тип", 150) {
        @Override public String text(Booking b) { return b.typeTitle(); }
    },
    ROOM("Комната", 90) {
        @Override public String text(Booking b) { return b.getRoom(); }
    },
    EMPLOYEE("Сотрудник", 130) {
        @Override public String text(Booking b) { return b.getEmployee(); }
    },
    START("Начало", 130) {
        @Override public String text(Booking b) { return b.getStart().format(Formats.DATE_TIME); }
    },
    END("Конец", 130) {
        @Override public String text(Booking b) { return b.getEnd().format(Formats.DATE_TIME); }
    },
    EXTRA("Дополнительно", 170) {
        @Override public String text(Booking b) { return b.extraText(); }
    };

    private final String title;
    private final double width;

    BookingColumn(String title, double width) {
        this.title = title;
        this.width = width;
    }

    public String title() { return title; }
    public double width() { return width; }
    public abstract String text(Booking b);
}
