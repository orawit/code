package de.uni_koblenz.west.koral.master.statisticsDB.impl.multi_file.log.counter;

public interface Counter extends AutoCloseable, Iterable<Long> {

	public void countFor(long element);

	public long getFrequency(long element);

	public void reset();

	/**
	 * Is supposed to delete any created data and release all resources.
	 */
	@Override
	public void close();
}
