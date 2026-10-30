package booking.ds;

// Дерево интервалов: двоичное дерево поиска по началу + maxEnd
public final class IntervalTree<K extends Comparable<? super K>, T extends Interval<K>> implements IntervalStore<K, T> {

    // Собственный узел
    private static final class Node<K extends Comparable<? super K>, T extends Interval<K>> {
        T value;
        Node<K, T> left, right;
        K maxEnd;                       // максимум high() во всём поддереве

        Node(T value) {
            this.value = value;
            this.maxEnd = value.high();
        }
    }

    private Node<K, T> root;
    private int size;

    // Порядок в дереве: (начало, uid) — записи с одинаковым началом различаются по uid
    private int compare(T a, T b) {
        int c = a.low().compareTo(b.low());
        return c != 0 ? c : Integer.compare(a.uid(), b.uid());
    }

    @SuppressWarnings("unchecked")
    private Node<K, T> node(Object o) { return (Node<K, T>) o; }

    //увеличение массива
    private static Object[] grow(Object[] a, int len) {
        if (len < a.length) return a;
        Object[] b = new Object[a.length * 2];
        System.arraycopy(a, 0, b, 0, len);
        return b;
    }

    @Override public int size() { return size; }

    // вставка: O(h)
    @Override
    public void add(T e, Trace trace) {
        if (e.high().compareTo(e.low()) <= 0) {
            throw new IllegalArgumentException("конец интервала должен быть позже начала");
        }
        Node<K, T> fresh = new Node<>(e);
        if (root == null) {
            root = fresh;
        } else {
            Node<K, T> cur = root;
            while (true) {
                //помечаем как посещенный во время обхода
                Trace.mark(trace, cur.value, Trace.Kind.VISITED);
                // обновляем maxEnd «по пути вниз»
                if (cur.maxEnd.compareTo(e.high()) < 0) cur.maxEnd = e.high();
                if (compare(e, cur.value) < 0) {
                    if (cur.left == null) { cur.left = fresh; break; }
                    cur = cur.left;
                } else {
                    if (cur.right == null) { cur.right = fresh; break; }
                    cur = cur.right;
                }
            }
        }
        size++;
        //метка о вставке
        Trace.mark(trace, e, Trace.Kind.INSERTED);
    }

    // поиск по ключу (началу): O(h)
    // При равных началах возвращает запись с наименьшим uid (она левее остальных).
    @Override
    public T findByKey(K key, Trace trace) {
        Node<K, T> cur = root, best = null;
        while (cur != null) {
            Trace.mark(trace, cur.value, Trace.Kind.VISITED);
            int c = key.compareTo(cur.value.low());
            if (c < 0) cur = cur.left;
            else if (c > 0) cur = cur.right;
            else { best = cur; cur = cur.left; }
        }
        if (best == null) return null;
        Trace.mark(trace, best.value, Trace.Kind.MATCH);
        return best.value;
    }

    @Override
    public boolean removeByKey(K key, Trace trace) {
        T found = findByKey(key, null);
        return found != null && remove(found, trace);
    }

    //удаление конкретной записи: O(h)
    @Override
    //принимает сущность е
    public boolean remove(T e, Trace trace) {
        Object[] path = new Object[32];   // предки удаляемого узла
        int len = 0;
        Node<K, T> cur = root;
        while (cur != null) {
            Trace.mark(trace, cur.value, Trace.Kind.VISITED);
            int c = compare(e, cur.value);
            if (c == 0) break;
            path = grow(path, len);
            path[len++] = cur;
            cur = c < 0 ? cur.left : cur.right;
        }
        if (cur == null) return false;

        if (cur.left != null && cur.right != null) {
            // два ребёнка: подставляем минимальный элемент правого поддерева (преемника)
            path = grow(path, len);
            path[len++] = cur;
            Node<K, T> s = cur.right;
            while (true) {
                Trace.mark(trace, s.value, Trace.Kind.VISITED);
                if (s.left == null) break;
                path = grow(path, len);
                path[len++] = s;
                s = s.left;
            }
            cur.value = s.value;
            Node<K, T> parent = node(path[len - 1]);
            //чтобы родитель ссылался не на s а на потомка s
            if (parent.left == s) parent.left = s.right; else parent.right = s.right;
        } else { //хотя бы одного ребенка нет
            Node<K, T> child = cur.left != null ? cur.left : cur.right;
            if (len == 0) {
                root = child;
            } else {
                Node<K, T> parent = node(path[len - 1]);
                if (parent.left == cur) parent.left = child; else parent.right = child;
            }
        }
        for (int i = len - 1; i >= 0; i--) recalc(node(path[i]));   // maxEnd пересчитываем снизу вверх
        size--;
        return true;
    }

    private void recalc(Node<K, T> n) {
        K m = n.value.high();
        if (n.left != null && n.left.maxEnd.compareTo(m) > 0) m = n.left.maxEnd;
        if (n.right != null && n.right.maxEnd.compareTo(m) > 0) m = n.right.maxEnd;
        n.maxEnd = m;
    }

    //  дополнительные операции
    // Брони, пересекающиеся с [from; to)
    @Override
    //все пересечения
    public ArrayBuf<T> findOverlapping(K from, K to, Trace trace) {
        return collect(from, to, null, false, trace);
    }

    // Конфликт: первая запись, пересекающаяся с [from; to) и подходящая под filter (комната)
    @Override
    public T findConflict(K from, K to, Matcher<T> filter, Trace trace) {
        ArrayBuf<T> r = collect(from, to, filter, true, trace);
        return r.isEmpty() ? null : r.get(0);
    }

    // Обход «слева направо» с отсечением поддеревьев по maxEnd и по ключу
    private ArrayBuf<T> collect(K from, K to, Matcher<T> filter, boolean firstOnly, Trace trace) {
        ArrayBuf<T> out = new ArrayBuf<>();
        //пустое дерево-пустой результат
        if (root == null) return out;
        if (root.maxEnd.compareTo(from) <= 0) {  // всё дерево заканчивается раньше запроса-отсекаем все дерево
            Trace.mark(trace, root.value, Trace.Kind.PRUNED);
            return out;
        }
        Object[] stack = new Object[32];
        int sp = 0;
        Node<K, T> cur = root;
        while (cur != null || sp > 0) {
            // спуск влево, пока в левом поддереве есть шанс пересечься
            while (cur != null) {
                Trace.mark(trace, cur.value, Trace.Kind.VISITED);
                stack = grow(stack, sp);
                stack[sp++] = cur;
                Node<K, T> l = cur.left;
                if (l != null && l.maxEnd.compareTo(from) > 0) {
                    cur = l; // в левом поддереве есть интервал, кончающийся позже from
                } else {
                    if (l != null) Trace.mark(trace, l.value, Trace.Kind.PRUNED);   // max_end(лев.) <= from → пропускаем целиком
                    cur = null;
                }
            }
            //проверяем сам узел
            Node<K, T> n = node(stack[--sp]);
            T v = n.value;
            boolean startsBeforeEnd = v.low().compareTo(to) < 0;
            // пересечение: v.low < to && from < v.high && filter
            if (startsBeforeEnd && from.compareTo(v.high()) < 0 && (filter == null || filter.matches(v))) {
                out.add(v);
                Trace.mark(trace, v, Trace.Kind.MATCH);
                if (firstOnly) return out; // нашли первое — выходим
            }
            Node<K, T> r = n.right;
            // вправо — только если ключи справа ещё < to и есть конец > from
            if (r != null && startsBeforeEnd && r.maxEnd.compareTo(from) > 0) {
                cur = r;
            } else if (r != null) {
                Trace.mark(trace, r.value, Trace.Kind.PRUNED);   // все ключи справа >= to или правое поддерево кончается раньше from
            }
        }
        return out;
    }
    //служебные операции
    //всё содержимое дерева в ArrayBuf
    @Override
    public ArrayBuf<T> toBuf(boolean preOrder) {
        ArrayBuf<T> out = new ArrayBuf<>(size);
        Object[] stack = new Object[32];
        int sp = 0;  // индекс следующей свободной ячейки
        if (preOrder) {   // корень первым: повторная вставка воссоздаёт ту же форму дерева
            if (root != null) stack[sp++] = root;
            while (sp > 0) {
                Node<K, T> n = node(stack[--sp]);
                out.add(n.value);
                // Кладём детей в стек В ОБРАТНОМ порядке (сначала right, потом left)
                if (n.right != null) { stack = grow(stack, sp); stack[sp++] = n.right; }
                if (n.left != null) { stack = grow(stack, sp); stack[sp++] = n.left; }
            }
        } else {         // по возрастанию (начало, uid)
            Node<K, T> cur = root;
            while (cur != null || sp > 0) {
                while (cur != null) { stack = grow(stack, sp); stack[sp++] = cur; cur = cur.left; }
                // Левого поддерева больше нет — обрабатываем узел
                Node<K, T> n = node(stack[--sp]);
                out.add(n.value);
                cur = n.right;
            }
        }
        return out;
    }

    @Override
    public int height() {
        if (root == null) return 0;
        Object[] st = new Object[32]; //узлы, которые ещё надо посетить
        int[] dp = new int[32];  //их глубины (на каком уровне от корня они лежат)
        int sp = 0, best = 0;
        st[sp] = root; dp[sp++] = 1;
        while (sp > 0) {
            sp--; // снять верхний узел
            Node<K, T> n = node(st[sp]);
            int d = dp[sp];
            if (d > best) best = d; // обновить максимум, если этот узел глубже всех виденных
            // Пробуем обоих детей
            for (int side = 0; side < 2; side++) {
                Node<K, T> ch = side == 0 ? n.left : n.right;
                if (ch == null) continue;
                if (sp == st.length) {
                    st = grow(st, sp);
                    int[] nd = new int[dp.length * 2];
                    System.arraycopy(dp, 0, nd, 0, sp);
                    dp = nd;
                }
                // Кладём ребёнка на глубину d+1
                st[sp] = ch; dp[sp++] = d + 1;
            }
        }
        return best;
    }

    // Снимок верхних уровней для рисования
    @Override
    public TreeSnapshot<K, T> snapshot(int levels) {
        ArrayBuf<TreeSnapshot.NodeInfo<K, T>> out = new ArrayBuf<>();
        // Обходим дерево, начиная с корня
        walk(root, 0, null, levels, out);
        // size и height берём из самого дерева; levels сохраняем, чтобы потребитель
        // снимка знал, сколько уровней реально показано
        return new TreeSnapshot<>(out, size, height(), levels);
    }

    private void walk(Node<K, T> n, int depth, TreeSnapshot.NodeInfo<K, T> parent, int levels,
                      ArrayBuf<TreeSnapshot.NodeInfo<K, T>> out) {
        if (n == null) return;
        // Создаём описание узла: значение, maxEnd (для отладки/подписей),
        // глубина, ссылка на родителя.
        TreeSnapshot.NodeInfo<K, T> info = new TreeSnapshot.NodeInfo<>(n.value, n.maxEnd, depth, parent);
        if (depth + 1 >= levels) {
            // Помечаем, что у узла ЕСТЬ дети, которых мы не показали
            info.hasHidden = n.left != null || n.right != null;
            info.col = out.size();
            out.add(info);
            return;
        }
        walk(n.left, depth + 1, info, levels, out);
        info.col = out.size();  // номер колонки = место в симметричном обходе
        out.add(info);
        walk(n.right, depth + 1, info, levels, out);
    }

    // Проверка инвариантов: порядок ключей, maxEnd, число достижимых узлов
    @Override
    public Integrity checkIntegrity() {
        int count = 0, order = 0, maxErr = 0;
        T prev = null;
        Object[] stack = new Object[32];
        int sp = 0;
        long guard = 0, limit = 4L * size + 1_000_000L;
        Node<K, T> cur = root;
        while ((cur != null || sp > 0) && guard++ < limit) {
            while (cur != null && guard++ < limit) { stack = grow(stack, sp); stack[sp++] = cur; cur = cur.left; }
            if (sp == 0) break;
            Node<K, T> n = node(stack[--sp]);
            count++;
            if (prev != null && compare(prev, n.value) > 0) order++;
            prev = n.value;
            K m = n.value.high();
            if (n.left != null && n.left.maxEnd.compareTo(m) > 0) m = n.left.maxEnd;
            if (n.right != null && n.right.maxEnd.compareTo(m) > 0) m = n.right.maxEnd;
            if (m.compareTo(n.maxEnd) != 0) maxErr++;
            cur = n.right;
        }
        return new Integrity(count, size, order, maxErr);
    }

    @Override
    public void clear() {
        root = null;
        size = 0;
    }
}
