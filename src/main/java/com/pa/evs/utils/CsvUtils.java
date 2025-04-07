package com.pa.evs.utils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pa.evs.dto.AddressDto;
import com.pa.evs.dto.BuildingDto;
import com.pa.evs.dto.DeviceSettingDto;
import com.pa.evs.dto.LogDto;
import com.pa.evs.dto.MeterCommissioningReportDto;
import com.pa.evs.dto.P1OnlineStatusDto;
import com.pa.evs.enums.DeviceType;
import com.pa.evs.model.CARequestLog;
import com.pa.evs.sv.SettingService;
import com.pa.evs.utils.CsvUtils.CsvRecordConverter;

public class CsvUtils {
    
    private static final Logger logger = LoggerFactory.getLogger(CsvUtils.class);
    public static final String EXPORT_TEMP = System.getProperty("user.home") + '/' + "ca-request-log";
    
    private static final ThreadLocal<String> KEY_PASS = new ThreadLocal<>();
    
    static {
        try {
            Files.createDirectories(Paths.get(EXPORT_TEMP));
        } catch (Exception e) {
            logger.error(e.getMessage());
        }
        
        try {
            Files.createDirectories(Paths.get(EXPORT_TEMP + "/logs"));
        } catch (Exception e) {
            logger.error(e.getMessage());
        }
    }

    public static File writeCaRequestLogCsv(List<CARequestLog> listInput, String fileName, Long activateDate) throws IOException{
        listInput = listInput.stream().filter(input -> !input.getUid().equals("server.csr")).collect(Collectors.toList());
        List<String> headers = Arrays.asList(
                "eSIM ID(ICCID)", "Profile", "ActivationDate(yyyy-mm-dd)");
        return toCsv(headers, listInput, CsvUtils::toCSVRecord, buildPathFile(fileName), activateDate);
    }
    
    public static File writeMeterCsv(List<CARequestLog> listInput, String fileName, Long activateDate) throws IOException{
        listInput = listInput.stream().filter(input -> !input.getUid().equals("server.csr")).collect(Collectors.toList());
        List<String> headers = Arrays.asList(
                "MCU SN", "MCU UUID", "ESIM ID", "MSN", "VENDOR", "LAST SEEN", "Building", "Block", "Level", "Unit");
        return toCsv(headers, listInput, (idx, it, l) -> CsvUtils.toCSVRecord(idx, it), buildPathFile(fileName), activateDate);
    }
    
    public static File writeMCUCsv(List<CARequestLog> listInput, String fileName, List<String> sns) throws IOException{
        listInput = listInput.stream().filter(input -> !input.getUid().equals("server.csr")).collect(Collectors.toList());
        List<String> headers = Arrays.asList(
                "MCU SN", "MCU UUID", "eSIM ID", "MSN", "Status", "P2 CoupleState", "Version", "Vendor", "Last Seen", "Group", "EnrollTime");
        
        List<CARequestLog> tmp = new ArrayList<>();
        for (String sn : sns) {
        	CARequestLog mcu = listInput.stream().filter(c -> c.getSn().equalsIgnoreCase(sn)).findFirst().orElse(new CARequestLog());
        	if (mcu.getSn() == null) {
        		mcu.setSn(sn);
        	}
        	tmp.add(mcu);
        }
        return toCsv(headers, tmp, (idx, it, l) -> {
        	
        	CARequestLog mcu = it;
            List<String> record = new ArrayList<>();

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            sdf.setTimeZone(TimeZoneHolder.get());
            record.add(StringUtils.isNotBlank(mcu.getSn()) ? mcu.getSn() : "");
            
            if (mcu.getId() != null) {
	            record.add(StringUtils.isNotBlank(mcu.getUid()) ? mcu.getUid() : "");
	            record.add(StringUtils.isNotBlank(mcu.getCid()) ? mcu.getCid() : "");
	            record.add(StringUtils.isNotBlank(mcu.getMsn()) ? mcu.getMsn() : "");
	            record.add(mcu.getStatus() != null ? mcu.getStatus().toString() : "");
	            record.add(mcu.getType() != null ? mcu.getType().toString() : "");
	            record.add(mcu.getVer() != null ? mcu.getVer().toString() : "");
	            record.add(mcu.getVendor() != null ? mcu.getVendor().getName() : "");
	            record.add(mcu.getLastSubscribeDatetime() != null ? sdf.format(new Date(mcu.getLastSubscribeDatetime())) : "");
	            
	            record.add(mcu.getGroup() != null ? mcu.getGroup().getId().toString() : "");
	            record.add(mcu.getEnrollmentDatetime() != null ? sdf.format(new Date(mcu.getEnrollmentDatetime())) : "");
            } else {
            	record.add("unfind");
            }
            
            return postProcessCsv(record);
        }, buildPathFile(fileName), 1l);
    }
    
    // ID (Key),Building,Block,Level,Unit,Postcode,,Street Address,State.City,Coupled,UpdatedTime,Remark
    public static File writeAddressCsv(List<BuildingDto> listInput, String exportType, String fileName) throws IOException{
        List<String> headers = Arrays.asList(
				/* "Address ID", */"Building Name", "Block", "Level", "Unit", "Postcode", "Street Address", "State.City", "Coupled", "UpdatedTime", "Remark");
        List<String> headersWithMsnAndSn = Arrays.asList(
				/* "Address ID", */"MCU SN", "Meter SN", "City", "Street", "Postcode", "Building", "Block", "Level", "Unit", "Remark", "Coupled Date Time");

        headers = Arrays.stream(AppProps.get(SettingService.EXPORT_ADDRESS_HEADER).trim().split(" *, *")).collect(Collectors.toList());
        if ("address-only".equalsIgnoreCase(exportType)) {
        	headers = Arrays.stream("Building,Block,Level,Unit,Postcode,Street Address,State.City,UpdatedTime,Remark".trim().split(" *, *")).collect(Collectors.toList());
        	if ("DMS".equals(AppCodeSelectedHolder.get())) {
        		headers = Arrays.stream("Building,Block,Level,Unit,Postcode,Street Address,State.City,UpdatedTime,Location Tag,Remark".trim().split(" *, *")).collect(Collectors.toList());
        	}
        } else {
        	headers = headersWithMsnAndSn;
        }
        
        try {
        	KEY_PASS.set(exportType);
        	return toCsv(headers, listInput, CsvUtils::toCSVRecord, buildPathFile(fileName), null);
		} finally {
			KEY_PASS.remove();
		}
    }
    
    public static File writeImportAddressCsv(List<AddressDto> listInput, String exportType, String fileName) throws IOException{
        List<String> headers = Arrays.asList(
				/* "Address ID", */"Building Name", "Block", "Level", "Unit", "Postcode", "Street Address", "State.City", "Coupled", "UpdatedTime", "Remark");
        headers = Arrays.asList(
				/* "Address ID", */"Building Name", "Block", "Level", "Unit", "Postcode", "Street Address", "State.City", "Coupled Meter No.", "Coupled MCU SN");
        headers = Arrays.stream(AppProps.get(SettingService.EXPORT_ADDRESS_HEADER).trim().split(" *, *")).collect(Collectors.toList());
        if ("address-only".equalsIgnoreCase(exportType)) {
        	headers = Arrays.stream("Building,Block,Level,Unit,Postcode,Street Address,State.City,UpdatedTime,Remark".trim().split(" *, *")).collect(Collectors.toList());
        }
        headers.add("Message");
        try {
        	KEY_PASS.set(exportType);
        	return toCsv(headers, listInput, CsvUtils::toCSVRecord, buildPathFile(fileName), null);
		} finally {
			KEY_PASS.remove();
		}
        
    }
    
    public static File writeAlarmsLogCsv(List<LogDto> listInput, String fileName, Long activateDate) throws IOException{
        List<String> headers = Arrays.asList(
                "TIME", "TYPE", "TOPIC", "MID", "MSN", "SN", "COMMAND", "RAW MESSAGE", "STATUS", "ADDRESS");
        return toCsv(headers, listInput, CsvUtils::toCSVRecord, buildPathFile(fileName), activateDate);
    }
    
    public static File writeDeviceSettingsCsv(List<DeviceSettingDto> listInput, String fileName, Long activateDate) throws IOException{
        List<String> headers = Arrays.asList(
                "MSN", "PREVIOUS SETTING", "NEW SETTING", "STATUS");
        return toCsv(headers, listInput, CsvUtils::toCSVRecord, buildPathFile(fileName), activateDate);
    }

    private static List<String> toCSVRecord(int idx, DeviceSettingDto setting, Long activateDate) {
        List<String> record = new ArrayList<>();
        
        record.add(setting.getMsn());
        record.add(setting.getPreviousSetting());
        record.add(setting.getNewSetting());
        record.add(setting.getStatus());
        
        return postProcessCsv(record);
    }
    
    private static List<String> toCSVRecord(int idx, LogDto log, Long activateDate) {
        List<String> record = new ArrayList<>();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ssZ");
        sdf.setTimeZone(TimeZoneHolder.get());
        record.add(sdf.format(log.getCreateDate()));
        record.add(log.getType());
        record.add(log.getTopic());
        record.add((log.getMid() == null ? log.getOid() : log.getMid()) + "");
        record.add(log.getMsn());
        record.add(log.getSn());
        record.add(log.getPType());
        record.add(log.getRaw());
        record.add(log.getRepStatusDesc());
        record.add(log.getAddress());
        return postProcessCsv(record);
    }
    
    private static List<String> toCSVRecord(int idx, BuildingDto log, Long activateDate) {
        List<String> record = new ArrayList<>();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        sdf.setTimeZone(TimeZoneHolder.get());
        if (!"address-only".equalsIgnoreCase(KEY_PASS.get())) {
        	record.add(log.getAddress().getCoupleSn());
        	record.add(log.getAddress().getCoupleMsn());
        	record.add(log.getAddress().getCity());
        	record.add(log.getAddress().getStreet());
        	record.add(log.getAddress().getPostalCode());
        	record.add(log.getAddress().getBuilding());
            record.add(log.getAddress().getBlock());
            record.add(log.getAddress().getLevel());
            record.add(log.getAddress().getUnitNumber());
            record.add(log.getAddress().getRemark());
            record.add(log.getAddress().getCoupleTime() != null ? sdf.format(log.getAddress().getCoupleTime()) : "");
        } else {
        	record.add(log.getAddress().getBuilding());
            record.add(log.getAddress().getBlock());
            record.add(log.getAddress().getLevel());
            record.add(log.getAddress().getUnitNumber());
            record.add(log.getAddress().getPostalCode());
            record.add(log.getAddress().getStreet());
            record.add(log.getAddress().getCity());
            record.add(log.getAddress().getCoupleTime() != null ? sdf.format(log.getAddress().getCoupleTime()) : "");
            
            if ("DMS".equals(AppCodeSelectedHolder.get())) {
            	record.add(log.getAddress().getLocationTag());
            }
            record.add(log.getAddress().getRemark());
        }
        return postProcessCsv(record);
    }
    
    private static List<String> toCSVRecord(int idx, AddressDto log, Long activateDate) {
        List<String> record = new ArrayList<>();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        sdf.setTimeZone(TimeZoneHolder.get());
        record.add(log.getBuilding());
        record.add(log.getBlock());
        record.add(log.getLevel());
        record.add(log.getUnitNumber());
        record.add(log.getPostalCode());
        record.add(log.getStreet());
        record.add(log.getCity());

        if (!"address-only".equalsIgnoreCase(KEY_PASS.get())) {
            record.add(log.getCoupleMsn());
            record.add(log.getCoupleSn());      	
        }
        
        record.add(log.getCoupleTime() != null ? sdf.format(log.getCoupleTime()) : "");
        record.add(log.getRemark());
        record.add(StringUtils.isBlank(log.getMessage()) ? "success" : log.getMessage());
        return postProcessCsv(record);
    }
    
    private static List<String> toCSVRecord(int idx, CARequestLog caRequestLog, Long activateDate) {
        List<String> record = new ArrayList<>();

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        record.add(caRequestLog.getCid());
        record.add(StringUtils.isBlank(caRequestLog.getProfile()) ? "METER_SG_TATA_PA_B01" : caRequestLog.getProfile());
        record.add(
                activateDate != null ? sdf.format(new Date(activateDate))
                : caRequestLog.getActivationDate() != null ? sdf.format(new Date(caRequestLog.getActivationDate())) : "");
        
        return postProcessCsv(record);
    }
    
    //  "MCU SN", "MCU UUID", "ESIM ID", "MSN", "VENDOR", "LAST SEEN", "Building", "Block", "Level", "Unit"
    private static List<String> toCSVRecord(int idx, CARequestLog meter) {
        List<String> record = new ArrayList<>();

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        sdf.setTimeZone(TimeZoneHolder.get());
        record.add(StringUtils.isNotBlank(meter.getSn()) ? meter.getSn() : "");
        record.add(StringUtils.isNotBlank(meter.getUid()) ? meter.getUid() : "");
        record.add(StringUtils.isNotBlank(meter.getCid()) ? meter.getCid() : "");
        record.add(StringUtils.isNotBlank(meter.getMsn()) ? meter.getMsn() : "");
        record.add(meter.getVendor() != null ? meter.getVendor().getName() : "");
        record.add(meter.getLastSubscribeDatetime() != null ? sdf.format(new Date(meter.getLastSubscribeDatetime())) : "");
        record.add(meter.getBuilding() != null ? meter.getBuilding().getName() : "");
        record.add(meter.getBlock() != null ? meter.getBlock().getName() : "");
        record.add(meter.getFloorLevel() != null ? meter.getFloorLevel().getName() : "");
        record.add(meter.getBuildingUnit() != null ? meter.getBuildingUnit().getName() : "");
        
        return postProcessCsv(record);
    }
    
    public static String buildPathFile(String fileName) throws IOException {
        String parentFolder = (CsvUtils.EXPORT_TEMP + '/' + System.currentTimeMillis() + Math.random()).replace(".", "");
        Files.createDirectories(Paths.get(parentFolder));
        return parentFolder + '/' + fileName;
    }
    
    public static List<String> postProcessCsv(List<String> record) {
        List<String> escaped = new ArrayList<>(record.size());

        for (String column : record) {
            escaped.add(StringUtils.trimToEmpty(column));
        }

        return escaped;
    }
    
    public static <T> File toCsv(List<String> headers, List<T> items, CsvRecordConverter<T> converter, String filePath, Long activateDate)
            throws IOException {
        String[] headersArr = new String[headers.size()];
        for (int i = 0; i < headers.size(); i++) {
			headersArr[i] = /* '\ufeff' + */headers.get(i);
        }
        File f = new File(filePath);
        Writer fileWriter = new FileWriter(f);
        try (
                BufferedWriter writer = new BufferedWriter(fileWriter);
                CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT
                        .withDelimiter(',')
                        .withIgnoreHeaderCase()
                        .withTrim()
                        .withHeader(headersArr));
        ) {
            if (!CollectionUtils.isEmpty(items)) {
                int idx = 0;
                for (T item : items) {
                    List<String> record = converter.toCSVRecord(idx, item, activateDate);
                    for (int i = 0; i < record.size(); i++) {
						record.set(i, /* '\ufeff' + */record.get(i));
                    }
                    csvPrinter.printRecord(record);
                    idx++;
                }
            }
            csvPrinter.flush();
            writer.flush();
            fileWriter.flush();
            fileWriter.close();
        }
        return f;
    }
    
    @FunctionalInterface
    public interface CsvRecordConverter<T> {
        List<String> toCSVRecord(int idx, T record, Long activateDate);
    }
    
    private static List<String> toCSVRecord(int idx, MeterCommissioningReportDto log, Long acDate) {
        List<String> record = new ArrayList<>();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        sdf.setTimeZone(TimeZoneHolder.get());
        record.add(log.getMsn());
        record.add(log.getUid());
        
        // MCU Profile
        if (log.getId() != null) {
            record.add(
            		new StringBuilder()
            		.append("MCU SN: ").append(log.getSn()).append("\n")
            		.append("MCU UUID: ").append(log.getUid()).append("\n")
            		.append("ESIM ID: ").append(log.getCid()).append("\n")
            		.append("STATUS: ").append(log.getStatus()).append("\n")
            		.append("COUPLED STATE: ").append(log.getType()).append("\n")
            		.append("COUPLED USER: ").append(StringUtils.isNotBlank(log.getCoupledUser()) ? log.getCoupledUser() : "").append("\n")
            		.append("Onboarding Time: ").append(log.getLastOBRDate() != null ? (sdf.format(new Date(log.getLastOBRDate()))) : "").append("\n")
            		.toString()
        		);        	
        } else {
        	record.add("");
        }
        
        // Meter Data
        if (log.getId() != null) {
            record.add(
            		new StringBuilder()
            		.append("Kwh: ").append(log.getKwh()).append("\n")
            		.append("Kw: ").append(log.getKw()).append("\n")
            		.append("I: ").append(log.getI()).append("\n")
            		.append("V: ").append(log.getV()).append("\n")
            		.append("DTime: ").append(log.getDt() != null ? (sdf.format(new Date(log.getDt())) + " (Meter Time)") : "").append("\n")
            		.toString()
        		);       	
        } else {
        	record.add("");
        }
        
        // User ID
        record.add(log.getUserSubmit());
        
        // P2Checking time
        record.add(log.getTimeSubmit() != null ? sdf.format(new Date(log.getTimeSubmit())) : "");
        
        // Commit time
        record.add(log.getCreateDate() != null ? sdf.format(log.getCreateDate()) : "");
        
        // OnBoarding Time
        // record.add(log.getLastOBRDate() != null ? sdf.format(new Date(log.getLastOBRDate())) : "");
        
        // Meter photo
        if (log.getId() != null && log.getMeterPhotos() != null) {
        	record.add(Arrays.stream(log.getMeterPhotos().split(",")).collect(Collectors.joining("\n")));     	
        } else {
        	record.add("");
        }
        
        record.add(log.getIsPassed() != null ? (log.getIsPassed() ? "PASS" : "FAIL") : "No Submission");
        record.add(StringUtils.isBlank(log.getJobSheetNo()) ? "" : log.getJobSheetNo());
        return postProcessCsv(record);
    }
    
    private static List<String> toCSVRecord(int idx, P1OnlineStatusDto p1, Long acDate) {
        List<String> record = new ArrayList<>();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        sdf.setTimeZone(TimeZoneHolder.get());

        record.add(p1.getSn());
        record.add(p1.getUid());
        record.add(p1.getCid());
        record.add(p1.getMsn());
        record.add(p1.getType() == null ? DeviceType.NOT_COUPLED.name() : p1.getType().name());
        record.add(p1.getVersion());
        record.add(p1.getVendor() != null ? p1.getVendor().getName() : "");
        record.add(p1.getP1Online());
        record.add(p1.getP1OnlineLastUserSent());
        record.add(p1.getP1OnlineLastSent() != null ? sdf.format(new Date(p1.getP1OnlineLastSent())) : "");
        record.add(p1.getP1OnlineLastReceived() != null ? sdf.format(new Date(p1.getP1OnlineLastReceived())) : "");
        
        return postProcessCsv(record);
    }

	public static File writeMeterCommissionCsv(List<MeterCommissioningReportDto> results, String fileName) throws IOException {
        List<String> headers = Arrays.asList(
				"Meter SN", "MCU SN", "MCU Profile", "Meter Data", "P2Submit user", "P2Checking time", "Commit time", "Meter photo", "P2Checking Result", "Job sheet no");
        return toCsv(headers, results, CsvUtils::toCSVRecord, buildPathFile(fileName), new Date().getTime());
	}
	
	public static File writeP1OnlineCsv(List<P1OnlineStatusDto> results, String fileName) throws IOException {
        List<String> headers = Arrays.asList(
				"MCU SN", "MCU UUID", "ESIM ID", "MSN", "COUPLE STATE", "VERSION", "VENDOR", "STATUS", "USER SENT", "LAST SENT", "LAST RECEIVER");
        return toCsv(headers, results, CsvUtils::toCSVRecord, buildPathFile(fileName), new Date().getTime());
	}
	
	
	static List<String> uids = new ArrayList<>(); //
	static List<String> msns = new ArrayList<>(); //
	static Map<String, Map<String, Object>> info = new LinkedHashMap<>();
	public static void look(File f) throws Exception {
		if (f == null || !f.exists()) {
			return;
		}
		
		if (f.isDirectory()) {
			for (File ch : f.listFiles()) {
				look(ch);
			}
		}
		SimpleDateFormat sf = new SimpleDateFormat("YYYY-MM-dd HH:mm:ss.SSS");
		if (f.isFile() && f.getName().endsWith(".log")) {
			int[] counts = new int[] {0};
			for (String line : new String(Files.readAllBytes(f.toPath())).split("\r*\n")) {
				counts[0] = counts[0] + 1;
				for (int idx = 0; idx < msns.size(); idx++) {
					String msn = msns.get(idx);
					if (line.contains(">Subscribe") && line.contains(msn)) {
						Map<String, Object> i = info.computeIfAbsent(msn, k -> new LinkedHashMap<>());
						// System.out.println("Match " + msn + " -> " + f.getAbsolutePath() + "(" + counts[0] + ") " + line);
						
						String time = line.substring(0, 23);
						try {
							long t = sf.parse(time).getTime();
							long existT = (long) i.computeIfAbsent("t", k -> 0l);
							if (t > existT) {
								i.put("line", line);
								i.put("file", f.getAbsolutePath());
								i.put("t", t);
								i.put("time", time);
								i.put("msn", msn);
								i.put("uid", uids.get(idx));
							}
						} catch (Exception e) {
							//
						}
						
					}
				}
			}
		}
	}
	
	public static void main(String[] args) throws Exception {
		
		
		List<String> checks = Arrays.asList(
				"BIE2IEYAAMADYAFIAA-202006000722",
				"BIE2IEYAAMAH4AA5AA-202102001473",
				"BIE2IEYAAMAHAABUAA-202102001474",
				"BIE2IEYAAMALCAELAA-202006000784",
				"BIE2IEYAAMAHUAEBAA-202006000825",
				"BIE2IEYAAMAEKAFHAA-202006000840",
				"BIE2IEYAAMAFEAFBAA-202006000841",
				"BIE2IEYAAMAFIAC2AA-202006001014",
				"BIE2IEYAAMAEMAFMAA-202006001027",
				"BIE2IEYAAMAAWADPAA-202006001074",
				"BIE2IEYAAMAKGAECAA-202006001083",
				"BIE2IEYAAMAIAACPAA-202006001099",
				"BIE2IEYAAMAFMABQAA-202006001117",
				"BIE2IEYAAMAHIABWAA-202006001138",
				"BIE2IEYAAMADGAFAAA-202006000421",
				"BIE2IEYAAMAEEABVAA-202006000521",
				"BIE2IEYAAMAEYAFMAA-202102002156"
				);
		checks.forEach(s -> {
			uids.add(s.split("-")[0]);
			msns.add(s.split("-")[1]);
		});
		
		try {
			String t = "{\"202006001117\":{\"t\":1704036961903,\"line\":\"2024-05-08 22:36:01.903 jms-95 - Thread-11475 DEBUG com.pa.evs.sv.impl.EVSPAServiceImpl - >Subscribe 27572 202006001117 MDT evs/pa/data  \",\"file\":\"F:\\\\vmw\\\\temp-logs\\\\tmp_log_f\\\\app-05-08-2024-1.log\\\\app-05-08-2024-1.log\",\"time\":\"2024-05-08 22:36:01.903\",\"msn\":\"202006001117\",\"uid\":\"BIE2IEYAAMAFMABQAA\"},\"202006001138\":{\"t\":1704038259642,\"line\":\"2024-05-04 22:57:39.642 jms-157 - Thread-165358 DEBUG com.pa.evs.sv.impl.EVSPAServiceImpl - >Subscribe 62546 202006001138 MDT evs/pa/data  \",\"file\":\"F:\\\\vmw\\\\temp-logs\\\\tmp_log_f\\\\app-05-04-2024-1.log\\\\app-05-04-2024-1.log\",\"time\":\"2024-05-04 22:57:39.642\",\"msn\":\"202006001138\",\"uid\":\"BIE2IEYAAMAHIABWAA\"},\"202006000421\":{\"t\":1704038491900,\"line\":\"2024-05-16 23:01:31.900 jms-72 - Thread-12733 DEBUG com.pa.evs.sv.impl.EVSPAServiceImpl - >Subscribe 32582 202006000421 MDT evs/pa/data  \",\"file\":\"F:\\\\vmw\\\\temp-logs\\\\tmp_log_f\\\\app-05-16-2024-1.log\\\\app-05-16-2024-1.log\",\"time\":\"2024-05-16 23:01:31.900\",\"msn\":\"202006000421\",\"uid\":\"BIE2IEYAAMADGAFAAA\"},\"202006000825\":{\"t\":1704019480611,\"line\":\"2024-05-05 17:44:40.611 jms-180 - Thread-182552 DEBUG com.pa.evs.sv.impl.EVSPAServiceImpl - >Subscribe 16332 202006000825 MDT evs/pa/data  \",\"file\":\"F:\\\\vmw\\\\temp-logs\\\\tmp_log_f\\\\app-05-05-2024-1.log\\\\app-05-05-2024-1.log\",\"time\":\"2024-05-05 17:44:40.611\",\"msn\":\"202006000825\",\"uid\":\"BIE2IEYAAMAHUAEBAA\"},\"202006000841\":{\"t\":1704020672955,\"line\":\"2024-05-05 18:04:32.955 jms-180 - Thread-182782 DEBUG com.pa.evs.sv.impl.EVSPAServiceImpl - >Subscribe 10914 202006000841 MDT evs/pa/data  \",\"file\":\"F:\\\\vmw\\\\temp-logs\\\\tmp_log_f\\\\app-05-05-2024-1.log\\\\app-05-05-2024-1.log\",\"time\":\"2024-05-05 18:04:32.955\",\"msn\":\"202006000841\",\"uid\":\"BIE2IEYAAMAFEAFBAA\"},\"202006001014\":{\"t\":1704030848432,\"line\":\"2024-05-12 20:54:08.432 jms-108 - Thread-9299 DEBUG com.pa.evs.sv.impl.EVSPAServiceImpl - >Subscribe 64716 202006001014 MDT evs/pa/data  \",\"file\":\"F:\\\\vmw\\\\temp-logs\\\\tmp_log_f\\\\app-05-12-2024-1.log\\\\app-05-12-2024-1.log\",\"time\":\"2024-05-12 20:54:08.432\",\"msn\":\"202006001014\",\"uid\":\"BIE2IEYAAMAFIAC2AA\"},\"202006001027\":{\"t\":1704031597114,\"line\":\"2024-05-08 21:06:37.114 jms-86 - Thread-10697 DEBUG com.pa.evs.sv.impl.EVSPAServiceImpl - >Subscribe 38546 202006001027 MDT evs/pa/data  \",\"file\":\"F:\\\\vmw\\\\temp-logs\\\\tmp_log_f\\\\app-05-08-2024-1.log\\\\app-05-08-2024-1.log\",\"time\":\"2024-05-08 21:06:37.114\",\"msn\":\"202006001027\",\"uid\":\"BIE2IEYAAMAEMAFMAA\"},\"202006001074\":{\"t\":1704034429855,\"line\":\"2024-05-07 21:53:49.855 jms-139 - Thread-31807 DEBUG com.pa.evs.sv.impl.EVSPAServiceImpl - >Subscribe 23292 202006001074 MDT evs/pa/data  \",\"file\":\"F:\\\\vmw\\\\temp-logs\\\\tmp_log_f\\\\app-05-07-2024-1.log\\\\app-05-07-2024-1.log\",\"time\":\"2024-05-07 21:53:49.855\",\"msn\":\"202006001074\",\"uid\":\"BIE2IEYAAMAAWADPAA\"},\"202006001083\":{\"t\":1704035197281,\"line\":\"2024-05-09 22:06:37.281 jms-120 - Thread-32195 DEBUG com.pa.evs.sv.impl.EVSPAServiceImpl - >Subscribe 10127 202006001083 MDT evs/pa/data  \",\"file\":\"F:\\\\vmw\\\\temp-logs\\\\tmp_log_f\\\\app-05-09-2024-1.log\\\\app-05-09-2024-1.log\",\"time\":\"2024-05-09 22:06:37.281\",\"msn\":\"202006001083\",\"uid\":\"BIE2IEYAAMAKGAECAA\"},\"202006001099\":{\"t\":1704036024752,\"line\":\"2024-05-12 22:20:24.752 jms-131 - Thread-10082 DEBUG com.pa.evs.sv.impl.EVSPAServiceImpl - >Subscribe 15416 202006001099 MDT evs/pa/data  \",\"file\":\"F:\\\\vmw\\\\temp-logs\\\\tmp_log_f\\\\app-05-12-2024-1.log\\\\app-05-12-2024-1.log\",\"time\":\"2024-05-12 22:20:24.752\",\"msn\":\"202006001099\",\"uid\":\"BIE2IEYAAMAIAACPAA\"},\"202006000521\":{\"t\":1704001785895,\"line\":\"2024-05-03 12:49:45.895 jms-191 - Thread-121950 DEBUG com.pa.evs.sv.impl.EVSPAServiceImpl - >Subscribe 33059 202006000521 MDT evs/pa/data  \",\"file\":\"F:\\\\vmw\\\\temp-logs\\\\tmp_log_f\\\\app-05-03-2024-1.log\\\\app-05-03-2024-1.log\",\"time\":\"2024-05-03 12:49:45.895\",\"msn\":\"202006000521\",\"uid\":\"BIE2IEYAAMAEEABVAA\"},\"202102002156\":{\"t\":1704013494829,\"line\":\"2024-05-03 16:04:54.829 jms-180 - Thread-132990 DEBUG com.pa.evs.sv.impl.EVSPAServiceImpl - >Subscribe 50383 202102002156 MDT evs/pa/data  \",\"file\":\"F:\\\\vmw\\\\temp-logs\\\\tmp_log_f\\\\app-05-03-2024-1.log\\\\app-05-03-2024-1.log\",\"time\":\"2024-05-03 16:04:54.829\",\"msn\":\"202102002156\",\"uid\":\"BIE2IEYAAMAEYAFMAA\"},\"202006000722\":{\"t\":1704013753811,\"line\":\"2024-05-03 16:09:13.811 jms-157 - Thread-133269 DEBUG com.pa.evs.sv.impl.EVSPAServiceImpl - >Subscribe 6564 202006000722 MDT evs/pa/data  \",\"file\":\"F:\\\\vmw\\\\temp-logs\\\\tmp_log_f\\\\app-05-03-2024-1.log\\\\app-05-03-2024-1.log\",\"time\":\"2024-05-03 16:09:13.811\",\"msn\":\"202006000722\",\"uid\":\"BIE2IEYAAMADYAFIAA\"},\"202102001473\":{\"t\":1704015728574,\"line\":\"2024-05-03 16:42:08.574 jms-191 - Thread-134760 DEBUG com.pa.evs.sv.impl.EVSPAServiceImpl - >Subscribe 21203 202102001473 MDT evs/pa/data  \",\"file\":\"F:\\\\vmw\\\\temp-logs\\\\tmp_log_f\\\\app-05-03-2024-1.log\\\\app-05-03-2024-1.log\",\"time\":\"2024-05-03 16:42:08.574\",\"msn\":\"202102001473\",\"uid\":\"BIE2IEYAAMAH4AA5AA\"},\"202102001474\":{\"t\":1704015849449,\"line\":\"2024-05-03 16:44:09.449 jms-180 - Thread-134819 DEBUG com.pa.evs.sv.impl.EVSPAServiceImpl - >Subscribe 20873 202102001474 MDT evs/pa/data  \",\"file\":\"F:\\\\vmw\\\\temp-logs\\\\tmp_log_f\\\\app-05-03-2024-1.log\\\\app-05-03-2024-1.log\",\"time\":\"2024-05-03 16:44:09.449\",\"msn\":\"202102001474\",\"uid\":\"BIE2IEYAAMAHAABUAA\"},\"202006000784\":{\"t\":1704017571327,\"line\":\"2024-05-03 17:12:51.327 jms-157 - Thread-136278 DEBUG com.pa.evs.sv.impl.EVSPAServiceImpl - >Subscribe 41002 202006000784 MDT evs/pa/data  \",\"file\":\"F:\\\\vmw\\\\temp-logs\\\\tmp_log_f\\\\app-05-03-2024-1.log\\\\app-05-03-2024-1.log\",\"time\":\"2024-05-03 17:12:51.327\",\"msn\":\"202006000784\",\"uid\":\"BIE2IEYAAMALCAELAA\"},\"202006000840\":{\"t\":1704020826947,\"line\":\"2024-05-03 18:07:06.947 jms-166 - Thread-138280 DEBUG com.pa.evs.sv.impl.EVSPAServiceImpl - >Subscribe 55714 202006000840 MDT evs/pa/data  \",\"file\":\"F:\\\\vmw\\\\temp-logs\\\\tmp_log_f\\\\app-05-03-2024-1.log\\\\app-05-03-2024-1.log\",\"time\":\"2024-05-03 18:07:06.947\",\"msn\":\"202006000840\",\"uid\":\"BIE2IEYAAMAEKAFHAA\"}}";
			@SuppressWarnings("unchecked")
			Map<String, Map<String, Object>> m = new ObjectMapper().readValue(t, Map.class);
			info.putAll(m);
		} catch (Exception e) {
			//
		}
		
		String fileName = UUID.randomUUID().toString() + ".csv";
		List<String> headers = Arrays.asList(
				"MSN", "UID", "LAST SUBSCRIBE", "LOG FILE"
				);
		
		List<Map<String, Object>> tmp = new ArrayList<>();
		for (String msn : msns) {
			tmp.add(info.computeIfAbsent(msn, k -> new LinkedHashMap<>()));
		}
		
		File file = toCsv(headers, tmp, (idx, it, l) -> {
        	
            List<String> record = new ArrayList<>();

            record.add("'" + (String) it.get("msn"));
            record.add((String) it.get("uid"));
            record.add("'" + (String) it.get("time"));
            String f = (String) it.get("file");
            if (f != null) {
            	record.add(f.replaceAll("^.*[\\\\/]([^\\\\/]+)$", "$1"));
            }
            
            return postProcessCsv(record);
        }, buildPathFile(fileName), 1l);
		System.out.println(file);
		
		// {"202006001117":{"t":1704036961903,"line":"2024-05-08 22:36:01.903 jms-95 - Thread-11475 DEBUG com.pa.evs.sv.impl.EVSPAServiceImpl - >Subscribe 27572 202006001117 MDT evs/pa/data  ","file":"F:\\vmw\\temp-logs\\tmp_log_f\\app-05-08-2024-1.log\\app-05-08-2024-1.log","time":"2024-05-08 22:36:01.903","msn":"202006001117","uid":"BIE2IEYAAMAFMABQAA"},"202006001138":{"t":1704038259642,"line":"2024-05-04 22:57:39.642 jms-157 - Thread-165358 DEBUG com.pa.evs.sv.impl.EVSPAServiceImpl - >Subscribe 62546 202006001138 MDT evs/pa/data  ","file":"F:\\vmw\\temp-logs\\tmp_log_f\\app-05-04-2024-1.log\\app-05-04-2024-1.log","time":"2024-05-04 22:57:39.642","msn":"202006001138","uid":"BIE2IEYAAMAHIABWAA"},"202006000421":{"t":1704038491900,"line":"2024-05-16 23:01:31.900 jms-72 - Thread-12733 DEBUG com.pa.evs.sv.impl.EVSPAServiceImpl - >Subscribe 32582 202006000421 MDT evs/pa/data  ","file":"F:\\vmw\\temp-logs\\tmp_log_f\\app-05-16-2024-1.log\\app-05-16-2024-1.log","time":"2024-05-16 23:01:31.900","msn":"202006000421","uid":"BIE2IEYAAMADGAFAAA"},"202006000825":{"t":1704019480611,"line":"2024-05-05 17:44:40.611 jms-180 - Thread-182552 DEBUG com.pa.evs.sv.impl.EVSPAServiceImpl - >Subscribe 16332 202006000825 MDT evs/pa/data  ","file":"F:\\vmw\\temp-logs\\tmp_log_f\\app-05-05-2024-1.log\\app-05-05-2024-1.log","time":"2024-05-05 17:44:40.611","msn":"202006000825","uid":"BIE2IEYAAMAHUAEBAA"},"202006000841":{"t":1704020672955,"line":"2024-05-05 18:04:32.955 jms-180 - Thread-182782 DEBUG com.pa.evs.sv.impl.EVSPAServiceImpl - >Subscribe 10914 202006000841 MDT evs/pa/data  ","file":"F:\\vmw\\temp-logs\\tmp_log_f\\app-05-05-2024-1.log\\app-05-05-2024-1.log","time":"2024-05-05 18:04:32.955","msn":"202006000841","uid":"BIE2IEYAAMAFEAFBAA"},"202006001014":{"t":1704030848432,"line":"2024-05-12 20:54:08.432 jms-108 - Thread-9299 DEBUG com.pa.evs.sv.impl.EVSPAServiceImpl - >Subscribe 64716 202006001014 MDT evs/pa/data  ","file":"F:\\vmw\\temp-logs\\tmp_log_f\\app-05-12-2024-1.log\\app-05-12-2024-1.log","time":"2024-05-12 20:54:08.432","msn":"202006001014","uid":"BIE2IEYAAMAFIAC2AA"},"202006001027":{"t":1704031597114,"line":"2024-05-08 21:06:37.114 jms-86 - Thread-10697 DEBUG com.pa.evs.sv.impl.EVSPAServiceImpl - >Subscribe 38546 202006001027 MDT evs/pa/data  ","file":"F:\\vmw\\temp-logs\\tmp_log_f\\app-05-08-2024-1.log\\app-05-08-2024-1.log","time":"2024-05-08 21:06:37.114","msn":"202006001027","uid":"BIE2IEYAAMAEMAFMAA"},"202006001074":{"t":1704034429855,"line":"2024-05-07 21:53:49.855 jms-139 - Thread-31807 DEBUG com.pa.evs.sv.impl.EVSPAServiceImpl - >Subscribe 23292 202006001074 MDT evs/pa/data  ","file":"F:\\vmw\\temp-logs\\tmp_log_f\\app-05-07-2024-1.log\\app-05-07-2024-1.log","time":"2024-05-07 21:53:49.855","msn":"202006001074","uid":"BIE2IEYAAMAAWADPAA"},"202006001083":{"t":1704035197281,"line":"2024-05-09 22:06:37.281 jms-120 - Thread-32195 DEBUG com.pa.evs.sv.impl.EVSPAServiceImpl - >Subscribe 10127 202006001083 MDT evs/pa/data  ","file":"F:\\vmw\\temp-logs\\tmp_log_f\\app-05-09-2024-1.log\\app-05-09-2024-1.log","time":"2024-05-09 22:06:37.281","msn":"202006001083","uid":"BIE2IEYAAMAKGAECAA"},"202006001099":{"t":1704036024752,"line":"2024-05-12 22:20:24.752 jms-131 - Thread-10082 DEBUG com.pa.evs.sv.impl.EVSPAServiceImpl - >Subscribe 15416 202006001099 MDT evs/pa/data  ","file":"F:\\vmw\\temp-logs\\tmp_log_f\\app-05-12-2024-1.log\\app-05-12-2024-1.log","time":"2024-05-12 22:20:24.752","msn":"202006001099","uid":"BIE2IEYAAMAIAACPAA"},"202006000521":{"t":1704001785895,"line":"2024-05-03 12:49:45.895 jms-191 - Thread-121950 DEBUG com.pa.evs.sv.impl.EVSPAServiceImpl - >Subscribe 33059 202006000521 MDT evs/pa/data  ","file":"F:\\vmw\\temp-logs\\tmp_log_f\\app-05-03-2024-1.log\\app-05-03-2024-1.log","time":"2024-05-03 12:49:45.895","msn":"202006000521","uid":"BIE2IEYAAMAEEABVAA"},"202102002156":{"t":1704013494829,"line":"2024-05-03 16:04:54.829 jms-180 - Thread-132990 DEBUG com.pa.evs.sv.impl.EVSPAServiceImpl - >Subscribe 50383 202102002156 MDT evs/pa/data  ","file":"F:\\vmw\\temp-logs\\tmp_log_f\\app-05-03-2024-1.log\\app-05-03-2024-1.log","time":"2024-05-03 16:04:54.829","msn":"202102002156","uid":"BIE2IEYAAMAEYAFMAA"},"202006000722":{"t":1704013753811,"line":"2024-05-03 16:09:13.811 jms-157 - Thread-133269 DEBUG com.pa.evs.sv.impl.EVSPAServiceImpl - >Subscribe 6564 202006000722 MDT evs/pa/data  ","file":"F:\\vmw\\temp-logs\\tmp_log_f\\app-05-03-2024-1.log\\app-05-03-2024-1.log","time":"2024-05-03 16:09:13.811","msn":"202006000722","uid":"BIE2IEYAAMADYAFIAA"},"202102001473":{"t":1704015728574,"line":"2024-05-03 16:42:08.574 jms-191 - Thread-134760 DEBUG com.pa.evs.sv.impl.EVSPAServiceImpl - >Subscribe 21203 202102001473 MDT evs/pa/data  ","file":"F:\\vmw\\temp-logs\\tmp_log_f\\app-05-03-2024-1.log\\app-05-03-2024-1.log","time":"2024-05-03 16:42:08.574","msn":"202102001473","uid":"BIE2IEYAAMAH4AA5AA"},"202102001474":{"t":1704015849449,"line":"2024-05-03 16:44:09.449 jms-180 - Thread-134819 DEBUG com.pa.evs.sv.impl.EVSPAServiceImpl - >Subscribe 20873 202102001474 MDT evs/pa/data  ","file":"F:\\vmw\\temp-logs\\tmp_log_f\\app-05-03-2024-1.log\\app-05-03-2024-1.log","time":"2024-05-03 16:44:09.449","msn":"202102001474","uid":"BIE2IEYAAMAHAABUAA"},"202006000784":{"t":1704017571327,"line":"2024-05-03 17:12:51.327 jms-157 - Thread-136278 DEBUG com.pa.evs.sv.impl.EVSPAServiceImpl - >Subscribe 41002 202006000784 MDT evs/pa/data  ","file":"F:\\vmw\\temp-logs\\tmp_log_f\\app-05-03-2024-1.log\\app-05-03-2024-1.log","time":"2024-05-03 17:12:51.327","msn":"202006000784","uid":"BIE2IEYAAMALCAELAA"},"202006000840":{"t":1704020826947,"line":"2024-05-03 18:07:06.947 jms-166 - Thread-138280 DEBUG com.pa.evs.sv.impl.EVSPAServiceImpl - >Subscribe 55714 202006000840 MDT evs/pa/data  ","file":"F:\\vmw\\temp-logs\\tmp_log_f\\app-05-03-2024-1.log\\app-05-03-2024-1.log","time":"2024-05-03 18:07:06.947","msn":"202006000840","uid":"BIE2IEYAAMAEKAFHAA"}}

	}
}
