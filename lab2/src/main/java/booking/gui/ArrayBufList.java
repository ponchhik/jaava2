package booking.gui;

import booking.ds.ArrayBuf;

import javafx.collections.ObservableListBase;

// Адаптер для TableView: показывает ArrayBuf как список БЕЗ копирования (для 10^5 строк копия была бы заметна).
// Это только вид — основное хранилиЫще остаётся деревом. Список неизменяемый: при обновлении создаётся новый.
public final class ArrayBufList<T> extends ObservableListBase<T> {
    private final ArrayBuf<T> source;
    private final int size;

    public ArrayBufList(ArrayBuf<T> source) { this(source, Integer.MAX_VALUE); }

    // limit — сколько первых элементов показывать
    public ArrayBufList(ArrayBuf<T> source, int limit) {
        this.source = source;
        this.size = Math.min(source.size(), limit);
    }

    @Override public T get(int index) {
        if (index < 0 || index >= size) throw new IndexOutOfBoundsException("index " + index + ", size " + size);
        return source.get(index);
    }

    @Override public int size() { return size; }
}
