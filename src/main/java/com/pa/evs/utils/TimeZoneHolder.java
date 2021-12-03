package com.pa.evs.utils;

import java.util.TimeZone;

public final class TimeZoneHolder {

	public static final ThreadLocal<TimeZone> TZ = new ThreadLocal<>();
	private TimeZoneHolder() {};
	
	public static TimeZone set(String tz) {
		try {
			TZ.set(TimeZone.getTimeZone(tz));
		} catch (Exception e) {/**/}
		
		return get();
	}
	
	public static TimeZone get() {
		if (TZ.get() == null) {
			return TimeZone.getDefault();
		}
		return TZ.get();
	}
	
	public static void remove() {
		TZ.remove();
	}
}
