package booking.io;

import java.io.IOException;

// Нет файла, нет доступа, неверная кодировка
public class CsvFileException extends CsvLoadException {

    public CsvFileException(IOException cause) {
        super(LoadErrorCode.FILE_READ_ERROR, 0,
                cause.getClass().getSimpleName() + ": " + cause.getMessage(), cause);
    }
}
