package de.uni_koblenz.west.koral.master.statisticsDB.impl.multi_file.storage;

import java.io.IOException;

public interface RowStorage extends AutoCloseable {

	/**
	 * TODO: What does this do? Should it be idempotent? - Can be called after close() to reopen - Throws exception if
	 * createIfNotExisting is false and file doesn't exist
	 *
	 * @param createIfNotExisting
	 */
	public void open(boolean createIfNotExisting);

	/**
	 *
	 * @param rowId
	 * @return Null if no entry found
	 * @throws IOException
	 */
	public byte[] readRow(long rowId) throws IOException;

	/**
	 * @return False if there is not enough space left, true otherwise.
	 */
	public boolean writeRow(long rowId, byte[] row) throws IOException;

	/**
	 * @return All rows contained in the storage. Be careful to not call this on large stores.
	 */
	public byte[] getRows() throws IOException;

	public void storeRows(byte[] rows) throws IOException;

	public boolean defrag(long[] freeSpaceIndexData);

	/**
	 * TODO: If valid == false, storage must be reopened
	 *
	 * @return
	 */
	public boolean valid();

	/**
	 * If no useful information is contained, so that the storage can be cleaned up/deleted.
	 *
	 * @return
	 */
	public boolean isEmpty();

	/**
	 * TODO: Length of valid data or used space? Currently only used for index space.
	 *
	 * @return
	 */
	public long length();

	public int getRowLength();

	public void flush() throws IOException;

	public void delete();

	@Override
	public void close();
}
