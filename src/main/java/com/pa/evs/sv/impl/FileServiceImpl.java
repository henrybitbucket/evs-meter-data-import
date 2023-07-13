package com.pa.evs.sv.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.hibernate.Session;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.pa.evs.dto.SFileDto;
import com.pa.evs.model.SFile;
import com.pa.evs.repository.SFileRepository;
import com.pa.evs.sv.FileService;
import com.pa.evs.utils.AppProps;
import com.pa.evs.utils.SecurityUtils;

@Service
public class FileServiceImpl implements FileService {

	@Autowired
	SFileRepository sFileRepository;

	@Autowired
	EntityManager em;
	
	@Override
	@Transactional
	public void saveFile(MultipartFile[] files, String type, String altName, String uid, String description) {

		for (MultipartFile file : files) {
			
			long limit = Long.parseLong(AppProps.get("MAX_FILE_SIZE_LIMIT", "10000000"));
			try {
				if (file.getSize() > limit) {
					throw new RuntimeException("File size error (limit: " + limit + " bytes)");
				}
				sFileRepository.save(
						SFile
						.builder()
						.uid(uid)
						.createdDate(new Date())
						.content(em.unwrap(Session.class).getLobHelper().createBlob(file.getInputStream(), file.getSize()))
						.type(type)
						.description(description)
						.size(file.getSize())
						.contentType(file.getContentType())
						.originalName(file.getOriginalFilename())
						.altName(altName)
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
		
		IOUtils.copyLarge(file.getContent().getBinaryStream(), response.getOutputStream());
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
			response.setHeader("Content-disposition", "attachment; filename=" + file.getAltName() + extension + "");	
		}
		
		if (file.getContentType().matches("^(image/).*")) {
			response.setHeader("Cache-Control", "max-age=86400, public");	
		}
		
		IOUtils.copyLarge(file.getContent().getBinaryStream(), response.getOutputStream());
	}
}
