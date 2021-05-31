package com.pa.evs.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.security.cert.X509Certificate;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.net.PrintCommandListener;
import org.apache.commons.net.ftp.FTPReply;
import org.apache.commons.net.ftp.FTPSClient;

public class FtpClient {

	private static FtpClient instance = new FtpClient();

	private String server;
	private int port;
	private String user;
	private String password;
	private FTPSClient ftp;

	private FtpClient() {
	}

	public static FtpClient getInstance(String server, int port, String user, String password) {
		instance.server = server;
		instance.port = port;
		instance.user = user;
		instance.password = password;
		return instance;
	}

	public static FtpClient getInstance() {

		if (StringUtils.isBlank(instance.server)) {
			throw new RuntimeException("Use getInstance(String server, int port, String user, String password) first");
		}
		return instance;
	}

	// constructor

	void open() throws Exception {

		System.setProperty("https.protocols", "TLSv1,TLSv1.1,TLSv1.2");

		TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
			public java.security.cert.X509Certificate[] getAcceptedIssuers() {
				return null;
			}

			public void checkClientTrusted(X509Certificate[] certs, String authType) {
			}

			public void checkServerTrusted(X509Certificate[] certs, String authType) {
			}
		} };

		SSLContext sslContext = SSLContext.getInstance("TLSv1.2");
		sslContext.init(null, trustAllCerts, new java.security.SecureRandom());

		ftp = new FTPSClient(true, sslContext);
		ftp.setStrictReplyParsing(false);
		ftp.addProtocolCommandListener(new PrintCommandListener(new PrintWriter(System.out)));

		ftp.connect(server, port);
		int reply = ftp.getReplyCode();
		if (!FTPReply.isPositiveCompletion(reply)) {
			ftp.disconnect();
			throw new IOException("Exception in connecting to FTP Server");
		}

		ftp.login(user, password);
	}

	void close() throws IOException {
		ftp.disconnect();
	}

	public void downloadFile(String source, String destination) throws Exception {

		while (ftp == null || !ftp.isConnected()) {
			open();
			try {
				Thread.sleep(1000l);
			} catch (InterruptedException e) {
				//
			}
		}
		ftp.retrieveFile(source, new FileOutputStream(destination));
	}

	public void putFileToPath(File file, String path) throws Exception {
		while (ftp == null || !ftp.isConnected()) {
			open();
			try {
				Thread.sleep(1000l);
			} catch (InterruptedException e) {
				//
			}
		}
		ftp.storeFile(path, new FileInputStream(file));
	}

	/**
	 * public static void main(String[] args) throws Exception {
	 * 
	 * getInstance("gethcp.com", 22, "root", "Thn123@456_kru");
	 * getInstance().putFileToPath(new File("D:/var/raspi/temp/chanel.txt"),
	 * "/root/"); }
	 */
}
