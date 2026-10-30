package booking.ds;

// Собственное множество int на открытой адресации (для проверки повторяющихся id)
public final class IntSet {
    private int[] table = new int[64];
    private boolean[] used = new boolean[64];
    private int size;

    public boolean add(int x) {
        if (size * 2 >= table.length) rehash();
        int i = slot(x, table.length);
        while (used[i]) {
            if (table[i] == x) return false;
            i = (i + 1) & (table.length - 1);
        }
        used[i] = true;
        table[i] = x;
        size++;
        return true;
    }

    private static int slot(int x, int cap) {
        int h = x * 0x9E3779B1;
        return (h ^ (h >>> 15)) & (cap - 1);
    }

    private void rehash() {
        int[] oldT = table;
        boolean[] oldU = used;
        table = new int[oldT.length * 2];
        used = new boolean[oldT.length * 2];
        size = 0;
        for (int i = 0; i < oldT.length; i++) if (oldU[i]) add(oldT[i]);
    }
}
