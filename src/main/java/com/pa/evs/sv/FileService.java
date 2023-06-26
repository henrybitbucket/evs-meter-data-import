package com.pa.evs.sv;

import java.util.Optional;

import javax.servlet.http.HttpServletResponse;

import org.springframework.web.multipart.MultipartFile;

import com.pa.evs.model.SFile;

public interface FileService {

	void saveFile(MultipartFile[] files, String type, String altName, String uid, String description);

	Object getFiles(String types, String altNames, String uids);

	Optional<SFile> findById(Long id);

	void downloadFile(Long id, HttpServletResponse response) throws Exception;

	void downloadFile(String altName, HttpServletResponse response) throws Exception;
}
