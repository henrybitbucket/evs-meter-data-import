package com.pa.evs.utils;

import java.io.FileOutputStream;
import java.io.IOException;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;

public class JFtpClient {

	private static final Logger LOG = LogManager.getLogger(JFtpClient.class);
	private static JFtpClient instance = new JFtpClient();

	private String server;
	int port;
	private String user;
	private String password;
	private JSch ftp;
	Session jschSession;
	ChannelSftp sftp;

	private JFtpClient() {
	}

	public static JFtpClient getInstance(String server, int port, String user, String password) {
		instance.server = server;
		instance.port = port;
		instance.user = user;
		instance.password = password;
		return instance;
	}

	public static JFtpClient getInstance() {

		if (StringUtils.isBlank(instance.server)) {
			throw new RuntimeException("Use getInstance(String server, int port, String user, String password) first");
		}
		return instance;
	}

	void open() {
		try {
			ftp = new JSch();
			jschSession = ftp.getSession(user, server, port);
			java.util.Properties config = new java.util.Properties();
			config.put("StrictHostKeyChecking", "no");
			jschSession.setConfig(config);
			jschSession.setPassword(password);
			jschSession.connect();

			sftp = (ChannelSftp) jschSession.openChannel("sftp");
			sftp.connect();
			LOG.info("Connected to sftp://{} !", server);			
		} catch (Exception e) {
			LOG.error(e.getMessage(), e);
		}
	}

	void close() throws IOException {
		jschSession.disconnect();
	}

	public void downloadFile(String source, String destination) {

		int count = 0;
		while ((sftp == null || !sftp.isConnected()) && count < 5) {
			open();
			try {
				Thread.sleep(1000l);
			} catch (InterruptedException e) {
				//
			}
		}
		try {
			sftp.get(source, new FileOutputStream(destination));
			LOG.info("Download {} to {} successfully!", source, destination);			
		} catch (Exception e) {
			LOG.error(e.getMessage(), e);
		}
	}

	public void putFileToPath(String src, String desc) throws Exception {
		src = new java.io.File(src).getAbsolutePath();
		LOG.info("Begin upload {} to sftp://{}:{}", src, server, desc);
		int count = 0;
		while ((sftp == null || !sftp.isConnected()) && count < 5) {
			open();
			try {
				Thread.sleep(1000l);
			} catch (InterruptedException e) {
				//
			}
			count++;
		}
		
		if (sftp == null || !sftp.isConnected()) {
			LOG.error("Fail to connect {}", server);
		}
		if (!desc.endsWith("/")) {
			desc = desc + "/";
		}
		try {
			sftp.put(src, desc);
			LOG.info("Upload {} to sftp://{}:{} is successfully!", src, server, desc);
		} catch (Exception e) {
			LOG.error(e.getMessage(), e);
		}
	}

	/**
	 * public static void main(String[] args) throws Exception {
	 * 
	 * getInstance("gethcp.com", 22, "root", "Thn123@456_Kru");
	 * getInstance().putFileToPath("/var/raspi/temp/chanel.txt", "/root/"); }
	 */
}
