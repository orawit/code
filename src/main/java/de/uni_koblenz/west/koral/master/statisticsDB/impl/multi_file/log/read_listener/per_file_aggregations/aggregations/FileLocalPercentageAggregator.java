package de.uni_koblenz.west.koral.master.statisticsDB.impl.multi_file.log.read_listener.per_file_aggregations.aggregations;

import de.uni_koblenz.west.koral.master.statisticsDB.impl.multi_file.log.read_listener.per_file_aggregations.Aggregator;

public class FileLocalPercentageAggregator extends Aggregator {

	public FileLocalPercentageAggregator() {}

	@Override
	protected float aggregate(long accumulatedValue, long accumulationCounter, long extraValue) {
		return (accumulatedValue / (float) accumulationCounter) * 100;
	}

}
