package com.pa.evs.schedule;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.apache.commons.io.FileUtils;
import org.hibernate.Session;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pa.evs.dto.ExportReportDto;
import com.pa.evs.dto.ReportJasperParameterDto;
import com.pa.evs.enums.JasperFormat;
import com.pa.evs.model.Report;
import com.pa.evs.model.ReportFile;
import com.pa.evs.model.ReportTask;
import com.pa.evs.repository.ReportFileRepository;
import com.pa.evs.repository.ReportTaskRepository;
import com.pa.evs.sv.ReportService;
import com.pa.evs.utils.JasperUtil;

import jakarta.persistence.EntityManager;

@Component
public class ReportTaskJob implements Job {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    
    
    @Value("${evs.pa.report.jasperDir}")
    private String jasperDir;
    
    @Override
    public void execute(JobExecutionContext cntxt) throws JobExecutionException {
        logger.debug("Executing report scheduled event at: " + new Date());
        JobDataMap ma = cntxt.getJobDetail().getJobDataMap();
        ReportService reportService = (ReportService) ma.get("REPORT_SERVICE");
        EntityManager em = (EntityManager) ma.get("ENTITY_MANAGER");
        Long reportTaskID = (Long) ma.get("REPORT_TASK_ID");
        ReportTaskRepository reportTaskRepository = (ReportTaskRepository) ma.get("REPORT_TASK_REPOSITORY");
        ReportFileRepository reportFileRepository = (ReportFileRepository) ma.get("REPORT_FILE_REPOSITORY");
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
        logger.debug("Report: " + report.getId());
        logger.debug("List Parameters: " + listParameters.size());
        logger.debug("Report Task Id: " + reportTaskID);
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
                
                InputStream file = new FileInputStream(exportFile);
             
                try {
                    Optional<ReportTask> reportTask = reportTaskRepository.findById(reportTaskID);
                    ReportFile reportFile = new ReportFile();
                    if (file != null) {
                        try {
                        	reportFile.setBinBlob(em.unwrap(Session.class).getLobHelper().createBlob(file, exportFile.length()));
                        	reportFile.setFileFormat(format.toString().toLowerCase());
                        	reportFile.setFileName(report.getReportName() + "-" + Calendar.getInstance().getTimeInMillis() + "." + format.toString().toLowerCase());
                        	reportFile.setReportTask(reportTask.get());
                        	reportFileRepository.save(reportFile);
                        	reportFileRepository.flush();
                        } finally {
                            if (exportFile != null) {
                                FileUtils.deleteQuietly(exportFile);
                            }
                        }
                    }              
                } catch (Exception ex) {
                    logger.error("Error update report", ex);
                }           
            } catch (Exception e) {
                logger.error("", e);             
            }
            
        } catch (Exception e) {
            logger.error("Error when executing report task", e);
        }

    }
}
