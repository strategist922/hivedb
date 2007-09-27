package org.hivedb.util.functional;

import java.util.Hashtable;
import java.util.Map;
public class QuickCache {
	Map<Object, Object> cache = new Hashtable<Object, Object>();
	@SuppressWarnings("unchecked")
	public<T> T get(Object key, Delay<T> delay)
	{
		if (!cache.containsKey(key))
			cache.put(key, delay.f());
		return (T) cache.get(key);
	}	
}
