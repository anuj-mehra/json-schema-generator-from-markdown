package com.anuj.markdowntojson.cache.datatypes;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Component;

/**
 * 
 * @author anujmehra
 *
 */
@Component("datatypesCache")
public class DatatypesCache{

	/**
	 * 
	 */
	private final Map<String, List<String>> datatypeCache = new LinkedHashMap<String, List<String>>();

	/**
	 * 
	 * @param key
	 * @param value
	 */
	public void updateDatatypeCache(String key, List<String> value){
		datatypeCache.put(key, value);
	}

	/**
	 * 
	 * @param key
	 * @return
	 */
	public List<String> getDatatype(String key){
		return datatypeCache.get(key);
	}

	/**
	 * 
	 * @return
	 */
	public Map<String,List<String>> getDatatypeCacheMap(){
		return datatypeCache;
	}
	
	/**
	 * 
	 * @return
	 */
	public Set<String> getDatatypeNames(){
		return datatypeCache.keySet();
	}

}
