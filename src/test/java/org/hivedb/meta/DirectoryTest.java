package org.hivedb.meta;

import org.hivedb.*;
import org.hivedb.Lockable.Status;
import org.hivedb.persistence.Schema;
import org.hivedb.configuration.persistence.HiveConfigurationSchema;
import org.hivedb.management.HiveConfigurationSchemaInstaller;
import org.hivedb.directory.Directory;
import org.hivedb.directory.DirectoryWrapper;
import static org.hivedb.directory.DirectoryWrapper.semaphoreToId;
import org.hivedb.directory.KeySemaphore;
import org.hivedb.configuration.persistence.IndexSchema;
import org.hivedb.util.AssertUtils;
import org.hivedb.util.Lists;
import org.hivedb.util.database.HiveDbDialect;
import org.hivedb.util.database.test.H2TestCase;
import org.hivedb.util.functional.Atom;
import org.hivedb.util.functional.Transform;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;
import org.springframework.jdbc.core.simple.SimpleJdbcDaoSupport;

import java.sql.Types;
import java.util.*;

public class DirectoryTest extends H2TestCase {
  private PartitionDimension dimension;
  private SecondaryIndex nameIndex, numIndex;
  private String secondaryKeyString = "secondary key";
  private Integer secondaryKeyNum = 1;
  private Resource resource;

  public Collection<Schema> getSchemas() {
    return Arrays.asList(new Schema[]{
      new HiveConfigurationSchema(getConnectString(H2TestCase.TEST_DB)),
      new IndexSchema(createPopulatedPartitionDimension())});
  }

  private void prepare() {
    try {
      new HiveConfigurationSchemaInstaller(getConnectString(H2TestCase.TEST_DB)).run();
      Hive hive = null; //= Hive.create(getConnectString(H2TestCase.TEST_DB), partitionDimensionName(), Types.INTEGER, CachingDataSourceProvider.getInstance(), null);
      dimension = createPopulatedPartitionDimension();
//      dimension.setId(hive.getPartitionDimension().getId());
      resource = Atom.getFirstOrThrow(dimension.getResources());
      hive.addResource(resource);
      numIndex = resource.getSecondaryIndex("num");
      nameIndex = resource.getSecondaryIndex("name");
      for (SecondaryIndex secondaryIndex : resource.getSecondaryIndexes()) {
        hive.addSecondaryIndex(resource, secondaryIndex);
      }
      hive.addNode(new Node("node", H2TestCase.TEST_DB, "", HiveDbDialect.H2));
      SimpleJdbcDaoSupport dao = new SimpleJdbcDaoSupport();
//      dao.setDataSource(hive.getDataSourceProvider().getDataSource(getConnectString(H2TestCase.TEST_DB)));
      //		dao.getJdbcTemplate().update("SET TRACE_LEVEL_SYSTEM_OUT 3");
      throw new UnsupportedOperationException("Not yet implemented");
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Before
  @Override
  public void beforeMethod() {
    deleteDatabasesAfterEachTest = true;
    super.afterMethod();
    super.beforeMethod();
    prepare();
  }

  protected PartitionDimensionImpl createPopulatedPartitionDimension() {
    return new PartitionDimensionImpl(
      Hive.NEW_OBJECT_ID,
      partitionDimensionName(),
      Types.INTEGER,
      getConnectString(H2TestCase.TEST_DB),
      createResources());
  }

  protected Collection<Resource> createResources() {
    ArrayList<Resource> resources = new ArrayList<Resource>();
    resources.add(createResource());
    return resources;
  }

  protected ResourceImpl createResource() {
    final ResourceImpl resource = new ResourceImpl("FOO", Types.INTEGER, false, createSecondaryIndexes());
    return resource;
  }

  private Collection<SecondaryIndex> createSecondaryIndexes() {
    return Arrays.asList(
      new SecondaryIndex("name", Types.VARCHAR),
      new SecondaryIndex("num", Types.INTEGER));
  }

  protected PartitionDimension createEmptyPartitionDimension() {
    return new PartitionDimensionImpl(
      Hive.NEW_OBJECT_ID,
      partitionDimensionName(),
      Types.INTEGER,
      getConnectString(H2TestCase.TEST_DB),
      new ArrayList<Resource>());
  }

  protected String partitionDimensionName() {
    return "member";
  }

  private Hive getHive() {
    throw new UnsupportedOperationException("Not yet implemented");
//    return Hive.load(getConnectString(H2TestCase.TEST_DB), CachingDataSourceProvider.getInstance());
  }

  @Test
  public void testInsertPrimaryIndexKey() throws Exception {
    Directory d = getDirectory();
    Integer key = new Integer(43);
    Hive hive = getHive();
    Node firstNode = Atom.getFirst(hive.getNodes());
    d.insertPrimaryIndexKey(Atom.getFirst(hive.getNodes()), key);
    for (Integer id : Transform.map(semaphoreToId(), d.getKeySemamphoresOfPrimaryIndexKey(key)))
      assertEquals((Integer) firstNode.getId(), id);
  }

  @Test
  public void testInsertPrimaryIndexKeyMultipleNodes() throws Exception {
    Directory d = getDirectory();
    Hive hive = getHive();
    Integer key = new Integer(43);
    for (Node node : hive.getNodes())
      d.insertPrimaryIndexKey(node, key);
    Collection<Integer> nodeIds = Transform.map(semaphoreToId(), d.getKeySemamphoresOfPrimaryIndexKey(key));
    AssertUtils.assertUnique(nodeIds);
    assertEquals(hive.getNodes().size(), nodeIds.size());
  }

  @Test
  public void testDeletePrimaryIndexKey() throws Exception {
    insertKeys(getHive());
    Directory d = getDirectory();
    for (String key : getPrimaryIndexOrResourceKeys()) {
      d.deletePrimaryIndexKey(key);
      assertEquals(0, d.getKeySemamphoresOfPrimaryIndexKey(key).size());
    }
  }

  @Test
  public void testDeletePrimaryIndexKeyMultipleNodes() throws Exception {
    Directory d = getDirectory();
    Hive hive = getHive();
    for (String key : getPrimaryIndexOrResourceKeys())
      for (Node node : hive.getNodes())
        d.insertPrimaryIndexKey(node, key);
    for (String key : getPrimaryIndexOrResourceKeys()) {
      d.deletePrimaryIndexKey(key);
      assertEquals(0, d.getKeySemamphoresOfPrimaryIndexKey(key).size());
    }
  }

  @Test
  public void testGetNodeIdsOfPrimaryIndexKey() throws Exception {
    insertKeys(getHive());
    Directory d = getDirectory();
    for (String key : getPrimaryIndexOrResourceKeys())
      assertEquals(1, d.getKeySemamphoresOfPrimaryIndexKey(key).size());
  }

  @Test
  public void testGetNodeIdsOfSecondaryIndexKeys() throws Exception {
    insertKeys(getHive());
    Directory d = getDirectory();
    assertTrue(d.getKeySemaphoresOfSecondaryIndexKey(nameIndex, secondaryKeyString).size() >= 1);
  }

  @Test
  public void testGetKeySemaphoresOfSecondaryIndexKey() throws Exception {
    insertKeys(getHive());
    Directory d = getDirectory();
    assertEquals(1, d.getKeySemaphoresOfSecondaryIndexKey(nameIndex, secondaryKeyString).size());
  }

  @Test
  public void testGetKeySemaphoresOfResourceIds() throws Exception {
    insertKeys(getHive());
    Directory d = getDirectory();
    for (String key : getPrimaryIndexOrResourceKeys())
      assertEquals(1, d.getKeySemaphoresOfResourceId(resource, key).size());
  }

  @Test
  public void testGetKeySemaphoresOfPartitioningResourceIds() throws Exception {
    Hive hive = null; //= Hive.load(getConnectString(H2TestCase.TEST_DB), CachingDataSourceProvider.getInstance());
    hive.deleteResource(resource);
    resource = Atom.getFirstOrNull(dimension.getResources());
    resource.setPartitioningResource(true);
    hive.addResource(resource);

    resource = hive.getPartitionDimension().getResource(resource.getName());

    insertKeys(getHive());
    Directory d = getDirectory();
    for (String key : getPrimaryIndexOrResourceKeys())
      assertEquals(1, d.getKeySemaphoresOfResourceId(resource, key).size());
  }

  @Test
  public void testGetPrimaryIndexKeysOfResourceId() throws Exception {
    Directory d = getDirectory();
    Hive hive = getHive();
    for (String key : getPrimaryIndexOrResourceKeys()) {
      d.insertPrimaryIndexKey(Atom.getFirstOrThrow(hive.getNodes()), key);
      d.insertResourceId(resource, key + 1, key);
      assertEquals(key, d.getPrimaryIndexKeyOfResourceId(resource, key + 1).toString());
    }
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testInsertRelatedSecondaryIndexKeys() throws Exception {
    //beforeMethod = false;
    //afterMethod = false;
    //System.out.println("insertRelated begin");
    Hive hive = getHive();
    //System.out.println(hive.toString());
    //insertKeys(getHive());
    Directory d = getDirectory();
    for (String primaryIndexKey : getPrimaryIndexOrResourceKeys()) {
      hive.directory().insertPrimaryIndexKey(primaryIndexKey);
      d.insertResourceId(resource, primaryIndexKey, primaryIndexKey);

      Map<SecondaryIndex, Collection<Object>> secondaryIndexKeyMap = new Hashtable<SecondaryIndex, Collection<Object>>();
      secondaryIndexKeyMap.put(nameIndex, Arrays.asList(new Object[]{
        secondaryKeyString
      }));
      secondaryIndexKeyMap.put(numIndex, Arrays.asList(new Object[]{
        secondaryKeyNum
      }));
      // TODO: for some reason the BatchIndexWriter won't find the tables when running through maven
      //d.batch().insertSecondaryIndexKeys(secondaryIndexKeyMap, primaryIndexKey);
      for (SecondaryIndex secondaryIndex : secondaryIndexKeyMap.keySet()) {
        for (Object secondaryIndexKeyNum : secondaryIndexKeyMap.get(secondaryIndex)) {
          hive.directory().insertSecondaryIndexKey(secondaryIndex.getResource().getName(), secondaryIndex.getName(), secondaryIndexKeyNum, primaryIndexKey);
        }
      }
      hive.directory().insertSecondaryIndexKey(numIndex.getResource().getName(), numIndex.getName(), secondaryKeyNum, primaryIndexKey);
      assertEquals(1, d.getSecondaryIndexKeysOfResourceId(nameIndex, primaryIndexKey).size());
      assertEquals(secondaryKeyString, Atom.getFirst(d.getSecondaryIndexKeysOfResourceId(nameIndex, primaryIndexKey)));
      assertEquals(1,
        d.getSecondaryIndexKeysOfResourceId(numIndex, primaryIndexKey).size());
      assertEquals(secondaryKeyNum,
        Atom.getFirst(d.getSecondaryIndexKeysOfResourceId(numIndex, primaryIndexKey)));
    }
  }

  @Test
  public void testUpdatePrimaryIndexKeyReadOnly() throws Exception {
    insertKeys(getHive());
    Directory d = getDirectory();
    for (String key : getPrimaryIndexOrResourceKeys()) {
      d.updatePrimaryIndexKeyReadOnly(key, true);
      for (KeySemaphore s : d.getKeySemamphoresOfPrimaryIndexKey(key))
        assertTrue(s.getStatus().equals(Status.readOnly));
    }
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testDeleteSecondaryIndexKey() throws Exception {
    insertKeys(getHive());
    Directory d = getDirectory();
    for (String pkey : getPrimaryIndexOrResourceKeys()) {
      Collection secondaryKeys = d.getSecondaryIndexKeysOfResourceId(nameIndex, pkey);
      assertTrue(secondaryKeys.size() > 0);
      for (Object skey : secondaryKeys)
        d.deleteSecondaryIndexKey(nameIndex, skey, pkey);
      assertTrue(d.getSecondaryIndexKeysOfResourceId(nameIndex, pkey).size() == 0);
    }
  }

  @Test
  public void testDoesPrimaryIndexKeyExist() throws Exception {
    insertKeys(getHive());
    Directory d = getDirectory();
    assertTrue(d.doesPrimaryIndexKeyExist(Atom.getFirst(getPrimaryIndexOrResourceKeys())));
    assertTrue(!d.doesPrimaryIndexKeyExist(new Integer(378465784)));
  }

  @Test
  public void testGetKeySemaphoresOfPrimaryIndexKey() throws Exception {
    insertKeys(getHive());
    Directory d = getDirectory();
    for (String pkey : getPrimaryIndexOrResourceKeys())
      assertEquals(1, d.getKeySemamphoresOfPrimaryIndexKey(pkey).size());
  }

  @Test
  public void testGetKeySemaphoresOfPrimaryIndexKeyMultiNode() throws Exception {
    Directory d = getDirectory();
    Hive hive = getHive();
    for (String pkey : getPrimaryIndexOrResourceKeys()) {
      for (Node node : hive.getNodes())
        d.insertPrimaryIndexKey(node, pkey);
      assertEquals(hive.getNodes().size(), d.getKeySemamphoresOfPrimaryIndexKey(pkey).size());
    }
  }

  @Test
  public void testGetReadOnlyOfPrimaryIndexKey() throws Exception {
    insertKeys(getHive());
    Directory d = getDirectory();
    for (String pkey : getPrimaryIndexOrResourceKeys()) {
      assertEquals(false, Lists.or(Transform.map(DirectoryWrapper.semaphoreToReadOnly(), d.getKeySemamphoresOfPrimaryIndexKey(pkey))));
      d.updatePrimaryIndexKeyReadOnly(pkey, true);
      assertTrue(Lists.or(Transform.map(DirectoryWrapper.semaphoreToReadOnly(), d.getKeySemamphoresOfPrimaryIndexKey(pkey))));
    }
  }

  @Test
  public void testGetReadOnlyOfResourceId() throws Exception {
    insertKeys(getHive());
    Directory d = getDirectory();
    for (String pkey : getPrimaryIndexOrResourceKeys()) {
      assertEquals(false, Lists.or(Transform.map(DirectoryWrapper.semaphoreToReadOnly(), d.getKeySemaphoresOfResourceId(resource, pkey))));
      d.updatePrimaryIndexKeyReadOnly(pkey, true);
      assertTrue(Lists.or(Transform.map(DirectoryWrapper.semaphoreToReadOnly(), d.getKeySemaphoresOfResourceId(resource, pkey))));
    }
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testGetNodeIdsOfSecondaryIndexKey() throws Exception {
    insertKeys(getHive());
    Directory d = getDirectory();
    for (String pkey : getPrimaryIndexOrResourceKeys()) {
      Collection skeys = d.getSecondaryIndexKeysOfResourceId(nameIndex, pkey);
      for (Object skey : skeys) {
        assertTrue(d.getKeySemaphoresOfSecondaryIndexKey(nameIndex, skey).size() > 0);
      }
    }
  }

  @Test
  public void testGetNodeSemphoresOfSecondaryIndexKey() throws Exception {
    insertKeys(getHive());
    Directory d = getDirectory();
    Collection<KeySemaphore> skeys = d.getKeySemaphoresOfSecondaryIndexKey(nameIndex, secondaryKeyString);
    assertEquals(1, skeys.size());
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testGetSecondaryIndexKeysOfPrimaryIndexKey() throws Exception {
    insertKeys(getHive());
    Directory d = getDirectory();
    for (String pkey : getPrimaryIndexOrResourceKeys()) {
      Collection skeys = d.getSecondaryIndexKeysOfResourceId(nameIndex, pkey);
      assertTrue(skeys.size() > 0);
      assertEquals(secondaryKeyString, Atom.getFirst(skeys));
    }

  }

  @Test
  public void testDeleteAllSecondaryKeyForResourceId() throws Exception {
    insertKeys(getHive());
    Directory d = getDirectory();
    for (String key : getPrimaryIndexOrResourceKeys()) {
      assertTrue(d.getSecondaryIndexKeysOfResourceId(numIndex, key).size() > 0);
      // TODO: for some reason the BatchIndexWriter won't find the tables when running through maven
      //d.batch().deleteAllSecondaryIndexKeysOfResourceId(resource, key);
      for (SecondaryIndex secondaryIndex : resource.getSecondaryIndexes()) {
        for (Object secondaryIndexKey : d.getSecondaryIndexKeysOfResourceId(secondaryIndex, key)) {
          d.deleteSecondaryIndexKey(secondaryIndex, secondaryIndexKey, key);
          ;
        }
      }
      assertEquals(0, d.getSecondaryIndexKeysOfResourceId(numIndex, key).size());
    }
  }

  @Test
  public void testGetSecondaryKeyForResourceId() throws Exception {
    insertKeys(getHive());
    Directory d = getDirectory();
    for (String key : getPrimaryIndexOrResourceKeys())
      assertEquals(1, d.getSecondaryIndexKeysOfResourceId(nameIndex, key).size());
  }

  @Test
  public void testDeleteResourceId() throws Exception {
    insertKeys(getHive());
    Directory d = getDirectory();
    for (String key : getPrimaryIndexOrResourceKeys()) {
      // TODO: for some reason the BatchIndexWriter won't find the tables when running through maven
      //d.batch().deleteAllSecondaryIndexKeysOfResourceId(resource, key);
      for (SecondaryIndex secondaryIndex : resource.getSecondaryIndexes()) {
        for (Object secondaryIndexKey : d.getSecondaryIndexKeysOfResourceId(secondaryIndex, key)) {
          d.deleteSecondaryIndexKey(secondaryIndex, secondaryIndexKey, key);
          ;
        }
      }
      assertEquals(0, d.getSecondaryIndexKeysOfResourceId(numIndex, key).size());
      d.deleteResourceId(resource, key);
      assertFalse(d.doesResourceIdExist(resource, key));
      assertEquals(0, d.getSecondaryIndexKeysOfResourceId(nameIndex, key).size());
      assertEquals(0, d.getSecondaryIndexKeysOfResourceId(numIndex, key).size());
    }
  }

  @Test
  public void testUpdatePrimaryIndexKeyOfResourceId() throws Exception {
    insertKeys(getHive());
    Directory d = getDirectory();
    String firstKey = Atom.getFirst(getPrimaryIndexOrResourceKeys());
    for (String key : getPrimaryIndexOrResourceKeys()) {
      d.updatePrimaryIndexKeyOfResourceId(resource, key, firstKey);
      assertEquals(firstKey, d.getPrimaryIndexKeyOfResourceId(resource, key).toString());
    }
  }


  private Directory getDirectory() {
    throw new UnsupportedOperationException("Not yet implemented");
//    return new DbDirectory(dimension, CachingDataSourceProvider.getInstance().getDataSource(dimension.getIndexUri()));
  }

  @Override
  public Collection<String> getDatabaseNames() {
    return Arrays.asList(new String[]{H2TestCase.TEST_DB, "data1", "data2"});
  }

  private Collection<String> getPrimaryIndexOrResourceKeys() {
    return Arrays.asList(new String[]{"1", "2", "3", "4"});
  }

  private void insertKeys(Hive hive) throws HiveLockableException {
    Directory d = getDirectory();
    Resource resource = dimension.getResource(createResource().getName());
    for (String key : getPrimaryIndexOrResourceKeys()) {
      hive.directory().insertPrimaryIndexKey(key);
      d.insertResourceId(resource, key, key);
      hive.directory().insertSecondaryIndexKey(nameIndex.getResource().getName(), nameIndex.getName(), secondaryKeyString, key);
      hive.directory().insertSecondaryIndexKey(numIndex.getResource().getName(), numIndex.getName(), secondaryKeyNum, key);
    }
  }
}
