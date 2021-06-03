package com.pa.evs.sv.impl;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pa.evs.model.CARequestLog;
import com.pa.evs.model.Log;
import com.pa.evs.repository.CARequestLogRepository;
import com.pa.evs.repository.LogRepository;
import com.pa.evs.sv.CommonService;
import com.pa.evs.utils.ApiUtils;
import com.pa.evs.utils.JFtpClient;
import com.pa.evs.utils.Mqtt;
import com.pa.evs.utils.ZipUtils;

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

	@Value("${portal.pa.ca.request.url}")
	private String caRequestUrl;
	
	private JFtpClient jftpClient = null;
	
	@Autowired
	private LogRepository logRepository;
	
	@Autowired
	private CARequestLogRepository caRequestLogRepository;
	
	private static final ExecutorService EX = Executors.newFixedThreadPool(10);
	
	@Override
	public void publish(String topic, Object message) throws Exception {
		
		try {
			Mqtt.publish(Mqtt.getInstance(evsPAMQTTAddress), topic, message);
		} catch (Exception e) {
			LOG.error(e.getMessage(), e);
		}
	}
	
	private void ftpUpload(Map<String, Object> src) throws Exception {
		
		SimpleDateFormat sf = new SimpleDateFormat();
		sf.setTimeZone(UTC);
		sf.applyPattern("yyyyMMdd");
		
		Map<String, Object> header = (Map<String, Object>) src.get("header");
		Map<String, Object> payload = (Map<String, Object>) src.get("payload");
		List<Map<String, Object>> data = (List<Map<String, Object>>) payload.get("data");
		
		File file = new File(evsDataFolder + "/FTP_LOG/evsv3ga100_" + header.get("msn") + "_" + sf.format(new Date()) + ".log");
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
	
	private void handleMDT(Map<String, Object> data, String type, Log log) throws Exception {
		
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
		
		//save log
		Map<String, Object> publishData = new HashMap<>(data);
		publishData.put("type", type);
		Log logP = Log.build(publishData, "PUBLISH");
		logP.setMqttAddress(evsPAMQTTAddress);
		logRepository.save(logP);
	}
	
	private void handleOBR(String type, Log log) throws Exception {
		
		//publish
		Map<String, Object> data = new HashMap<>();
		Map<String, Object> header = new HashMap<>();
		data.put("header", header);
		header.put("oid", log.getMid());
		header.put("uid", log.getUid());
		header.put("gid", log.getGid());
		header.put("msn", log.getMsn());
		header.put("status", 0);
		publish("evs/pa/" + log.getUid(), data);
		
		//save log
		Map<String, Object> publishData = new HashMap<>(data);
		publishData.put("type", type);
		Log logP = Log.build(publishData, "PUBLISH");
		logP.setMqttAddress(evsPAMQTTAddress);
		logRepository.save(logP);
		
		// Send file
		data = new HashMap<>();
		header = new HashMap<>();
		data.put("header", header);
		header.put("mid", log.getMid());
		header.put("uid", log.getUid());
		header.put("gid", log.getGid());
		header.put("msn", log.getMsn());
		header.put("sig", log.getSig());
		Map<String, Object> payload = new HashMap<>();
		data.put("payload", payload);
		payload.put("id", log.getUid());
		payload.put("cmd", "ACT");
		
		List<String> svCA = caRequestLogRepository.findCAByUid("server.csr");
		payload.put("p1", svCA.isEmpty() ? null : svCA.get(0));
		List<String> ca = caRequestLogRepository.findCAByUid(log.getUid());
		payload.put("p2", ca.isEmpty() ? null : ca.get(0));
		publish("evs/pa/" + log.getUid(), data);
		
		//save log
		publishData = data;
		publishData.put("type", type);
		logP = Log.build(publishData, "PUBLISH");
		logP.setMqttAddress(evsPAMQTTAddress);
		logRepository.save(logP);
	}
	
	private void handleOnSubscribe(final MqttMessage mqttMessage) {
		try {
			Map<String, Object> data = MAPPER.readValue(mqttMessage.getPayload(), Map.class);
			
			//save log
			Log log = Log.build(data, "SUBSCRIBE");
			log.setMqttAddress(evsPAMQTTAddress);
			logRepository.save(log);
			
			Map<String, Object> payload = (Map<String, Object>) data.get("payload");
			String type = (String) payload.get("type");
			
			if ("MDT".equalsIgnoreCase(type)) {
				handleMDT(data, type, log);
			}
			
			if ("OBR".equalsIgnoreCase(type)) {
				handleOBR(type, log);
			}
			
		} catch (Exception e) {
			LOG.error(e.getMessage(), e);
		}
	}
	
	private void subscribe() {
		try {
			Mqtt.subscribe(Mqtt.getInstance(evsPAMQTTAddress), evsPASubscribeTopic, o -> {
				final MqttMessage mqttMessage = (MqttMessage) o;
				LOG.info(evsPASubscribeTopic + " -> " + new String(mqttMessage.getPayload()));
				EX.submit(() -> handleOnSubscribe(mqttMessage));
				return null;
			});
		} catch (Exception e) {
			LOG.error(e.getMessage(), e);
		}
	}
	

	private void prepareFolder() {
		if (StringUtils.isBlank(evsDataFolder)) {
			evsDataFolder = "/home/evs-data";
		}
		
		try {
			jftpClient = JFtpClient.getInstance(evsFtpHost, evsFtpPort, evsFtpUsername, evsFtpPassword);
		} catch (Exception e) {/**/}
		
		try {
			Arrays.asList("", "/IN_CSR", "/ERR_CSR", "/FTP_LOG")
			.forEach(sf -> {
				File f = new File(evsDataFolder + sf);
				if (!f.exists()) {
					f.mkdir();
				}
			});
		} catch (Exception e) {/**/}
	}
	
	@PostConstruct
	public void init() {
		
		prepareFolder();
		
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

		SchedulerHelper.scheduleJob("0 0/1 * * * ? *", () -> {
			Optional<CARequestLog> opt = caRequestLogRepository.findByUid("server.csr");
			if (!opt.isPresent() || opt.get().getRequireRefresh() == Boolean.TRUE) {
				CARequestLog server = !opt.isPresent() ? new CARequestLog() : opt.get();
				Map<String, Object> data = requestCA(caRequestUrl, new ClassPathResource("sv-ca/server.csr"), null);
				server.setUid((String)data.get("uid"));
				server.setCertificate((String)data.get("pemBase64"));
				server.setMsn((String)data.get("msn"));
				server.setRaw((String)data.get("cas"));
				server.setStartDate((Long)data.get("startDate"));
				server.setEndDate((Long)data.get("endDate"));
				server.setRequireRefresh(false);
				caRequestLogRepository.save(server);
			}
		}, "Server.csr");
		
		SchedulerHelper.scheduleJob("0/1 * * * * ? *", () -> {
			File[] fs = new File(evsDataFolder).listFiles();
			for (File f : fs) {
				if (f.exists() && f.isFile() && f.getName().endsWith(".zip")) {
					ZipUtils.unzip(f.getAbsolutePath(), evsDataFolder + "/IN_CSR");
					File out = new File(evsDataFolder + "/OUT_CSR/" + f.getName() + '.' + System.currentTimeMillis());
					f.renameTo(out);
				}
			}
		}, "UNZIP");
		
		SchedulerHelper.scheduleJob("0/1 * * * * ? *", () -> {
			File f = new File(evsDataFolder + "/IN_CSR");
			for (File csr : f.listFiles()) {
				if (csr.isFile() && csr.getName().endsWith(".csr")) {
					try {
						String uuid = csr.getName().replaceAll("\\.csr$", "");
						Optional<CARequestLog> opt = caRequestLogRepository.findByUid(uuid);
						CARequestLog caLog = opt.isEmpty() ? new CARequestLog() : opt.get();
						Map<String, Object> data = requestCA(caRequestUrl, new FileSystemResource(csr), uuid);
						caLog.setUid(uuid);
						caLog.setCertificate((String)data.get("pemBase64"));
						caLog.setRaw((String)data.get("cas"));
						caLog.setStartDate((Long)data.get("startDate"));
						caLog.setEndDate((Long)data.get("endDate"));
						caLog.setRequireRefresh(false);
						caRequestLogRepository.save(caLog);
						File out = new File(csr.getAbsolutePath().replace("IN_CSR", "OUT_CSR"));
						Files.deleteIfExists(out.toPath());
						csr.renameTo(out);
						LOG.info("Done request CA {}", csr.getName());
					} catch (Exception e) {
						File error = new File(csr.getAbsolutePath().replace("IN_CSR", "ERR_CSR"));
						try {
							Files.deleteIfExists(error.toPath());
						} catch (IOException e1) {/**/}
						csr.renameTo(error);
						LOG.error("Error request CA {}", csr.getName());
						LOG.error(e.getMessage(), e);
					}
					
				}
			}
		}, "REQUEST_CA");
	}
	
	private static Map<String, Object> requestCA(String caRequestUrl, Resource resource, String uuid) {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.MULTIPART_FORM_DATA);
		MultiValueMap<String, Object> data = new LinkedMultiValueMap<>();
		if (StringUtils.isBlank(uuid)) {
			uuid = resource.getFilename();
		}
		data.add("msn", uuid);
		data.add("files", resource);
		HttpEntity<Object> entity = new HttpEntity<>(data, headers);
		Map<String, Object> respose = ApiUtils.getRestTemplate().exchange(caRequestUrl, HttpMethod.POST, entity, Map.class).getBody();
		String pem = (respose.get("cas") + "").replaceAll(".*\"CertificateChain\": \"(-----BEGIN CERTIFICATE-----[\na-zA-Z0-9=\\\\/+]+-----END CERTIFICATE-----).*", "$1").replace("\\n", "\n");
		
		if (!pem.contains("-----BEGIN CERTIFICATE-----")) {
			throw new RuntimeException("CA request ERROR " + uuid + "\n" + respose.get("cas"));
		}
		respose.put("pem", pem);
		respose.put("pemBase64", Base64.getEncoder().encodeToString(pem.getBytes(StandardCharsets.UTF_8)));
		respose.put("uid", uuid);
		return respose;
	}
	
	public static void main(String[] args) throws Exception {
		/**System.out.println(requestCA("http://54.254.171.4:8880/api/evs-ca-request", new ClassPathResource("sv-ca/server.csr"), null));*/
		
		Mqtt.subscribe("evs/pa/BIERWXAABMAB2AEBAA", o -> {
			MqttMessage mqttMessage = (MqttMessage) o;
			LOG.info("1 -> " + new String(mqttMessage.getPayload()));
			return null;
		});
		
		String json = "{\"header\":{\"mid\":1001,\"uid\":\"BIERWXAABMAB2AEBAA\",\"gid\":\"BIERWXAAA4AFBABABXX\",\"msn\":\"201906000032\",\"sig\":\"Base64(ECC_SIGN(payload))\"},\"payload\":{\"id\":\"BIERWXAABMAB2AEBAA\",\"type\":\"OBR\",\"data\":\"201906000137\"}}";
		Mqtt.publish("evs/pa/data", new ObjectMapper().readValue(json, Map.class));
	}
}
