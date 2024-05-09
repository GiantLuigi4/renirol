package tfc.renirol.util.set;

import java.util.*;

/**
 * a HashSet which keeps tracks of how many times each object has been added
 * just like a HashSet, this is backed by a HashMap
 * however, this holds a Map<T,int[]>
 * for sets of data where there are many duplicate entries (the intended usecsse for a QHS), a QHS will perform approximately twice as well
*/
@SuppressWarnings("unchecked")
public class QuantizedHashSet<T> extends AbstractSet<T> {
	private final HashMap<T, int[]> ts = new HashMap<>();
	
	@Override
	public int size() {
		int o = 0;
		for (int[] value : ts.values()) o += value[0];
		return o;
	}
	
	@Override
	public boolean isEmpty() {
		return ts.isEmpty();
	}
	
	@Override
	public boolean contains(Object o) {
		return ts.containsKey((T)o);
	}
	
	@Override
	public Iterator<T> iterator() {
		final Iterator<Map.Entry<T, int[]>> itr = ts.entrySet().iterator();
		//noinspection SpellCheckingInspection
		return new Iterator<>() {
			T current = null;
			int qnty = 0;
			
			@Override
			public final boolean hasNext() {
				return itr.hasNext() || qnty != 0;
			}
			
			@Override
			public final T next() {
				if (qnty == 0) {
					Map.Entry<T, int[]> entry = itr.next();
					current = entry.getKey();
					qnty = entry.getValue()[0];
				}
				qnty--;
				return current;
			}
		};
	}
	
	@Override
	public Object[] toArray() {
		Object[] array = new Object[size()];
		int id = 0;
		for (T t : this) array[id] = t;
		return array;
	}
	
	@Override
	public <T1> T1[] toArray(T1[] a) {
		Object[] array = toArray();
		array = Arrays.copyOf(array, array.length, a.getClass());
		return (T1[]) array;
	}
	
	@Override
	public boolean add(T t) {
		int[] i = ts.get(t);
		if (i == null) ts.put(t, new int[]{1});
		else i[0] += 1;
		return true;
	}
	
	@Override
	public boolean remove(Object o) {
		int[] i = ts.get((T) o);
		if (i == null) return false;
		
		else if (i[0] == 1) ts.remove(o);
		else i[0] -= 1;
		return true;
	}
	
	@Override
	public void clear() {
		ts.clear();
	}
}
