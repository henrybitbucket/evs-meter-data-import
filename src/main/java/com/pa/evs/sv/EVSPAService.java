package com.pa.evs.sv;

import java.io.InputStream;
import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import com.pa.evs.dto.PaginDto;
import com.pa.evs.dto.PiLogDto;
import com.pa.evs.model.PiLog;

public interface EVSPAService {

	void publish(String topic, Object message, String type) throws Exception;

	Long nextvalMID();

    boolean upload(String fileName, String version, String hashCode, InputStream in);

	void uploadDeviceCsr(MultipartFile file);

	String getS3URL(String objectKey);

	void ping(String uuid, String hide);

	void searchPi(PaginDto<?> pagin);

	List<PiLogDto> searchPiLog(Long piId, String msn, Long mid);

	void ftpRes(String msn, Long mid, String piUuid, String status);
}
