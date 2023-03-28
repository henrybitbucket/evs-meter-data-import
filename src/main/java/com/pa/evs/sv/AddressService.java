package com.pa.evs.sv;

import java.io.IOException;

import org.springframework.web.multipart.MultipartFile;

public interface AddressService {

	void handleUpload(MultipartFile file) throws IOException;

}
