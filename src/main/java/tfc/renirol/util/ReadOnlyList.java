package tfc.renirol.util;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

public class ReadOnlyList<T> implements List<T> {
    List<T> backing;

    public ReadOnlyList(List<T> backing) {
        this.backing = backing;
    }

    @Override
    public int size() {
        return backing.size();
    }

    @Override
    public boolean isEmpty() {
        return backing.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        return backing.contains(o);
    }

    @Override
    public Iterator<T> iterator() {
        return backing.iterator();
    }

    @Override
    public Object[] toArray() {
        return backing.toArray();
    }

    @Override
    public <T1> T1[] toArray(T1[] a) {
        return backing.toArray(a);
    }

    @Override
    public boolean add(T t) {
        throw new RuntimeException("Not supported");
    }

    @Override
    public boolean remove(Object o) {
        throw new RuntimeException("Not supported");
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        return backing.contains(c);
    }

    @Override
    public boolean addAll(Collection<? extends T> c) {
        throw new RuntimeException("Not supported");
    }

    @Override
    public boolean addAll(int index, Collection<? extends T> c) {
        throw new RuntimeException("Not supported");
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        throw new RuntimeException("Not supported");
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        throw new RuntimeException("Not supported");
    }

    @Override
    public void clear() {
        throw new RuntimeException("Not supported");
    }

    @Override
    public boolean equals(Object o) {
        return backing.equals(o);
    }

    @Override
    public int hashCode() {
        return backing.hashCode();
    }

    @Override
    public T get(int index) {
        return backing.get(index);
    }

    @Override
    public T set(int index, T element) {
        throw new RuntimeException("Not supported");
    }

    @Override
    public void add(int index, T element) {
        throw new RuntimeException("Not supported");
    }

    @Override
    public T remove(int index) {
        throw new RuntimeException("Not supported");
    }

    @Override
    public int indexOf(Object o) {
        return backing.indexOf(o);
    }

    @Override
    public int lastIndexOf(Object o) {
        return backing.lastIndexOf(o);
    }

    @Override
    public ListIterator<T> listIterator() {
        return new ReadOnlyListIterator();
    }

    @Override
    public ListIterator<T> listIterator(int index) {
        return new ReadOnlyListIterator(index);
    }

    @Override
    public List<T> subList(int fromIndex, int toIndex) {
        return new ReadOnlyList<>(backing.subList(fromIndex, toIndex));
    }

    class ReadOnlyListIterator implements ListIterator<T> {
        ListIterator<T> back;

        public ReadOnlyListIterator(ListIterator<T> back) {
            this.back = back;
        }

        public ReadOnlyListIterator() {
            back = backing.listIterator();
        }

        public ReadOnlyListIterator(int start) {
            back = backing.listIterator(start);
        }

        @Override
        public boolean hasNext() {
            return back.hasNext();
        }

        @Override
        public T next() {
            return back.next();
        }

        @Override
        public boolean hasPrevious() {
            return back.hasPrevious();
        }

        @Override
        public T previous() {
            return back.previous();
        }

        @Override
        public int nextIndex() {
            return back.nextIndex();
        }

        @Override
        public int previousIndex() {
            return back.previousIndex();
        }

        @Override
        public void remove() {
            throw new RuntimeException("Not supported");
        }

        @Override
        public void set(T t) {
            throw new RuntimeException("Not supported");
        }

        @Override
        public void add(T t) {
            throw new RuntimeException("Not supported");
        }
    }
}
