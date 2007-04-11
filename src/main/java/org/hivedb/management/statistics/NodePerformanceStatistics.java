package org.hivedb.management.statistics;

public interface NodePerformanceStatistics {
	
	public long getAverageReadCount();
	public long getMinReadCount();
	public long getMaxReadCount();
	public double getVarianceReadCount();
	public long getWindowReadCount();
	public long getIntervalReadCount();
	public void addToReadCount(long value);
	public void incrementReadCount();
	public void decrementReadCount();
	
	public long getAverageReadFailures();
	public long getMinReadFailures();
	public long getMaxReadFailures();
	public double getVarianceReadFailures();
	public long getWindowReadFailures();
	public long getIntervalReadFailures();
	public void addToReadFailures(long value);
	public void incrementReadFailures();
	public void decrementReadFailures();

	public long getAverageWriteCount();
	public long getMinWriteCount();
	public long getMaxWriteCount();
	public double getVarianceWriteCount();
	public long getWindowWriteCount();
	public long getIntervalWriteCount();
	public void addToWriteCount(long value);
	public void incrementWriteCount();
	public void decrementWriteCount();
	
	public long getAverageWriteFailures();
	public long getMinWriteFailures();
	public long getMaxWriteFailures();
	public double getVarianceWriteFailures();
	public long getWindowWriteFailures();
	public long getIntervalWriteFailures();
	public void addToWriteFailures(long value);
	public void incrementWriteFailures();
	public void decrementWriteFailures();
}