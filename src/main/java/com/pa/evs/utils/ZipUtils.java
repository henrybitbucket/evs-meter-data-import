package com.pa.evs.utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.tomcat.util.http.fileupload.IOUtils;

public class ZipUtils {

	private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(ZipUtils.class);

	public static void unzip(String zipFilePath, String destDir) {
		File dir = new File(destDir);
		if (!dir.exists())
			dir.mkdirs();
		try {
			try (java.util.zip.ZipFile zipFile = new ZipFile(zipFilePath)) {
				Enumeration<? extends ZipEntry> entries = zipFile.entries();
				while (entries.hasMoreElements()) {
					try {
						ZipEntry entry = entries.nextElement();
						File entryDestination = new File(destDir + File.separator, entry.getName());
						if (entry.isDirectory()) {
							entryDestination.mkdirs();
						} else {
							entryDestination.getParentFile().mkdirs();
							try (InputStream in = zipFile.getInputStream(entry);
									OutputStream out = new FileOutputStream(entryDestination)) {
								IOUtils.copy(in, out);
							}
						}
					} catch (Exception e) {/**/}
					
				}
			}

		} catch (IOException e) {
			LOG.error(e.getMessage(), e);
		}
	}
}