package com.pa.evs.utils;
import java.util.Properties;

import org.apache.commons.lang3.StringUtils;

public final class Version {

	private static final Properties VERSION = new Properties();
	private static final long INIT = System.currentTimeMillis();
	public static String TEXT = "";
	
	public static String getVersion() {
		return (String) VERSION.getOrDefault("version", "0.0.1-SNAPSHOT");
	}
	
	public static String getBuildtime() {
		return (String) VERSION.getOrDefault("buildtime", "");
	}
	
	private static String text() {
		return new StringBuilder()
				.append(INIT)
				.append("-")
				.append(VERSION.get("buildtime"))
				.append("-")
				.append(VERSION.get("artifactId"))
				.append("(")
				.append(VERSION.get("branch"))
				.append("-")
				.append(VERSION.get("lcm"))
				.append("/")
				.append((VERSION.get("by") + "").replaceAll("\t", "/t"))
				.append(")")
				.toString()
				.replaceAll("\t", " ")
				.replaceAll(" +", " ");
	}
	
	public static String getCommitNumber() {
		String branch = (String) VERSION.getOrDefault("branch", "");
		String hash = (String) VERSION.getOrDefault("hash", "");
		if (StringUtils.isNotBlank(branch) && StringUtils.isNotBlank(hash)) {
			return branch + "(" + hash + ")";	
		}
		return "";
	}
	
	static {
		try {
			VERSION.load(Thread.currentThread().getContextClassLoader().getResourceAsStream("version.properties"));
			TEXT = text();
		} catch (Exception e) {}
	}
}
