package org.hivedb.meta.directory;

import org.hivedb.meta.Node;
import org.hivedb.meta.Resource;
import org.hivedb.meta.SecondaryIndex;

import java.util.Collection;
import java.util.Map;

public interface Directory {
  boolean doesPrimaryIndexKeyExist(Object primaryIndexKey);

  Collection<KeySemaphore> getKeySemamphoresOfPrimaryIndexKey(Object primaryIndexKey);

  void deletePrimaryIndexKey(Object primaryIndexKey);

  Collection<KeySemaphore> getKeySemaphoresOfResourceId(Resource resource, Object id);

  void deleteResourceId(Resource resource, Object id);

  boolean doesSecondaryIndexKeyExist(SecondaryIndex index, Object secondaryIndexKey, Object resourceId);

  void deleteSecondaryIndexKey(SecondaryIndex index, Object secondaryIndexKey, Object resourceId);

  boolean doesResourceIdExist(Resource resource, Object resourceId);

  Collection<KeySemaphore> getKeySemaphoresOfSecondaryIndexKey(SecondaryIndex secondaryIndex, Object secondaryIndexKey);

  Object insertPrimaryIndexKey(Node node, Object primaryIndexKey);

  Object insertResourceId(Resource resource, Object id, Object primaryIndexKey);

  Object insertSecondaryIndexKey(SecondaryIndex secondaryIndex, Object secondaryIndexKey, Object resourceId);

  Object updatePrimaryIndexKeyOfResourceId(Resource r, Object resourceId, Object newPrimaryIndexKey);

  Object updatePrimaryIndexKeyReadOnly(Object primaryIndexKey, boolean readOnly);

  Object getPrimaryIndexKeyOfResourceId(Resource resource, Object resourceId);

  void deleteSecondaryIndexKeys(Map<SecondaryIndex, Collection<Object>> secondaryIndexValueMap, Object resourceId);

  Object insertSecondaryIndexKeys(Map<SecondaryIndex, Collection<Object>> secondaryIndexValueMap, Object resourceId);

  Collection getSecondaryIndexKeysOfResourceId(SecondaryIndex secondaryIndex, Object id);
}
