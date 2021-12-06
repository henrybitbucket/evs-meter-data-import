package com.pa.evs.sv;

import com.pa.evs.dto.ExportReportDto;
import com.pa.evs.dto.PaginDto;
import com.pa.evs.dto.ReportDto;
import com.pa.evs.dto.ReportJasperParameterDto;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.List;

public interface ReportService {

    void getReports(PaginDto<ReportDto> pagin);

    void createReport(MultipartFile file, String reportName);

    void updateReport(MultipartFile file, Long id, String reportName);

    void deleteReport(Long id);

    List<ReportJasperParameterDto> getParameters(Long id);

    void doExportReport(File exportFile, ExportReportDto dto);
}
