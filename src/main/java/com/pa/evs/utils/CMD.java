package com.pa.evs.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.OutputStream;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * 
 * @author thanh
 *
 */
public final class CMD {
	
	static final Logger LOG = LogManager.getLogger(CMD.class);
	
	private CMD() {
	}
	
	public static boolean isWindow() {
		
		return System.getProperty("os.name").toLowerCase().contains("window");
	}
	
	public static void getCurrentJavaProcessInfo() {
		
		String pId = java.lang.management.ManagementFactory.getRuntimeMXBean().getName().split("@")[0];
		String javaHome = System.getenv("JAVA_HOME");
		exec("\"" + javaHome + "/bin/jcmd\" " + pId + " GC.heap_info", (String)null);
	}
	
	public static void gc(String pId) {
		
		if (StringUtils.isBlank(pId)) {
			pId = java.lang.management.ManagementFactory.getRuntimeMXBean().getName().split("@")[0];			
		}
		
		LOG.info("start run GC " + pId);
		exec("jcmd " + pId + " GC.run", null);
		LOG.info("end run GC " + pId);
	}
	
	public static void gc() {
		
		try {
			gc(null);			
		} catch (Exception e) {
			LOG.error(e.getMessage(), e);
		}
	}
	
	public static void exec(String command, String pwd) {
		
		exec0(command, pwd);
	}
	
	private static void exec0(String command, String pwd) {
		
		try {
			
			LOG.info("------------------- " + command + " ---------------");

			if ("pid".equalsIgnoreCase(command)) {
				
				String pId = java.lang.management.ManagementFactory.getRuntimeMXBean().getName().split("@")[0];
				command = "echo " + pId;
			}

			if (command.length() == 0)
	            throw new IllegalArgumentException("Empty command");

			if (command.startsWith("top") && command.indexOf("-n 1 -b") < 0) {
				command = "top -n 1 -b";
			}
			
			String[] cmds = new String[] {null, null, command};
			if (isWindow()) {
				cmds[0] = "cmd.exe";
				cmds[1] = "/c";
			} else {
				cmds[0] = "/bin/sh";
				cmds[1] = "-c";
			}

			ProcessBuilder builder = new ProcessBuilder(cmds).directory(new File(StringUtils.isBlank(pwd) ? "/" : pwd));
			builder.redirectErrorStream(true).start();
			
	        Process p = builder.redirectErrorStream(true).start();
			
			BufferedReader stdInput = new BufferedReader(new InputStreamReader(p.getInputStream()));
			
            String s = null;
            while ((s = stdInput.readLine()) != null) {
            	LOG.info(s);
            }
            
		} catch (Exception e) {
			LOG.error(e.getMessage(), e);
		}
		
	}
	
	public static void exec(String command, String pwd, OutputStream out) {
		
		exec0(command, pwd, out);
	}
	
	private  static void exec0(String command, String pwd, OutputStream out) {
		
		try {
			
			LOG.info("------------------- " + command + " ---------------");
			
			if ("pid".equalsIgnoreCase(command)) {
				
				String pId = java.lang.management.ManagementFactory.getRuntimeMXBean().getName().split("@")[0];
				command = "echo " + pId;
			}

			if (command.length() == 0)
	            throw new IllegalArgumentException("Empty command");

			if (command.startsWith("top")) {
				command = "top -n 1 -b";
			}
			
			String[] cmds = new String[] {null, null, command};
			if (isWindow()) {
				cmds[0] = "cmd.exe";
				cmds[1] = "/c";
			} else {
				cmds[0] = "/bin/sh";
				cmds[1] = "-c";
			}
			
	        ProcessBuilder builder = new ProcessBuilder(cmds).directory(new File(StringUtils.isBlank(pwd) ? "/" : pwd));
			
	        builder.redirectErrorStream(true).start();
	        
	        Process p = builder.redirectErrorStream(true).start();
			
			BufferedReader stdInput = new BufferedReader(new InputStreamReader(p.getInputStream()));
            
            String s = null;
            while ((s = stdInput.readLine()) != null) {
            	out.write((s + "\n").getBytes());
            }
            
		} catch (Exception e) {
			LOG.error(e.getMessage(), e);
		}
	}
}
