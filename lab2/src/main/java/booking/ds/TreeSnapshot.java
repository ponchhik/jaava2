package booking.ds;

// Неизменяемый «снимок» верхних уровней дерева для рисования (copy-on-read):
// панель рисует копию и не держит блокировку структуры.
public final class TreeSnapshot<K, T> {

    public static final class NodeInfo<K, T> {
        public final T value;
        public final K maxEnd;
        public final int depth;
        public final NodeInfo<K, T> parent;
        public int col;                 // порядковый номер слева направо
        public boolean hasHidden;       // под узлом есть скрытые уровни
        public Trace.Kind kind;         // подсветка (null — обычный узел)

        public NodeInfo(T value, K maxEnd, int depth, NodeInfo<K, T> parent) {
            this.value = value;
            this.maxEnd = maxEnd;
            this.depth = depth;
            this.parent = parent;
        }
    }

    private final ArrayBuf<NodeInfo<K, T>> nodes;
    private final int total;
    private final int height;
    private final int levels;

    public TreeSnapshot(ArrayBuf<NodeInfo<K, T>> nodes, int total, int height, int levels) {
        this.nodes = nodes;
        this.total = total;
        this.height = height;
        this.levels = levels;
    }

    public int count() { return nodes.size(); }
    public NodeInfo<K, T> get(int i) { return nodes.get(i); }
    public int total() { return total; }
    public int height() { return height; }
    public int levels() { return levels; }

    // Раскрашивает узлы по следу операции. MATCH/INSERTED сильнее PRUNED, PRUNED сильнее VISITED.
    public void applyTrace(Trace trace) {
        if (trace == null) return;
        for (int i = 0; i < nodes.size(); i++) {
            NodeInfo<K, T> n = nodes.get(i);
            for (int j = 0; j < trace.size(); j++) {
                if (trace.valueAt(j) != n.value) continue;
                Trace.Kind k = trace.kindAt(j);
                if (n.kind == null || rank(k) > rank(n.kind)) n.kind = k;
            }
        }
    }

    private static int rank(Trace.Kind k) {
        return switch (k) {
            case VISITED -> 1;
            case PRUNED -> 2;
            case MATCH, INSERTED -> 3;
        };
    }
}
