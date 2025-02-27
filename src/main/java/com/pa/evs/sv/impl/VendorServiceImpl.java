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

import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityManager;

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

import com.fasterxml.jackson.databind.ObjectMapper;
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
				".*\"Certificate\": *\"(-----BEGIN CERTIFICATE-----[\na-zA-Z0-9=\\\\/+]+-----END CERTIFICATE-----).*",
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
	
	public static void main(String[] args) throws Exception  {
		
//		String b64 = "LS0tLS1CRUdJTiBDRVJUSUZJQ0FURS0tLS0tCk1JSUNWVENDQWZ5Z0F3SUJBZ0lRR1MvZUp0TTVZNjd6M0VFY1NIVVpKVEFLQmdncWhrak9QUVFEQWpCU01Rc3cKQ1FZRFZRUUdFd0pUUnpFTE1Ba0dBMVVFQ2d3Q1VFRXhEakFNQmdOVkJBc01CVUpWTlRBd01SSXdFQVlEVlFRRApEQWxRUVVOQklFWkdSa1l4RWpBUUJnTlZCQWNNQ1VGc1pYaGhibVJ5WVRBZUZ3MHlNekV3TVRjeE1EVXhNalphCkZ3MHlOREE1TURFeE1UVXhNalphTUcweEN6QUpCZ05WQkFZVEFsTkhNUXN3Q1FZRFZRUUlEQUpPUVRFU01CQUcKQTFVRUJ3d0pVMmx1WjJGd2IzSmxNUXN3Q1FZRFZRUUtEQUpRUVRFT01Bd0dBMVVFQ3d3RlFsVTFNREF4SURBZQpCZ05WQkFNTUYzTmxjblpsY2k1b1pHSnpiV0Z5ZEdodmJXVXVZMjl0TUhZd0VBWUhLb1pJemowQ0FRWUZLNEVFCkFDSURZZ0FFRXhtQ3Btc2paRlhmRDJLY1hORnJtQktJNGpUS2dYSkpiOXpTaEFmQWhwZFp5OVRxS0ZaRkdYeUsKcGxmQVNSZTd6L3VDTlpTZitKZjIwNzJ0YUs0RWk3ekVJSTd1VHRwR0gyRnlsbUNHUXAwbndFclVUSlNQY0dPbgpWaG12Mm5YTG8zd3dlakFKQmdOVkhSTUVBakFBTUI4R0ExVWRJd1FZTUJhQUZOY1A4SWJLVGh1TTd6RlNrVkphCk1ESnNEcXJGTUIwR0ExVWREZ1FXQkJTY3Npc1hEV2lMUmlRVnd4Q2g3STlPckxseER6QU9CZ05WSFE4QkFmOEUKQkFNQ0JhQXdIUVlEVlIwbEJCWXdGQVlJS3dZQkJRVUhBd0VHQ0NzR0FRVUZCd01DTUFvR0NDcUdTTTQ5QkFNQwpBMGNBTUVRQ0lEYjhsTXlGTFpMN3Q5cCtWbzE1SU5oZ2xFYkJ4MWVrUmhsdDNPb2VMVmw5QWlCRzZES0xvT2hPClZNK0x5Q2hqNEJOK0JENWZicTBpbnpCLzB6cWJSWE9GN1E9PQotLS0tLUVORCBDRVJUSUZJQ0FURS0tLS0tCg==";
//		System.out.println(new String(Base64.getDecoder().decode(b64.getBytes())));
		
		String json = "{\"cas\":\"{\\\"Certificate\\\":\\\"-----BEGIN CERTIFICATE-----\\\\nMIIFJDCCAwygAwIBAgIJVh5d8MCy/9bhMA0GCSqGSIb3DQEBCwUAMBIxEDAOBgNV\\\\nBAMMB1Jvb3QgQ0EwHhcNMjUwMjI3MDkzODAyWhcNMzAwMjI3MDkzODAyWjA5MQsw\\\\nCQYDVQQGEwJERTEQMA4GA1UECgwHVmVyaWRvczEYMBYGA1UEAwwPU1NMIFNlcnZl\\\\nciBEZW1vMHYwEAYHKoZIzj0CAQYFK4EEACIDYgAEExmCpmsjZFXfD2KcXNFrmBKI\\\\n4jTKgXJJb9zShAfAhpdZy9TqKFZFGXyKplfASRe7z/uCNZSf+Jf2072taK4Ei7zE\\\\nII7uTtpGH2FylmCGQp0nwErUTJSPcGOnVhmv2nXLo4ICAjCCAf4wCQYDVR0TBAIw\\\\nADAOBgNVHQ8BAf8EBAMCBaAwHQYDVR0lBBYwFAYIKwYBBQUHAwIGCCsGAQUFBwMB\\\\nMB8GA1UdIwQYMBaAFCSTfK2EJnolgSe3/vNPEBv+N/CpMB0GA1UdDgQWBBScsisX\\\\nDWiLRiQVwxCh7I9OrLlxDzAwBgNVHR8EKTAnMCWgI6Ahhh9odHRwOi8vbG9jYWxo\\\\nb3N0L2NybC9Sb290Q0EuY3JsMD4GCCsGAQUFBwEBBDIwMDAuBggrBgEFBQcwAoYi\\\\naHR0cDovL2xvY2FsaG9zdC9jYWNlcnQvUm9vdENBLmNlcjBfBgNVHSAEWDBWMFQG\\\\nDSsGAQQBg9AzCgEDBQIwQzBBBggrBgEFBQcCARY1aHR0cDovL2NhLmRndHJ1c3Qu\\\\ndGVzdC5yZWxpZWZ2YWxpZGF0aW9uLmNvbS5iZC9jcGNwcy8wga4GA1UdEQSBpjCB\\\\no4IJbG9jYWxob3N0ghYqLnZlcmlkb3MuaW50ZXJuYWwuY29tgg0qLnZlcmlkb3Mu\\\\nY29tghoqLmludGVybmFsLm5ldHNldGdsb2JhbC5yc4IRKi5uZXRzZXRnbG9iYWwu\\\\ncnOCEnN0YXJmaXNoZGVtby5sb2NhbIILKi5uZXRzZXQucnOCByoubG9jYWyCFiou\\\\naW50ZXJuYWwudmVyaWRvcy5jb20wDQYJKoZIhvcNAQELBQADggIBAAXfeKhXJ5cG\\\\nowiUMIO6D/QzBXipymlmS9hRgXSZUkRx20bm0nmfLOGhsvVrcHUDuPisnrXPNDKi\\\\nYJ6BkSGl1V5fSz2SsjTrwv0lhsvVEQvoQs13jy70LxQ4l68ab/2HojtVwhDfYB35\\\\n4XdaJWFaf+yclRQcCbx9yE/ycp9NZoRfFw+pj6d7KMaONQrgAIMbzBf8w4AFy6+d\\\\nlgiAT1cDBDArbVZftVdIcaqwtzksfZU1TqzZF8RL8YOWkkhanIzel5u7mJse2KeR\\\\nOlnoBfnO+6CUs4H5z5lIuoNpr+IO07qe9g5Wdd4JBm8fe5iPn9TjRoeCju2B+jCD\\\\n+l30kHDKCuW8JiBtIL55Fm1x6u5Fsl/ToiJprayjq4HKKsJe7eQeJuO233/91HU/\\\\nPiFuRn0xnoNe1Rr0XxUCgPgj7JYEWq5p/HSoXVny1WYkEkvupbIhGnkE/6FrMG57\\\\n79pDbMIKeZ9TL2fRg/dkaOwIYF8EN5eT9kUcTeQ6CzAjmWJTM1DBJBFbsKd4QjrQ\\\\nCJPHMg5yM5yB2DbPnCMozOqg9POoLd6LgS92/c4HTxcLXytgEJX8CcLUf9gNtYH0\\\\nrrDcbzr78PxzRFHTMzm6aLuG3pPdEs/dss27jMc51JBcMenNU2QtWOX1KIzjdfSU\\\\nADrMWzk6EbVhxEqJhXTurBQvn2aYe8zK\\\\n-----END CERTIFICATE-----\\\\n\\\"}\",\"startDate\":1740649083106,\"endDate\":1772185083106}";
		Map<String, Object> response = new ObjectMapper().readValue(json, Map.class);
		String pem = (response.get("cas") + "").replaceAll(
				".*\"Certificate\": *\"(-----BEGIN CERTIFICATE-----[\na-zA-Z0-9=\\\\/+]+-----END CERTIFICATE-----).*",
				"$1").replace("\\n", "\n");

		if (!pem.contains("-----BEGIN CERTIFICATE-----")) {
			throw new RuntimeException();
		}
		System.out.println(pem);
		response.put("pem", pem);
		response.put("pemBase64", Base64.getEncoder().encodeToString(pem.getBytes(StandardCharsets.UTF_8)));
	}
}
