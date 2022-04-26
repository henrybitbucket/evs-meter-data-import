package com.pa.evs.sv;

import java.io.File;
import java.io.InputStream;
import java.util.List;

import com.pa.evs.model.CARequestLog;
import org.springframework.web.multipart.MultipartFile;

import com.pa.evs.dto.LogBatchDto;
import com.pa.evs.dto.PaginDto;
import com.pa.evs.dto.PiLogDto;
import com.pa.evs.model.Log;
import com.pa.evs.model.Users;

public interface EVSPAService {

	Log publish(String topic, Object message, String type) throws Exception;

	Long nextvalMID();

    boolean upload(String fileName, String version, String hashCode, InputStream in);

	void uploadDeviceCsr(MultipartFile file);

	String getS3URL(String objectKey);

	void ping(String uuid, String hide);

	void searchPi(PaginDto<?> pagin);

	List<PiLogDto> searchPiLog(Long piId, String msn, Long mid);

	void ftpRes(String msn, Long mid, String piUuid, String status, String fileName);

	Log publish(String topic, Object message, String type, String batchId) throws Exception;

	void searchBatchLog(PaginDto<?> pagin);
	
	void searchBatchLogsByUser (PaginDto<LogBatchDto> pagin);

	List<CARequestLog> findDevicesInGroup(List<Long> listGroupId);

	void createTaskLog(String uuid, Long groupTaskId, Users user);
	
	String getListFileName(String uuid);
	
	File getMeterFile(String fileName);  

}
