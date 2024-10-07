package com.pa.evs.sv.impl;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.annotation.PostConstruct;
import javax.persistence.EntityManager;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.Session;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.multipart.MultipartFile;

import com.pa.evs.dto.VendorDto;
import com.pa.evs.model.Vendor;
import com.pa.evs.repository.CARequestLogRepository;
import com.pa.evs.repository.DMSVendorMCAccRepository;
import com.pa.evs.repository.FirmwareRepository;
import com.pa.evs.repository.VendorRepository;
import com.pa.evs.sv.VendorService;
import com.pa.evs.utils.ApiUtils;
import com.pa.evs.utils.AppProps;

@Service
@Transactional
public class VendorServiceImpl implements VendorService {

	private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(VendorServiceImpl.class);

	@Autowired
	VendorRepository vendorRepository;

	@Autowired
	CARequestLogRepository caRequestLogRepository;

	@Autowired
	FirmwareRepository firmwareRepository;

	@Autowired
	EntityManager em;
	
	@Autowired
	DMSVendorMCAccRepository dmsVendorMCAccRepository;

	@Value("${portal.pa.ca.request.url}")
	private String caRequestUrl;

	@Value("${evs.pa.data.folder}")
	private String evsDataFolder;

	@PostConstruct
	@Transactional
	public void init() {
		new Thread(() -> {
			try {
				Vendor vendor = vendorRepository.findByName("Default");
				if (vendor == null) {
					vendor = new Vendor();
					vendor.setName("Default");
					vendor = vendorRepository.save(vendor);
				}
				caRequestLogRepository.updateVendor(vendor.getId());
				firmwareRepository.updateVendor(vendor.getId());

				// build all file cer and key of vendor
				List<Vendor> verdors = vendorRepository.findAll();
				for (Vendor ven : verdors) {
					AppProps.getContext().getBean(this.getClass()).buildPathFileOfVendor(ven, null, null);
				}
			} catch (Exception e) {
				LOG.error(e.getMessage(), e);
			}
		}).start();
	}

	@Override
	public List<VendorDto> getVendors() {
		List<VendorDto> res = new ArrayList<>();
		List<Vendor> verdors = vendorRepository.findAll();
		for (Vendor ven : verdors) {
			VendorDto dto = new VendorDto();
			dto.setId(ven.getId());
			dto.setName(ven.getName());
			dto.setDescrption(ven.getDescription());
			dto.setKeyType(ven.getKeyType());
			dto.setSignatureAlgorithm(ven.getSignatureAlgorithm());
			
			res.add(dto);
		}
		return res;
	}

	@Transactional
	@Override
	public void updateVendorMasterKey(Long vendorId, String signatureAlgorithm, String keyType, MultipartFile csr,
			MultipartFile prkey) throws Exception {
		LOG.info(" start update vendor master key ");
		if (vendorId == null) {
			throw new Exception("vendorId cannot be null!");
		}
		Optional<Vendor> vendorOtp = vendorRepository.findById(vendorId);
		if (vendorOtp.isPresent()) {
			vendorOtp.get().setSignatureAlgorithm(signatureAlgorithm);
			vendorOtp.get().setKeyType(keyType);
			vendorOtp.get().setKeyContent(
					em.unwrap(Session.class).getLobHelper().createBlob(prkey.getInputStream(), prkey.getSize()));
			vendorOtp.get().setKeyPath(buildKeyPath(vendorId, signatureAlgorithm));
			vendorOtp.get().setCsrPath(buildCsrPath(vendorId, signatureAlgorithm));
			vendorOtp.get().setCsrBlob(
					em.unwrap(Session.class).getLobHelper().createBlob(csr.getInputStream(), csr.getSize()));
			vendorRepository.save(vendorOtp.get());
			vendorRepository.flush();
			//build new path file key of vendor
		    buildPathFileOfVendor(vendorRepository.findById(vendorId).get(), prkey.getInputStream(), csr.getInputStream());
		} else {
			throw new Exception("Vendor not found !");
		}
	}
	
	@Transactional
	public void buildPathFileOfVendor(Vendor ven, InputStream keyContent, InputStream csrContent) {
		if (StringUtils.isNotBlank(ven.getKeyPath()) && ven.getKeyContent() != null) {
			File temp = new File(ven.getKeyPath() + ".tmp");
			try (OutputStream out = new FileOutputStream(temp)) {
				IOUtils.copy(keyContent == null ? ven.getKeyContent().getBinaryStream() : keyContent, out);
				if (temp.length() > 0l) {
					Files.copy(temp.toPath(), new File(ven.getKeyPath()).toPath(), StandardCopyOption.REPLACE_EXISTING);
				}
			} catch (Exception e) {
				LOG.error("Error when build path key : ", e.getMessage(), e);
			} finally {
				try {
					Files.deleteIfExists(temp.toPath());
				} catch (Exception e2) {
					//
				}
			}
		}
		if (StringUtils.isNotBlank(ven.getCsrPath()) && ven.getCsrBlob() != null) {
			File temp = new File(ven.getCsrPath() + ".tmp");
			try (OutputStream out = new FileOutputStream(temp)) {
				IOUtils.copy(csrContent == null ? ven.getCsrBlob().getBinaryStream() : csrContent, out);
				if (temp.length() > 0l) {
					Files.copy(temp.toPath(), new File(ven.getCsrPath()).toPath(), StandardCopyOption.REPLACE_EXISTING);
				}
			} catch (Exception e) {
				LOG.error("Error when build path csr : ", e.getMessage(), e);
			} finally {
				try {
					Files.deleteIfExists(temp.toPath());
				} catch (Exception e2) {
					//
				}
			}
		}
		if (StringUtils.isBlank(ven.getLabel())) {
			ven.setLabel(ven.getName().replace("inkfields", "inksfield"));
			vendorRepository.save(ven);
		}
	}

	private String buildKeyPath(Long vendorId, String signatureAlgorithm) {
		return evsDataFolder.replaceAll("\\\\", "/") + "/" + "master_key_vendor_" + vendorId + "_"
				+ signatureAlgorithm.replaceAll("[^a-zA-Z0-9]", "") + "_" + System.currentTimeMillis() + ".key";
	}

	private String buildCsrPath(Long vendorId, String signatureAlgorithm) {
		return evsDataFolder.replaceAll("\\\\", "/") + "/" + "csr_vendor_" + vendorId + "_"
				+ signatureAlgorithm.replaceAll("[^a-zA-Z0-9]", "") + "_" + System.currentTimeMillis() + ".csr";
	}

	@Override
	public void refreshVendorCertificate(Long vendorId) throws Exception {
		LOG.info(" start refresh vendor certificate ");
		if (vendorId == null) {
			throw new Exception("vendorId cannot be null!");
		}
		Optional<Vendor> opt = vendorRepository.findById(vendorId);
		if (opt.isPresent()) {
			Map<String, Object> data = requestCA(caRequestUrl, new FileSystemResource(opt.get().getCsrPath()), null);
			opt.get().setCertificate((String) data.get("pemBase64"));
			vendorRepository.save(opt.get());
		}
	}

	@SuppressWarnings("unchecked")
	private static Map<String, Object> requestCA(String caRequestUrl, Resource resource, String uuid) {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.MULTIPART_FORM_DATA);
		MultiValueMap<String, Object> data = new LinkedMultiValueMap<>();
		if (StringUtils.isBlank(uuid)) {
			uuid = resource.getFilename();
		}
		data.add("msn", uuid);
		data.add("files", resource);
		HttpEntity<Object> entity = new HttpEntity<>(data, headers);
		Map<String, Object> response = ApiUtils.getRestTemplate()
				.exchange(caRequestUrl, HttpMethod.POST, entity, Map.class).getBody();
		String pem = (response.get("cas") + "").replaceAll(
				".*\"Certificate\": \"(-----BEGIN CERTIFICATE-----[\na-zA-Z0-9=\\\\/+]+-----END CERTIFICATE-----).*",
				"$1").replace("\\n", "\n");

		if (!pem.contains("-----BEGIN CERTIFICATE-----")) {
			throw new RuntimeException("CA request ERROR " + uuid + "\n" + response.get("cas"));
		}
		response.put("pem", pem);
		response.put("pemBase64", Base64.getEncoder().encodeToString(pem.getBytes(StandardCharsets.UTF_8)));
		response.put("uid", uuid);
		return response;
	}
	
	@Override
	public Vendor saveVendor(VendorDto dto) {
		if (dto.getId() != null) {
			Optional<Vendor> vendorOpt = vendorRepository.findById(dto.getId());
			if (!vendorOpt.isPresent()) {
				throw new RuntimeException("Vendor not found!");
			}
			Vendor vendor = vendorOpt.get();
			vendor.setName(dto.getName());
			vendor.setDescription(dto.getDescrption());
			vendorRepository.save(vendor);
			return vendor;
		} else {
			Vendor vendorOpt = vendorRepository.findByName(dto.getName());
			if (vendorOpt != null) {
				throw new RuntimeException("Vendor name already exists!");
			}
			Vendor newVendor = new Vendor();
			newVendor.setName(dto.getName());
			newVendor.setDescription(dto.getDescrption());
			vendorRepository.save(newVendor);
			return newVendor;
		}
	}
}
