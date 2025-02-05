package com.pa.evs.ctrl;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.pa.evs.dto.ExportReportDto;
import com.pa.evs.dto.MeterFileDataDto;
import com.pa.evs.dto.PaginDto;
import com.pa.evs.dto.ReportDto;
import com.pa.evs.dto.ReportJasperParameterDto;
import com.pa.evs.dto.ResponseDto;
import com.pa.evs.enums.JasperFormat;
import com.pa.evs.sv.ReportService;
import com.pa.evs.utils.JasperUtil;
import com.pa.evs.utils.TimeZoneHolder;

import io.swagger.v3.oas.annotations.Hidden;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@RestController
@Hidden
public class ReportController {

    static final Logger logger = LogManager.getLogger(ReportController.class);

    @Value("${evs.pa.report.jasperDir}")
    private String jasperDir;

    @Autowired private ReportService reportService;

    @PostMapping("/api/reports")
    public ResponseEntity<Object> getReports(HttpServletRequest httpServletRequest, @RequestBody PaginDto<ReportDto> pagin) throws Exception {
        try {
            reportService.getReports(pagin);
        } catch (Exception e) {
            return ResponseEntity.ok(ResponseDto.builder().success(false).message(e.getMessage()).build());
        }
        return ResponseEntity.ok(ResponseDto.builder().success(true).response(pagin).build());
    }
    
    @PostMapping("/api/meterFileDatas")
    public ResponseEntity<Object> getMeterFileDatas(HttpServletRequest httpServletRequest, @RequestBody PaginDto<MeterFileDataDto> pagin) throws Exception {
        try {
            reportService.getMeterFileDatas(pagin);
        } catch (Exception e) {
            return ResponseEntity.ok(ResponseDto.builder().success(false).message(e.getMessage()).build());
        }
        return ResponseEntity.ok(ResponseDto.builder().success(true).response(pagin).build());
    }

    @PostMapping("/api/report/{reportName}/create")
    public ResponseEntity<Object> createReport(@RequestBody MultipartFile file,
                               HttpServletRequest req, HttpServletResponse res,
                               @PathVariable String reportName) throws IOException {
        try {
            reportService.createReport(file, reportName);
        } catch (Exception e) {
            return ResponseEntity.ok(ResponseDto.builder().success(false).message(e.getMessage()).build());
        }
        return ResponseEntity.ok(ResponseDto.builder().success(true).build());
    }

    @PutMapping("/api/report/{id}/{reportName}/update")
    public ResponseEntity<Object> updateReport(@RequestBody MultipartFile file,
                               HttpServletRequest req, HttpServletResponse res,
                               @PathVariable Long id,
                               @PathVariable String reportName) throws IOException {
        try {
            reportService.updateReport(file, id, reportName);
        } catch (Exception e) {
            return ResponseEntity.ok(ResponseDto.builder().success(false).message(e.getMessage()).build());
        }
        return ResponseEntity.ok(ResponseDto.builder().success(true).build());
    }

    @DeleteMapping("/api/report/{id}")
    public ResponseEntity<Object> deleteReport(HttpServletRequest httpServletRequest, @PathVariable final Long id){
        try {
            reportService.deleteReport(id);
        } catch (Exception e) {
            return ResponseEntity.ok(ResponseDto.builder().success(false).message(e.getMessage()).build());
        }
        return ResponseEntity.ok(ResponseDto.builder().success(true).build());
    }

    @GetMapping("/api/report/parameters/{id}")
    public Object getParameters(HttpServletRequest httpServletRequest, @PathVariable final Long id){
        List<ReportJasperParameterDto> parameters = reportService.getParameters(id);
        return ResponseDto.builder().success(true).response(parameters).build();
    }

    @GetMapping("/api/report/formats")
    public Object getFormats(HttpServletRequest httpServletRequest){
        return ResponseDto.builder().success(true).response(JasperFormat.values()).build();
    }

    @PostMapping("/api/report/execute")
    public ResponseEntity<Object> getRelatedLogs(HttpServletRequest httpServletRequest, HttpServletResponse response, @RequestBody ExportReportDto dto) throws Exception {
        try {
            File exportFile = JasperUtil.getTempFile(jasperDir);
            reportService.doExportReport(exportFile, dto);
            logger.debug("Export file: " + exportFile.getName());

            try (FileInputStream fis = new FileInputStream(exportFile)) {
                response.setContentLengthLong(exportFile.length());
                response.setHeader(HttpHeaders.CONTENT_TYPE, getContentType(dto.getFormat()));
                response.setHeader(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, "name");
                response.setHeader("name", exportFile.getName());
                response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + exportFile.getName() + "\"");
                IOUtils.copy(fis, response.getOutputStream());
            } finally {
                FileUtils.deleteDirectory(exportFile.getParentFile());
            }
            TimeZoneHolder.remove();
            return ResponseEntity.ok(ResponseDto.builder().success(true).build());
        } catch (Exception e) {
            logger.error("", e);
            return ResponseEntity.ok(ResponseDto.builder().success(false).message(e.getMessage()).build());
        }
    }

    private String getContentType(JasperFormat format) {
        String content = "";
        if (JasperFormat.CSV == format) {
            content = "text/html";
        } else {
            content = "application/" + format.getExt();
        }
        return content;
    }
}
