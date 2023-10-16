package com.pa.evs.sv;

import java.util.List;

import org.springframework.stereotype.Repository;
import org.springframework.web.multipart.MultipartFile;

import com.pa.evs.dto.VendorDto;

@Repository
public interface VendorService {

	List<VendorDto> getVendors();

	void updateVendorMasterKey(Long vendorId, String signatureAlgorithm, String keyType,
			MultipartFile csr, MultipartFile prkey) throws Exception;
	
	void refreshVendorCertificate(Long vendorId) throws Exception;

}
