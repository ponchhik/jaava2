package booking.io;

// Коды ошибок загрузки CSV
public enum LoadErrorCode {
    FILE_READ_ERROR("Не удалось прочитать файл"),
    WRONG_FIELD_COUNT("Неверное число полей"),
    BAD_NUMBER("Неверный числовой формат"),
    BAD_DATE_TIME("Неверный формат даты/времени"),
    UNKNOWN_TYPE("Неизвестный тип брони"),
    BAD_DAY_OF_WEEK("Неизвестный день недели"),
    VALIDATION_FAILED("Данные не прошли проверку"),
    DUPLICATE_ID("Повторяющийся id");

    private final String title;

    LoadErrorCode(String title) {
        this.title = title;
    }

    // Краткое описание кода для показа в GUI
    public String getTitle() {
        return title;
    }
}
