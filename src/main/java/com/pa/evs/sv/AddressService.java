package com.pa.evs.sv;

import java.io.IOException;
import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import com.pa.evs.dto.AddressDto;

public interface AddressService {

	List<AddressDto> handleUpload(MultipartFile file, String importType) throws IOException;

}
