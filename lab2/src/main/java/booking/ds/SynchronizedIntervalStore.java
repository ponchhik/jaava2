package booking.ds;

// Потокобезопасная обёртка: каждая операция — критическая секция (synchronized на экземпляре).
// Составные операции (replace = remove + add) выполняются под одной блокировкой.
public final class SynchronizedIntervalStore<K extends Comparable<? super K>, T extends Interval<K>>
        implements IntervalStore<K, T> {

    private IntervalTree<K, T> tree;

    public SynchronizedIntervalStore(IntervalTree<K, T> tree) { this.tree = tree; }

    // Атомарная подмена дерева (после успешной фоновой загрузки)
    public synchronized void swapTree(IntervalTree<K, T> fresh) { this.tree = fresh; }

    @Override public synchronized int size() { return tree.size(); }
    @Override public synchronized void add(T e, Trace t) { tree.add(e, t); }
    @Override public synchronized T findByKey(K key, Trace t) { return tree.findByKey(key, t); }
    @Override public synchronized boolean removeByKey(K key, Trace t) { return tree.removeByKey(key, t); }
    @Override public synchronized boolean remove(T e, Trace t) { return tree.remove(e, t); }
    @Override public synchronized boolean replace(T o, T n, Trace t) { return tree.replace(o, n, t); }
    @Override public synchronized ArrayBuf<T> findOverlapping(K from, K to, Trace t) { return tree.findOverlapping(from, to, t); }
    @Override public synchronized T findConflict(K from, K to, Matcher<T> f, Trace t) { return tree.findConflict(from, to, f, t); }
    @Override public synchronized ArrayBuf<T> toBuf(boolean preOrder) { return tree.toBuf(preOrder); }
    @Override public synchronized TreeSnapshot<K, T> snapshot(int levels) { return tree.snapshot(levels); }
    @Override public synchronized int height() { return tree.height(); }
    @Override public synchronized Integrity checkIntegrity() { return tree.checkIntegrity(); }
    @Override public synchronized void clear() { tree.clear(); }
}
