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
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;

import com.pa.evs.dto.AddressDto;
import com.pa.evs.dto.BuildingDto;
import com.pa.evs.dto.DeviceSettingDto;
import com.pa.evs.dto.LogDto;
import com.pa.evs.dto.MeterCommissioningReportDto;
import com.pa.evs.dto.P1OnlineStatusDto;
import com.pa.evs.enums.DeviceType;
import com.pa.evs.model.CARequestLog;
import com.pa.evs.sv.SettingService;

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
    }

    public static File writeCaRequestLogCsv(List<CARequestLog> listInput, String fileName, Long activateDate) throws IOException{
        listInput = listInput.stream().filter(input -> !input.getUid().equals("server.csr")).collect(Collectors.toList());
        List<String> headers = Arrays.asList(
                "eSIM", "Profile", "ActivationDate(yyyy-mm-dd)");
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
    
    private static String buildPathFile(String fileName) throws IOException {
        String parentFolder = (CsvUtils.EXPORT_TEMP + '/' + System.currentTimeMillis() + Math.random()).replace(".", "");
        Files.createDirectories(Paths.get(parentFolder));
        return parentFolder + '/' + fileName;
    }
    
    private static List<String> postProcessCsv(List<String> record) {
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
}
