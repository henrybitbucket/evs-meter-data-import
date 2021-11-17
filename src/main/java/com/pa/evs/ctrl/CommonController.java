package com.pa.evs.ctrl;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
import com.pa.evs.converter.ExceptionConvertor;
import com.pa.evs.dto.Command;
import com.pa.evs.dto.FirmwareDto;
import com.pa.evs.dto.GroupDto;
import com.pa.evs.dto.PaginDto;
import com.pa.evs.dto.ResponseDto;
import com.pa.evs.enums.CommandEnum;
import com.pa.evs.model.CARequestLog;
import com.pa.evs.model.Log;
import com.pa.evs.sv.CaRequestLogService;
import com.pa.evs.sv.EVSPAService;
import com.pa.evs.sv.FirmwareService;
import com.pa.evs.sv.GroupService;
import com.pa.evs.sv.LogService;
import com.pa.evs.utils.CMD;
import com.pa.evs.utils.RSAUtil;
import com.pa.evs.utils.SimpleMap;

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

	@Value("${evs.pa.privatekey.path}")
	private String pkPath;

    @Value("${evs.pa.mqtt.timeout:30}")
	private long otaTimeout;

    @Value("${evs.pa.mqtt.publish.topic.alias}") private String alias;
	
	private String caFolder;
	
	public static final Map<Object, String> MID_TYPE = new LinkedHashMap<>();
	
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
            if(!ca.isPresent()) {
                return ResponseEntity.<Object>ok(ResponseDto.builder().success(false).build());
            }

            Map<String, Object> data = command.getData();
            SimpleMap<String, Object> map = SimpleMap.init("id", command.getUid()).more("cmd", command.getCmd());
            if (CommandEnum.CFG.name().equals(command.getCmd())) {
                SimpleMap<String, Object> simpleMap = new SimpleMap<>();
                command.getData().forEach((k,v) -> simpleMap.put(k, Integer.parseInt((String)data.get(k))));
                map.more("p1", simpleMap);
            }

            String sig = RSAUtil.initSignedRequest(pkPath, new ObjectMapper().writeValueAsString(map));

            Object mid = evsPAService.nextvalMID();
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

            evsPAService.publish(alias + command.getUid(), SimpleMap.init(
                    "header", SimpleMap.init("uid", command.getUid()).more("mid", mid).more("gid", command.getUid()).more("msn", ca.get().getMsn()).more("sig", sig)
                ).more(
                    "payload", map
                ), command.getCmd());
            
            Thread.sleep(2000l);
            return ResponseEntity.ok(ResponseDto.builder().success(true).response(mid).build());
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
            return ResponseEntity.<Object>ok(ResponseDto.builder().success(false).message(e.getMessage()).build());
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
    
    @PostMapping("/api/firm-ware/upload/{version}/{hashCode}")
    public ResponseEntity<Object> uploadFirmware(
            HttpServletRequest httpServletRequest,
            @PathVariable final String version,
            @PathVariable final String hashCode,
            @RequestParam(value = "file") final MultipartFile file) throws Exception {
        
        try {

            firmwareService.upload(version, hashCode, file);
            evsPAService.upload(file.getOriginalFilename(), version, hashCode, file.getInputStream());
            
        } catch (Exception e) {
            return ResponseEntity.<Object>ok(ResponseDto.<Object>builder().success(false).message(e.getMessage()).build());
        }
        return ResponseEntity.<Object>ok(ResponseDto.<Object>builder().success(true).build());
    }
    
    @PutMapping("/api/firm-ware/upload/{id}/{version}/{hashCode}")
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

    @PostMapping("/api/device-csr/upload")
    public ResponseEntity<Object> uploadDeviceCsr(
            HttpServletRequest httpServletRequest,
            @RequestParam(value = "file") final MultipartFile file) throws Exception {

        try {
            evsPAService.uploadDeviceCsr(file);
        } catch (Exception e) {
            return ResponseEntity.<Object>ok(ResponseDto.<Object>builder().success(false).message(e.getMessage()).build());
        }
        return ResponseEntity.<Object>ok(ResponseDto.<Object>builder().success(true).build());
    }


    @PostMapping("/api/logs")
    public ResponseEntity<Object> getRelatedLogs(HttpServletRequest httpServletRequest, @RequestBody PaginDto<Log> pagin) throws Exception {
        try {
            logService.getRelatedLogs(pagin);
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
			CMD.exec("cd " + caFolder + " && sh aw-install.sh", null);
	        try {
	        	LOG.info("Test get S3 {}", evsPAService.getS3URL("pa-meter-2.bin"));
			} catch (Exception e) {
				LOG.error(e.getMessage(), e);
			}
		}
	}
}
