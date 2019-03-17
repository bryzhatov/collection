import java.util.*;

public class HashMap<K, V> extends AbstractMap<K, V> implements Map<K, V> {
    private static final double STEP_INCREASE = 1.5;
    private final double LOAD_FACTOR;
    private Node<K, V>[] nodes;
    private long version;
    private int capacity;
    private int size;

    public HashMap() {
        this.capacity = 16;
        this.LOAD_FACTOR = 0.75;
        this.nodes = (Node<K, V>[]) new Node[capacity];
    }

    public HashMap(int capacity) {
        this();
        this.capacity = capacity;
    }

    public HashMap(int capacity, double loadFactor) {
        this.capacity = capacity;
        this.LOAD_FACTOR = loadFactor;
        this.nodes = (Node<K, V>[]) new Node[capacity];
    }

    @Override
    public V put(K key, V value) {
        reBuild();
        int index = getIndexEntry(key);

        Node<K, V> newNode = new Node<>(key, value);
        Node<K, V> head = nodes[index];

        if (head == null) {
            nodes[index] = newNode;
            size++;
            version++;
            return null;
        }

        if (key == null) {
            return addNull(newNode);
        } else {
            return addNotNull(newNode);
        }
    }

    @Override
    public V get(Object key) {
        Node<K, V> node = find((K) key);
        return node != null ? node.value : null;
    }

    @Override
    public V remove(Object key) {
        Node<K, V> head = nodes[getIndexEntry(key)];
        if (head == null) {
            return null;
        }

        Node<K, V> previous = null;
        Node<K, V> cursor = head;
        Node<K, V> next = head.next;

        while (cursor != null) {
            if (cursor.key.hashCode() == key.hashCode() && cursor.key.equals(key)) {
                if (previous == null) {
                    V old = cursor.value;
                    nodes[getIndexEntry(key)] = next;
                    size--;
                    version++;
                    return old;
                }

                V old = cursor.value;
                previous.next = next;
                size--;
                version++;
                return old;
            }
            previous = cursor;
            cursor = cursor.next;
            next = cursor.next;
        }
        return null;
    }

    @Override
    public boolean containsKey(Object key) {
        return get(key) != null;
    }

    @Override
    public boolean containsValue(Object value) {
        return values().contains(value);
    }

    @Override
    public void clear() {
        for (int i = 0; i < nodes.length; i++) {
            nodes[i] = null;
        }
        size = 0;
        version++;
    }

    @Override
    public Collection<V> values() {
        List<V> values = new ArrayList<>();
        for (Entry<K, V> entry : entrySet()) {
            values.add(entry.getValue());
        }
        return values;
    }

    @Override
    public Set<Entry<K, V>> entrySet() {
        return new AbstractSet<Entry<K, V>>() {

            @Override
            public Iterator<Entry<K, V>> iterator() {
                return new HashMapIterator();
            }

            @Override
            public boolean contains(Object o) {
                if (o instanceof Map.Entry) {
                    Map.Entry entry = (Entry) o;
                    K key = (K) entry.getKey();
                    V value = (V) entry.getValue();
                    return containsKey(key) && containsValue(value);
                }
                return false;
            }

            @Override
            public boolean remove(Object o) {
                if (o instanceof Map.Entry) {
                    Map.Entry entry = (Entry) o;
                    K key = (K) entry.getKey();
                    if (HashMap.this.containsKey(key)) {
                        HashMap.this.remove(key);
                        return true;
                    }
                }
                return false;
            }

            @Override
            public int size() {
                return size;
            }

            @Override
            public void clear() {
                HashMap.this.clear();
            }
        };
    }

    private class HashMapIterator implements Iterator<Entry<K, V>> {
        int basketIndex = -1;
        Node<K, V> current;
        Node<K, V> next;
        int iterationIndex = -1;
        boolean throwException = true;
        long versionIterator = version;

        HashMapIterator() {
            changeBasket();
        }

        @Override
        public boolean hasNext() {
            return next != null || changeBasket();
        }

        @Override
        public Entry<K, V> next() {
            checkLastModified();

            if (next == null) {
                if (!changeBasket()) {
                    return null;
                }
            }
            iterationIndex++;
            current = next;
            next = next.next;
            throwException = false;
            return current;
        }

        @Override
        public void remove() {
            checkLastModified();

            if (throwException) {
                throw new IllegalStateException();
            }
            if (current != null) {
                HashMap.this.remove(current.key);
                versionIterator = version;
                throwException = true;
            }
        }

        private boolean changeBasket() {
            while (next == null && basketIndex < capacity - 1) {
                basketIndex++;
                next = nodes[basketIndex];
                iterationIndex = -1;
                if (next != null) {
                    return true;
                }
            }
            return false;
        }

        private void checkLastModified() {
            if (versionIterator != version) {
                throw new ConcurrentModificationException();
            }
        }
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public boolean isEmpty() {
        return size() == 0;
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> m) {
        Iterator<? extends K> keys = m.keySet().iterator();
        Iterator<? extends V> values = m.values().iterator();

        for (int i = 0; i < m.size(); i++) {
            put(keys.next(), values.next());
        }
    }

    @Override
    public Set<K> keySet() {
        TreeSet<K> keys = new TreeSet<>();
        for (Entry<K, V> entry : entrySet()) {
            keys.add(entry.getKey());
        }
        return keys;
    }

    @Override
    public V replace(K key, V value) {
        if (containsKey(key)) {
            return put(key, value);
        }
        return null;
    }

    private Node<K, V> find(K key) {
        Node<K, V> head = nodes[getIndexEntry(key)];
        if (head == null) {
            return null;
        }
        if (key == null) {
            return findNull();
        } else {
            return findNotNull(key);
        }
    }

    private Node<K, V> findNull() {
        Node<K, V> cursor = nodes[0];

        while (cursor != null) {
            if (cursor.key == null) {
                return cursor;
            }
            cursor = cursor.next;
        }
        return null;
    }

    private V addNull(Node<K, V> node) {
        Node<K, V> cursor = nodes[getIndexEntry(node.key)];

        while (cursor != null) {
            if (cursor.key == null) {
                V old = cursor.value;
                cursor.value = node.value;
                version++;
                return old;
            }
            if (cursor.hasNext()) {
                cursor = cursor.next;
            } else {
                cursor.next = node;
                size++;
                version++;
                return null;
            }
        }
        throw new IllegalArgumentException();
    }

    private V addNotNull(Node<K, V> node) {
        Node<K, V> cursor = nodes[getIndexEntry(node.key)];
        while (cursor != null) {
            if (cursor.key.hashCode() == node.key.hashCode()
                    && cursor.key.equals(node.key)) {
                V old = cursor.value;
                cursor.value = node.value;
                version++;
                return old;
            } else {
                if (cursor.hasNext()) {
                    cursor = cursor.next;
                } else {
                    cursor.next = node;
                    version++;
                    size++;
                    return null;
                }
            }
        }
        throw new IllegalArgumentException();
    }

    private Node<K, V> findNotNull(K key) {
        Node node = nodes[getIndexEntry(key)];
        while (node != null) {
            if (node.key.hashCode() == key.hashCode() && node.key.equals(key)) {
                return node;
            }
            node = node.next;
        }
        return null;
    }

    private int getIndexEntry(Object key) {
        if (key == null) {
            return 0;
        } else {
            return Math.abs(key.hashCode() % capacity);
        }
    }

    private void reBuild() {
        if (((double) size / capacity) >= LOAD_FACTOR) {
            Node<K, V>[] old = nodes;

            capacity *= STEP_INCREASE;
            nodes = (Node<K, V>[]) new Node[capacity];

            for (Node<K, V> entry : old) {
                put(entry.getKey(), entry.getValue());
                size--;
            }
        }

    }

    private static class Node<K, V> implements Map.Entry<K, V> {
        private Node<K, V> next;
        private K key;
        private V value;

        Node(K key, V value) {
            this.value = value;
            this.key = key;
        }

        boolean hasNext() {
            return next != null;
        }

        @Override
        public K getKey() {
            return key;
        }

        @Override
        public V getValue() {
            return value;
        }

        @Override
        public V setValue(V value) {
            V old = this.value;
            this.value = value;
            return old;
        }

        @Override
        public String toString() {
            return "Node{" +
                    "key=" + key +
                    ", value=" + value +
                    '}';
        }
    }
}
