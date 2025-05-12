package com.pa.evs.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.net.URL;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class CMD {
	
	static final Logger LOG = LogManager.getLogger(CMD.class);
	
	public static String ip;
	public static String mac;
	public static String publicIp;
	public static String uuid;
	
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
	
	public static String publicIp() {
		String ip = null;
		try {
			URL whatismyip = new URL("http://checkip.amazonaws.com");
			InputStream inS = whatismyip.openStream();
			BufferedReader in = new BufferedReader(new InputStreamReader(inS));
			ip = in.readLine();
			inS.close();
		} catch (Exception e) {
			//
		}
		CMD.publicIp = ip;
		return ip;

	}
	
	public static String lanIp() {
		String ip = null;
		try {
			Socket socket = new Socket("122.248.198.76", 80);
			ip = socket.getLocalAddress().getHostAddress();
			socket.close();
		} catch (Exception e) {
			//
		}
		CMD.ip = ip;
		return ip;
		
	}
	
	public static String mac() {
		String mac = null;
		try {
			Socket socket = new Socket("www.google.com", 80);
			String ip = socket.getLocalAddress().getHostAddress();
			socket.close();
			Enumeration<NetworkInterface> nets = NetworkInterface.getNetworkInterfaces();
	        for (NetworkInterface netint : Collections.list(nets)) {
	        	Enumeration<InetAddress> inetAddresses = netint.getInetAddresses();
	            for (InetAddress inetAddress : Collections.list(inetAddresses)) {
	            	if (inetAddress.getHostAddress().equalsIgnoreCase(ip)) {
	            		byte[] hardwareAddress = netint.getHardwareAddress();
	                	if (hardwareAddress != null) {
	                		String[] hexadecimal = new String[hardwareAddress.length];
	            	    	for (int i = 0; i < hardwareAddress.length; i++) {
	            	    	    hexadecimal[i] = String.format("%02X", hardwareAddress[i]);
	            	    	}
	            	    	mac = String.join("-", hexadecimal);
	                	}
	            	}
	            }
	        }
	        mac = mac.replace("-", "");
		} catch (Exception e) {
			//
		}
		CMD.mac = mac;
		return mac;
		
	}
	
	static {
		try {
			publicIp();
			lanIp();
			mac();
			uuid = ip + '-' + publicIp + '-' + mac;
			new Timer().schedule(new TimerTask() {
				@Override
				public void run() {
					publicIp();
					lanIp();
					mac();
					uuid = ip + '-' + publicIp + '-' + mac;
				}
			}, 5 * 60L * 1000L);
		} catch (Exception e) {/**/}
	}
}
