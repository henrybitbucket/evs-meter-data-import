/**
 * 
 */
package com.pa.evs.utils;

/**
 * @author BINH
 *
 */
public final class StringUtil {
	public static boolean isNotEmpty(String s) {
		return (s != null && s.length() > 0);
	}

	public static boolean isEmpty(String str) {
		return str == null || str.trim().length() == 0;
	}

	public static String toStringOrNull(Object obj) {
		return (null == obj) ? null : obj.toString();
	}

	public static boolean isNotEmpty(Object obj) {
		return (obj instanceof String) ? (obj.toString().length() > 0) : (obj != null);
	}

	public static String toStringOrEmpty(Object obj) {
		return (null == obj) ? "" : obj.toString();
	}

	public static boolean isNotWhitespaced(String s) {
		return (s != null && s.trim().length() > 0);
	}
}
