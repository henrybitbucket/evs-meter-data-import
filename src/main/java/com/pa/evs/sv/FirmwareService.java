package com.pa.evs.sv;
import java.util.Map;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.pa.evs.dto.FirmwareDto;
import com.pa.evs.dto.PaginDto;
import com.pa.evs.model.Firmware;

public interface FirmwareService {

    void upload(String version, String hashCode, Long vendor, MultipartFile file) throws Exception;

    void getUploadedFirmwares(PaginDto<FirmwareDto> dto);

    @Transactional
    void deleteFirmware(Long id);
    
    void editFirmware(Long id, String version, String hashCode, MultipartFile file) throws Exception;

    Map<Long, Firmware> getLatestFirmware();
}
