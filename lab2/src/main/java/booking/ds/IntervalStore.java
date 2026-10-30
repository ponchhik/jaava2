package booking.ds;

// Хранилище интервалов: основные операции + дополнительные
public interface IntervalStore<K extends Comparable<? super K>, T extends Interval<K>> extends Bazhraniliha<K, T> {

    // Результат проверки инвариантов
    record Integrity(int reachable, int declaredSize, int orderErrors, int maxEndErrors) {
        public boolean ok() {
            return reachable == declaredSize && orderErrors == 0 && maxEndErrors == 0;
        }
    }

    void add(T entity, Trace trace);
    T findByKey(K key, Trace trace);
    boolean removeByKey(K key, Trace trace);
    boolean remove(T entity, Trace trace);

    // Дополнительные операции
    ArrayBuf<T> findOverlapping(K from, K to, Trace trace);                     // брони, пересекающиеся с интервалом
    T findConflict(K from, K to, Matcher<T> filter, Trace trace);                // конфликт комнаты (первый подходящий)

    // Служебное
    ArrayBuf<T> toBuf(boolean preOrder);
    TreeSnapshot<K, T> snapshot(int levels);
    int height();
    Integrity checkIntegrity();
    void clear();

    default boolean replace(T oldEntity, T newEntity, Trace trace) {
        if (!remove(oldEntity, trace)) return false;
        add(newEntity, trace);
        return true;
    }

    @Override default void add(T entity) { add(entity, null); }
    @Override default T findByKey(K key) { return findByKey(key, null); }
    @Override default boolean removeByKey(K key) { return removeByKey(key, null); }
}
