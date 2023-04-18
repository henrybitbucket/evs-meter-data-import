package com.pa.evs.utils;

import java.util.Arrays;
import java.util.Properties;

import org.springframework.boot.env.OriginTrackedMapPropertySource;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.AbstractEnvironment;
import org.springframework.core.env.EnumerablePropertySource;

public class AppProps {

	static Properties properties = new Properties();
	
	public static String get(String key) {
		
		return (String) properties.get(key);
	}
	
	public static String get(String key, String defaultValue) {
		
		String rs = (String) properties.get(key);
		if (rs != null) {
			return rs;
		}
		return defaultValue;
	}
	
	public static void set(Object key, Object value) {
		if (key != null && value != null) {
			properties.put(key, value);	
		}
	}
	
	@SuppressWarnings("rawtypes")
	public static void load(ApplicationContext applicationContext) {
		((AbstractEnvironment) applicationContext.getEnvironment()).getPropertySources().forEach(ps -> {
			if (ps instanceof EnumerablePropertySource && ps instanceof OriginTrackedMapPropertySource) {
				Arrays.stream(((EnumerablePropertySource) ps).getPropertyNames()).forEach(key -> properties.put(key, applicationContext.getEnvironment().getProperty(key)));
			}
		});
	}
}
