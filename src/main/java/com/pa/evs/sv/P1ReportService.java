package com.pa.evs.sv;

import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import com.pa.evs.dto.PaginDto;

public interface P1ReportService {

	void save(MultipartFile file) throws Exception;

	void save(List<MultipartFile> files) throws Exception;

	void search(PaginDto<Object> pagin);
}
