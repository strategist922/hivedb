package org.hivedb.management.statistics;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertTrue;
import static org.testng.AssertJUnit.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Random;

import org.hivedb.Hive;
import org.hivedb.meta.PartitionDimension;
import org.hivedb.meta.SecondaryIndex;
import org.hivedb.meta.persistence.IndexSchema;
import org.hivedb.util.database.test.H2HiveTestCase;
import org.hivedb.util.functional.Atom;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class TestPartitionKeyStatisticsPersistence extends H2HiveTestCase {
	private Collection<Integer> keys;
	private Hive hive;
	private PartitionDimension partitionDimension;
	private SecondaryIndex secondaryIndex;
	
	@Override
	public Collection<String> getDatabaseNames() {
		return Arrays.asList(new String[] {getHiveDatabaseName()});
	}
	
	@BeforeMethod
	public void setUp() throws Exception{
		hive = Hive.load(getConnectString(getHiveDatabaseName()));
		hive.addPartitionDimension(createPopulatedPartitionDimension());
		hive.addSecondaryIndex(hive.getPartitionDimension(partitionDimensionName()).getResource(createResource().getName()), createSecondaryIndex());
		new IndexSchema(hive.getPartitionDimension(partitionDimensionName())).install();
		hive.addNode(hive.getPartitionDimension(partitionDimensionName()), createNode(getHiveDatabaseName()));
		
		secondaryIndex = hive.getPartitionDimension(partitionDimensionName()).getResource(createResource().getName()).getSecondaryIndex(createSecondaryIndex().getName());

		partitionDimension = hive.getPartitionDimension(partitionDimensionName());
		keys = new ArrayList<Integer>();
		Random rand = new Random();
		for(int i=0; i<5; i++) {
			Integer key = new Integer(rand.nextInt());
			hive.insertPrimaryIndexKey(partitionDimension.getName(), key);
			keys.add(key);
		}
	}
	
	@Test
	public void testUpdate() {
		PartitionKeyStatisticsDao dao = new PartitionKeyStatisticsDao(getDataSource(getHiveDatabaseName()));
		PartitionKeyStatistics frozen = null;
		PartitionKeyStatistics thawed = null;
		try {
			frozen = dao.findByPartitionKey(partitionDimension, keys.iterator()
					.next());
			frozen.setChildRecordCount(23);
			dao.update(frozen);
			thawed = dao.findByPartitionKey(partitionDimension, frozen
					.getKey());
		} catch (Exception e) {
			e.printStackTrace();
			fail("Error creating the statistics entry");
		}
	
		assertEquals(frozen.getChildRecordCount(), thawed.getChildRecordCount());
		//microsecond discrepancies between test time and update time since update 
		//time is set in the DAO
		assertTrue(Math.abs(frozen.getLastUpdated().getTime() - thawed.getLastUpdated().getTime()) < 10);

	}

	@Test
	public void testFindByPartitionKey() throws Exception {
		PartitionKeyStatisticsDao dao = new PartitionKeyStatisticsDao(getDataSource(getHiveDatabaseName()));
		PartitionKeyStatisticsBean frozen = dao.findByPartitionKey(partitionDimension, Atom.getFirst(keys));
		assertNotNull(frozen);
	}

	@Test
	public void testIncrementChildRecords() {
		PartitionKeyStatisticsDao dao = new PartitionKeyStatisticsDao(getDataSource(getHiveDatabaseName()));
		PartitionKeyStatistics frozen = new PartitionKeyStatisticsBean(partitionDimension, keys
				.iterator().next(), new Date(System.currentTimeMillis()));
		frozen.setChildRecordCount(21);
				dao.update(frozen);
			dao.incrementChildRecordCount(frozen.getPartitionDimension(),
					frozen.getKey(), 2);
			PartitionKeyStatistics thawed = dao.findByPartitionKey(frozen
					.getPartitionDimension(), frozen.getKey());
			assertEquals(frozen.getChildRecordCount() + 2, thawed
					.getChildRecordCount());
		
	}

	@Test
	public void testDecrementChildRecords() {
		PartitionKeyStatisticsDao dao = new PartitionKeyStatisticsDao(getDataSource(getHiveDatabaseName()));
		PartitionKeyStatistics frozen = new PartitionKeyStatisticsBean(partitionDimension, keys
				.iterator().next(), new Date(System.currentTimeMillis()));
		frozen.setChildRecordCount(21);
		
			dao.update(frozen);
			dao.decrementChildRecordCount(frozen.getPartitionDimension(),
					frozen.getKey(), 2);
			PartitionKeyStatistics thawed = dao.findByPartitionKey(frozen
					.getPartitionDimension(), frozen.getKey());
			assertEquals(frozen.getChildRecordCount() - 2, thawed
					.getChildRecordCount());
		
	}
	
	@Test
	public void testFindAllByNode() throws Exception {
		PartitionKeyStatisticsDao dao = new PartitionKeyStatisticsDao(getDataSource(getHiveDatabaseName()));

		List<PartitionKeyStatistics> stats = dao.findAllByNodeAndDimension(
				partitionDimension,
				Atom.getFirst(partitionDimension.getNodes()));
		assertNotNull(stats);
		assertEquals(5, stats.size());
		for(PartitionKeyStatistics s : stats) {
			assertTrue(keys.contains(s.getKey()));
			assertEquals(PartitionKeyStatisticsBean.class, s.getClass());
		}
	}

	//TODO: update to accommodate indexing changes
//	@Test
//	public void testSecondaryIndexHooks() throws Exception {
//		Object key = Atom.getFirst(keys);
//
//		PartitionKeyStatisticsDao dao = new PartitionKeyStatisticsDao(getDataSource(getHiveDatabaseName()));
//		PartitionKeyStatistics frozen = dao.findByPartitionKey(partitionDimension, key);
//		
//		hive.insertSecondaryIndexKey(secondaryIndex.getName(), secondaryIndex.getResource().getName(), secondaryIndex.getResource().getPartitionDimension().getName(), new Integer(1), key);
//		hive.insertSecondaryIndexKey(secondaryIndex.getName(), secondaryIndex.getResource().getName(), secondaryIndex.getResource().getPartitionDimension().getName(), new Integer(2), key);
//		hive.insertSecondaryIndexKey(secondaryIndex.getName(), secondaryIndex.getResource().getName(), secondaryIndex.getResource().getPartitionDimension().getName(), new Integer(3), key);
//
//		PartitionKeyStatistics thawed = dao.findByPartitionKey(partitionDimension,
//				frozen.getKey());
//
//		assertEquals(frozen.getChildRecordCount() + 3, thawed
//				.getChildRecordCount());
//	}
	
	@Test
	public void testRetrieval() {
		int failures = 0;
		PartitionKeyStatisticsDao dao = new PartitionKeyStatisticsDao(getDataSource(getHiveDatabaseName()));
		
		for(Object key : keys)
			try {
				dao.findByPartitionKey(partitionDimension, key);
			} catch( Exception e) {
				failures++;
			}
		assertEquals(0, failures);
	}
	
	@Test
	public void testUpdateDates() {
		int failures = 0;
		PartitionKeyStatisticsDao dao = new PartitionKeyStatisticsDao(getDataSource(getHiveDatabaseName()));
		for(Object key : keys)
			try {
				PartitionKeyStatistics stats = dao.findByPartitionKey(partitionDimension, key);
//				System.out.println(stats.getLastUpdated());
			} catch( Exception e) {
				failures++;
			}
		assertEquals(0, failures);
	}
}