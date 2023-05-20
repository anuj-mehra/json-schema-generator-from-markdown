package com.anuj.markdowntojson.schemagenerator.manager.schema.datatypes.cache;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

/**
 * 
 * @author anujmehra
 *
 */
@Component("compositeDatatypeCache")
public class CompositeDatatypeCache {

	/**
	 * 
	 */
	private final Map<String, String> compositeDataTypeCacheMap = new LinkedHashMap<String, String>();

	/**
	 * 
	 * @param key
	 * @param value
	 */
	public void updateCompositeDatatypeCache(String key, String value){
		compositeDataTypeCacheMap.put(key, value);
	}

	/**
	 * 
	 * @param key
	 * @return
	 */
	public String getCompositeDatatype(String key){
		return compositeDataTypeCacheMap.get(key);
	}

	/**
	 * 
	 * @return
	 */
	public Map<String,String> getCompositeDatatypeCacheMap(){
		return compositeDataTypeCacheMap;
	}
	
}
