package de.uni_koblenz.west.koral.master.statisticsDB.impl.multi_file.storage;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

class InMemoryRowStorage implements RowStorage {

	private final int rowLength;

	private Map<Long, byte[]> blocks;

	private final int cacheBlockSize;

	private final long maxBlockCount;

	private final SharedSpaceManager cacheSpaceManager;

	private final int rowsPerBlock;

	private final boolean rowsAsBlocks;

	private final long fileId;

	private InMemoryRowStorage(long fileId, int rowLength, int cacheBlockSize, long maxCacheSize,
			SharedSpaceManager cacheSpaceManager) {
		this.fileId = fileId;
		this.rowLength = rowLength;
		if (cacheBlockSize >= rowLength) {
			// Default case: At least one row fits into a block
			rowsAsBlocks = false;
			rowsPerBlock = cacheBlockSize / rowLength;
			// Fit size of block to row data
			this.cacheBlockSize = rowsPerBlock * rowLength;
		} else {
			System.err
					.println("Warning: In InMemoryRowStorage ID " + fileId + " the cache block size (" + cacheBlockSize
							+ ") is smaller than row length (" + rowLength + "). Resizing blocks to row length.");
			rowsAsBlocks = true;
			rowsPerBlock = 1;
			this.cacheBlockSize = rowLength;
		}
		maxBlockCount = maxCacheSize / this.cacheBlockSize;
		this.cacheSpaceManager = cacheSpaceManager;

		open(true);
	}

	public InMemoryRowStorage(long fileId, int rowLength, int cacheBlockSize, long maxCacheSize) {
		this(fileId, rowLength, cacheBlockSize, maxCacheSize, null);
	}

	public InMemoryRowStorage(long fileId, int rowLength, int cacheBlockSize, SharedSpaceManager cacheSpaceManager) {
		this(fileId, rowLength, cacheBlockSize, -1, cacheSpaceManager);
	}

	@Override
	public void open(boolean createIfNotExisting) {
		// The createIfNotExisting flag does not matter here, because there is no persisted data that has to be checked
		// for being missing/corrupted.
		blocks = new TreeMap<>();
	}

	@Override
	public byte[] readRow(long rowId) throws IOException {
		if (!valid()) {
			throw new IllegalStateException("FileId " + fileId + ": Cannot operate on a closed storage");
		}
		if (rowsAsBlocks) {
			return blocks.get(rowId);
		}
		long blockId = rowId / rowsPerBlock;
		int blockOffset = (int) (rowId % rowsPerBlock) * rowLength;
		byte[] block = blocks.get(blockId);
		if (block != null) {
			byte[] row = new byte[rowLength];
			System.arraycopy(block, blockOffset, row, 0, rowLength);
			return row;
		} else {
			return null;
		}
	}

	@Override
	public boolean writeRow(long rowId, byte[] row) throws IOException {
		if (!valid()) {
			throw new IllegalStateException("FileId " + fileId + ": Cannot operate on a closed storage");
		}
		if (rowsAsBlocks) {
			if (!spaceForOneBlockAvailable()) {
				return false;
			}
			blocks.put(rowId, row);
			return true;
		}
		long blockId = rowId / rowsPerBlock;
		int blockOffset = (int) (rowId % rowsPerBlock) * rowLength;
		byte[] block = blocks.get(blockId);
		if (block != null) {
			System.arraycopy(row, 0, block, blockOffset, row.length);
			// TODO: Necessary?
			blocks.put(blockId, block);
			return true;
		} else {
			if (!spaceForOneBlockAvailable()) {
				return false;
			} else {
				block = new byte[cacheBlockSize];
				System.arraycopy(row, 0, block, blockOffset, row.length);
				blocks.put(blockId, block);
				return true;
			}
		}
	}

	@Override
	public Iterator<Entry<Long, byte[]>> getBlockIterator() {
		if (!valid()) {
			throw new IllegalStateException("FileId " + fileId + ": Cannot operate on a closed storage");
		}
		return blocks.entrySet().iterator();
	}

	/**
	 * Reserving of shared cache space must be ensured before calling this method.
	 */
	@Override
	public void storeBlocks(Iterator<Entry<Long, byte[]>> blockIterator) throws IOException {
		if (!valid()) {
			throw new IllegalStateException("FileId " + fileId + ": Cannot operate on a closed storage");
		}
		while (blockIterator.hasNext()) {
			Entry<Long, byte[]> blockEntry = blockIterator.next();
			if (blockEntry.getValue().length < cacheBlockSize) {
				throw new RuntimeException("FileId " + fileId + ": Given block too short: "
						+ blockEntry.getValue().length + " but cacheBlockSize is " + cacheBlockSize);
			}
			blocks.put(blockEntry.getKey(), blockEntry.getValue());
		}
	}

	@Override
	public boolean valid() {
		return blocks != null;
	}

	@Override
	public boolean isEmpty() {
		if (!valid()) {
			throw new IllegalStateException("Cannot operate on a closed storage");
		}
		return blocks.isEmpty();
	}

	@Override
	public long length() {
		if (!valid()) {
			throw new IllegalStateException("FileId " + fileId + ": Cannot operate on a closed storage");
		}
		return blocks.size() * rowsPerBlock * rowLength;
	}

	@Override
	public int getRowLength() {
		return rowLength;
	}

	@Override
	public void flush() {
		// Nothing to flush to
	}

	@Override
	public void delete() {
		blocks = null;
	}

	/**
	 * Clears the storage. Following calls to valid() return false.
	 */
	@Override
	public void close() {
		if (cacheSpaceManager != null) {
			cacheSpaceManager.releaseAll(this);
		}
		delete();
	}

	private boolean spaceForOneBlockAvailable() {
		if (cacheSpaceManager == null) {
			return blocks.size() < maxBlockCount;
		} else {
			return cacheSpaceManager.request(this, cacheBlockSize);
		}
	}

	@Override
	public boolean makeRoom() {
		// Can't make room in an in-memory-only implementation
		return false;
	}

}
