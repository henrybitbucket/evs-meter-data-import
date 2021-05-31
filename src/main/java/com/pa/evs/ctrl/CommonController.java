package com.pa.evs.ctrl;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pa.evs.converter.ExceptionConvertor;
import com.pa.evs.dto.ResponseDto;
import com.pa.evs.sv.CommonService;

@RestController
public class CommonController {

	static final Logger LOG = LogManager.getLogger(CommonController.class);
	
	@Autowired
	ExceptionConvertor exceptionCnvertor;
	
	@Autowired CommonService commonService;
	
    @GetMapping("/api/message/publish")//http://localhost:8080/api/message/publish?topic=a&messageKey=1&message=a
    public ResponseEntity<?> sendMessage(
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
}
