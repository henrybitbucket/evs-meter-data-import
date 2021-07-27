package com.pa.evs.sv;

import java.io.InputStream;

import org.springframework.web.multipart.MultipartFile;

public interface EVSPAService {

	void publish(String topic, Object message, String type) throws Exception;

	Long nextvalMID();

    boolean upload(String fileName, String version, String hashCode, InputStream in);

	void uploadDeviceCsr(MultipartFile file);

	String getS3URL(String objectKey);
}
