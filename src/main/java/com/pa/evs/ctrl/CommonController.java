package com.pa.evs.ctrl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pa.evs.LocalMapStorage;
import com.pa.evs.converter.ExceptionConvertor;
import com.pa.evs.dto.AddressLogDto;
import com.pa.evs.dto.Command;
import com.pa.evs.dto.DeviceRemoveLogDto;
import com.pa.evs.dto.FirmwareDto;
import com.pa.evs.dto.GroupDto;
import com.pa.evs.dto.LogBatchDto;
import com.pa.evs.dto.LogDto;
import com.pa.evs.dto.MeterCommissioningReportDto;
import com.pa.evs.dto.P1OnlineStatusDto;
import com.pa.evs.dto.P2JobDto;
import com.pa.evs.dto.P2ReportAckDto;
import com.pa.evs.dto.PaginDto;
import com.pa.evs.dto.ResponseDto;
import com.pa.evs.dto.SFileDto;
import com.pa.evs.dto.SettingDto;
import com.pa.evs.enums.CommandEnum;
import com.pa.evs.enums.DeviceStatus;
import com.pa.evs.enums.DeviceType;
import com.pa.evs.model.CARequestLog;
import com.pa.evs.model.Company;
import com.pa.evs.model.Log;
import com.pa.evs.model.Pi;
import com.pa.evs.model.RelayStatusLog;
import com.pa.evs.repository.CARequestLogRepository;
import com.pa.evs.repository.LogRepository;
import com.pa.evs.repository.RelayStatusLogRepository;
import com.pa.evs.sv.AddressLogService;
import com.pa.evs.sv.AddressService;
import com.pa.evs.sv.AuthenticationService;
import com.pa.evs.sv.CaRequestLogService;
import com.pa.evs.sv.DMSAddressService;
import com.pa.evs.sv.EVSPAService;
import com.pa.evs.sv.FileService;
import com.pa.evs.sv.FirmwareService;
import com.pa.evs.sv.GroupService;
import com.pa.evs.sv.LogService;
import com.pa.evs.sv.MeterCommissioningReportService;
import com.pa.evs.sv.P1OnlineStatusService;
import com.pa.evs.sv.P1ReportService;
import com.pa.evs.sv.SettingService;
import com.pa.evs.sv.VendorService;
import com.pa.evs.sv.impl.EVSPAServiceImpl;
import com.pa.evs.utils.AppCodeSelectedHolder;
import com.pa.evs.utils.AppProps;
import com.pa.evs.utils.CMD;
import com.pa.evs.utils.CsvUtils;
import com.pa.evs.utils.RSAUtil;
import com.pa.evs.utils.SchedulerHelper;
import com.pa.evs.utils.SecurityUtils;
import com.pa.evs.utils.SimpleMap;
import com.pa.evs.utils.TimeZoneHolder;

import springfox.documentation.annotations.ApiIgnore;

@RestController
@ApiIgnore
public class CommonController {

	static final Logger LOG = LogManager.getLogger(CommonController.class);
	
	@Autowired
	ExceptionConvertor exceptionCnvertor;
	
	@Autowired EVSPAService evsPAService;
	
	@Autowired CaRequestLogService caRequestLogService;
	
	@Autowired CARequestLogRepository caRequestLogRepository;
	
	@Autowired FirmwareService firmwareService;

	@Autowired LogService logService;
	
	@Autowired GroupService groupService;
	
	@Autowired AddressService addressService;
	
	@Autowired DMSAddressService dmsAddressService;
	
	@Autowired AddressLogService addressLogService;
	
	@Autowired AuthenticationService authenticationService;
	
	@Autowired SettingService settingService;

    @Value("${evs.pa.mqtt.timeout:30}")
	private long otaTimeout;

    @Value("${evs.pa.mqtt.publish.topic.alias}")
    private String alias;
    
    @Value("${evs.pa.csr.folder}")
    private String csrFolder;
    
	@Value("${s3.photo.bucket.name}")
	private String photoBucketName;
	
	@Value("${s3.p1.provisioning.bucket.name}")
	private String p1ProvisioningBucketName;
	
	@Value("${evs.pa.data.folder}") 
	private String evsDataFolder;
	
	private String caFolder;

    @Autowired LocalMapStorage localMap;
    
    @Autowired VendorService vendorService;

    @Autowired FileService fileService;
    
    @Autowired MeterCommissioningReportService meterCommissioningReportService;
    
    @Autowired P1ReportService p1ReportService;
    
    @Autowired P1OnlineStatusService p1OnlineStatusService;
    
	@Autowired RelayStatusLogRepository relayStatusLogRepository;
	
	@Autowired LogRepository logRepository;
	
	public static final Map<Object, String> MID_TYPE = new LinkedHashMap<>();
	
	public static final ThreadLocal<Map<String, Object>> CMD_DESC = new ThreadLocal<>();
	
	public static final ThreadLocal<Map<String, Object>> CMD_OPTIONS = new ThreadLocal<>();
	
    @GetMapping("/api/message/publish")//http://localhost:8080/api/message/publish?topic=a&messageKey=1&message=a
    public ResponseEntity<?> sendGMessage(
    		HttpServletRequest httpServletRequest,
    		@RequestParam String topic,
    		@RequestParam String messageKey,
    		@RequestParam String message
    		) throws Exception {
    	
    	/**Mqtt.publish(topic, new Payload<>(messageKey, message), 2, true);*/
    	String json = "{\"header\":{\"mid\":8985,\"uid\":\"BIERWXAABMADGAFHAA\",\"msn\":\"201906000021\",\"sig\":\"3065023100E63D9474849F426557A5367E208B093B3510003B395A0EEBD49FC44EA32F45C582E3B3D55B22FE001B45EFDB6FCFCA7802307D5709A727AB075667FBFAFB5EE8FEC1BADEF6872FE0D811A1900AB86F71DACCBC64CF1ECAFA0F21ABEA197F5BC124B1\"},\"payload\":{\"id\":\"BIERWXAABMADGAFHAA\",\"type\":\"MDT\",\"data\":[{\"uid\":\"BIERWXAABMADGAFHAA\",\"msn\":\"201906000021\",\"kwh\":\"0.0\",\"kw\":\"0.0\",\"i\":\"0.0\",\"v\":\"244.6\",\"pf\":\"10.0\",\"dt\":\"2021-05-31T13:26:51\"}]}}";
    	evsPAService.publish("dev/evs/pa/data", new ObjectMapper().readValue(json, Map.class), "TEST");
        return ResponseEntity.<Object>ok(ResponseDto.<Object>builder().success(true).build());
    }
    
    @PostMapping("/api/message/publish")//http://localhost:8080/api/message/publish
    public ResponseEntity<?> sendPMessage(
    		HttpServletRequest httpServletRequest,
    		@RequestBody Map<String, Object> json1
    		) throws Exception {
    	
    	String json = "{\"header\":{\"mid\":1001,\"uid\":\"BIERWXAABMAB2AEBAA\",\"gid\":\"BIERWXAAA4AFBABABXX\",\"msn\":\"201906000032\",\"sig\":\"Base64(ECC_SIGN(payload))\"},\"payload\":{\"id\":\"BIERWXAABMAB2AEBAA\",\"type\":\"OBR\",\"data\":\"201906000137\"}}";
    	evsPAService.publish("dev/evs/pa/data", new ObjectMapper().readValue(json, Map.class), "TEST");
        return ResponseEntity.<Object>ok(ResponseDto.<Object>builder().success(true).build());
    }
    
    /**
     * 
     *
     *<pre>
    var command = {
	    "msn": '202206000056',
	    "cmd": 'lockoff',
	    "data": {
	        "meter_sn": '202206000056',
	        "command": 'lockoff'
	    },
	    "options": {
	        "DIRECT": true,
	        "topic": "pa/evs/ntu/crc"
	    }
	}

	fetch("http://localhost:7770/api/command?timeZone=Asia/Bangkok", {
	  "headers": {
	    "authorization": "Bearer eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJoZW5yeUBnbWFpbC5jb20iLCJleHAiOjE2OTU2OTk1OTMsImlhdCI6MTY5NTY5NTk5M30.QdezHSFzZg8YQNauIfBKV58bDysCphfcPxDsT9-PKNQC0ziGHzLFgheY84OfqE_XsxRkQ5tXb2FNPfsfmS-KHQ",
	    "content-type": "application/json",
	    "x-kl-ajax-request": "Ajax_Request"
	  },
	  "referrer": "http://localhost:7770/devices?queryAllDate=true&advancedSearch=true&queryMsn=202206000056",
	  "referrerPolicy": "strict-origin-when-cross-origin",
	  "body": JSON.stringify(command),
	  "method": "POST",
	  "mode": "cors"
	})
	.then(rp => rp.json())
	.then(rp => console.info(rp));
	</pre>
    */
    @PostMapping("/api/command")//http://localhost:8080/api/command
    public ResponseEntity<?> sendCommand(
            HttpServletRequest httpServletRequest,
            @RequestBody Command command
            ) throws Exception {
        
        try {
            Optional<CARequestLog> ca = caRequestLogService.findByUid(command.getUid());
            if (!ca.isPresent()) {
            	ca = caRequestLogService.findByMsn(command.getMsn());
            }
            
            if (!ca.isPresent()) {
                return ResponseEntity.<Object>ok(ResponseDto.builder().success(false).message("device not exists!").build());
            }
            
            AppProps.getContext().getBean(EVSPAServiceImpl.class).updateDeviceCsrInfo(ca.get());
            
            Long mid = evsPAService.nextvalMID(ca.get().getVendor());
            command.setUid(ca.get().getUid());
            command.getOptions().put("uid", ca.get().getUid());
            command.getOptions().put("mid", mid);
            CMD_OPTIONS.set(command.getOptions());

            if (command.getOptions().get("topic") != null 
            		&& "true".equalsIgnoreCase("" + command.getOptions().get("DIRECT"))) {
            	evsPAService.publish(command.getOptions().get("topic") + "", command.getData(), command.getCmd());
            	return ResponseEntity.ok(ResponseDto.builder().success(true).response(mid).build());
            }
            
            
            Map<String, Object> data = command.getData();
            SimpleMap<String, Object> map = SimpleMap.init("id", command.getUid()).more("cmd", command.getCmd());
            if (CommandEnum.CFG.name().equals(command.getCmd())) {
                SimpleMap<String, Object> simpleMap = new SimpleMap<>();
                command.getData().forEach((k,v) -> simpleMap.put(k, Integer.parseInt((String)data.get(k))));
                map.more("p1", simpleMap);
                localMap.getCfgMap().put(mid, command.getData());
            }
            LOG.debug("sendCommand : evs.pa.privatekey.path: " + ca.get().getVendor().getKeyPath());
            String sig = BooleanUtils.isTrue(ca.get().getVendor().getEmptySig()) ? "" : RSAUtil.initSignedRequest(ca.get().getVendor().getKeyPath(), new ObjectMapper().writeValueAsString(map), ca.get().getVendor().getSignatureAlgorithm());

            if ("TCM_INFO".equalsIgnoreCase(command.getType())) {
            	LOG.debug("sendCommand TCM_INFO: " + mid + " " + ca.get().getMsn());
            	MID_TYPE.put(mid, command.getType());
            }

            //check OTA is in progress ot NOT
            if (!"TCM_INFO".equalsIgnoreCase(command.getType()) && "INF".equalsIgnoreCase(command.getCmd())) {
                long timeDiff = Calendar.getInstance().getTimeInMillis() - (ca.get().getLastOtaDate() == null ? Calendar.getInstance().getTimeInMillis() : ca.get().getLastOtaDate());
                if (timeDiff <= otaTimeout * 60 * 1000 && ca.get().getIsOta() != null && ca.get().getIsOta()) {
                    return ResponseEntity.<Object>ok(ResponseDto.builder().success(false).message("OTA is in processing. This request is skipped").build());
                }
            }
            
            LOG.info("> Calling api send cmd: " + command.getCmd() + " type: " + command.getType() + " for uid: " + command.getUid() + " mid: " + mid);
            if (StringUtils.isNotBlank(command.getType())) {
            	// Ex: ECH_P1_ONLINE_TEST
            	CMD_DESC.set(SimpleMap.init(command.getUid() + "_" + mid, command.getType()));
            }
            Log resultLog = evsPAService.publish(alias + command.getUid(), SimpleMap.init(
                    "header", SimpleMap.init("uid", command.getUid()).more("mid", mid).more("gid", command.getUid()).more("msn", ca.get().getMsn()).more("sig", sig)
                ).more(
                    "payload", map
                ), command.getCmd(), command.getBatchId());
            
            Thread.sleep(2000l);
            
            if ("RLS".equalsIgnoreCase(command.getCmd()) || "PW0".equalsIgnoreCase(command.getCmd()) || "PW1".equalsIgnoreCase(command.getCmd())) {
            	String commandSendBy = SecurityUtils.getEmail();
            	String batchUuid = UUID.randomUUID().toString();
        		RelayStatusLog rl = new RelayStatusLog();
        		rl.setBatchUuid(batchUuid);
        		rl.setCommand(command.getCmd());
        		rl.setComment(command.getCmd());
        		rl.setFilters("queryUuid=" + ca.get().getUid());
        		rl.setCommandSendBy(commandSendBy);
        		rl.setTotalCount(1);
        		rl.setCurrentCount(resultLog == null ? 0 : 1);
        		rl.setErrorCount(resultLog == null ? 1 : 0);
        		rl.setUid(command.getUid());
        		rl.setMid(mid);
        		
        		if (resultLog != null) {
        			resultLog.setRlsBatchUuid(batchUuid);
        			logRepository.save(resultLog);
        		}
        		
        		relayStatusLogRepository.save(rl);
            }
            
            return ResponseEntity.ok(ResponseDto.builder().success(true).response(mid).build());
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
            return ResponseEntity.<Object>ok(ResponseDto.builder().success(false).message(e.getMessage()).build());
        } finally {
        	CMD_OPTIONS.remove();
        	CMD_DESC.remove();
		}
    }
    
    @PostMapping("/api/link-msn")
    public ResponseEntity<?> linkMsn(
    		HttpServletRequest httpServletRequest,
    		@RequestBody Map<String, Object> map,
    		HttpServletRequest request
    		) throws Exception {
    	
		try {
			map.put("request", request);
			caRequestLogService.linkMsn(map);
		} catch (Exception e) {
			LOG.error(e.getMessage(), e);
			return ResponseEntity.<Object>ok(ResponseDto.<Object>builder().success(false).errorDescription(e.getMessage()).build());
		}
        return ResponseEntity.<Object>ok(ResponseDto.<Object>builder().success(true).build());
    }

    @PostMapping("/api/un-link-msn/{uId}")
    public ResponseEntity<?> unLinkMsn(
    		HttpServletRequest httpServletRequest,
    		@PathVariable final String uId
    		) throws Exception {
    	
		try {
			caRequestLogService.unLinkMsn(uId);
		} catch (Exception e) {
			LOG.error(e.getMessage(), e);
			return ResponseEntity.<Object>ok(ResponseDto.<Object>builder().success(false).errorDescription(e.getMessage()).build());
		}
        return ResponseEntity.<Object>ok(ResponseDto.<Object>builder().success(true).build());
    }
    
    @DeleteMapping("/api/remove-device/{uId}")
    public ResponseEntity<?> removeDevice(
    		HttpServletRequest httpServletRequest,
    		@PathVariable final String uId,
    		@RequestBody final String reason
    		) throws Exception {
    	
		try {
			caRequestLogService.removeDevice(uId, reason);
		} catch (Exception e) {
			LOG.error(e.getMessage(), e);
			return ResponseEntity.<Object>ok(ResponseDto.<Object>builder().success(false).errorDescription(e.getMessage()).build());
		}
        return ResponseEntity.<Object>ok(ResponseDto.<Object>builder().success(true).build());
    }
    
    @PostMapping("/api/device-logs")
    public ResponseEntity<?> getDeviceRemoveLogs(HttpServletResponse response, @RequestBody PaginDto<DeviceRemoveLogDto> pagin) throws IOException {
    	try {
    		caRequestLogService.getDeviceRemoveLogs(pagin);
        } catch (Exception e) {
            return ResponseEntity.<Object>ok(ResponseDto.<Object>builder().success(false).message(e.getMessage()).build());
        }
        return ResponseEntity.<Object>ok(ResponseDto.<Object>builder().success(true).response(pagin).build());
    }
    
    @GetMapping("/api/test-link-msn")
    public ResponseEntity<?> testlinkMsn(
    		HttpServletRequest httpServletRequest,
    		HttpServletRequest request
    		) throws Exception {
    	
		try {
			Map<String, Object> map = SimpleMap.init("sn", "SM02AMX21AAA01AA0390").more("msn", "301002110002");
			map.put("request", request);
			caRequestLogService.linkMsn(map);
		} catch (Exception e) {
			LOG.error(e.getMessage(), e);
			return ResponseEntity.<Object>ok(ResponseDto.<Object>builder().success(false).errorDescription(e.getMessage()).build());
		}
        return ResponseEntity.<Object>ok(ResponseDto.<Object>builder().success(true).build());
    }

    @GetMapping("/api/firm-ware/get/{version}/{hashCode}/{vendor}")
    public ResponseEntity<Object> getFirmware(
            HttpServletRequest httpServletRequest,
            @PathVariable final String version,
            @PathVariable final String hashCode,
            @PathVariable final Long vendor,
            @RequestParam final String fileName) throws Exception {
        
        try {

        	return ResponseEntity.<Object>ok(ResponseDto.<Object>builder().response(evsPAService.getS3URL(vendor, version + "/" + fileName)).success(true).build());
            
        } catch (Exception e) {
            return ResponseEntity.<Object>ok(ResponseDto.<Object>builder().success(false).message(e.getMessage()).build());
        }
    }
    
    @PostMapping("/api/firm-ware/upload/{version}/{hashCode}/{vendor}")
    public ResponseEntity<Object> uploadFirmware(
            HttpServletRequest httpServletRequest,
            @PathVariable final String version,
            @PathVariable final String hashCode,
            @PathVariable final Long vendor,
            @RequestParam(value = "file") final MultipartFile file) throws Exception {
        
        try {

            firmwareService.upload(version, hashCode, vendor, file);
            evsPAService.upload(file.getOriginalFilename(), vendor + "/" + version, hashCode, file.getInputStream());
            
        } catch (Exception e) {
            return ResponseEntity.<Object>ok(ResponseDto.<Object>builder().success(false).message(e.getMessage()).build());
        }
        return ResponseEntity.<Object>ok(ResponseDto.<Object>builder().success(true).build());
    }
    
    @PutMapping("/api/firm-ware/upload/{id}/{version}/{hashCode}/{vendor}")
    public ResponseEntity<Object> editFirmware(
            HttpServletRequest httpServletRequest,
            @PathVariable final Long id,
            @PathVariable final String version,
            @PathVariable final String hashCode,
            @RequestParam(value = "file") final MultipartFile file) throws Exception {
        try {
            firmwareService.editFirmware(id, version, hashCode, file);
        } catch (Exception e) {
            return ResponseEntity.<Object>ok(ResponseDto.<Object>builder().success(false).message(e.getMessage()).build());
        }
        return ResponseEntity.<Object>ok(ResponseDto.<Object>builder().success(true).build());
    }
    
    @PostMapping("/api/firm-wares")
    public ResponseEntity<Object> getUploadedFirmwares(HttpServletRequest httpServletRequest, @RequestBody PaginDto<FirmwareDto> pagin) throws Exception {
        try {
            firmwareService.getUploadedFirmwares(pagin);
        } catch (Exception e) {
            return ResponseEntity.<Object>ok(ResponseDto.<Object>builder().success(false).message(e.getMessage()).build());
        }
        return ResponseEntity.<Object>ok(ResponseDto.<Object>builder().success(true).response(pagin).build());
    }
    
    @DeleteMapping("/api/firm-ware/{id}")
    public ResponseEntity<Object> deleteFirmwares(HttpServletRequest httpServletRequest, @PathVariable final Long id) throws Exception {
        try {
            firmwareService.deleteFirmware(id);
        } catch (Exception e) {
            return ResponseEntity.<Object>ok(ResponseDto.<Object>builder().success(false).message(e.getMessage()).build());
        }
        return ResponseEntity.<Object>ok(ResponseDto.<Object>builder().success(true).build());
    }

    @PostMapping("/api/device-csr/upload/{vendor}")
    public ResponseEntity<Object> uploadDeviceCsr(
            HttpServletRequest httpServletRequest,
            @PathVariable final Long vendor,
            @RequestParam(value = "file") final MultipartFile file) throws Exception {

        try {
            Object error = evsPAService.uploadDeviceCsr(file, vendor);
            if (error != null) {
            	return ResponseEntity.<Object>ok(ResponseDto.<Object>builder().success(false).message("Device validation error!").response(error).build());
            }
        } catch (Exception e) {
            return ResponseEntity.<Object>ok(ResponseDto.<Object>builder().success(false).message(e.getMessage()).build());
        }
        return ResponseEntity.<Object>ok(ResponseDto.<Object>builder().success(true).build());
    }


    @PostMapping("/api/logs")
    public ResponseEntity<Object> getRelatedLogs(HttpServletRequest httpServletRequest, HttpServletResponse response, @RequestBody PaginDto<LogDto> pagin) throws Exception {
        try {

        	if (BooleanUtils.isTrue((Boolean) pagin.getOptions().get("downloadCsv"))) {
        		pagin.setLimit(Integer.MAX_VALUE);
            }

            logService.getRelatedLogs(pagin);

        	if (BooleanUtils.isTrue((Boolean) pagin.getOptions().get("downloadCsv"))) {
        		String timeZone = (String) pagin.getOptions().get("timeZone");
        		if (StringUtils.isNotBlank(timeZone)) {
        			TimeZoneHolder.set(timeZone);
        		}        	

        		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
        		sdf.setTimeZone(TimeZoneHolder.get());
                String tag = sdf.format(new Date());
                String fileName = "log-" + tag + ".csv";
                File file = CsvUtils.writeAlarmsLogCsv(pagin.getResults(), fileName, null);

                try (FileInputStream fis = new FileInputStream(file)) {
                    response.setContentLengthLong(file.length());
                    response.setHeader(HttpHeaders.CONTENT_TYPE, "application/csv");
                    response.setHeader(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, "name");
                    response.setHeader("name", fileName);
                    response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"");
                    IOUtils.copy(fis, response.getOutputStream());
                } finally {
                    FileUtils.deleteDirectory(file.getParentFile());
                }
                TimeZoneHolder.remove();
                return ResponseEntity.<Object>ok(ResponseDto.<Object>builder().success(true).build());
            }
        } catch (Exception e) {
            return ResponseEntity.<Object>ok(ResponseDto.<Object>builder().success(false).message(e.getMessage()).build());
        }
        return ResponseEntity.<Object>ok(ResponseDto.<Object>builder().success(true).response(pagin).build());
    }
    
    @PostMapping("/api/meter/logs")
    public ResponseEntity<Object> getMeterLogs(HttpServletRequest httpServletRequest, @RequestBody Map<String, Object> map) throws Exception {
        try {
            return ResponseEntity.<Object>ok(ResponseDto.<Object>builder().success(true).response(logService.getMeterLog(map)).build());
        } catch (Exception e) {
            return ResponseEntity.<Object>ok(ResponseDto.<Object>builder().success(false).message(e.getMessage()).build());
        }
    }

    @PostMapping("/api/device-groups")
    public ResponseEntity<Object> getDeviceGroups(HttpServletRequest httpServletRequest, @RequestBody PaginDto<GroupDto> pagin) throws Exception {
        try {
            groupService.getGroupDevies(pagin);
        } catch (Exception e) {
            return ResponseEntity.<Object>ok(ResponseDto.<Object>builder().success(false).message(e.getMessage()).build());
        }
        return ResponseEntity.<Object>ok(ResponseDto.<Object>builder().success(true).response(pagin).build());
    }
    
    @PostMapping("/api/device-group")
    public ResponseEntity<Object> addGroupDevice(HttpServletRequest httpServletRequest, @RequestBody GroupDto dto) throws Exception {
        try {
            groupService.addGroupDevice(dto);
        } catch (Exception e) {
            return ResponseEntity.<Object>ok(ResponseDto.<Object>builder().success(false).message(e.getMessage()).build());
        }
        return ResponseEntity.<Object>ok(ResponseDto.<Object>builder().success(true).build());
    }
     
    @DeleteMapping("/api/device-group/{id}")
    public ResponseEntity<Object> deleteGroupDevice(HttpServletRequest httpServletRequest, @PathVariable final Long id) throws Exception {
        try {
            groupService.deleteGroupDevice(id);
        } catch (Exception e) {
            return ResponseEntity.<Object>ok(ResponseDto.<Object>builder().success(false).message(e.getMessage()).build());
        }
        return ResponseEntity.<Object>ok(ResponseDto.<Object>builder().success(true).build());
    }
    
    //http://localhost:7770/api/ping?type=ftpRes&uuid=192.168.1.2&ieiId=MMS-IEI-EVS03-Standby&status=NEW&msn=201906000021&mid=9054
    @GetMapping("/api/ping")
    public ResponseEntity<Object> ping(HttpServletRequest httpServletRequest,
    		@RequestParam(required = true) String type,
    		@RequestParam(required = false) String uuid,
    		@RequestParam(required = false) String msn,
    		@RequestParam(required = false) Long mid,
    		@RequestParam(required = false) String status,
    		@RequestParam(required = false) String fileName,
    		@RequestParam(required = false) String hide,
    		@RequestParam(required = false) String ieiId,
    		@RequestParam(required = false) String location,
    		@RequestParam(required = false) Boolean isEdit,
    		@RequestParam(required = false) Boolean distributeFlag,
    		@RequestParam(required = false) Long logId
    		) throws Exception {
        try {
        	
        	LOG.info("PI Ping0: " + type + ",  " + ieiId + ", " + uuid + ", " + msn + ", " + status + ", " + mid);
            if ("ping".equalsIgnoreCase(type)) {
            	if (StringUtils.isBlank(ieiId)) {
            		return ResponseEntity.<Object>ok(ResponseDto.<Object>builder().success(false).message("ieiId is required").build());
            	}
            	LOG.info("PI Ping1: " + type + ",  " + ieiId + ", " + uuid + ", " + msn + ", " + status + ", " + mid);
            	Pi pi = new Pi();
            	pi.setHide(BooleanUtils.toBoolean(hide));
            	pi.setUuid(uuid);
            	pi.setIeiId(ieiId);
            	pi.setDistributeFlag(distributeFlag == Boolean.TRUE);
            	pi.setLocation(location);
            	evsPAService.ping(pi, isEdit, true);
            } else if ("ftpRes".equalsIgnoreCase(type)) {
            	LOG.info("PI Ping2: " + type + ",  " + ieiId + ", " + msn + ", " + status + ", " + mid);
            	evsPAService.ftpRes(msn, mid, uuid, ieiId, status, fileName, logId);
            }
        } catch (Exception e) {
            return ResponseEntity.<Object>ok(ResponseDto.<Object>builder().success(false).message(e.getMessage()).build());
        }
        return ResponseEntity.<Object>ok(ResponseDto.<Object>builder().success(true).build());
    }
    
	@GetMapping("/api/pis")
	public Object searchPi(HttpServletRequest request) {
		PaginDto<?> pagin = new PaginDto<>();
		pagin.setOffset(request.getParameter("offset"));
		pagin.setLimit(request.getParameter("limit"));
		evsPAService.searchPi(pagin);
		return pagin;
	}
	
	@GetMapping("/api/batch-logs")
	public Object searchBatchLog(HttpServletRequest request) {
		PaginDto<?> pagin = new PaginDto<>();
		pagin.setOffset(request.getParameter("offset"));
		pagin.setLimit(request.getParameter("limit"));
		pagin.setKeyword(request.getParameter("search"));
		pagin.getOptions().put("groupTaskId", (request.getParameter("groupTaskId")));
		evsPAService.searchBatchLog(pagin);
		return pagin;
	}
	
	@PostMapping("/api/batch-logs/search")
    public ResponseEntity<Object> searchBatchLogsByUser(HttpServletRequest httpServletRequest, @RequestBody PaginDto<LogBatchDto> pagin) throws Exception {
        try {
        	evsPAService.searchBatchLogsByUser(pagin);
        } catch (Exception e) {
        	e.printStackTrace();
            return ResponseEntity.ok(ResponseDto.builder().success(false).message(e.getMessage()).build());
        }
        return ResponseEntity.ok(ResponseDto.builder().success(true).response(pagin).build());
    }
	
	@PostMapping("/api/logs/search")
    public ResponseEntity<Object> searchLogsByUser(HttpServletRequest httpServletRequest, @RequestBody PaginDto<LogDto> pagin) throws Exception {
        try {
        	logService.searchLog(pagin);
        } catch (Exception e) {
        	e.printStackTrace();
            return ResponseEntity.ok(ResponseDto.builder().success(false).message(e.getMessage()).build());
        }
        return ResponseEntity.ok(ResponseDto.builder().success(true).response(pagin).build());
    }
	
	@GetMapping("/api/pi/logs")
	public Object searchPiLog(
			@RequestParam(required = true) String msn,
    		@RequestParam(required = true) Long mid,
    		@RequestParam(required = false) Long piId
			) {
		return ResponseEntity.<Object>ok(ResponseDto.<Object>builder().response(evsPAService.searchPiLog(piId, msn, mid)).success(true).build());
	}

    @PostMapping("/api/devices-in-groups")
    public ResponseEntity<Object> getDevicesInGroup(HttpServletRequest httpServletRequest, @RequestBody List<Long> listGroupId) throws Exception {
        PaginDto<CARequestLog> pagin;
        try {
            pagin = caRequestLogService.getDevicesInGroup(listGroupId);
        } catch (Exception e) {
            return ResponseEntity.<Object>ok(ResponseDto.<Object>builder().success(false).message(e.getMessage()).build());
        }
        return ResponseEntity.<Object>ok(ResponseDto.<Object>builder().success(true).response(pagin).build());
    }

    @GetMapping("/api/command-enum")
    public ResponseEntity<List<CommandEnum>> getCommandEnum() {
        List<CommandEnum> commandEnumList = new ArrayList<CommandEnum>(EnumSet.allOf(CommandEnum.class));
        List<CommandEnum> commandEnumResult = new ArrayList();
        for (CommandEnum command : commandEnumList) {
            if(command.isVisible()) {
                commandEnumResult.add(command);
            }
        }

        return ResponseEntity.ok(commandEnumResult);
    }
    
    @GetMapping("/api/file-name/{ieiId}")
    public ResponseEntity<String> getListFileName(@PathVariable(required = false) String ieiId) {
        String fileNamesResult = evsPAService.getFileName(ieiId);        
        return ResponseEntity.ok(fileNamesResult);
    }
    
    @PostMapping("/api/download-meter-file/{fileName}")
    public ResponseEntity<Object> getMeterFile(HttpServletRequest httpServletRequest, HttpServletResponse response, @PathVariable(required = false) String fileName) throws Exception {
	    try {	
	    	File file = evsPAService.getMeterFile(fileName);
	        try (FileInputStream fis = new FileInputStream(file)) {
	            response.setContentLengthLong(file.length());
	            response.setHeader(HttpHeaders.CONTENT_TYPE, "application/octet-stream");
	            response.setHeader(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, "name");
	            response.setHeader("name", file.getName());
	            response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.getName() + "\"");
	            IOUtils.copy(fis, response.getOutputStream());
	        } finally {
	  
	        }
	        return ResponseEntity.<Object>ok(ResponseDto.<Object>builder().success(true).build());
	    } catch (Exception e) {
	        return ResponseEntity.<Object>ok(ResponseDto.<Object>builder().success(false).message(e.getMessage()).build());
	    }
    }
    
    @GetMapping("/api/getMDTMessage")
    public ResponseEntity<Object> getMDTMessage(@RequestParam(required = false) Integer limit, @RequestParam String ieiId, @RequestParam(required = false) String status) {
    	LOG.debug("/api/getMDTMessage: " + ieiId);
    	if (StringUtils.isBlank(ieiId)) {
    		return ResponseEntity.<Object>ok(ResponseDto.<Object>builder().success(false).message("ieiId is required!").build());
    	}
    	try {
    		List<Log> mdtMessages = evsPAService.getMDTMessage(limit, ieiId, status);
    		return ResponseEntity.ok(mdtMessages);
    	} catch (Exception e) {
    		return ResponseEntity.<Object>ok(ResponseDto.<Object>builder().success(false).message(e.getMessage()).build());
    	}
    } 

    @PostMapping("/api/address/upload")
    public ResponseEntity<Object> updateAddress(
            HttpServletRequest httpServletRequest,
            @RequestParam String importType,
            @RequestParam(value = "file") final MultipartFile file, HttpServletResponse response) throws Exception {
        
        try {
        	File csv;
        	if ("DMS".equals(AppCodeSelectedHolder.get())) {
        		csv = CsvUtils.writeImportAddressCsv(dmsAddressService.handleUpload(file, importType), importType, "import_result_" + System.currentTimeMillis() + ".csv");
        	} else {
        		csv = CsvUtils.writeImportAddressCsv(addressService.handleUpload(file, importType), importType, "import_result_" + System.currentTimeMillis() + ".csv");
        	}
        	String fileName = file.getName();
            
            try (FileInputStream fis = new FileInputStream(csv)) {
                response.setContentLengthLong(csv.length());
                response.setHeader(HttpHeaders.CONTENT_TYPE, "application/csv");
                response.setHeader(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, "name");
                response.setHeader("name", fileName);
                response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"");
                IOUtils.copy(fis, response.getOutputStream());
            } finally {
                FileUtils.deleteDirectory(csv.getParentFile());
            }
        } catch (Exception e) {
        	LOG.error(e.getMessage(), e);
            return ResponseEntity.<Object>ok(ResponseDto.<Object>builder().success(false).message(e.getMessage()).build());
        }
        return ResponseEntity.<Object>ok(ResponseDto.<Object>builder().success(true).build());
    }
    
    @GetMapping("/api/vendors")
    public ResponseEntity<Object> getVendors(HttpServletRequest httpServletRequest) throws Exception {
    	return ResponseEntity.<Object>ok(ResponseDto.<Object>builder().response(vendorService.getVendors()).success(true).build());
    }
    
    @PostMapping("/api/address-logs")
    public ResponseEntity<Object> getAddressLogs(HttpServletRequest httpServletRequest, @RequestBody PaginDto<AddressLogDto> pagin) throws Exception {
        try {
            addressLogService.getAddressLogs(pagin);
        } catch (Exception e) {
            return ResponseEntity.<Object>ok(ResponseDto.<Object>builder().success(false).message(e.getMessage()).build());
        }
        return ResponseEntity.<Object>ok(ResponseDto.<Object>builder().success(true).response(pagin).build());
    }
    
    // curl -X POST -H "Content-Type: multipart/form-data" -F "uid=BIERWXAABMAKWAEAAA" -F "type=MMS_P2_TEST" -F "altName=P2_TEST_BIERWXAABMAKWAEAAA" -F "files=@C:/Users/tonyk/Downloads/P2_meter_data.PNG" http://localhost:7770/api/file-upload
    @PostMapping("/api/file-upload")
	public Object fileUpload(@RequestParam MultipartFile files, 
			HttpServletRequest req, HttpServletResponse res,
			@RequestParam(required = false) String type,
			@RequestParam(required = false) String altName,
			@RequestParam(required = false) String desc,
			@RequestParam(required = true) String uid) throws IOException {
    	LOG.debug("Invoke fileUpload uid: {}", uid);
    	fileService.saveFile(new MultipartFile[] {files}, new String[] {type}, new String[] {altName}, new String[] {uid}, new String[] {desc}, req.getParameterValues("replaceByOriginalName"), photoBucketName);
    	return ResponseEntity.<Object>ok(ResponseDto.<Object>builder().success(true).build());
	}
    
    @PostMapping("api/p1-files-upload")
    public ResponseEntity<?> uploadP1Files(HttpServletRequest req, HttpServletResponse res,
    		@RequestParam("files") MultipartFile[] files,
    		@RequestParam(required = false) String[] uid,
    		@RequestParam(required = false) String[] type,
			@RequestParam(required = false) String[] altName,
			@RequestParam(required = false) String[] desc) {
    	
    	LOG.debug("Invoke P1 fileUpload");
    	List<Map<String, String>> errors = new ArrayList<>();
    	try {
            fileService.saveFile(files, 
            		req.getParameterValues("type"), 
            		req.getParameterValues("altName"), 
            		req.getParameterValues("uid"), 
            		req.getParameterValues("desc"),
            		req.getParameterValues("replaceByOriginalName"),
            		p1ProvisioningBucketName);
        } catch (Exception e) {
			e.printStackTrace();
			return ResponseEntity.<Object>ok(ResponseDto.<Object>builder().success(false).message(e.getMessage()).build());
		}
        
        if (!errors.isEmpty()) {
        	return ResponseEntity.<Object>ok(ResponseDto.<Object>builder().success(false).response(errors).build());
        }
        
        return ResponseEntity.<Object>ok(ResponseDto.<Object>builder().success(true).build());
    }
    
    @PostMapping("/api/p1-files")
 	public Object getP1FileUploads(HttpServletResponse res, @RequestBody PaginDto<SFileDto> pagin) throws IOException {
    	try {
    		fileService.getP1Files(pagin);
    	} catch (Exception e) {
            return ResponseEntity.<Object>ok(ResponseDto.<Object>builder().success(false).message(e.getMessage()).build());
        }
    	return ResponseEntity.<Object>ok(ResponseDto.<Object>builder().success(true).response(pagin).build());
 	}
    
    @DeleteMapping("/api/p1-file")
 	public Object deleteP1File(HttpServletResponse res, @RequestParam(required = true) String altName) throws IOException {
    	LOG.debug("Invoke delete P1 file");
    	try {
    		fileService.deleteP1File(altName);
    	} catch (Exception e) {
            return ResponseEntity.<Object>ok(ResponseDto.<Object>builder().success(false).message(e.getMessage()).build());
        }
    	return ResponseEntity.<Object>ok(ResponseDto.<Object>builder().success(true).build());
 	}
    
    // http://localhost:7770/api/files?uids=BIERWXAABMAKWAEAAA,BIERWXAABMAKWAEAAA&types=MMS_P1_TEST,MMS_P2_TEST
    @GetMapping("/api/files")
	public Object getFileUploads(
			HttpServletRequest req, HttpServletResponse res,
			@RequestParam(required = false) String types,// A,B
			@RequestParam(required = false) String altNames,// A,B
			@RequestParam(required = false) String uids// A, B
			) throws IOException {
    	LOG.debug("Invoke getFileUploads uid: {}", uids);
    	return ResponseEntity.<Object>ok(ResponseDto.<Object>builder().success(true).response(fileService.getFiles(types, altNames, uids)).build());
	}
    
    // http://localhost:7770/api/file?id=1
    @GetMapping("/api/file/{uid}/{id}")
	public void downloadFile(HttpServletResponse response, 
			@PathVariable(required = true) Long id,
			@PathVariable(required = true) String uid) throws Exception {
    	fileService.downloadFile(id, uid, response);
	}
    
    // http://localhost:7770/api/file/P2_TEST_BIERWXAABMAKWAEAAA
    @GetMapping("/api/file/{altName}")
	public void downloadFile(HttpServletResponse response, 
			@PathVariable(required = true) String altName) throws Exception {
    	fileService.downloadFile(altName, response);
	}
    
    @PostMapping("/api/submit-meter-commission")
	public ResponseEntity<Object> saveMeterCommissionSubmit(HttpServletResponse response, @RequestBody MeterCommissioningReportDto dto) {
    	try {
    		meterCommissioningReportService.save(dto);
    	} catch (Exception e) {
            return ResponseEntity.<Object>ok(ResponseDto.<Object>builder().success(false).message(e.getMessage()).build());
        }
    	return ResponseEntity.<Object>ok(ResponseDto.<Object>builder().success(true).build());
    }
    
    @PostMapping("/api/bulk-submit-meter-commission")
	public ResponseEntity<Object> saveBulkMeterCommissionSubmit(HttpServletResponse response, @RequestBody List<MeterCommissioningReportDto> dtos) {
    	try {
    		meterCommissioningReportService.save(dtos);
    	} catch (Exception e) {
            return ResponseEntity.<Object>ok(ResponseDto.<Object>builder().success(false).message(e.getMessage()).build());
        }
    	return ResponseEntity.<Object>ok(ResponseDto.<Object>builder().success(true).build());
    }
    
    @PostMapping("/api/bulk-submit-report-ack")
	public ResponseEntity<Object> saveBulkReportAck(HttpServletResponse response, @RequestBody List<P2ReportAckDto> dtos) {
    	try {
    		meterCommissioningReportService.saveP2ReportAck(dtos);
    	} catch (Exception e) {
            return ResponseEntity.<Object>ok(ResponseDto.<Object>builder().success(false).message(e.getMessage()).build());
        }
    	return ResponseEntity.<Object>ok(ResponseDto.<Object>builder().success(true).build());
    }
    
    @PostMapping("/api/bulk-submit-p1-report")
    public ResponseEntity<?> saveBulkP1Report(HttpServletRequest req, HttpServletResponse res,
    		@RequestParam("file") MultipartFile file) {
    	
    	LOG.debug("Invoke P1 p1-report");
    	try {
    		p1ReportService.save(file);
        } catch (Exception e) {
			return ResponseEntity.<Object>ok(ResponseDto.<Object>builder().success(false).message(e.getMessage()).build());
		}
        
        
        return ResponseEntity.<Object>ok(ResponseDto.<Object>builder().success(true).build());
    }
    
    
    @PostMapping("/api/p1_reports")
	public ResponseEntity<Object> getP1Report(HttpServletResponse response, @RequestBody PaginDto<Object> pagin) {
    	try {
    		p1ReportService.search(pagin);
    	} catch (Exception e) {
            return ResponseEntity.<Object>ok(ResponseDto.<Object>builder().success(false).message(e.getMessage()).build());
        }
    	return ResponseEntity.<Object>ok(ResponseDto.<Object>builder().success(true).response(pagin).build());
    }
    
    @DeleteMapping("/api/p1_report/{id}")
	public ResponseEntity<Object> deleteP1Report(HttpServletResponse response, @PathVariable Long id) {
    	try {
    		p1ReportService.deleteP1Report(id);
    	} catch (Exception e) {
            return ResponseEntity.<Object>ok(ResponseDto.<Object>builder().success(false).message(e.getMessage()).build());
        }
    	return ResponseEntity.<Object>ok(ResponseDto.<Object>builder().success(true).build());
    }
    
    @GetMapping("/api/add-device-test/{uid}/{sn}/{msn}")
	public ResponseEntity<Object> addDeviceTest(@PathVariable String uid, @PathVariable String sn, @PathVariable String msn) {
    	try {
    		Optional<CARequestLog> opt = caRequestLogRepository.findByUid(uid);
			CARequestLog caLog = !opt.isPresent() ? new CARequestLog() : opt.get();
			if (caLog.getId() == null) {
				CMD.exec("cp " + csrFolder + "/" + "BIE2IEYAAMAHOABRAA.csr " + csrFolder + "/" + uid + ".csr", null);
				caLog.setSn(sn);
				caLog.setMsn(msn);
			}
			caLog.setUid(uid);
			if (caLog.getStatus() == null) {
				caLog.setStatus(DeviceStatus.OFFLINE);	
			}
			if (caLog.getType() == null) {
				caLog.setType(DeviceType.NOT_COUPLED);	
			}
			caLog.setEnrollmentDatetime(Calendar.getInstance().getTimeInMillis());
			caLog.setRequireRefresh(false);
			caLog.setVendor(caRequestLogRepository.findByUid("BIE2IEYAAMAHOABRAA").get().getVendor());
			caRequestLogRepository.save(caLog);
    	} catch (Exception e) {
            return ResponseEntity.<Object>ok(ResponseDto.<Object>builder().success(false).message(e.getMessage()).build());
        }
    	return ResponseEntity.<Object>ok(ResponseDto.<Object>builder().success(true).build());
    }
    
    @PostMapping("/api/meter-commissions")
	public ResponseEntity<Object> getMeterCommissions(HttpServletResponse response, @RequestBody PaginDto<MeterCommissioningReportDto> pagin) {
    	try {
    		meterCommissioningReportService.search(pagin);
    		
    		if ("true".equalsIgnoreCase(pagin.getOptions().get("exportCSV") + "")) {
    	        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
    	        String tag = sdf.format(new Date());
    	        String fileName = "meter-commissions-reports-" + tag + ".csv";
                File file = CsvUtils.writeMeterCommissionCsv(pagin.getResults(), fileName);
                
                try (FileInputStream fis = new FileInputStream(file)) {
                    response.setContentLengthLong(file.length());
                    response.setHeader(HttpHeaders.CONTENT_TYPE, "application/csv");
                    response.setHeader(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, "name");
                    response.setHeader("name", fileName);
                    response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"");
                    IOUtils.copy(fis, response.getOutputStream());
                } finally {
                    FileUtils.deleteDirectory(file.getParentFile());
                }
            }
    	} catch (Exception e) {
            return ResponseEntity.<Object>ok(ResponseDto.<Object>builder().success(false).message(e.getMessage()).build());
        }
    	return ResponseEntity.<Object>ok(ResponseDto.<Object>builder().success(true).response(pagin).build());
    }
    
    @GetMapping("/api/last-submitted-meter-commission")
	public ResponseEntity<Object> getLastSubmit(HttpServletResponse response, @RequestParam(required = true) String uid, @RequestParam(required = true) String msn) throws Exception {
    	try {
    		MeterCommissioningReportDto dto = meterCommissioningReportService.getLastSubmit(uid, msn);
    		return ResponseEntity.<Object>ok(ResponseDto.<Object>builder().success(true).response(dto).build());
    	} catch (Exception e) {
            return ResponseEntity.<Object>ok(ResponseDto.<Object>builder().success(false).message(e.getMessage()).build());
        }
	}
    
    @GetMapping("/api/p2/job-no/{jobNo}/jobs")
	public ResponseEntity<Object> getP2Jobs(
			HttpServletResponse response, @PathVariable(required = true) String jobNo, 
			@RequestParam(required = false) String hasSubmitReport,
			@RequestParam(required = false) String contractOrder,
			@RequestParam(required = true) String p2Worker,
			@RequestParam(required = false) String msn) throws Exception {
    	try {
    		
    		if ("all".equalsIgnoreCase(jobNo)) {
    			return ResponseEntity.<Object>ok(ResponseDto.<Object>builder().success(true).response(meterCommissioningReportService.getP2Jobs(hasSubmitReport, msn, p2Worker, contractOrder)).build());
    		}
    		if ("NA".equalsIgnoreCase(jobNo)) {
    			jobNo = null;
    		}
    		Object dto = meterCommissioningReportService.getOrNewP2Job(jobNo, hasSubmitReport, p2Worker);
    		return ResponseEntity.<Object>ok(ResponseDto.<Object>builder().success(true).response(dto).build());
    	} catch (Exception e) {
            return ResponseEntity.<Object>ok(ResponseDto.<Object>builder().success(false).message(e.getMessage()).build());
        }
	}
    
    @PostMapping("/api/p2/job-no/{jobNo}/job")
	public ResponseEntity<Object> saveMeterCommissionSubmit(HttpServletResponse response, @RequestBody P2JobDto dto) {
    	try {
    		String createBy = SecurityUtils.getEmail();
    		meterCommissioningReportService.saveP2Job(dto);
    	} catch (Exception e) {
            return ResponseEntity.<Object>ok(ResponseDto.<Object>builder().success(false).message(e.getMessage()).build());
        }
    	return ResponseEntity.<Object>ok(ResponseDto.<Object>builder().success(true).build());
    }
    
    @DeleteMapping("/api/p2/job-no/{jobNo}/job")
	public ResponseEntity<Object> deleteP2Job(HttpServletResponse response, @PathVariable(required = true) String jobNo) {
    	try {
    		meterCommissioningReportService.deleteP2Job(jobNo);
    	} catch (Exception e) {
            return ResponseEntity.<Object>ok(ResponseDto.<Object>builder().success(false).message(e.getMessage()).build());
        }
    	return ResponseEntity.<Object>ok(ResponseDto.<Object>builder().success(true).build());
    }
    
    @PostMapping("/api/p2/{manager}/add-worker")
	public ResponseEntity<Object> addP2Workers(
			HttpServletResponse response, 
			@PathVariable String manager,
			@RequestBody List<String> workers) {
    	try {
    		meterCommissioningReportService.addP2Worker(manager, workers);
    	} catch (Exception e) {
            return ResponseEntity.<Object>ok(ResponseDto.<Object>builder().success(false).message(e.getMessage()).build());
        }
    	return ResponseEntity.<Object>ok(ResponseDto.<Object>builder().success(true).build());
    }
    
    @DeleteMapping("/api/p2/{manager}/delete-worker/{worker}")
	public ResponseEntity<Object> deleteP2Worker(
			HttpServletResponse response, 
			@PathVariable String manager,
			@PathVariable String worker) {
    	try {
    		meterCommissioningReportService.deleteP2Worker(manager, worker);
    	} catch (Exception e) {
            return ResponseEntity.<Object>ok(ResponseDto.<Object>builder().success(false).message(e.getMessage()).build());
        }
    	return ResponseEntity.<Object>ok(ResponseDto.<Object>builder().success(true).build());
    }
    
    @GetMapping("/api/p2/{manager}/get-workers")
	public ResponseEntity<Object> getP2WorkerByManager(
			HttpServletResponse response, 
			@PathVariable String manager) {
    	try {
    		return ResponseEntity.<Object>ok(ResponseDto.<Object>builder().response(meterCommissioningReportService.getP2WorkerByManager(manager)).success(true).build());
    	} catch (Exception e) {
            return ResponseEntity.<Object>ok(ResponseDto.<Object>builder().success(false).message(e.getMessage()).build());
        }
    }
    @GetMapping("/api/p2/get-managers")
	public ResponseEntity<Object> getP2Managers(
			HttpServletResponse response) {
    	try {
    		return ResponseEntity.<Object>ok(ResponseDto.<Object>builder().response(meterCommissioningReportService.getP2Managers()).success(true).build());
    	} catch (Exception e) {
            return ResponseEntity.<Object>ok(ResponseDto.<Object>builder().success(false).message(e.getMessage()).build());
        }
    }
    
    @PostMapping("/api/p1-online-statuses")
	public ResponseEntity<Object> getP1OnlineStatuses(HttpServletResponse response, @RequestBody PaginDto<P1OnlineStatusDto> pagin) {
    	try {
    		p1OnlineStatusService.search(pagin);
    		
    		if ("true".equalsIgnoreCase(pagin.getOptions().get("exportCSV") + "")) {
    	        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
    	        String tag = sdf.format(new Date());
    	        String fileName = "p1-online-status-reports-" + tag + ".csv";
                File file = CsvUtils.writeP1OnlineCsv(pagin.getResults(), fileName);
                
                try (FileInputStream fis = new FileInputStream(file)) {
                    response.setContentLengthLong(file.length());
                    response.setHeader(HttpHeaders.CONTENT_TYPE, "application/csv");
                    response.setHeader(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, "name");
                    response.setHeader("name", fileName);
                    response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"");
                    IOUtils.copy(fis, response.getOutputStream());
                } finally {
                    FileUtils.deleteDirectory(file.getParentFile());
                }
            }
    	} catch (Exception e) {
            return ResponseEntity.<Object>ok(ResponseDto.<Object>builder().success(false).message(e.getMessage()).build());
        }
    	return ResponseEntity.<Object>ok(ResponseDto.<Object>builder().success(true).response(pagin).build());
    }
    
    //curl -X POST -H "Content-Type: multipart/form-data" -F "vendorId=1" -F "signatureAlgorithm=" -F "keyType=" -F "csr=@/home/henry/server/server.csr" -F "prkey=@/home/henry/server/server.key" -H "Authorization: Bearer m3a3ec06e6-d8b7-4e85-aa25-0fb5a3e95ee1" "http://localhost:7770/api/update-vendor-master-key"
	@PostMapping("/api/update-vendor-master-key")
	public ResponseEntity<Object> updateVendorMasterKey(HttpServletRequest req, HttpServletResponse res,
			@RequestParam(required = true) Long vendorId, @RequestParam(required = false) String signatureAlgorithm,
			@RequestParam(required = false) String keyType, @RequestParam MultipartFile csr,
			@RequestParam MultipartFile prkey) {
		File fileCsr = null;
		File fileKey = null;
		try {
			
			if (csr != null) {
				fileCsr = new File(evsDataFolder.replaceAll("\\\\", "/") + "/" + vendorId + "_csrTemp_"
						+ csr.getOriginalFilename());
				try (OutputStream outStream = new FileOutputStream(fileCsr.getPath())) {
					outStream.write(csr.getBytes());
				}
			}
			if (fileCsr != null && StringUtils.isBlank(signatureAlgorithm)) {
				signatureAlgorithm = RSAUtil.getSignatureAlgorithm(fileCsr.getPath().replaceAll("\\\\", "/"));
			}
			
			if (prkey != null) {
				fileKey = new File(evsDataFolder.replaceAll("\\\\", "/") + "/" + vendorId + "_keyTemp_"
						+ prkey.getOriginalFilename());
				try (OutputStream outStream = new FileOutputStream(fileKey.getPath())) {
					outStream.write(prkey.getBytes());
				}
			}
			if (fileKey != null && StringUtils.isBlank(keyType)) {
				keyType = RSAUtil.getKeyType(fileCsr.getPath().replaceAll("\\\\", "/"));
			}
			if (fileCsr != null && fileKey != null && RSAUtil.validateServerKeyAndCsrKey(
					fileKey.getPath().replaceAll("\\\\", "/"), fileCsr.getPath().replaceAll("\\\\", "/"))) {

				vendorService.updateVendorMasterKey(vendorId, signatureAlgorithm, keyType, csr, prkey);
				return ResponseEntity
						.<Object>ok(ResponseDto.<Object>builder().success(true).message("updated successfuly").build());
			} else {
				return ResponseEntity.<Object>ok(ResponseDto.<Object>builder().success(false)
						.message("Error : check not match combo private-key, csr").build());
			}
		} catch (Exception e) {
			return ResponseEntity
					.<Object>ok(ResponseDto.<Object>builder().success(false).message(e.getMessage()).build());
		} finally {
			try {
				Files.deleteIfExists(fileCsr.toPath());
				Files.deleteIfExists(fileKey.toPath());
			} catch (Exception e2) {/**/}
		}
	}
	
	//curl -X POST -H "Content-Type: multipart/form-data" -F "vendorId=1"  -H "Authorization: Bearer m3a3ec06e6-d8b7-4e85-aa25-0fb5a3e95ee1" http://localhost:7770/api/refresh-vendor-certificate
	//curl -X POST -H "Content-Type: multipart/form-data" -F "vendorId=2"  -H "Authorization: Bearer m3a3ec06e6-d8b7-4e85-aa25-0fb5a3e95ee1" http://localhost:7770/api/refresh-vendor-certificate
	//curl -X POST -H "Content-Type: multipart/form-data" -F "vendorId=3"  -H "Authorization: Bearer m3a3ec06e6-d8b7-4e85-aa25-0fb5a3e95ee1" http://localhost:7770/api/refresh-vendor-certificate
	//curl -X POST -H "Content-Type: multipart/form-data" -F "vendorId=4"  -H "Authorization: Bearer m3a3ec06e6-d8b7-4e85-aa25-0fb5a3e95ee1" http://localhost:7770/api/refresh-vendor-certificate
	//curl -X POST -H "Content-Type: multipart/form-data" -F "vendorId=5"  -H "Authorization: Bearer m3a3ec06e6-d8b7-4e85-aa25-0fb5a3e95ee1" http://localhost:7770/api/refresh-vendor-certificate
	@PostMapping("/api/refresh-vendor-certificate")
	public ResponseEntity<Object> refreshVendorCertificate(HttpServletRequest req, HttpServletResponse res,
			@RequestParam(required = true) Long vendorId) {
		try {
			vendorService.refreshVendorCertificate(vendorId);
		} catch (Exception e) {
			return ResponseEntity
					.<Object>ok(ResponseDto.<Object>builder().success(false).message(e.getMessage()).build());
		}
		return ResponseEntity.<Object>ok(ResponseDto.<Object>builder().success(true).build());
	}

	@PostConstruct
	public void init() {
		new Thread(() -> {
			if (StringUtils.isBlank(caFolder)) {
				caFolder = "/home/temp_ca";
			}
			try {
				
				File f = new File(caFolder);
				if (f.exists()) {
					f.delete();
				}
				f.mkdir();
				f = new File(caFolder + '/' + "aw-install.sh");
				if (!f.exists() && f.createNewFile()) {
					IOUtils.copy(Thread.currentThread().getContextClassLoader().getResourceAsStream("aw-install.sh"), new FileOutputStream(f));
				}
			} catch (IOException e) {/**/}
			
			if (!CMD.isWindow()) {
				// CMD.exec("cd " + caFolder + " && sh aw-install.sh", null);
		        try {
		        	LOG.info("Test get S3 {}", evsPAService.getS3URL(null, "pa-meter-2.bin"));
				} catch (Exception e) {
					LOG.error(e.getMessage(), e);
				}
			}
			
			try {
				SettingDto st = settingService.findByKey(SettingService.TIME_CHECK_PI_ONLINE);
				if (st == null || st.getValue() == null || !st.getValue().matches("^[0-9]+$")) {
					settingService.save(SettingDto.builder().key(SettingService.TIME_CHECK_PI_ONLINE).value("30").build());
				}
				
				SettingDto st1 = settingService.findByKey(SettingService.TIME_LOGIN_EXPIRED);
				if (st1 == null || st1.getValue() == null || !st1.getValue().matches("^[0-9]+$")) {
					settingService.save(SettingDto.builder().key(SettingService.TIME_LOGIN_EXPIRED).value("3600").build());
				}
				
				SettingDto st2 = settingService.findByKey(SettingService.EXPORT_ADDRESS_HEADER);
				if (st2 == null) {
					settingService.save(SettingDto.builder().key(SettingService.EXPORT_ADDRESS_HEADER).value("Building,Block,Level,Unit,Postcode,Street Address,State.City,Coupled Meter No.,Coupled MCU SN,UpdatedTime,Remark").build());
				} else {
					AppProps.set(SettingService.EXPORT_ADDRESS_HEADER, st2.getValue());
				}
				settingService.findAll();
			} catch (Exception e) {
				//
			}
			
			SchedulerHelper.scheduleJob("0 1/10 * * * ? *",  evsPAService::updateMissingFileName, "UPDATE_PI_LOG_FILE_NAME");
			
			authenticationService.initDataAuths();
		}).start();
	}
}
