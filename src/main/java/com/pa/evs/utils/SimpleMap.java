package com.pa.evs.utils;

import java.util.LinkedHashMap;

/**
 * 
 * @author thanh
 *
 * @param <K>
 * @param <V>
 */
public class SimpleMap<K, V> extends LinkedHashMap<K, V> {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6432425980803990708L;

	public static <K> SimpleMap<K, Object> init(K k, Object v) {
		
		SimpleMap<K, Object> map = new SimpleMap<>();
		map.put(k, v);
		return map;
	}
	
	public static <K, V> SimpleMap<K, V> initKV(K k, V v) {
		
		SimpleMap<K, V> map = new SimpleMap<>();
		map.put(k, v);
		return map;
	}

	public SimpleMap<K, V> more(K k, V v) {
		this.put(k, v);
		return this;
	}
}
