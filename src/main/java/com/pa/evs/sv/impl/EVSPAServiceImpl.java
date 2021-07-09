package com.pa.evs.sv.impl;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.time.Instant;
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
import java.util.concurrent.TimeUnit;

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

import com.amazonaws.ClientConfiguration;
import com.amazonaws.Protocol;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.metrics.AwsSdkMetrics;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pa.evs.model.CARequestLog;
import com.pa.evs.model.Log;
import com.pa.evs.repository.CARequestLogRepository;
import com.pa.evs.repository.LogRepository;
import com.pa.evs.sv.EVSPAService;
import com.pa.evs.sv.MeterService;
import com.pa.evs.utils.ApiUtils;
import com.pa.evs.utils.JFtpClient;
import com.pa.evs.utils.Mqtt;
import com.pa.evs.utils.RSAUtil;
import com.pa.evs.utils.SimpleMap;
import com.pa.evs.utils.ZipUtils;

@Component
@SuppressWarnings("unchecked")
public class EVSPAServiceImpl implements EVSPAService {

	private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(EVSPAServiceImpl.class);
	
	private static final ObjectMapper MAPPER = new ObjectMapper();
	
	private static final TimeZone UTC = TimeZone.getTimeZone("UTC");
	
	private static final int QUALITY_OF_SERVICE = 0;
	
	@Value("${evs.pa.data.folder}")
	private String evsDataFolder;
	
	@Value("${evs.pa.ftp.folder}")
	private String evsFtpFolder;
	
	@Value("${evs.pa.subscribe.send.topic}")
	private String evsPASubscribeTopic;

	@Value("${evs.pa.subscribe.resp.topic}")
	private String evsPARespSubscribeTopic;

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

	@Value("${evs.pa.privatekey.path}")
	private String pkPath;

	@Value("${evs.pa.csr.folder}")
	private String csrFolder;

	@Value("${evs.pa.firmware.version}")
	private String firmwareVersion;

	@Value("${evs.pa.firmware.objectkey}")
	private String firmwareObjectKey;

	@Value("${evs.pa.firmware.hash}")
	private String firmwareHash;

	@Value("${s3.access.expireTime:15}")
	private long expireTime;

	private JFtpClient jftpClient = null;
	
	@Autowired
	private LogRepository logRepository;
	
	@Autowired
	private CARequestLogRepository caRequestLogRepository;
	
	private static final ExecutorService EX = Executors.newFixedThreadPool(10);
	
	@Autowired
	private MeterService meterService;
	
	private AmazonS3Client s3Client = null;
	
	@Value("${s3.bucket.name}")
	private String bucketName;
	
	@Value("${s3.access.id}")
	private String accessID;
	
	@Value("${s3.access.key}")
	private String accessKey;
	
	@Override
	public void publish(String topic, Object message) throws Exception {
		try {
			Mqtt.publish(Mqtt.getInstance(evsPAMQTTAddress), topic, message, QUALITY_OF_SERVICE, false);
			LOG.info("Publish " + topic + " -> " + new ObjectMapper().writeValueAsString(message));
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
	
	private int validateUidAndMsn(Log log) {
		Optional<CARequestLog> opt = caRequestLogRepository.findByUidAndMsn(log.getUid(), log.getMsn());
		if (!opt.isPresent()) {
			LOG.error("Not found binding of msn: {} for uuid: {}", log.getMsn(), log.getUid());
		}
		return opt.isPresent() ? 0 : -1;
	}
	
	private void handleMDT(Map<String, Object> data, String type, Log log, int status) throws Exception {

		if (status == 0) {
			status = validateUidAndMsn(log);
		}
		if (status == 0) {
			//FTP
			ftpUpload(data);
		}
		
		//Publish
		data = new HashMap<>();
		Map<String, Object> header = new HashMap<>();
		data.put("header", header);
		header.put("oid", log.getMid());
		header.put("uid", log.getUid());
		header.put("gid", log.getGid());
		header.put("msn", log.getMsn());
		header.put("status", status);
		publish("evs/pa/" + log.getUid(), data);
		
		//save log
		Map<String, Object> publishData = new HashMap<>(data);
		publishData.put("type", type);
		Log logP = Log.build(publishData, "PUBLISH");
		logP.setMqttAddress(evsPAMQTTAddress);
		logRepository.save(logP);
	}
	
	private void handleRLSRes(Map<String, Object> data, String type, Log log, int status) throws Exception {

		//Publish
		data = new HashMap<>();
		Map<String, Object> header = new HashMap<>();
		data.put("header", header);
		header.put("oid", log.getMid());
		header.put("msn", log.getMsn());
		Map<String, Object> payload = new HashMap<>();
		payload.put("id", log.getMsn());
		payload.put("type", "RLS");
		payload.put("data", status);
		data.put("payload", payload);
		meterService.publish(data);
	}
	
	private void handleINFRes(Map<String, Object> data, String type, Log log, int status) throws Exception {
		// chua validate resp signature
		if (status == 0) {
			Map<String, Object> payload1 = (Map<String, Object>) data.get("payload");
			Map<String, Object> data1 = (Map<String, Object>) payload1.get("data");
			if (firmwareVersion.equals(data1.get("ver"))) {
				status = -1;
			}
		}

		if (status == 0) {
			status = validateUidAndMsn(log);
		}

		if (status == 0) {
			//FTP
			String urlS3 = getS3URL(firmwareObjectKey);
			//Publish
			data = new HashMap<>();
			Map<String, Object> header = new HashMap<>();
			data.put("header", header);
			header.put("mid", log.getMid());
			header.put("uid", log.getUid());
			header.put("gid", log.getGid());
			header.put("msn", log.getMsn());
			
			Map<String, Object> payload = new HashMap<>();
			data.put("payload", payload);
			payload.put("id", log.getUid());
			payload.put("cmd", "OTA");
			payload.put("p1", SimpleMap.init("ver", "1.0.1").more("hash", firmwareHash).more("url", urlS3));
			
			header.put("sig", RSAUtil.initSignedRequest(pkPath, new ObjectMapper().writeValueAsString(payload)));
			publish("evs/pa/" + log.getUid(), data);
			
			//save log
			Map<String, Object> publishData = new HashMap<>(data);
			publishData.put("type", type);
			Log logP = Log.build(publishData, "PUBLISH");
			logP.setMqttAddress(evsPAMQTTAddress);
			logRepository.save(logP);
		}
	}
	
	private void handleOTARes(Map<String, Object> data, String type, Log log, int status) throws Exception {

		Map<String, Object> savehData = new HashMap<>(data);
		savehData.put("type", type);
		Log logP = Log.build(savehData, "PUBLISH");
		logP.setMqttAddress(evsPAMQTTAddress);
		logRepository.save(logP);
	}
	
	private void handleOBR(String type, Log log, int status) throws Exception {

		if(status == 0) {
			status = validateUidAndMsn(log);
		}

		//publish
		Map<String, Object> data = new HashMap<>();
		Map<String, Object> header = new HashMap<>();
		data.put("header", header);
		header.put("oid", log.getMid());
		header.put("uid", log.getUid());
		header.put("gid", log.getGid());
		header.put("msn", log.getMsn());
		header.put("status", status);
		publish("evs/pa/" + log.getUid(), data);

		//wait 5s
		LOG.debug("sleep 5s");
		TimeUnit.SECONDS.sleep(5);
		
		//save log
		Map<String, Object> publishData = new HashMap<>(data);
		publishData.put("type", type);
		Log logP = Log.build(publishData, "PUBLISH");
		logP.setMqttAddress(evsPAMQTTAddress);
		logRepository.save(logP);
		
		if (status == 0) {
			// Send file
			data = new HashMap<>();
			header = new HashMap<>();
			data.put("header", header);
			header.put("mid", log.getMid());
			header.put("uid", log.getUid());
			header.put("gid", log.getGid());
			header.put("msn", log.getMsn());
			//header.put("sig", log.getSig());
			Map<String, Object> payload = new HashMap<>();
			data.put("payload", payload);
			payload.put("id", log.getUid());
			payload.put("cmd", "ACT");
			/*List<String> ca = caRequestLogRepository.findCAByUid(log.getUid());
			payload.put("p1", ca.isEmpty() ? null : ca.get(0));*/
			List<String> svCA = caRequestLogRepository.findCAByUid("server.csr");
			payload.put("p2", svCA.isEmpty() ? null : svCA.get(0));

			String sig = RSAUtil.initSignedRequest(pkPath, new ObjectMapper().writeValueAsString(payload));
			header.put("sig", sig);

			publish("evs/pa/" + log.getUid(), data);
		}
		
		//save log
		publishData = new HashMap<>(data);
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

			Map<String, Object> header = (Map<String, Object>) data.get("header");
			Map<String, Object> payload = (Map<String, Object>) data.get("payload");
			String type = (String) payload.get("type");

			int status = 0;
			boolean verifySign = RSAUtil.verifySign(csrFolder + log.getUid() + ".csr",
					new ObjectMapper().writeValueAsString(payload), (String) header.get("sig"));
			LOG.debug("handleOnSubscribe, type: {}, verifySign: {}", type, verifySign);
			if (!verifySign) {
				status = -1;
			}
			
			if ("MDT".equalsIgnoreCase(type)) {
				handleMDT(data, type, log, status);
			}
			
			if ("OBR".equalsIgnoreCase(type)) {
				handleOBR(type, log, status);
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
			log.setMqttAddress(evsPAMQTTAddress);
			logRepository.save(log);

			//TO-DO: need base on mid of response and mapping with request and update status log to know
			//got response or not
			Map<String, Object> header = (Map<String, Object>) data.get("header");
			Map<String, Object> payload = (Map<String, Object>) data.get("payload");
			String type = log.getPType();

			int status = 0;
			boolean verifySign = RSAUtil.verifySign(csrFolder + log.getUid() + ".csr",
					new ObjectMapper().writeValueAsString(payload), (String) header.get("sig"));
			LOG.debug("HandleOnRespSubscribe, type: {}, verifySign: {}", type, verifySign);
			if (!verifySign) {
				status = -1;
			}
			if ("RLS".equalsIgnoreCase(type)) {
				handleRLSRes(data, type, log, status);
			}
			
			if ("INF".equalsIgnoreCase(type)) {
				handleINFRes(data, type, log, status);
			}

			if ("OTA".equalsIgnoreCase(type)) {
				handleOTARes(data, type, log, status);
			}
			
		} catch (Exception e) {
			LOG.error(e.getMessage(), e);
		}
	}
	
	private void subscribe() {
		//request
		try {
			Mqtt.subscribe(Mqtt.getInstance(evsPAMQTTAddress), evsPASubscribeTopic, QUALITY_OF_SERVICE, o -> {
				final MqttMessage mqttMessage = (MqttMessage) o;
				LOG.info(evsPASubscribeTopic + " -> " + new String(mqttMessage.getPayload()));
				EX.submit(() -> handleOnSubscribe(mqttMessage));
				return null;
			});
		} catch (Exception e) {
			LOG.error(e.getMessage(), e);
		}
		//response
		try {
			Mqtt.subscribe(Mqtt.getInstance(evsPAMQTTAddress), evsPARespSubscribeTopic, QUALITY_OF_SERVICE, o -> {
				final MqttMessage mqttMessage = (MqttMessage) o;
				LOG.info(evsPARespSubscribeTopic + " -> " + new String(mqttMessage.getPayload()));
				EX.submit(() -> handleOnRespSubscribe(mqttMessage));
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
			File root = new File(evsDataFolder);
			if (!root.exists()) {
				root.mkdirs();
			}
			Arrays.asList("", "/IN_CSR", "/OUT_CSR", "/ERR_CSR", "/FTP_LOG")
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
						CARequestLog caLog = !opt.isPresent() ? new CARequestLog() : opt.get();
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
		
		try {
			initS3();
		} catch (Exception e) {/**/}
	}
	
	private void initS3() {
		
		/**
		 * https://842807477657.signin.aws.amazon.com/console
		 * henry/P0wer!2
		 */
		AWSCredentials credentials = new BasicAWSCredentials(accessID, accessKey);
		ClientConfiguration clientconfig = new ClientConfiguration();
        clientconfig.setProtocol(Protocol.HTTPS);
        clientconfig.setConnectionTimeout(2000);
        clientconfig.setConnectionMaxIdleMillis(2000);
        clientconfig.setMaxConnections(3);
        AwsSdkMetrics.disableMetrics();        
        s3Client = new AmazonS3Client(credentials, clientconfig);
        s3Client.setRegion(Region.getRegion(Regions.AP_SOUTHEAST_1));
	}
	
	private String getS3URL(String objectKey) {
		java.util.Date expiration = new java.util.Date();
		long expTimeMillis = Instant.now().toEpochMilli();
		expTimeMillis += 1000 * 60 * expireTime;
		expiration.setTime(expTimeMillis);

		// Generate the presigned URL.
		GeneratePresignedUrlRequest generatePresignedUrlRequest = new GeneratePresignedUrlRequest(bucketName + "/" + firmwareVersion,
				objectKey).withMethod(com.amazonaws.HttpMethod.GET).withExpiration(expiration);
		return s3Client.generatePresignedUrl(generatePresignedUrlRequest).toString();
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
		String pem = (respose.get("cas") + "").replaceAll(".*\"Certificate\": \"(-----BEGIN CERTIFICATE-----[\na-zA-Z0-9=\\\\/+]+-----END CERTIFICATE-----).*", "$1").replace("\\n", "\n");
		
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
		
		/*Mqtt.subscribe(null, "evs/pa/data", QUALITY_OF_SERVICE, o -> {
			MqttMessage mqttMessage = (MqttMessage) o;
			LOG.info("1 -> " + new String(mqttMessage.getPayload()));
			return null;
		});*/
		
		//String json = "{\"header\":{\"mid\":1001,\"uid\":\"BIERWXAABMAB2AEBAA\",\"gid\":\"BIERWXAAA4AFBABABXX\",\"msn\":\"201906000032\",\"sig\":\"Base64(ECC_SIGN(payload))\"},\"payload\":{\"id\":\"BIERWXAABMAB2AEBAA\",\"type\":\"OBR\",\"data\":\"201906000137\"}}";
		//Mqtt.publish("evs/pa/data", new ObjectMapper().readValue(json, Map.class), QUALITY_OF_SERVICE, false);
		//Mqtt.publish("evs/pa/data", new ObjectMapper().readValue(json, Map.class), QUALITY_OF_SERVICE, false);
		//qtt.publish("evs/pa/data", new ObjectMapper().readValue(json, Map.class), QUALITY_OF_SERVICE, false);
		/*Map<String, Object> map = new HashMap<>();
		map.put("id", "BIERWXAABMAGSAEAAA");
		map.put("cmd", "PW1");
		String payload = new ObjectMapper().writeValueAsString(map);
		System.out.println(payload);
		String sig = RSAUtil.initSignedRequest("D://server.key", payload);
		System.out.println(sig);*/

		String json = "{\n" +
				"   \"header\":{\n" +
				"      \"oid\":234001,\n" +
				"      \"uid\":\"BIERWXAAA4AFMACLXX\",\n" +
				"      \"gid\":\"BIERWXAAA4AFBABABXX\",\n" +
				"      \"msn\":\"201906000032\",\n" +
				"      \"sig\":\"Base64(ECC_SIGN(payload))\"\n" +
				"   },\n" +
				"   \"payload\":{\n" +
				"      \"id\":\"BIERWXAAA4AFMACLXX\",\n" +
				"      \"type\":\"INF\",\n" +
				"      \"data\":{\n" +
				"        \"ver\":\"1.0.0\",\n" +
				"        \"rdti\":30,\n" +
				"        \"pdti\":60,\n" +
				"        \"pdtm\":32\n" +
				"      }\n" +
				"   }\n" +
				"}";
		Mqtt.publish("evs/pa/resp", new ObjectMapper().readValue(json, Map.class), QUALITY_OF_SERVICE, false);
	}
}
