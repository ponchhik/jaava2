package booking.service;

import booking.model.Booking;

// Бронь не может быть сохранена: комната уже занята другой бронью в это время
public final class BookingConflictException extends Exception {
    private final transient Booking conflicting;

    public BookingConflictException(Booking candidate, Booking conflicting) {
        super("Конфликт: комната «" + candidate.getRoom() + "» уже занята\n" + conflicting);
        this.conflicting = conflicting;
    }

    public Booking getConflicting() { return conflicting; }
}
