package com.pa.evs.sv.impl;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.web.multipart.MultipartFile;

import com.pa.evs.dto.PaginDto;
import com.pa.evs.dto.SFileDto;
import com.pa.evs.model.CARequestLog;
import com.pa.evs.model.SFile;
import com.pa.evs.repository.CARequestLogRepository;
import com.pa.evs.repository.SFileRepository;
import com.pa.evs.sv.EVSPAService;
import com.pa.evs.sv.FileService;
import com.pa.evs.utils.AppProps;
import com.pa.evs.utils.CMD;
import com.pa.evs.utils.SecurityUtils;

@Service
public class FileServiceImpl implements FileService {
	
	private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(FileServiceImpl.class);

	@Autowired
	SFileRepository sFileRepository;
	
	@Autowired
	EVSPAService evsPAService;
	
	@Autowired
	CARequestLogRepository caRequestLogRepository;

	@Autowired
	EntityManager em;
	
	@Value("${s3.photo.bucket.name}")
	private String photoBucketName;
	
	@Value("${evs.pa.fakeS3Url:false}")
	private boolean fakeS3Url;
	
	@Value("${s3.access.expireTime:15}")
	private long expireTime;
	
	@Override
	@Transactional
	public void saveFile(MultipartFile[] files, String type, String altName, String uid, String description, String bucketName) {

		for (MultipartFile file : files) {
			
			long limit = Long.parseLong(AppProps.get("MAX_FILE_SIZE_LIMIT", "10000000"));
			try {
				if (file.getSize() > limit) {
					throw new RuntimeException("File size error (limit: " + limit + " bytes)");
				}
				Long now = System.currentTimeMillis();
				String fileName = ("-").concat(now.toString()).concat("-").concat(file.getOriginalFilename());

				Optional<CARequestLog> caOpt = caRequestLogRepository.findByUid(uid);

				if (caOpt.isPresent()) {
					fileName = caOpt.get().getSn().concat(fileName);
				} else {
					fileName = uid.concat(fileName);
				}

				evsPAService.upload(fileName, file.getInputStream(), file.getContentType(), bucketName);

				sFileRepository.save(
						SFile
						.builder()
						.uid(uid)
						.createdDate(new Date())
						.type(type)
						.description(description)
						.size(file.getSize())
						.contentType(file.getContentType())
						.originalName(file.getOriginalFilename())
						.altName(fileName)
						.uploadedBy(SecurityUtils.getEmail())
						.build()
					);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

	}

	@SuppressWarnings("unchecked")
	@Override
	public Object getFiles(String types, String altNames, String uids) {
		
		if (StringUtils.isBlank(uids)) {
			return new ArrayList<>();
		}
		if (types == null) {
			types = "";
		}
		if (altNames == null) {
			altNames = "";
		}
		List<String> cTypes = Arrays.stream(types.split(" *, *")).filter(StringUtils::isNotBlank).collect(Collectors.toList());
		List<String> cAltNames = Arrays.stream(altNames.split(" *, *")).filter(StringUtils::isNotBlank).collect(Collectors.toList());
		List<String> cUids = Arrays.stream(uids.split(" *, *")).filter(StringUtils::isNotBlank).collect(Collectors.toList());
		StringBuilder sql = new StringBuilder();
		sql.append(" FROM SFile WHERE 1=1 ");
		sql.append(" AND uid in :uids ");
		if (StringUtils.isNotBlank(types)) {
			sql.append(" AND type in :types ");
		}
		if (StringUtils.isNotBlank(altNames)) {
			sql.append(" AND altName in :altNames ");
		}
		
		sql.append(" ORDER BY createdDate DESC ");
		Query query = em.createQuery(sql.toString());
		query.setParameter("uids", cUids);	
		if (StringUtils.isNotBlank(types)) {
			query.setParameter("types", cTypes);	
		}
		if (StringUtils.isNotBlank(altNames)) {
			query.setParameter("altNames", cAltNames);	
		}
		
		List<SFileDto> rp = new ArrayList<>();
		query.getResultList().forEach(fr -> rp.add(SFileDto.from((SFile) fr)));
		
		return rp;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public PaginDto<SFileDto> getP1Files(PaginDto<SFileDto> pagin) {
        
		StringBuilder sqlBuilder = new StringBuilder("FROM SFile sf ");
		StringBuilder sqlCountBuilder = new StringBuilder("SELECT count(*) FROM SFile sf");

		StringBuilder sqlCommonBuilder = new StringBuilder();
		if (CollectionUtils.isEmpty(pagin.getOptions())) {
			sqlCommonBuilder.append(" WHERE 1=1 ");
		} else {
			sqlCommonBuilder.append(" WHERE    ");
			Map<String, Object> map = pagin.getOptions();
	        
	        String uid = (String) map.get("uid");
	        String type = (String) map.get("type");
	        String altName = (String) map.get("altName");
	        
			if (StringUtils.isNotBlank(uid)) {
				sqlCommonBuilder.append(" uid like '%" + uid + "%' AND ");
			}
			if (StringUtils.isNotBlank(type)) {
				sqlCommonBuilder.append(" type like '%" + type + "%' AND ");
			}
			if (StringUtils.isNotBlank(altName)) {
				sqlCommonBuilder.append(" altName like '%" + altName + "%' AND ");
			}
			sqlCommonBuilder.delete(sqlCommonBuilder.length() - 4, sqlCommonBuilder.length());

			if (sqlCommonBuilder.length() < 10) {
				sqlCommonBuilder.append(" 1 = 1 ");
			}
		}
		
		sqlBuilder.append(sqlCommonBuilder).append(" ORDER BY id DESC");
		sqlCountBuilder.append(sqlCommonBuilder);

		if (pagin.getOffset() == null || pagin.getOffset() < 0) {
            pagin.setOffset(0);
        }
        
        if (pagin.getLimit() == null || pagin.getLimit() <= 0) {
            pagin.setLimit(10);
        }
        
        Query queryCount = em.createQuery(sqlCountBuilder.toString());
		Query query = em.createQuery(sqlBuilder.toString());
		query.setFirstResult(pagin.getOffset());
		query.setMaxResults(pagin.getLimit());

		Long count = ((Number) queryCount.getSingleResult()).longValue();
		pagin.setTotalRows(count);
		pagin.setResults(new ArrayList<>());
		if (count == 0l) {
			return pagin;
		}
		
		List<SFileDto> rp = new ArrayList<>();
		query.getResultList().forEach(fr -> rp.add(SFileDto.from((SFile) fr)));
		
		pagin.setResults(rp);
		return pagin;
	}

	@Override
	public Optional<SFile> findById(Long id) {
		return sFileRepository.findById(id);
	}

	@SuppressWarnings("unchecked")
	@Override
	@Transactional(readOnly = true)
	public void downloadFile(String altName, HttpServletResponse response) throws Exception {
		
		List<SFile> fs = em.createQuery("FROM SFile WHERE altName='" + altName + "' order by createdDate DESC ").setMaxResults(1).getResultList();
		if (fs.isEmpty()) {
			throw new RuntimeException("File not found!");
		}
		SFile file = fs.get(0);
		if (StringUtils.isBlank(file.getContentType())) {
			file.setContentType("application/octet-stream");
		}
		response.setContentType(file.getContentType());
		String extension = "";
		if (!"application/octet-stream".equalsIgnoreCase(file.getContentType())) {
			extension = "." + file.getContentType().replaceAll(".*/([^/]+)$", "$1");
		} else {
			response.setHeader("Content-disposition", "attachment; filename=" + file.getAltName() + extension + "");
		}
		
		if (file.getContentType().matches("^(image/).*")) {
			response.setHeader("Cache-Control", "max-age=86400, public");	
		}
		
		String s3FileUrl = getS3FileUrl(file.getAltName() + extension);
		
		response.setStatus(302);
		response.setHeader("Location", s3FileUrl);
	}
	
	@Override
	@Transactional(readOnly = true)
	public void downloadFile(Long id, String uid, HttpServletResponse response) throws Exception {
		
		SFile file = this.findById(id).orElseThrow(() -> new RuntimeException("File not found!"));
		if (!uid.equals(file.getUid())) {
			throw new RuntimeException("file not found");
		}
		if (StringUtils.isBlank(file.getContentType())) {
			file.setContentType("application/octet-stream");
		}
		response.setContentType(file.getContentType());
		String extension = "";
		if (!"application/octet-stream".equalsIgnoreCase(file.getContentType())) {
			extension = "." + file.getContentType().replaceAll(".*/([^/]+)$", "$1");
		} else {
			response.setHeader("Content-disposition", "attachment; filename=" + file.getAltName());	
		}
		
		if (file.getContentType().matches("^(image/).*")) {
			response.setHeader("Cache-Control", "max-age=86400, public");	
		}
		
		String s3FileUrl = getS3FileUrl(file.getAltName());
		
		response.setStatus(302);
		response.setHeader("Location", s3FileUrl);
	}
	
	public String getS3FileUrl(String fileName) {
		if (fakeS3Url) {
			LOG.debug("Using fake s3 url");
			return "http://gridhutautomation.com/pa-meter-2.bin";
		}
		String bcName = photoBucketName + "/" + fileName;
		LOG.info("getS3URL: " + bcName);
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		CMD.exec("/usr/local/aws/bin/aws s3 presign \"s3://" + bcName + "\" --expires-in " + (60 * expireTime), null, bos);
		String rs = new String(bos.toByteArray(), StandardCharsets.UTF_8).replaceAll("[\n\r]", "");
		try {
			bos.close();
		} catch (IOException e) {
			LOG.error(e.getMessage(), e);
		}
		return rs;
	}
}
