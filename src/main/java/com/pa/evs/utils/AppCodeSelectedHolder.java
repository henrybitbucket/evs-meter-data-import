package com.pa.evs.utils;

public final class AppCodeSelectedHolder {

	public static final ThreadLocal<String> A_C = new ThreadLocal<>();
	private AppCodeSelectedHolder() {};
	
	public static String set(String ac) {
		try {
			A_C.set(ac);
		} catch (Exception e) {/**/}
		
		return get();
	}
	
	public static String get() {
		return A_C.get();
	}
	
	public static void remove() {
		A_C.remove();
	}
}
