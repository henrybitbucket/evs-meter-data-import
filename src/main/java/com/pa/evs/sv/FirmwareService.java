package com.pa.evs.sv;
import java.io.IOException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.pa.evs.dto.FirmwareDto;
import com.pa.evs.dto.PaginDto;

public interface FirmwareService {

    void upload(String version, String hashCode, MultipartFile file) throws IOException;

    void getUploadedFirmwares(PaginDto<FirmwareDto> dto);

    @Transactional
    void deleteFirmware(Long id);
    
    void editFirmware(Long id, String version, String hashCode, MultipartFile file) throws Exception;

}
