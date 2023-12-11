package com.pa.evs.sv;

import java.util.Optional;

import javax.servlet.http.HttpServletResponse;

import org.springframework.web.multipart.MultipartFile;

import com.pa.evs.dto.PaginDto;
import com.pa.evs.dto.SFileDto;
import com.pa.evs.model.SFile;

public interface FileService {

	void saveFile(MultipartFile[] files, String type[], String altName[], String uid[], String description[], String[] replaceByOriginalNames, String bucketName);

	Object getFiles(String types, String altNames, String uids);

	Optional<SFile> findById(Long id);

	void downloadFile(String altName, HttpServletResponse response) throws Exception;

	void downloadFile(Long id, String uid, HttpServletResponse response) throws Exception;

	PaginDto<SFileDto> getP1Files(PaginDto<SFileDto> pagin);

	void deleteP1File(String altName) throws Exception;
}
