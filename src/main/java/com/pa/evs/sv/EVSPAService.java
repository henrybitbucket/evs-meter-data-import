package com.pa.evs.sv;

import java.io.InputStream;

import org.springframework.web.multipart.MultipartFile;

public interface EVSPAService {

	void publish(String topic, Object message) throws Exception;

    boolean upload(String fileName, String version, String hashCode, InputStream in);
}
