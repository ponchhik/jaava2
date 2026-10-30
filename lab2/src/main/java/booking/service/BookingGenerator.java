package booking.service;

import booking.model.Booking;
import booking.model.PastBooking;
import booking.model.RecurringBooking;
import booking.model.SingleBooking;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.Random;
import java.util.Set;

// Генератор случайных броней для тестового набора данных
public final class BookingGenerator {
    private static final String[] ROOMS = {"Альфа", "Бета", "Гамма", "Дельта", "Эпсилон", "Зета",
            "Эта", "Тета", "Йота", "Каппа", "Лямбда", "Мю"};
    private static final String[] EMPLOYEES = {"Иванов И.", "Петрова А.", "Сидоров П.", "Кузнецова М.",
            "Смирнов Д.", "Попова Е.", "Васильев К.", "Соколова О.", "Михайлов Р.", "Новикова Л."};
    private static final String[] TOPICS = {"Планёрка", "Обсуждение бюджета", "Интервью", "Демо клиенту",
            "Ретроспектива", "Обучение", "1:1"};
    private static final DayOfWeek[] WORKDAYS = {DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
            DayOfWeek.THURSDAY, DayOfWeek.FRIDAY};

    private static final double PAST_SHARE = 0.15;
    private static final double SINGLE_CUMULATIVE_SHARE = 0.80;
    private static final int MIN_DURATION_MINUTES = 30;
    private static final int DURATION_STEP_MINUTES = 15;
    private static final int DURATION_STEP_COUNT = 11;

    private final Random rnd;

    public BookingGenerator(long seed) { rnd = new Random(seed); }

    public static String[] rooms() { return ROOMS.clone(); }

    public String randomRoom() { return ROOMS[rnd.nextInt(ROOMS.length)]; }

    public Booking next(int id) {
        double r = rnd.nextDouble();
        String room = randomRoom();
        String employee = EMPLOYEES[rnd.nextInt(EMPLOYEES.length)];
        LocalDateTime start = randomStart(r < PAST_SHARE);
        LocalDateTime end = start.plusMinutes(MIN_DURATION_MINUTES + DURATION_STEP_MINUTES * rnd.nextInt(DURATION_STEP_COUNT));    // 30..180 минут
        if (r < PAST_SHARE) return new PastBooking(id, room, employee, start, end);
        if (r < SINGLE_CUMULATIVE_SHARE) return new SingleBooking(id, room, employee, start, end, TOPICS[rnd.nextInt(TOPICS.length)]);
        Set<DayOfWeek> days = EnumSet.noneOf(DayOfWeek.class);
        for (DayOfWeek d : WORKDAYS) if (rnd.nextInt(3) == 0) days.add(d);
        if (days.isEmpty()) days.add(WORKDAYS[rnd.nextInt(WORKDAYS.length)]);
        return new RecurringBooking(id, room, employee, start, end, days);
    }

    // Рабочий день, время 08:00–18:45 с шагом 15 минут
    private LocalDateTime randomStart(boolean past) {
        LocalDate base = past ? LocalDate.of(2025, 1, 1) : LocalDate.of(2026, 10, 1);
        LocalDate d = base.plusDays(rnd.nextInt(past ? 365 : 730));
        if (d.getDayOfWeek() == DayOfWeek.SATURDAY) d = d.plusDays(2);
        if (d.getDayOfWeek() == DayOfWeek.SUNDAY) d = d.plusDays(1);
        return d.atTime(8 + rnd.nextInt(11), 15 * rnd.nextInt(4));
    }
}
