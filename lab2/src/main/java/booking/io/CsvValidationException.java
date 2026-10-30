package booking.io;

import java.util.List;

//validate() вернул ошибки или id повторяется.
public class CsvValidationException extends CsvLoadException {
    private final List<String> problems;

    public CsvValidationException(LoadErrorCode code, int lineNo, List<String> problems) {
        super(code, lineNo, String.join("; ", problems));
        this.problems = List.copyOf(problems);
    }

    public List<String> getProblems() { return problems; }
}
