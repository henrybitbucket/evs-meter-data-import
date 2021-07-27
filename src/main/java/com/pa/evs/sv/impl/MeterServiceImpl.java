package com.pa.evs.sv.impl;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.TimeZone;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;

import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pa.evs.model.CARequestLog;
import com.pa.evs.model.Log;
import com.pa.evs.repository.CARequestLogRepository;
import com.pa.evs.repository.LogRepository;
import com.pa.evs.sv.EVSPAService;
import com.pa.evs.sv.MeterService;
import com.pa.evs.utils.Mqtt;
import com.pa.evs.utils.RSAUtil;
import com.pa.evs.utils.SimpleMap;

@Component
@SuppressWarnings("unchecked")
public class MeterServiceImpl implements MeterService {

	private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(MeterServiceImpl.class);
	
	private static final ObjectMapper MAPPER = new ObjectMapper();
	
	static final TimeZone UTC = TimeZone.getTimeZone("UTC");
	
	private static final int QUALITY_OF_SERVICE = 0;
	
	@Value("${evs.meter.subscribe.send.topic}")
	private String evsMeterSubscribeTopic;

	@Value("${evs.meter.subscribe.resp.topic}")
	private String evsMeterRespSubscribeTopic;

	@Value("${evs.meter.mqtt.address}")
	private String evsMeterMQTTAddress;
	
	@Value("${evs.pa.privatekey.path}")
	private String pkPath;

	@Value("${evs.pa.csr.folder}")
	private String csrFolder;
	
	@Autowired
	private LogRepository logRepository;
	
	@Autowired
	private CARequestLogRepository caRequestLogRepository;
	
	@Autowired
	private EVSPAService evsPAService;
	
	private static final ExecutorService EX = Executors.newFixedThreadPool(10);
	
	@SuppressWarnings("rawtypes")
	@Override
	public void publish(String topic, Object message, String type) throws Exception {
		try {
			Mqtt.publish(Mqtt.getInstance(evsMeterMQTTAddress), topic, message, QUALITY_OF_SERVICE, false);
			LOG.info("Publish " + topic + " -> " + new ObjectMapper().writeValueAsString(message));
			
			//wait 5s
			LOG.debug("sleep 5s");
			TimeUnit.SECONDS.sleep(5);
			
			//save log
			Map<String, Object> publishData = new HashMap<>((Map) message);
			publishData.put("type", type);
			Log logP = Log.build(publishData, "PUBLISH");
			logP.setTopic(topic);
			logP.setMqttAddress(evsMeterMQTTAddress);
			logRepository.save(logP);
		} catch (Exception e) {
			LOG.error(e.getMessage(), e);
		}
	}
	
	@SuppressWarnings("rawtypes")
	@Override
	public void publish(Object message, String type) throws Exception {
		try {
			Mqtt.publish(Mqtt.getInstance(evsMeterMQTTAddress), evsMeterRespSubscribeTopic, message, QUALITY_OF_SERVICE, false);
			LOG.info("Publish " + evsMeterRespSubscribeTopic + " -> " + new ObjectMapper().writeValueAsString(message));
			
			//wait 5s
			LOG.debug("sleep 5s");
			TimeUnit.SECONDS.sleep(5);
			
			//save log
			Map<String, Object> publishData = new HashMap<>((Map) message);
			publishData.put("type", type);
			Log logP = Log.build(publishData, "PUBLISH");
			logP.setTopic(evsMeterRespSubscribeTopic);
			logP.setMqttAddress(evsMeterMQTTAddress);
			logRepository.save(logP);
		} catch (Exception e) {
			LOG.error(e.getMessage(), e);
		}
	}
	
	private void handleRLS(String type, Log log, int status) throws Exception {

		//publish
		Optional<CARequestLog> ca = caRequestLogRepository.findOneByMsn(log.getMsn());
		if (!ca.isPresent()) {
			status = -1;
		} else {
			log.setUid(ca.get().getUid());
		}
		
		String payload = new ObjectMapper().writeValueAsString(SimpleMap.init("id", log.getUid()).more("cmd", "RLS"));
		String sig = RSAUtil.initSignedRequest(pkPath, payload);
		
		Map<String, Object> data = SimpleMap.init(
				"header", SimpleMap.init("uid", log.getUid()).more("mid", log.getMid()).more("gid", log.getUid()).more("msn", log.getMsn()).more("sig", sig)
			).more(
				"payload", SimpleMap.init("id", log.getUid()).more("cmd", log.getUid())
			);
		
		evsPAService.publish("evs/pa/" + log.getUid(), data, "RLS");
	}
	
	private void handleOnSubscribe(final MqttMessage mqttMessage) {
		try {
			Map<String, Object> data = MAPPER.readValue(mqttMessage.getPayload(), Map.class);
			
			//save log
			Log log = Log.build(data, "SUBSCRIBE");
			log.setMqttAddress(evsMeterMQTTAddress);
			logRepository.save(log);

			String type = log.getPType();
			
			if ("RLS".equalsIgnoreCase(type)) {
				handleRLS(type, log, 0);
			}
			
		} catch (Exception e) {
			LOG.error(e.getMessage(), e);
		}
	}

	private void handleOnRespSubscribe(final MqttMessage mqttMessage) {
		try {
			Map<String, Object> data = MAPPER.readValue(mqttMessage.getPayload(), Map.class);
			//save log
			Log log = Log.build(data, "SUBSCRIBE");
			log.setMqttAddress(evsMeterMQTTAddress);
			logRepository.save(log);

		} catch (Exception e) {
			LOG.error(e.getMessage(), e);
		}
	}
	
	private void subscribe() {
		//request
		try {
			Mqtt.subscribe(Mqtt.getInstance(evsMeterMQTTAddress), evsMeterSubscribeTopic, QUALITY_OF_SERVICE, o -> {
				final MqttMessage mqttMessage = (MqttMessage) o;
				LOG.info(evsMeterSubscribeTopic + " -> " + new String(mqttMessage.getPayload()));
				EX.submit(() -> handleOnSubscribe(mqttMessage));
				return null;
			});
		} catch (Exception e) {
			LOG.error(e.getMessage(), e);
		}
		//response
		try {
			Mqtt.subscribe(Mqtt.getInstance(evsMeterMQTTAddress), evsMeterRespSubscribeTopic, QUALITY_OF_SERVICE, o -> {
				final MqttMessage mqttMessage = (MqttMessage) o;
				LOG.info(evsMeterRespSubscribeTopic + " -> " + new String(mqttMessage.getPayload()));
				EX.submit(() -> handleOnRespSubscribe(mqttMessage));
				return null;
			});
		} catch (Exception e) {
			LOG.error(e.getMessage(), e);
		}
	}
	
	@PostConstruct
	public void init() {
		
		try {
			subscribe();
			new Timer().schedule(new TimerTask() {
				@Override
				public void run() {
					subscribe();
				}
			}, 5 * 60l * 1000l);
		} catch (Exception e) {
			LOG.error(e.getMessage(), e);
		}
	}
}
