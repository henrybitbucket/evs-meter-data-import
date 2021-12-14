package com.pa.evs.schedule;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pa.evs.dto.ExportReportDto;
import com.pa.evs.dto.ReportJasperParameterDto;
import com.pa.evs.dto.ResponseDto;
import com.pa.evs.enums.CommandEnum;
import com.pa.evs.enums.JasperFormat;
import com.pa.evs.model.CARequestLog;
import com.pa.evs.model.Group;
import com.pa.evs.model.Report;
import com.pa.evs.model.ReportTask;
import com.pa.evs.repository.ReportRepository;
import com.pa.evs.repository.ReportTaskRepository;
import com.pa.evs.sv.EVSPAService;
import com.pa.evs.sv.ReportService;
import com.pa.evs.utils.JasperUtil;
import com.pa.evs.utils.RSAUtil;
import com.pa.evs.utils.SimpleMap;
import com.pa.evs.utils.TimeZoneHolder;
import com.pa.evs.utils.Utils;

import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRParameter;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.export.JRCsvExporter;
import net.sf.jasperreports.engine.export.JRPdfExporter;
import net.sf.jasperreports.engine.export.ooxml.JRXlsxExporter;
import net.sf.jasperreports.engine.util.JRLoader;
import net.sf.jasperreports.engine.xml.JRXmlLoader;
import net.sf.jasperreports.export.ExporterInput;
import net.sf.jasperreports.export.SimpleExporterInput;
import net.sf.jasperreports.export.SimpleOutputStreamExporterOutput;
import net.sf.jasperreports.export.SimpleWriterExporterOutput;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Component
public class ReportTaskJob implements Job {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    
    
    @Value("${evs.pa.report.jasperDir}")
    private String jasperDir;
    
    @Override
    public void execute(JobExecutionContext cntxt) throws JobExecutionException {
        logger.debug("Executing report scheduled event at: " + new Date());
        JobDataMap ma = cntxt.getJobDetail().getJobDataMap();
        EVSPAService evsPAService = (EVSPAService) ma.get("EVS_PA_SERVICE");
        ReportService reportService = (ReportService) ma.get("REPORT_SERVICE");
        ReportTaskRepository reportTaskRepository = (ReportTaskRepository) ma.get("REPORT_TASK_REPOSITORY");
        Report report = (Report) ma.get("REPORT");
        String parameter = (String) ma.get("PARAMETER");
        JasperFormat format = (JasperFormat) ma.get("FORMAT");
        ObjectMapper mapper = new ObjectMapper();
        List<ReportJasperParameterDto> listParameters = null;
        try {
			listParameters = mapper.readValue(parameter, new TypeReference<List<ReportJasperParameterDto>>(){});
		} catch (JsonMappingException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (JsonProcessingException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
        String alias = (String) ma.get("ALIAS");
        String pkPath = (String) ma.get("PK_PATH");
        logger.debug("Report: " + report.getId());
        logger.debug("List Parameters: " + listParameters.size());
        try {
        	ExportReportDto exportReport = new ExportReportDto();
        	exportReport.setParameters(listParameters);
        	exportReport.setReportId(report.getId());
        	exportReport.setFormat(format);
        	
        	try {
            	File exportFile = JasperUtil.getTempFile(jasperDir);
            	reportService.doExportReport(exportFile, exportReport);
            	
                logger.debug("Export file: " + exportFile.getName());              
                logger.debug("Report: " + exportFile);
                
                try {
                    Optional<ReportTask> reportTask = reportTaskRepository.findById(null);
//                    if (StringUtils.isNotEmpty(reportName)) {
//                        report.get().setReportName(reportName);
//                    }
//                    if (file != null) {
//                        File fileBin = null;
//                        Utils.mkdirs(jasperDir);
//                        try (InputStream isSrc = file.getInputStream()) {
//                            fileBin = JasperUtil.getTempFile(jasperDir);
//                            JasperCompileManager.compileReportToFile(JRXmlLoader.load(isSrc),
//                                    fileBin.getAbsolutePath());
//                            reportTask.get().setBinBlob(FileUtils.readFileToByteArray(fileBin));
//                        } finally {
//                            if (fileBin != null) {
//                                FileUtils.deleteQuietly(fileBin);
//                            }
//                        }
//                    }
                    reportTaskRepository.save(reportTask.get());
                } catch (Exception ex) {
                    logger.error("Error update report", ex);
                }

//                try (FileInputStream fis = new FileInputStream(exportFile)) {
//                
//                    response.setContentLengthLong(exportFile.length());
//                    response.setHeader(HttpHeaders.CONTENT_TYPE, getContentType(dto.getFormat()));
//                    response.setHeader(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, "name");
//                    response.setHeader("name", exportFile.getName());
//                    response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + exportFile.getName() + "\"");
//                    IOUtils.copy(fis, response.getOutputStream());
//                } finally {
//                    FileUtils.deleteDirectory(exportFile.getParentFile());
//                }
//                TimeZoneHolder.remove();                
            } catch (Exception e) {
                logger.error("", e);             
            }
            
        } catch (Exception e) {
            logger.error("Error when executing report task", e);
        }

    }
}
