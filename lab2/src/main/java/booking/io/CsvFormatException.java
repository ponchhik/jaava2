package booking.io;

// Ошибка формата строки: число полей, число, дата, тип, день недели.
public class CsvFormatException extends CsvLoadException {

    public CsvFormatException(LoadErrorCode code, int lineNo, String details) {
        super(code, lineNo, details);
    }

    public CsvFormatException(LoadErrorCode code, int lineNo, String details, Throwable cause) {
        super(code, lineNo, details, cause);
    }
}
