package booking.ds;

public interface Interval<K extends Comparable<? super K>> {
    K low();    // ключ дерева — начало интервала
    K high();   // конец интервала (maxend)
    int uid();  // уникальный номер: различает записи с одинаковым началом
}
