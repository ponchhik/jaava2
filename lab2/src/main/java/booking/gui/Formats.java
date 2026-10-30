package booking.gui;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

// Единые форматы даты/времени в интерфейсе
public final class Formats {
    public static final String DATE_HINT = "гггг-мм-дд чч:мм";
    public static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    public static final DateTimeFormatter SHORT = DateTimeFormatter.ofPattern("MM-dd HH:mm");
    public static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");

    private Formats() { }

    public static LocalDateTime parse(String text) throws DateTimeParseException {
        return LocalDateTime.parse(text.trim(), DATE_TIME);
    }

    public static String badDateMessage(String text) {
        return "Неверный формат даты «" + text.trim() + "».\nОжидается: " + DATE_HINT;
    }
}
