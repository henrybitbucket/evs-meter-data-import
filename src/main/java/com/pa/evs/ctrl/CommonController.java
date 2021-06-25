package com.pa.evs.ctrl;

import java.util.Map;
import java.util.Optional;

import javax.servlet.http.HttpServletRequest;

import com.pa.evs.utils.RSAUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pa.evs.converter.ExceptionConvertor;
import com.pa.evs.dto.Command;
import com.pa.evs.dto.ResponseDto;
import com.pa.evs.model.CARequestLog;
import com.pa.evs.sv.CaRequestLogService;
import com.pa.evs.sv.CommonService;
import com.pa.evs.utils.SimpleMap;

@RestController
public class CommonController {

	static final Logger LOG = LogManager.getLogger(CommonController.class);
	
	@Autowired
	ExceptionConvertor exceptionCnvertor;
	
	@Autowired CommonService commonService;
	
	@Autowired CaRequestLogService caRequestLogService;

	@Value("${evs.pa.privatekey.path}")
	private String pkPath;
	
    @GetMapping("/api/message/publish")//http://localhost:8080/api/message/publish?topic=a&messageKey=1&message=a
    public ResponseEntity<?> sendGMessage(
    		HttpServletRequest httpServletRequest,
    		@RequestParam String topic,
    		@RequestParam String messageKey,
    		@RequestParam String message
    		) throws Exception {
    	
    	/**Mqtt.publish(topic, new Payload<>(messageKey, message), 2, true);*/
    	String json = "{\"header\":{\"mid\":8985,\"uid\":\"BIERWXAABMADGAFHAA\",\"msn\":\"201906000021\",\"sig\":\"3065023100E63D9474849F426557A5367E208B093B3510003B395A0EEBD49FC44EA32F45C582E3B3D55B22FE001B45EFDB6FCFCA7802307D5709A727AB075667FBFAFB5EE8FEC1BADEF6872FE0D811A1900AB86F71DACCBC64CF1ECAFA0F21ABEA197F5BC124B1\"},\"payload\":{\"id\":\"BIERWXAABMADGAFHAA\",\"type\":\"MDT\",\"data\":[{\"uid\":\"BIERWXAABMADGAFHAA\",\"msn\":\"201906000021\",\"kwh\":\"0.0\",\"kw\":\"0.0\",\"i\":\"0.0\",\"v\":\"244.6\",\"pf\":\"10.0\",\"dt\":\"2021-05-31T13:26:51\"}]}}";
    	commonService.publish("evs/pa/data", new ObjectMapper().readValue(json, Map.class));
        return ResponseEntity.<Object>ok(ResponseDto.<Object>builder().success(true).build());
    }
    
    @PostMapping("/api/message/publish")//http://localhost:8080/api/message/publish
    public ResponseEntity<?> sendPMessage(
    		HttpServletRequest httpServletRequest,
    		@RequestBody Map<String, Object> json1
    		) throws Exception {
    	
    	String json = "{\"header\":{\"mid\":1001,\"uid\":\"BIERWXAABMAB2AEBAA\",\"gid\":\"BIERWXAAA4AFBABABXX\",\"msn\":\"201906000032\",\"sig\":\"Base64(ECC_SIGN(payload))\"},\"payload\":{\"id\":\"BIERWXAABMAB2AEBAA\",\"type\":\"OBR\",\"data\":\"201906000137\"}}";
    	commonService.publish("evs/pa/data", new ObjectMapper().readValue(json, Map.class));
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
				return ResponseEntity.<Object>ok(ResponseDto.<Object>builder().success(false).build());
			}
			String payload = new ObjectMapper().writeValueAsString(SimpleMap.init("id", command.getUid()).more("cmd", command.getCmd()));
			String sig = RSAUtil.initSignedRequest(pkPath, payload);
			commonService.publish("evs/pa/" + command.getUid(), SimpleMap.init(
					"header", SimpleMap.init("uid", command.getUid()).more("mid", 234004).more("gid", command.getUid()).more("msn", ca.get().getMsn()).more("sig", sig)
				).more(
					"payload", SimpleMap.init("id", command.getUid()).more("cmd", command.getCmd())
				));
		} catch (Exception e) {
			LOG.error(e.getMessage(), e);
		}
        return ResponseEntity.<Object>ok(ResponseDto.<Object>builder().success(true).build());
    }
    
    @PostMapping("/api/link-msn")
    public ResponseEntity<?> linkMsn(
    		HttpServletRequest httpServletRequest,
    		@RequestBody Map<String, Object> map
    		) throws Exception {
    	
		try {
			caRequestLogService.linkMsn(map);
		} catch (Exception e) {
			LOG.error(e.getMessage(), e);
		}
        return ResponseEntity.<Object>ok(ResponseDto.<Object>builder().success(true).build());
    }
}
