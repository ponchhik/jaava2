package booking.ds;

// Собственный растущий массив. Используется только для результатов запросов,
// снимков и списка ошибок
public final class ArrayBuf<T> {
    private Object[] items;
    private int size;

    public ArrayBuf() { this(16); }

    public ArrayBuf(int capacity) {
        items = new Object[Math.max(capacity, 4)];
    }

    public void add(T x) {
        if (size == items.length) {
            Object[] bigger = new Object[size * 2];
            System.arraycopy(items, 0, bigger, 0, size);
            items = bigger;
        }
        items[size++] = x;
    }

    @SuppressWarnings("unchecked")
    public T get(int i) {
        if (i < 0 || i >= size) throw new IndexOutOfBoundsException("index " + i + ", size " + size);
        return (T) items[i];
    }

    public int size() { return size; }
    public boolean isEmpty() { return size == 0; }
}
