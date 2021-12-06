package com.pa.evs.utils;

import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;

public class JasperUtil {
	public static final String TEMPORARY_SUBDIR = "tmp";

	public static File getTempFile(String directory) throws IOException {
		File dir = StringUtils.isNotEmpty(directory) ? new File(directory, TEMPORARY_SUBDIR) : null;
		if (dir != null && !dir.exists() && !dir.mkdirs()) {
			dir = dir.getParentFile();
		}
		File file = File.createTempFile("jasper", null, dir);
		// file.deleteOnExit();
		return file;
	}

}
