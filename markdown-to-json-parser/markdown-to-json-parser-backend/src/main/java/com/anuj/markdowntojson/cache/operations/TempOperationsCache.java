package com.anuj.markdowntojson.cache.operations;

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
@Component("tempOperationsCache")
public class TempOperationsCache implements OperationsCache{

	/**
	 * 
	 */
	private final Map<String, List<String>> operationCache = new LinkedHashMap<String, List<String>>();

	/**
	 * 
	 * @param key
	 * @param value
	 */
	public  void updateOperationsCache(String key, List<String> value){
		operationCache.put(key, value);
	}

	/**
	 * 
	 * @param key
	 * @return
	 */
	public List<String> getOperations(String key){
		return operationCache.get(key);
	}

	/**
	 * 
	 * @return
	 */
	public Map<String,List<String>> getOperationsCacheMap(){
		return operationCache;
	}

	/**
	 * 
	 * @return
	 */
	public Set<String> getOperationsNames(){
		return operationCache.keySet();
	}

}
