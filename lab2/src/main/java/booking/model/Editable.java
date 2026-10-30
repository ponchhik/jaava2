package booking.model;

import java.util.List;

//Кнопка изменить в GUI доступна только для объектов, реализующих этот интерфейс
public interface Editable {
    //пустой список = данные корректны
    List<String> validate();
}
