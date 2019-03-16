package de.uni_koblenz.west.koral.master.statisticsDB.impl.multi_file.storage.caching;

/**
 * A generic LRU cache with O(1) operations. Uses a doubly-linked-list for access order plus an index (Map) for O(1)
 * access. Differs from LRUList by enforcing a given capacity limit.
 *
 * @author philipp
 *
 * @param <K>
 * @param <V>
 */
public class LRUCache<K, V> extends LRUList<K, V> {

	private final long capacity;

	/**
	 * Amount of elements in the cache, that is the doubly linked list. There may be more in the index map, depending on
	 * sub-implementations of {@link #removeEldest(DoublyLinkedNode)}.
	 */
	long size;

	public LRUCache(long capacity) {
		super();
		this.capacity = capacity;
	}

	@Override
	public void put(K key, V value) {
		if (size == capacity) {
			evict();
		}
		super.put(key, value);
		size++;
	}

	@Override
	protected void remove(DoublyLinkedNode node) {
		super.remove(node);
		size--;
	}

	/**
	 * @return The amount of elements in the cache, that is the doubly linked list. There may be more in the index map,
	 *         depending on sub-implementations of {@link #removeEldest(DoublyLinkedNode)}.
	 */
	@Override
	public long size() {
		return size;
	}

	@Override
	public void clear() {
		super.clear();
		size = 0;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder("Cache size: " + size + " [");
		for (DoublyLinkedNode node = head; node != null; node = node.after) {
			sb.append(node.key).append("=").append(node.value);
			if (node.after != null) {
				sb.append(", ");
			}
		}
		sb.append("]");
		sb.append("\nIndex: ");
		sb.append(index.toString());
		return sb.toString();
	}

}
