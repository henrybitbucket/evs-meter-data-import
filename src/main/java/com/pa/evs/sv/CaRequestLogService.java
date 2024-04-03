package com.pa.evs.sv;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.springframework.web.multipart.MultipartFile;

import com.pa.evs.dto.CaRequestLogDto;
import com.pa.evs.dto.DeviceRemoveLogDto;
import com.pa.evs.dto.DeviceSettingDto;
import com.pa.evs.dto.PaginDto;
import com.pa.evs.dto.RelayStatusLogDto;
import com.pa.evs.dto.ScreenMonitoringDto;
import com.pa.evs.model.CARequestLog;
import com.pa.evs.model.ScreenMonitoring;

public interface CaRequestLogService {
    
	Optional<CARequestLog> findByUid(String uid);

	void save(CaRequestLogDto dto) throws Exception;

	void linkMsn(Map<String, Object> map);
	
    PaginDto<CARequestLog> search(PaginDto<CARequestLog> pagin);

    File downloadCsv(List<CARequestLog> listInput, Long activateDate) throws IOException;

    List<String> getCids(boolean refresh);

    void setActivationDate(Long activationDate, Set<Long> ids);

    void checkDevicesOffline();

    Number countAlarms();

    Map<String, Integer> getCountDevices();

    void checkDatabase();

    List<ScreenMonitoring> getDashboard();

    void checkServerCertificate();

	void markViewAll();

    PaginDto<CARequestLog> getDevicesInGroup(List<Long> listGroupId);
    
    void searchCaRequestLog (PaginDto<CaRequestLogDto> pagin);

	void removePiLlog();

	void removeDevice(String eId, String reason);

	void unLinkMsn(String eId);

	Optional<CARequestLog> findByMsn(String msn);

	void updateCacheUidMsnDevice(String currentUid, String action);

	ScreenMonitoringDto mqttStatusCheck();

	PaginDto<DeviceRemoveLogDto> getDeviceRemoveLogs(PaginDto<DeviceRemoveLogDto> pagin);
	
	void sendRLSCommandForDevices(List<CARequestLog> listDevice, String command, Map<String, Object> options, String commandSendBy, String uuid);

	void getRelayStatusLogs(PaginDto<RelayStatusLogDto> pagin);
	
	List<Map<String, String>> batchCoupleDevices(List<Map<String, String>> listInput);

	void updateVendor(String msn, Long vendorId) throws Exception;

	PaginDto<CARequestLog> searchMMSMeter(PaginDto<CARequestLog> pagin);

	String updateMMSMeter(CARequestLog ca, String msn);

	List<DeviceSettingDto> uploadDeviceSettings(MultipartFile file, Boolean isProcess) throws IOException;

	void updateDevicesNode(List<Long> deviceIds, String ieiNode, Boolean isDistributed, Integer sendMDTToPi,
			PaginDto<CARequestLog> filter);
}
