package com.pa.evs.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class ZipUtils {

	private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(ZipUtils.class);
	
	public static void unzip(String zipFilePath, String destDir) {
		File dir = new File(destDir);
		if (!dir.exists())
			dir.mkdirs();
		FileInputStream fis;
		byte[] buffer = new byte[1024];
		try {
			fis = new FileInputStream(zipFilePath);
			ZipInputStream zis = new ZipInputStream(fis);
			ZipEntry ze = zis.getNextEntry();
			while (ze != null && !ze.isDirectory()) {
				
				String fileName = ze.getName();
				File newFile = new File(destDir + File.separator + fileName);
				new File(newFile.getParent()).mkdirs();
				if (!newFile.exists()) {
					Files.createFile(newFile.toPath());
				}
				FileOutputStream fos = new FileOutputStream(newFile);
				int len;
				while ((len = zis.read(buffer)) > 0) {
					fos.write(buffer, 0, len);
				}
				fos.close();
				zis.closeEntry();
				ze = zis.getNextEntry();
			}
			zis.closeEntry();
			zis.close();
			fis.close();
		} catch (IOException e) {
			LOG.error(e.getMessage(), e);
		}

	}

}