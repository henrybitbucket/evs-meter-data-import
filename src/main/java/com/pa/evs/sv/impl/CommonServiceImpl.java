package com.pa.evs.sv.impl;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.annotation.PostConstruct;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pa.evs.model.Log;
import com.pa.evs.repository.LogRepository;
import com.pa.evs.sv.CommonService;
import com.pa.evs.utils.JFtpClient;
import com.pa.evs.utils.Mqtt;

/**
 * 
 * @author thanh
 *
 */
@Component
@SuppressWarnings("unchecked")
public class CommonServiceImpl implements CommonService {

	private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(CommonServiceImpl.class);
	
	private static final ObjectMapper MAPPER = new ObjectMapper();
	
	private static final TimeZone UTC = TimeZone.getTimeZone("UTC");
	
	@Value("${evs.pa.data.folder}")
	private String evsDataFolder;
	
	@Value("${evs.pa.ftp.folder}")
	private String evsFtpFolder;
	
	@Value("${evs.pa.subscribe.topic}")
	private String evsPASubscribeTopic;
	
	@Value("${evs.pa.mqtt.address}")
	private String evsPAMQTTAddress;
	
	@Value("${evs.pa.ftp.host}")
	private String evsFtpHost;
	
	@Value("${evs.pa.ftp.port}")
	private Integer evsFtpPort;
	
	@Value("${evs.pa.ftp.username}")
	private String evsFtpUsername;
	
	@Value("${evs.pa.ftp.password}")
	private String evsFtpPassword;
	
	private JFtpClient jftpClient = null;
	
	@Autowired
	private LogRepository logRepository;
	
	private static final ExecutorService EX = Executors.newFixedThreadPool(10);
	
	private void ftpUpload(Map<String, Object> src) throws Exception {
		
		SimpleDateFormat sf = new SimpleDateFormat();
		sf.setTimeZone(UTC);
		sf.applyPattern("yyyyMMdd");
		
		Map<String, Object> header = (Map<String, Object>) src.get("header");
		Map<String, Object> payload = (Map<String, Object>) src.get("payload");
		List<Map<String, Object>> data = (List<Map<String, Object>>) payload.get("data");
		
		File file = new File(evsDataFolder + "/evsv3ga100_" + header.get("msn") + "_" + sf.format(new Date()) + ".log");
		if (!file.exists()) {
			Files.createFile(file.toPath());
		}
		
		try (FileOutputStream fos = new FileOutputStream(file)) {
			for (Map<String, Object> o : data) {
				String dt = ((String) o.get("dt")).replace(".000", "") + "Z";
				sf.applyPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");
				Date datetime = sf.parse(dt);
				sf.applyPattern("yyyy-MM-dd HH:mm:ss");
				dt = sf.format(datetime);
				fos.write((dt + "," + o.get("msn") + "," + o.get("kwh") + "," + o.get("kw") + "," + o.get("i") + "," + o.get("v") + "," + o.get("pf") + "\r\n")
				.getBytes(StandardCharsets.UTF_8));
			}
		} finally {/**/}
		
		jftpClient.putFileToPath(file.getAbsolutePath(), evsFtpFolder);
	}
	
	@Override
	public void publish(String topic, Object message) throws Exception {
		
		try {
			Mqtt.publish(Mqtt.getInstance(evsPAMQTTAddress), topic, message);
		} catch (Exception e) {
			LOG.error(e.getMessage(), e);
		}
	}
	
	
	private void subscribe() {
		try {
			Mqtt.subscribe(Mqtt.getInstance(evsPAMQTTAddress), evsPASubscribeTopic, o -> {
				final MqttMessage mqttMessage = (MqttMessage) o;
				LOG.info(evsPASubscribeTopic + " -> " + new String(mqttMessage.getPayload()));
				EX.submit(() -> {
					try {
						Map<String, Object> data = MAPPER.readValue(mqttMessage.getPayload(), Map.class);
						Log log = Log.build(data, "SUBSCRIBE");
						log.setMqttAddress(evsPAMQTTAddress);
						logRepository.save(log);
						
						//FTP
						ftpUpload(data);
						
						//Publish
						data = new HashMap<>();
						Map<String, Object> header = new HashMap<>();
						data.put("header", header);
						header.put("oid", log.getMid());
						header.put("uid", log.getUid());
						header.put("gid", log.getGid());
						header.put("msn", log.getMsn());
						header.put("status", 0);
						publish("evs/pa/" + log.getUid(), data);
						log = Log.build(data, "PUBLISH");
						log.setMqttAddress(evsPAMQTTAddress);
						logRepository.save(log);
					} catch (Exception e) {
						LOG.error(e.getMessage(), e);
					}
				});
				
				return null;
			});
		} catch (Exception e) {
			LOG.error(e.getMessage(), e);
		}
	}
	
	@PostConstruct
	public void init() {
		
		if (StringUtils.isBlank(evsDataFolder)) {
			evsDataFolder = "/home/evs-data";
		}
		
		try {
			jftpClient = JFtpClient.getInstance(evsFtpHost, evsFtpPort, evsFtpUsername, evsFtpPassword);
		} catch (Exception e) {/**/}
		
		try {
			
			File f = new File(evsDataFolder);
			if (!f.exists()) {
				f.mkdir();
			}
			
		} catch (Exception e) {/**/}
		
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
