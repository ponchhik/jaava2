package booking.gui;

import booking.io.CsvFileException;

// Текст ошибки для диалога
public final class ErrorMessages {
    private ErrorMessages() { }

    public static String describe(Throwable ex) {
        if (ex instanceof CsvFileException f) {
            return "[" + f.getCode() + "] " + f.getCode().getTitle() + ":\n" + f.getMessage();
        }
        return ex.getClass().getSimpleName() + ": " + ex.getMessage();
    }
}
