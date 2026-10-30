package booking.io;

public class CsvLoadException extends Exception {
    private final LoadErrorCode code;
    private final int lineNo;

    public CsvLoadException(LoadErrorCode code, int lineNo, String details) {
        super(details);
        this.code = code;
        this.lineNo = lineNo;
    }

    public CsvLoadException(LoadErrorCode code, int lineNo, String details, Throwable cause) {
        super(details, cause);
        this.code = code;
        this.lineNo = lineNo;
    }

    public LoadErrorCode getCode() { return code; }
    public int getLineNo() { return lineNo; }

    @Override
    public String toString() {
        String where = lineNo > 0 ? "Строка " + lineNo + " " : "";
        return where + "[" + code + "]: " + getMessage();
    }
}
