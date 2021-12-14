package com.pa.evs.sv.impl;

import com.pa.evs.dto.ExportReportDto;
import com.pa.evs.dto.PaginDto;
import com.pa.evs.dto.ReportDto;
import com.pa.evs.dto.ReportJasperParameterDto;
import com.pa.evs.enums.JasperFormat;
import com.pa.evs.model.Report;
import com.pa.evs.repository.ReportRepository;
import com.pa.evs.sv.ReportService;
import com.pa.evs.utils.JasperUtil;
import com.pa.evs.utils.Utils;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRParameter;
import net.sf.jasperreports.engine.JRPropertiesMap;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.export.JRCsvExporter;
import net.sf.jasperreports.engine.export.JRPdfExporter;
import net.sf.jasperreports.engine.export.ooxml.JRXlsxExporter;
import net.sf.jasperreports.engine.fill.JRAbstractLRUVirtualizer;
import net.sf.jasperreports.engine.fill.JRParameterDefaultValuesEvaluator;
import net.sf.jasperreports.engine.fill.JRSwapFileVirtualizer;
import net.sf.jasperreports.engine.util.JRLoader;
import net.sf.jasperreports.engine.util.JRSwapFile;
import net.sf.jasperreports.engine.xml.JRXmlLoader;
import net.sf.jasperreports.export.ExporterInput;
import net.sf.jasperreports.export.SimpleExporterInput;
import net.sf.jasperreports.export.SimpleOutputStreamExporterOutput;
import net.sf.jasperreports.export.SimpleWriterExporterOutput;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Session;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.sql.DataSource;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.sql.Blob;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class ReportServiceImpl implements ReportService {

    static final Logger logger = LogManager.getLogger(ReportServiceImpl.class);

    @Autowired
    private DataSource dataSource;

    @Autowired
    ReportRepository reportRepository;

    @Autowired
    EntityManager em;

    @Value("${evs.pa.report.jasperDir}")
    private String jasperDir;

    @Override
    public void getReports(PaginDto<ReportDto> pagin) {
        StringBuilder sqlBuilder = new StringBuilder("FROM Report");
        StringBuilder sqlCountBuilder = new StringBuilder("SELECT count(*) FROM Report");

        StringBuilder sqlCommonBuilder = new StringBuilder();
        sqlCommonBuilder.append(" WHERE 1=1 ");
        sqlBuilder.append(sqlCommonBuilder).append(" ORDER BY createDate DESC");
        sqlCountBuilder.append(sqlCommonBuilder);

        if (pagin.getOffset() == null || pagin.getOffset() < 0) {
            pagin.setOffset(0);
        }

        if (pagin.getLimit() == null || pagin.getLimit() <= 0) {
            pagin.setLimit(100);
        }

        Query queryCount = em.createQuery(sqlCountBuilder.toString());

        Long count = ((Number)queryCount.getSingleResult()).longValue();
        pagin.setTotalRows(count);
        pagin.setResults(new ArrayList<>());
        if (count == 0l) {
            return;
        }

        Query query = em.createQuery(sqlBuilder.toString());
        query.setFirstResult(pagin.getOffset());
        query.setMaxResults(pagin.getLimit());

        List<Report> list = query.getResultList();

        list.forEach(li -> {
            ReportDto dto = ReportDto.builder()
                    .id(li.getId())
                    .reportName(li.getReportName())
                    .build();
            pagin.getResults().add(dto);
        });
    }

    @Override
    public void createReport(MultipartFile file, String reportName) {
        Report report = new Report();
        report.setCreateDate(Calendar.getInstance().getTime());
        report.setReportName(reportName);
        try {
            File fileBin = null;
            Utils.mkdirs(jasperDir);
            try (InputStream isSrc = file.getInputStream()) {
                fileBin = JasperUtil.getTempFile(jasperDir);
                JasperCompileManager.compileReportToFile(JRXmlLoader.load(isSrc),
                        fileBin.getAbsolutePath());
                
                InputStream in = new FileInputStream(fileBin);
                report.setBinBlob(em.unwrap(Session.class).getLobHelper().createBlob(in, fileBin.length()));
                reportRepository.save(report);
                reportRepository.flush();
                in.close();
            } finally {
                if (fileBin != null) {
                    FileUtils.deleteQuietly(fileBin);
                }
            }
        } catch (Exception ex) {
            logger.error("Error add report", ex);
        }
    }

    @Override
    public void updateReport(MultipartFile file, Long id, String reportName) {
        try {
            Optional<Report> report = reportRepository.findById(id);
            if (StringUtils.isNotEmpty(reportName)) {
                report.get().setReportName(reportName);
            }
            if (file != null) {
                File fileBin = null;
                Utils.mkdirs(jasperDir);
                try (InputStream isSrc = file.getInputStream()) {
                    fileBin = JasperUtil.getTempFile(jasperDir);
                    JasperCompileManager.compileReportToFile(JRXmlLoader.load(isSrc),
                            fileBin.getAbsolutePath());
                    InputStream in = new FileInputStream(fileBin);
                    report.get().setBinBlob(em.unwrap(Session.class).getLobHelper().createBlob(in, fileBin.length()));
                    reportRepository.save(report.get());
                    reportRepository.flush();
                    in.close();
                } finally {
                    if (fileBin != null) {
                        FileUtils.deleteQuietly(fileBin);
                    }
                }
            }
            reportRepository.save(report.get());
        } catch (Exception ex) {
            logger.error("Error update report", ex);
        }
    }

    @Override
    public void deleteReport(Long id) {
        reportRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReportJasperParameterDto> getParameters(Long id) {
        logger.debug("Load Jasper Parameters");
        List<ReportJasperParameterDto> parameters = new ArrayList<>();
        try {
            Optional<Report> reportEntity = reportRepository.findById(id);
            JasperReport report = loadReport(reportEntity.get());
            Map<String, Object> defaultValuesMap = JRParameterDefaultValuesEvaluator
                    .evaluateParameterDefaultValues(report, new HashMap<>());
            for (JRParameter param : report.getParameters()) {
                if (!param.isSystemDefined() && param.isForPrompting()) {
                    ReportJasperParameterDto parameter = getJasperParameter(param.getName(),
                            param.getDescription(),
                            param.getValueClass());
                    if (param.hasProperties()) {
                        JRPropertiesMap map = param.getPropertiesMap();
                        Map<String, String> prop = new HashMap<>();
                        for (String name : map.getPropertyNames()) {
                            String value = map.getProperty(name);
                            prop.put(name, value);
                        }
                        parameter.setProperties(Collections.unmodifiableMap(prop));
                    }
                    if (defaultValuesMap.containsKey(param.getName())
                            && defaultValuesMap.get(param.getName()) != null
                            && parameter.getType() != ReportJasperParameterDto.ParameterType.DATE) {
                        parameter.setValue(defaultValuesMap.get(param.getName()));
                    }
                    parameter.setShow(true);
                    parameters.add(parameter);
                }
            }
        } catch (Exception ex) {
            logger.error("Error add report", ex);
        }
        return parameters;
    }

    @Override
    @Transactional(readOnly = true)
    public void doExportReport(File exportFile, ExportReportDto dto) {
        File swapFile = null;
        try {
            Optional<Report> reportEntity = reportRepository.findById(dto.getReportId());
            List<ReportJasperParameterDto> paramsNoInput = new ArrayList<>();
            JasperReport report = loadReport(reportEntity.get());
            for (JRParameter param : report.getParameters()) {
                if (!param.isSystemDefined() && !param.isForPrompting()) {
                    ReportJasperParameterDto parameter = getJasperParameter(param.getName(),
                            param.getDescription(),
                            param.getValueClass());
                    parameter.setShow(false);
                    paramsNoInput.add(parameter);
                }
            }
            dto.getParameters().addAll(paramsNoInput);
            swapFile = jasperExecuteReportSwap(dto.getParameters(), reportEntity.get());
            swapFile.deleteOnExit();

            logger.debug("executeJasperReport: Try to export: " + swapFile.getAbsolutePath() + " ("
                            + swapFile.length() + ")");

            ExporterInput exporterIn = new SimpleExporterInput(swapFile);

            if (dto.getFormat() == JasperFormat.PDF) {
                JRPdfExporter exporter = new JRPdfExporter();
                exporter.setExporterInput(exporterIn);
                SimpleOutputStreamExporterOutput exporterOut = new SimpleOutputStreamExporterOutput(
                        exportFile);
                exporter.setExporterOutput(exporterOut);
                exporter.exportReport();
            } else if (dto.getFormat() == JasperFormat.XLSX) {
                JRXlsxExporter exporter = new JRXlsxExporter();
                exporter.setExporterInput(exporterIn);
                SimpleOutputStreamExporterOutput exporterOut = new SimpleOutputStreamExporterOutput(
                        exportFile);
                exporter.setExporterOutput(exporterOut);
                exporter.exportReport();
            } else if (dto.getFormat() == JasperFormat.CSV) {
                JRCsvExporter exporter = new JRCsvExporter();
                exporter.setExporterInput(exporterIn);
                SimpleWriterExporterOutput exporterOut = new SimpleWriterExporterOutput(exportFile);
                exporter.setExporterOutput(exporterOut);
                exporter.exportReport();
            } else {
                throw new Exception("Unsupported report format: " + dto.getFormat());
            }

            logger.debug("executeJasperReport: Exported: " + exportFile.getAbsolutePath() + " ("
                    + exportFile.length() + ")");
        } catch (Exception ex) {
            logger.error("Error add report", ex);
        } finally {
            FileUtils.deleteQuietly(swapFile);
        }
    }

    private static <T> ReportJasperParameterDto getJasperParameter(String name, String description,
                                                                Class<?> cls) {
        ReportJasperParameterDto.ParameterType type = ReportJasperParameterDto.ParameterType.valueOfByClass(cls);
        logger.debug("Param: " + name + ",cls:" + cls.getName() + ",type: " + type);
        return getJasperParameter(name, description, null, ReportJasperParameterDto.ParameterType.valueOfByClass(cls));
    }

    private static <T> ReportJasperParameterDto getJasperParameter(String name, String description,
                                                                T value,
                                                                   ReportJasperParameterDto.ParameterType type) {
        ReportJasperParameterDto parameter = new ReportJasperParameterDto();
        parameter.setName(name);
        parameter.setDescription(description);
        parameter.setType(type);
        parameter.setValue(value);
        return parameter;
    }

    private JasperReport loadReport(Report reportEntity) throws Exception {
        logger.info("Load report from database");
        InputStream isBin = null;
        JasperReport jasperReport = null;
        try {
        	Blob blod = reportEntity.getBinBlob();
            if (blod != null) {
                jasperReport = (JasperReport) JRLoader.loadObject(blod.getBinaryStream());
            }
            logger.debug("Loaded report: " + jasperReport);
            return jasperReport;
        } finally {
            if (isBin != null) {
                try {
                    isBin.close();
                } catch (Exception ex) {
                    logger.error("Error close resource: " + ex.getMessage());
                }
            }
        }
    }

    private File jasperExecuteReportSwap(List<ReportJasperParameterDto> parameters, Report report) throws Exception {
        File file;
        JRAbstractLRUVirtualizer virtualizer = null;
        Connection connection = null;
        try {
            connection = dataSource.getConnection();
            JasperReport jasperReport = loadReport(report);
            file = JasperUtil.getTempFile(jasperDir);
            virtualizer = new JRSwapFileVirtualizer(1, new JRSwapFile(file.getParent(), 1024, 1024),
                    true);
            Map<String, Object> param = fillReportParameters(parameters);
            param.put(JRParameter.REPORT_VIRTUALIZER, virtualizer);
            JasperFillManager.fillReportToFile(jasperReport, file.getAbsolutePath(), param,
                    connection);

            logger.debug("jasperExecuteReportSwap: Report filled: " + file.getAbsolutePath() + " ("
                            + file.length() + ")");
            return file;
        } finally {
            if (virtualizer != null) {
                virtualizer.cleanup();
            }
            if (connection != null) {
                try {
                    connection.close();
                } catch (Exception ex) {
                    logger.error("Error close connection", ex);
                }
            }
        }
    }

    private static Map<String, Object> fillReportParameters(Collection<ReportJasperParameterDto> parameters) {
        Map<String, Object> params = new HashMap<>();
        if (parameters.size() != 0) {
            Map<String, ReportJasperParameterDto> paramMap = new HashMap<>();
            for (ReportJasperParameterDto p : parameters) {
                paramMap.put(p.getName(), p);
            }
            for (ReportJasperParameterDto reportParameter : parameters) {
                if (!params.containsKey(reportParameter.getName())) {
                    params.put(reportParameter.getName(), reportParameter.getValue());
                }
            }
            for (ReportJasperParameterDto p : parameters) {
                logger.debug("Param: " + p.getName() + ", type: " + p.getType() + ",val: " + params.get(p.getName()));
            }
        }
        return params;
    }

}
