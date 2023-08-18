package com.pa.evs.ctrl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

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
import com.pa.evs.dto.FirmwareDto;
import com.pa.evs.dto.GroupDto;
import com.pa.evs.dto.LogBatchDto;
import com.pa.evs.dto.LogDto;
import com.pa.evs.dto.MeterCommissioningReportDto;
import com.pa.evs.dto.PaginDto;
import com.pa.evs.dto.ResponseDto;
import com.pa.evs.enums.CommandEnum;
import com.pa.evs.model.CARequestLog;
import com.pa.evs.model.Log;
import com.pa.evs.model.Pi;
import com.pa.evs.sv.AddressLogService;
import com.pa.evs.sv.AddressService;
import com.pa.evs.sv.CaRequestLogService;
import com.pa.evs.sv.EVSPAService;
import com.pa.evs.sv.FileService;
import com.pa.evs.sv.FirmwareService;
import com.pa.evs.sv.GroupService;
import com.pa.evs.sv.LogService;
import com.pa.evs.sv.MeterCommissioningReportService;
import com.pa.evs.sv.VendorService;
import com.pa.evs.utils.AppProps;
import com.pa.evs.utils.CMD;
import com.pa.evs.utils.CsvUtils;
import com.pa.evs.utils.RSAUtil;
import com.pa.evs.utils.SchedulerHelper;
import com.pa.evs.utils.SimpleMap;
import com.pa.evs.utils.TimeZoneHolder;

@RestController
public class CommonController {

	static final Logger LOG = LogManager.getLogger(CommonController.class);
	
	@Autowired
	ExceptionConvertor exceptionCnvertor;
	
	@Autowired EVSPAService evsPAService;
	
	@Autowired CaRequestLogService caRequestLogService;
	
	@Autowired FirmwareService firmwareService;

	@Autowired LogService logService;
	
	@Autowired GroupService groupService;
	
	@Autowired AddressService addressService;
	
	@Autowired AddressLogService addressLogService;

	@Value("${evs.pa.privatekey.path}")
	private String pkPath;

    @Value("${evs.pa.mqtt.timeout:30}")
	private long otaTimeout;

    @Value("${evs.pa.mqtt.publish.topic.alias}") private String alias;
	
	private String caFolder;

    @Autowired LocalMapStorage localMap;
    
    @Autowired VendorService vendorService;

    @Autowired FileService fileService;
    
    @Autowired MeterCommissioningReportService meterCommissioningReportService;
	
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
                return ResponseEntity.<Object>ok(ResponseDto.builder().success(false).build());
            }
            command.setUid(ca.get().getUid());
            CMD_OPTIONS.set(command.getOptions());

            Long mid = evsPAService.nextvalMID(ca.get().getVendor());
            Map<String, Object> data = command.getData();
            SimpleMap<String, Object> map = SimpleMap.init("id", command.getUid()).more("cmd", command.getCmd());
            if (CommandEnum.CFG.name().equals(command.getCmd())) {
                SimpleMap<String, Object> simpleMap = new SimpleMap<>();
                command.getData().forEach((k,v) -> simpleMap.put(k, Integer.parseInt((String)data.get(k))));
                map.more("p1", simpleMap);
                localMap.getCfgMap().put(mid, command.getData());
            }

            String sig = BooleanUtils.isTrue(ca.get().getVendor().getEmptySig()) ? "" : RSAUtil.initSignedRequest(pkPath, new ObjectMapper().writeValueAsString(map));

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
            evsPAService.publish(alias + command.getUid(), SimpleMap.init(
                    "header", SimpleMap.init("uid", command.getUid()).more("mid", mid).more("gid", command.getUid()).more("msn", ca.get().getMsn()).more("sig", sig)
                ).more(
                    "payload", map
                ), command.getCmd(), command.getBatchId());
            
            Thread.sleep(2000l);
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
    		@PathVariable final String uId
    		) throws Exception {
    	
		try {
			caRequestLogService.removeDevice(uId);
		} catch (Exception e) {
			LOG.error(e.getMessage(), e);
			return ResponseEntity.<Object>ok(ResponseDto.<Object>builder().success(false).errorDescription(e.getMessage()).build());
		}
        return ResponseEntity.<Object>ok(ResponseDto.<Object>builder().success(true).build());
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
            evsPAService.uploadDeviceCsr(file, vendor);
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
        	File csv = CsvUtils.writeImportAddressCsv(addressService.handleUpload(file, importType), importType, "import_result_" + System.currentTimeMillis() + ".csv");
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
	public Object fileUpload(@RequestParam MultipartFile[] files, 
			HttpServletRequest req, HttpServletResponse res,
			@RequestParam(required = false) String type,
			@RequestParam(required = false) String altName,
			@RequestParam(required = false) String desc,
			@RequestParam(required = true) String uid) throws IOException {
    	LOG.debug("Invoke fileUpload uid: {}", uid);
    	fileService.saveFile(files, type, altName, uid, desc);
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
    
	@PostConstruct
	public void init() {
		
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
		
		SchedulerHelper.scheduleJob("0 1/10 * * * ? *",  evsPAService::updateMissingFileName, "UPDATE_PI_LOG_FILE_NAME");
	}
}
