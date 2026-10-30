package booking.ds;

// Базовый контракт хранилища
public interface Bazhraniliha<K, T> {
    void add(T entity);
    T findByKey(K key);
    boolean removeByKey(K key);
    int size();
}
