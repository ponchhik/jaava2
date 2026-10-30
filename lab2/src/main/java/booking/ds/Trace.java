package booking.ds;

// След операции: какие узлы посетили / нашли / отсекли. Нужен для подсветки в визуализации.
public final class Trace {
    public enum Kind { VISITED, MATCH, PRUNED, INSERTED }

    private static final int LIMIT = 20_000;   // хранимых записей (счётчики считают всё)

    private Object[] values = new Object[64];
    private Kind[] kinds = new Kind[64];
    private int size;
    private final int[] counts = new int[Kind.values().length];

    // null-безопасная отметка: дерево вызывает её, даже если след не нужен
    public static void mark(Trace t, Object value, Kind kind) {
        if (t != null) t.add(value, kind);
    }

    private void add(Object value, Kind kind) {
        counts[kind.ordinal()]++;
        if (size >= LIMIT) return;
        if (size == values.length) {
            Object[] v = new Object[size * 2];
            Kind[] k = new Kind[size * 2];
            System.arraycopy(values, 0, v, 0, size);
            System.arraycopy(kinds, 0, k, 0, size);
            values = v;
            kinds = k;
        }
        values[size] = value;
        kinds[size++] = kind;
    }

    public int size() { return size; }
    public Object valueAt(int i) { return values[i]; }
    public Kind kindAt(int i) { return kinds[i]; }
    public int count(Kind kind) { return counts[kind.ordinal()]; }
}
