package com.pa.evs.utils;

import java.util.Arrays;
import java.util.Properties;

import org.springframework.boot.env.OriginTrackedMapPropertySource;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.AbstractEnvironment;
import org.springframework.core.env.EnumerablePropertySource;

public class AppProps {

	static Properties properties = new Properties();
	
	public static ApplicationContext context = null;
	
	public static String get(String key) {
		
		String rs = (String) properties.get(key);
		if (rs == null) {
			rs = (String) properties.get(key.toUpperCase()); 
		}
		if (rs == null) {
			rs = (String) properties.get(key.toLowerCase()); 
		}
		return rs;
	}
	
	public static String get(String key, String defaultValue) {
		
		String rs = (String) properties.get(key);
		if (rs != null) {
			return rs;
		}
		rs = (String) properties.get(key.toUpperCase());
		if (rs != null) {
			return rs;
		}
		rs = (String) properties.get(key.toLowerCase());
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
	
	public static ApplicationContext getContext() {
		return context;
	}
	
	@SuppressWarnings("rawtypes")
	public static void load(ApplicationContext applicationContext) {
		context = applicationContext;
		((AbstractEnvironment) applicationContext.getEnvironment()).getPropertySources().forEach(ps -> {
			if (ps instanceof EnumerablePropertySource && ps instanceof OriginTrackedMapPropertySource) {
				Arrays.stream(((EnumerablePropertySource) ps).getPropertyNames()).forEach(key -> properties.put(key, applicationContext.getEnvironment().getProperty(key)));
			}
		});
	}
}
