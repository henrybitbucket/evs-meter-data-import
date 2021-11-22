package com.pa.evs.sv;

import java.io.InputStream;

import org.springframework.web.multipart.MultipartFile;

import com.pa.evs.dto.PaginDto;

public interface EVSPAService {

	void publish(String topic, Object message, String type) throws Exception;

	Long nextvalMID();

    boolean upload(String fileName, String version, String hashCode, InputStream in);

	void uploadDeviceCsr(MultipartFile file);

	String getS3URL(String objectKey);

	void ping(String uuid, String hide);

	void searchPi(PaginDto<?> pagin);

	void ftpRes(String msn, Long mid, String topic);
}
