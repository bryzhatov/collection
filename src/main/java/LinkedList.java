import java.util.*;

public class LinkedList<T> extends AbstractList<T> {
    private Node<T> firstElement;
    private Node<T> lastElement;
    private long version;
    private int size;

    @Override
    public T get(int index) {
        checkIndex(index);

        if (index == size - 1) {
            return lastElement.getValue();
        }
        return findNode(index).getValue();
    }

    @Override
    public boolean add(T element) {
        Node<T> elementNode = new Node<>(element);

        if (size == 0) {
            firstElement = elementNode;
            lastElement = firstElement;
        } else {
            lastElement.setNext(elementNode);
            elementNode.setPrevious(lastElement);
            lastElement = elementNode;
        }
        size++;
        version++;
        return true;
    }

    @Override
    public void add(int index, T element) {
        if (index < 0 || index > size) {
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + size);
        }

        Node<T> nodeElement = new Node<>(element);
        if (index == 0) {
            if (size == 0) {
                firstElement = nodeElement;
            } else {
                nodeElement.setNext(firstElement);
                firstElement.setPrevious(nodeElement);
                firstElement = nodeElement;
            }
        } else if (index == size) {
            lastElement.setNext(nodeElement);
            nodeElement.setPrevious(lastElement);
            lastElement = nodeElement;
        } else {
            Node<T> find = findNode(index);
            Node<T> prev = find.getPrevious();

            prev.setNext(nodeElement);
            find.setPrevious(nodeElement);

            nodeElement.setPrevious(prev);
            nodeElement.setNext(find);
        }
        version++;
        size++;
    }

    @Override
    public boolean addAll(int index, Collection<? extends T> c) {
        if (index < 0 || index > size) {
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + size);
        }

        if (c == null) {
            return false;
        }

        LinkedList<T> insertList = new LinkedList<>();
        insertList.addAll(c);

        if (index == 0) {
            if (size == 0) {
                firstElement = insertList.firstElement;
                lastElement = insertList.firstElement;
            } else {
                firstElement.setPrevious(insertList.lastElement);
                insertList.lastElement.setNext(firstElement);
                firstElement = insertList.firstElement;
            }
        } else if (index == size) {
            lastElement.setNext(insertList.firstElement);
            insertList.firstElement.setPrevious(lastElement);
            lastElement = insertList.lastElement;
        } else {
            Node<T> find = findNode(index);
            Node<T> prev = find.getPrevious();

            prev.setNext(insertList.firstElement);
            insertList.lastElement.setPrevious(prev);

            insertList.lastElement.setNext(find);
            find.setPrevious(insertList.lastElement);
        }
        size += insertList.size();
        version++;
        return true;
    }

    @Override
    public T set(int setIndex, T element) {
        checkIndex(setIndex);

        T old;
        if (setIndex == 0) {
            old = firstElement.getValue();
            firstElement.setValue(element);
        } else if (setIndex == size - 1) {
            old = lastElement.getValue();
            lastElement.setValue(element);
        } else {
            Node<T> foundNode = findNode(setIndex);
            old = foundNode.getValue();
            foundNode.setValue(element);
        }
        version++;
        return old;
    }

    @Override
    public T remove(int index) {
        checkIndex(index);

        T old;
        if (index == 0) {
            old = firstElement.getValue();
            if (firstElement.hasNext()) {
                firstElement = firstElement.getNext();
                firstElement.setPrevious(null);
            } else {
                firstElement = null;
            }
        } else if (index == size - 1) {
            Node<T> prev = lastElement.getPrevious();
            prev.setNext(null);
            old = lastElement.getValue();
            lastElement = prev;
        } else {
            Node<T> removeNode = findNode(index);
            Node<T> prev = removeNode.getPrevious();
            Node<T> next = removeNode.getNext();

            prev.setNext(next);
            next.setPrevious(prev);

            old = removeNode.getValue();
        }
        size--;
        version++;
        return old;
    }

    @Override
    public int indexOf(Object o) {
        if (o == null && firstElement == null) {
            return -1;
        }
        Node<T> find = firstElement;
        int i = 0;
        while (find != null) {
            if (find.getValue().equals(o)) {
                return i;
            }
            find = find.getNext();
            i++;
        }
        return -1;
    }

    @Override
    public int lastIndexOf(Object o) {
        if (o == null && lastElement == null) {
            return -1;
        }
        Node<T> find = lastElement;
        int i = size - 1;
        while (find != null) {
            if (find.getValue().equals(o)) {
                return i;
            }
            find = find.getPrevious();
            i--;
        }
        return -1;
    }

    @Override
    public void clear() {
        firstElement = null;
        lastElement = null;
        size = 0;
        version++;
    }

    @Override
    public Iterator<T> iterator() {
        return listIterator();
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public ListIterator<T> listIterator() {
        return new InnerListIterator();
    }

    public class InnerListIterator implements ListIterator<T> {
        private int cursorIndex = -1;
        private Node<T> cursorNode = firstElement;
        private long innerVersion = version;
        private boolean throwException = true;

        @Override
        public boolean hasNext() {
            return cursorIndex < size - 1;
        }

        @Override
        public T next() {
            checkLastModified();
            if (cursorNode == null) {
                throw new NoSuchElementException();
            }
            T val = cursorNode.getValue();
            cursorNode = cursorNode.getNext();
            cursorIndex++;
            throwException = false;
            return val;
        }

        @Override
        public boolean hasPrevious() {
            return cursorNode.hasPrevious();
        }

        @Override
        public T previous() {
            checkLastModified();
            if (cursorNode == null || cursorNode.getPrevious() == null) {
                throw new NoSuchElementException();
            }
            T val = cursorNode.getPrevious().getValue();
            cursorNode = cursorNode.getPrevious();
            cursorIndex--;
            throwException = false;
            return val;
        }

        @Override
        public int nextIndex() {
            return cursorNode != null && cursorNode.hasNext() ? cursorIndex + 1 : size;
        }

        @Override
        public int previousIndex() {
            return cursorNode != null && cursorNode.hasPrevious() ? cursorIndex - 1 : -1;
        }

        @Override
        public void remove() {
            checkCorrectCalls();
            throwException = true;
            LinkedList.this.remove(cursorIndex);
            innerVersion = version;
        }

        @Override
        public void set(T element) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void add(T element) {
            throw new UnsupportedOperationException();
        }

        private void checkLastModified() {
            if (innerVersion != version) {
                throw new ConcurrentModificationException();
            }
        }

        private void checkCorrectCalls() {
            if (throwException) {
                throw new IllegalStateException();
            }
        }
    }

    @Override
    public ListIterator<T> listIterator(int fromIndex) {
        checkIndex(fromIndex);
        return subList(fromIndex, size - 1).listIterator();
    }

    @Override
    public List<T> subList(int fromIndex, int toIndex) {
        if (fromIndex > toIndex) {
            throw new IllegalArgumentException();
        }
        if (fromIndex != 0 && toIndex != 0) {
            checkIndex(fromIndex);
            checkIndex(toIndex);
        }

        List<T> subList = new LinkedList<>();
        if (size > 0) {
            Node<T> startNode = findNode(fromIndex);

            for (int i = fromIndex; i <= toIndex; i++) {
                subList.add(startNode.getValue());
                startNode = startNode.getNext();
            }
        }
        return subList;
    }

    public T getFirstElement() {
        return firstElement != null ? firstElement.getValue() : null;
    }

    public T getLastElement() {
        return lastElement != null ? lastElement.getValue() : null;
    }

    private Node<T> findNode(int foundIndexNode) {
        checkIndex(foundIndexNode);

        int i;
        Node<T> foundNode;
        double half = size / 2.;

        if (foundIndexNode < half) {
            foundNode = firstElement;
            i = 0;
            while (foundIndexNode != i) {
                foundNode = foundNode.getNext();
                i++;
            }
        } else {
            foundNode = lastElement;
            i = size - 1;
            while (foundIndexNode != i) {
                foundNode = foundNode.getPrevious();
                i--;
            }
        }
        return foundNode;
    }

    private void checkIndex(int index) {
        if (index < 0 || index >= size) {
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + size);
        }
    }

    private static class Node<E> {
        private Node<E> next;
        private Node<E> previous;
        private E value;

        Node(E value) {
            this.value = value;
        }

        Node<E> getNext() {
            return next;
        }

        void setNext(Node<E> next) {
            this.next = next;
        }

        Node<E> getPrevious() {
            return previous;
        }

        void setPrevious(Node<E> previous) {
            this.previous = previous;
        }

        E getValue() {
            return value;
        }

        void setValue(E value) {
            this.value = value;
        }

        boolean hasNext() {
            return next != null;
        }

        boolean hasPrevious() {
            return previous != null;
        }
    }
}
