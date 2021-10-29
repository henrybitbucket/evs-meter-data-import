package com.pa.evs.sv.impl;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.ClientConfiguration;
import com.amazonaws.Protocol;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.http.IdleConnectionReaper;
import com.amazonaws.metrics.AwsSdkMetrics;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pa.evs.ctrl.CommonController;
import com.pa.evs.enums.DeviceStatus;
import com.pa.evs.model.CARequestLog;
import com.pa.evs.model.Log;
import com.pa.evs.model.MeterLog;
import com.pa.evs.repository.CARequestLogRepository;
import com.pa.evs.repository.LogRepository;
import com.pa.evs.repository.MeterLogRepository;
import com.pa.evs.sv.CaRequestLogService;
import com.pa.evs.sv.EVSPAService;
import com.pa.evs.sv.FirmwareService;
import com.pa.evs.sv.LogService;
import com.pa.evs.utils.ApiUtils;
import com.pa.evs.utils.CMD;
import com.pa.evs.utils.Mqtt;
import com.pa.evs.utils.RSAUtil;
import com.pa.evs.utils.SchedulerHelper;
import com.pa.evs.utils.SimpleMap;
import com.pa.evs.utils.ZipUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PostConstruct;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TimeZone;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Component
@SuppressWarnings("unchecked")
public class EVSPAServiceImpl implements EVSPAService {

	private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(EVSPAServiceImpl.class);

	private Map<Long, Long> localMap;
	private Map<Long, Long> onboardingMap;
	
	private static final ObjectMapper MAPPER = new ObjectMapper();
	
	private static final TimeZone UTC = TimeZone.getTimeZone("UTC");
	
	private static final int QUALITY_OF_SERVICE = 0;

	@Autowired LogService logService;

	@Autowired CaRequestLogService caRequestLogService;

	@Autowired private LogRepository logRepository;

	@Autowired private CARequestLogRepository caRequestLogRepository;

	@Autowired private FirmwareService firmwareService;

	@Autowired private MeterLogRepository meterLogRepository;
	
	@Value("${evs.pa.data.folder}") private String evsDataFolder;
	
	@Value("${evs.pa.subscribe.send.topic}") private String evsPASubscribeTopic;

	@Value("${evs.pa.subscribe.resp.topic}") private String evsPARespSubscribeTopic;

    @Value("${evs.pa.local.subscribe.send.topic}") private String evsMeterLocalSubscribeTopic;

    @Value("${evs.pa.local.subscribe.resp.topic}") private String evsMeterLocalRespSubscribeTopic;

	@Value("${evs.pa.local.data.send.topic}") private String evsMeterLocalDataSendTopic;

	//@Value("${evs.pa.local.data.resp.topic}") private String evsMeterLocalDataRespTopic;

	@Value("${evs.pa.mqtt.address}") private static String evsPAMQTTAddress;

	@Value("${evs.pa.mqtt.client.id}") private static String mqttClientId;

	@Value("${evs.pa.mqtt.publish.topic.alias}") private String alias;
	
	@Value("${portal.pa.ca.request.url}") private String caRequestUrl;

	@Value("${evs.pa.privatekey.path}") private String pkPath;

	@Value("${evs.pa.master.privatekey.path}") private String masterPkPath;

	@Value("${evs.pa.csr.folder}") private String csrFolder;

	@Value("${s3.access.expireTime:15}") private long expireTime;

	@Value("${evs.pa.validateSign:true}") private boolean validateSign;

	@Value("${s3.bucket.name}") private String bucketName;

	@Value("${s3.access.id}") private String accessID;

	@Value("${s3.access.key}") private String accessKey;

	private static final ExecutorService EX = Executors.newFixedThreadPool(10);
	
	private AmazonS3Client s3Client = null;
	
	@Override
	public void uploadDeviceCsr(MultipartFile file) {
		
		File f = new File(evsDataFolder + "/" + file.getOriginalFilename());
		try {
			Files.deleteIfExists(f.toPath());
			Files.createFile(f.toPath());
			IOUtils.copy(file.getInputStream(), new FileOutputStream(f));
		} catch (IOException e) {
			LOG.error(e.getMessage(), e);
		}
	}

	@SuppressWarnings("rawtypes")
	@Override
	public void publish(String topic, Object message, String type) throws Exception {
		try {
			Mqtt.publish(Mqtt.getInstance(evsPAMQTTAddress, mqttClientId), topic, message, QUALITY_OF_SERVICE, false);
			LOG.info("Publish " + topic + " -> " + new ObjectMapper().writeValueAsString(message));
			
			//wait 5s
			LOG.debug("sleep 5s");
			TimeUnit.SECONDS.sleep(5);
			
			//save log
			Map<String, Object> publishData = new HashMap<>((Map) message);
			publishData.put("type", type);
			Log logP = Log.build(publishData, "PUBLISH");
			logP.setTopic(topic);
			logP.setMqttAddress(evsPAMQTTAddress);
			logRepository.save(logP);
		} catch (Exception e) {
			LOG.error(e.getMessage(), e);
		}
	}

	private void publish(String topic, Object message) {
		try {
			Mqtt.publish(Mqtt.getInstance(evsPAMQTTAddress, mqttClientId), topic, message, QUALITY_OF_SERVICE, false);
			LOG.info("Publish " + topic + " -> " + new ObjectMapper().writeValueAsString(message));

			//wait 5s
			LOG.debug("sleep 5s");
			TimeUnit.SECONDS.sleep(5);

			//save log
			Map<String, Object> publishData = new HashMap<>((Map) message);
			Log logP = Log.build(publishData, "PUBLISH");
			logP.setTopic(topic);
			logP.setMqttAddress(evsPAMQTTAddress);
			logRepository.save(logP);
		} catch (Exception e) {
			LOG.error(e.getMessage(), e);
		}
	}
	
	private void sendToMeterClient(Map<String, Object> src, String type) throws Exception {

		SimpleDateFormat sf = new SimpleDateFormat();
		sf.setTimeZone(UTC);
		sf.applyPattern("yyyyMMdd");

		Map<String, Object> payload = (Map<String, Object>) src.get("payload");
		List<Map<String, Object>> data = (List<Map<String, Object>>) payload.get("data");

		for (Map<String, Object> o : data) {
			String dt = ((String) o.get("dt")).replace(".000", "") + "Z";
			sf.applyPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");
			Date datetime = sf.parse(dt);
			
			sf.applyPattern("yyyy-MM-dd HH:mm:ss");

			sf.applyPattern("yyyyMMdd");
			Map<String, Object> tmp = new HashMap<>(o);
			tmp.put("uid", payload.get("id"));
			tmp.put("dt", datetime.getTime());
			tmp.put("dtd", datetime);
			tmp.put("dtn", Integer.parseInt(sf.format(datetime)));
			meterLogRepository.save(MeterLog.build(tmp));
		}
		
		publish(evsMeterLocalDataSendTopic, src, type);
	}

	@Override
	public Long nextvalMID() {
		Number mid = logRepository.nextvalMID().longValue();
		// TC Module mid (message id) format is uint32, the max number is 4294967295
		if (mid.longValue() >= 4294967295l) {
			logRepository.nextvalMID(10000l);
			mid = logRepository.nextvalMID().longValue();
		}
		return mid.longValue();
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
			sendToMeterClient(data, type);
		}

		updateLastSubscribe(log);

		//Publish
		data = new HashMap<>();
		Map<String, Object> header = new HashMap<>();
		data.put("header", header);
		header.put("oid", log.getMid());
		header.put("uid", log.getUid());
		header.put("gid", log.getGid());
		header.put("msn", log.getMsn());
		header.put("status", status);
		publish(alias + log.getUid(), data, type);

	}
	
	private void handleRLSRes(Map<String, Object> data, String type, Log log) throws Exception {
		//Publish
		if (localMap.get(log.getOid()) != null) {
			Map<String, Object> header = (Map<String, Object>) data.get("header");
			Map<String, Object> payload = (Map<String, Object>) data.get("payload");
			header.put("oid", localMap.get(log.getOid()));
			header.remove("sig");
			header.remove("uid");
			payload.put("id", header.get("msn"));
			publish(evsMeterLocalRespSubscribeTopic, data, type);
			localMap.remove(log.getOid());
		}
	}
	
	private void handleINFRes(Map<String, Object> data, String type, Log log, int status) throws Exception {
		// chua validate resp signature
		if (status == 0) {
			Map<String, Object> payload1 = (Map<String, Object>) data.get("payload");
			Map<String, Object> data1 = (Map<String, Object>) payload1.get("data");
			if (firmwareService.getLatestFirmware().getVersion().equals(data1.get("ver"))) {
				status = -1;
			}
			if (data1.get("ver") != null) {
				log.setVer(data1.get("ver") + "");
			}
			if ("TCM_INFO".equalsIgnoreCase(CommonController.MID_TYPE.get(log.getOid()))) {
				LOG.debug("GET TCM_INFO: " + log.getOid() + " " + log.getMsn());
				Optional<CARequestLog> opt = caRequestLogRepository.findByUidAndMsn(log.getUid() + "", log.getMsn());
				if (opt.isPresent()) {
					opt.get().setVer(log.getVer());
					caRequestLogRepository.save(opt.get());
				}
				CommonController.MID_TYPE.remove(log.getOid());
				return;
			}
		}

		if (status == 0) {
			status = validateUidAndMsn(log);
		}

		if (status == 0) {
			LOG.debug("sleep 15s");
			TimeUnit.SECONDS.sleep(15);
			String urlS3 = getS3URL(firmwareService.getLatestFirmware().getFileName());
			if (log.getMid() == null) {
				log.setMid(nextvalMID());
			}
			//Publish
			data = new HashMap<>();
			Map<String, Object> header = new HashMap<>();
			data.put("header", header);
			header.put("mid", log.getMid());
			header.put("uid", log.getUid());
			header.put("gid", log.getUid());
			header.put("msn", log.getMsn());
			
			Map<String, Object> payload = new HashMap<>();
			data.put("payload", payload);
			payload.put("id", log.getUid());
			payload.put("cmd", "OTA");
			payload.put("p1", SimpleMap.init("ver", firmwareService.getLatestFirmware().getVersion()).more("hash",
					firmwareService.getLatestFirmware().getHashCode()).more("url", urlS3));
			
			header.put("sig", RSAUtil.initSignedRequest(pkPath, new ObjectMapper().writeValueAsString(payload)));
			publish(alias + log.getUid(), data, type);
		}
	}
	
	private void handleOTARes(Map<String, Object> data, String type, Log log, int status) throws Exception {

		/**Map<String, Object> savehData = new HashMap<>(data);
		savehData.put("type", type);
		Log logP = Log.build(savehData, "PUBLISH");
		logP.setMqttAddress(evsPAMQTTAddress);
		logRepository.save(logP);*/
	}
	
	private void handleOBR(String type, Log log, int status) throws Exception {

		if(status == 0) {
			status = validateUidAndMsn(log);
		}
		updateLastSubscribe(log);

		//publish
		Map<String, Object> data = new HashMap<>();
		Map<String, Object> header = new HashMap<>();
		data.put("header", header);
		header.put("oid", log.getMid());
		header.put("uid", log.getUid());
		header.put("gid", log.getGid());
		header.put("msn", log.getMsn());
		header.put("status", status);
		publish(alias + log.getUid(), data, type);

		//wait 5s
		LOG.debug("sleep 5s");
		TimeUnit.SECONDS.sleep(5);
		
		if (status == 0) {
			// Send file
			data = new HashMap<>();
			header = new HashMap<>();
			data.put("header", header);
			header.put("mid", log.getMid());
			header.put("uid", log.getUid());
			header.put("gid", log.getGid());
			header.put("msn", log.getMsn());
			Map<String, Object> payload = new HashMap<>();
			data.put("payload", payload);
			payload.put("id", log.getUid());
			payload.put("cmd", "ACT");
			List<String> svCA = caRequestLogRepository.findCAByUid("server.csr");
			payload.put("p1", svCA.isEmpty() ? null : svCA.get(0));

			String sig = RSAUtil.initSignedRequest(masterPkPath, new ObjectMapper().writeValueAsString(payload));
			header.put("sig", sig);

			publish(alias + log.getUid(), data, type);
			//put mid to check receive response or not
			onboardingMap.put(log.getMid(), log.getMid());
		}
	}
	
	private void handleOnSubscribe(final MqttMessage mqttMessage) {
		try {
			Map<String, Object> data = MAPPER.readValue(mqttMessage.getPayload(), Map.class);
			
			//save log
			Log log = Log.build(data, "SUBSCRIBE");
			log.setMqttAddress(evsPAMQTTAddress);
			log.setTopic(evsPASubscribeTopic);
			LOG.debug(">Subscribe " + log.getMid() + " " + log.getMsn() + " " + log.getPType() + " " + evsPASubscribeTopic);
			logRepository.save(log);
			updatePublishStatus(log);

			Map<String, Object> header = (Map<String, Object>) data.get("header");
			Map<String, Object> payload = (Map<String, Object>) data.get("payload");
			String type = (String) payload.get("type");

			int status = 0;
			if(validateSign) {
				boolean verifySign = RSAUtil.verifySign(csrFolder + log.getUid() + ".csr",
						new ObjectMapper().writeValueAsString(payload), (String) header.get("sig"));
				LOG.debug("handleOnSubscribe, type: {}, verifySign: {}", type, verifySign);
				if (!verifySign) {
					status = -1;
				}
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
			log.setTopic(evsPARespSubscribeTopic);
			LOG.debug(">Subscribe " + log.getMid() + " " + log.getMsn() + " " + log.getPType() + " " + evsPARespSubscribeTopic);
			logRepository.save(log);
			updatePublishStatus(log);

			//TO-DO: need base on mid of response and mapping with request and update status log to know
			//got response or not
			Map<String, Object> header = (Map<String, Object>) data.get("header");
			String type = log.getPType();

			int status = 0;
			if(validateSign && (data.get("payload") != null)) {
				Map<String, Object> payload = (Map<String, Object>) data.get("payload");
				boolean verifySign = RSAUtil.verifySign(csrFolder + log.getUid() + ".csr",
						new ObjectMapper().writeValueAsString(payload), (String) header.get("sig"));
				LOG.debug("HandleOnRespSubscribe, type: {}, verifySign: {}", type, verifySign);
				if (!verifySign) {
					status = -1;
				}
			}
			if ("RLS".equalsIgnoreCase(type)) {
				handleRLSRes(data, type, log);
			}
			
			if ("INF".equalsIgnoreCase(type)) {
				handleINFRes(data, type, log, status);
			}

			if ("OTA".equalsIgnoreCase(type)) {
				handleOTARes(data, type, log, status);
			}

			if (localMap.get(log.getOid()) != null && !"RLS".equalsIgnoreCase(type)) {
				header.put("oid", localMap.get(log.getOid()));
				header.remove("uid");
				if(data.get("payload") != null) {
					Map<String, Object> payload = (Map<String, Object>) data.get("payload");
					if(payload.get("id") != null){
						payload.put("id", header.get("msn"));
					}
				}
				publish(evsMeterLocalRespSubscribeTopic, data, type);
				localMap.remove(log.getOid());
			} else if (onboardingMap.get(log.getRmid()) != null && !"RLS".equalsIgnoreCase(type)) {
				if (log.getStatus() == 0) {
					Optional<CARequestLog> opt = caRequestLogRepository.findByUidAndMsn(log.getUid() + "", log.getMsn());
					if (opt.isPresent()) {
						opt.get().setOnboardingDatetime(Calendar.getInstance().getTimeInMillis());
						caRequestLogRepository.save(opt.get());
					}
				} else {
					LOG.debug("Onboarding process fail, MID = {}", log.getRmid());
				}
				onboardingMap.remove(log.getRmid());
			}
			updateLastSubscribe(log);

		} catch (Exception e) {
			LOG.error(e.getMessage(), e);
		}
	}

	private void updateLastSubscribe(Log log) {
		Optional<CARequestLog> opt = caRequestLogRepository.findByUidAndMsn(log.getUid() + "", log.getMsn());
		if (opt.isPresent()) {
			opt.get().setStatus(DeviceStatus.ONLINE);
			opt.get().setLastSubscribeDatetime(Calendar.getInstance().getTimeInMillis());
			caRequestLogRepository.save(opt.get());
		}
	}

	private void handleOnLocalRequestSubscribe(final MqttMessage mqttMessage) {
		try {
			Map<String, Object> data = MAPPER.readValue(mqttMessage.getPayload(), Map.class);
			//save log
			Log log = Log.build(data, "SUBSCRIBE");
			log.setMqttAddress(evsPAMQTTAddress);
			log.setTopic(evsMeterLocalSubscribeTopic);
			LOG.debug(">Subscribe " + log.getMid() + " " + log.getMsn() + " " + log.getPType() + " " + evsMeterLocalSubscribeTopic);
			logRepository.save(log);
			updatePublishStatus(log);
			
			Map<String, Object> payload = (Map<String, Object>) data.get("payload");
			String cmd = (String)payload.get("cmd");
			if("MDT".equals(cmd)) {
				handleLocalMDTRequest(data);
			} else {
				handleLocalCmdRequest(data);
			}
		} catch (Exception e) {
			LOG.error(e.getMessage(), e);
		}

	}

	private void handleLocalCmdRequest(Map<String, Object> data) throws Exception {
		Map<String, Object> header = (Map<String, Object>) data.get("header");
		Map<String, Object> payload = (Map<String, Object>) data.get("payload");
		Integer mid = (Integer)header.get("mid");
		String msn = (String)header.get("msn");
		String cmd = (String)payload.get("cmd");
		Optional<CARequestLog> caRequestLog = caRequestLogRepository.findByMsn(msn);
		if (caRequestLog.isPresent()) {
			Long nextMid = nextvalMID();
			localMap.put(nextMid, mid.longValue());
			header.put("mid", nextMid);
			header.put("uid", caRequestLog.get().getUid());
			header.put("gid", caRequestLog.get().getUid());
			payload.put("id", caRequestLog.get().getUid());
			String sig = RSAUtil.initSignedRequest(pkPath, new ObjectMapper().writeValueAsString(payload));
			header.put("sig", sig);
			publish(alias + caRequestLog.get().getUid(), data);
		} else {
			publish(evsMeterLocalRespSubscribeTopic, SimpleMap.init(
					"header", SimpleMap.init("oid", mid).more("type", cmd).more("status", -1)
			));
		}
	}

	private void handleLocalMDTRequest(Map<String, Object> data) throws Exception {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		Map<String, Object> header = (Map<String, Object>) data.get("header");
		Map<String, Object> payload = (Map<String, Object>) data.get("payload");
		Integer mid = (Integer)header.get("mid");
		String msn = (String)header.get("msn");
		Optional<CARequestLog> caRequestLog = caRequestLogRepository.findByMsn(msn);
		if (caRequestLog.isPresent()) {

			String kwh = logService.getKwh(caRequestLog.get().getUid(), payload);
			Map<String, Object> data1 = new HashMap<>();
			Map<String, Object> header1 = new HashMap<>();
			List<Map<String, Object>> dataTag = new ArrayList<>();
			data.put("header", header1);
			header1.put("mid", mid);
			header1.put("msn", msn);
			Map<String, Object> payload1 = new HashMap<>();
			data.put("payload", payload1);
			payload1.put("id", "");
			payload1.put("type", "MDT");
			payload1.put("data", dataTag);
			dataTag.add(SimpleMap.init("uid", "").more("msn", msn).more("kwh", kwh)
					.more("kw", "0.0").more("i", "0.0").more("v", "244.2").more("pf", "1.00")
					.more("dt", sdf.format(Calendar.getInstance().getTime())));
			publish(evsMeterLocalRespSubscribeTopic, data1);

		} else {
			publish(evsMeterLocalRespSubscribeTopic, SimpleMap.init(
					"header", SimpleMap.init("oid", mid).more("type", "MDT").more("status", -1)
			));
		}
	}

	private void subscribe() {
		//request
		try {
			Mqtt.subscribe(Mqtt.getInstance(evsPAMQTTAddress, mqttClientId), evsPASubscribeTopic, QUALITY_OF_SERVICE, o -> {
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
			Mqtt.subscribe(Mqtt.getInstance(evsPAMQTTAddress, mqttClientId), evsPARespSubscribeTopic, QUALITY_OF_SERVICE, o -> {
				final MqttMessage mqttMessage = (MqttMessage) o;
				LOG.info(evsPARespSubscribeTopic + " -> " + new String(mqttMessage.getPayload()));
				EX.submit(() -> handleOnRespSubscribe(mqttMessage));
				return null;
			});
		} catch (Exception e) {
			LOG.error(e.getMessage(), e);
		}

		try {
			Mqtt.subscribe(Mqtt.getInstance(evsPAMQTTAddress, mqttClientId), evsMeterLocalSubscribeTopic, QUALITY_OF_SERVICE, o -> {
				final MqttMessage mqttMessage = (MqttMessage) o;
				LOG.info(evsMeterLocalSubscribeTopic + " -> " + new String(mqttMessage.getPayload()));
				EX.submit(() -> handleOnLocalRequestSubscribe(mqttMessage));
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

		LOG.debug("Init EVSPAServiceImpl ... ");
		localMap = new ConcurrentHashMap<>();
		onboardingMap = new ConcurrentHashMap<>();
		
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
			for (File ful : f.listFiles()) {
				if (ful.isFile() && ful.getName().endsWith(".csv")) {
					try {
						String[] lines = new String(Files.readAllBytes(ful.toPath()), StandardCharsets.UTF_8).split("\r*\n");
						for (int i = 1; i < lines.length; i++) {
							String[] details = lines[i].split(" *, *");
							if (details.length > 2) {
								String uuid = details[1];
								Optional<CARequestLog> opt = caRequestLogRepository.findByUid(uuid);
								CARequestLog caLog = !opt.isPresent() ? new CARequestLog() : opt.get();
								caLog.setUid(uuid);
								caLog.setSn(details[0]);
								caLog.setCid(details[2]);
								caLog.setRequireRefresh(false);
								caRequestLogRepository.save(caLog);
							}
						}
						Files.delete(ful.toPath());
					} catch (Exception e) {
						LOG.error(e.getMessage(), e);
					}
					
				}
				if (ful.isFile() && ful.getName().endsWith(".csr")) {
					try {
						String uuid = ful.getName().replaceAll("\\.csr$", "");
						Optional<CARequestLog> opt = caRequestLogRepository.findByUid(uuid);
						CARequestLog caLog = !opt.isPresent() ? new CARequestLog() : opt.get();
						//Map<String, Object> data = requestCA(caRequestUrl, new FileSystemResource(ful), uuid);
						caLog.setUid(uuid);
						//caLog.setCertificate((String)data.get("pemBase64"));
						//caLog.setRaw((String)data.get("cas"));
						//caLog.setStartDate((Long)data.get("startDate"));
						//caLog.setEndDate((Long)data.get("endDate"));
						caLog.setMsn(null);
						caLog.setStatus(DeviceStatus.NOT_COUPLED);
						caLog.setEnrollmentDatetime(Calendar.getInstance().getTimeInMillis());
						caLog.setRequireRefresh(false);
						caRequestLogRepository.save(caLog);
						File out = new File(ful.getAbsolutePath().replace("IN_CSR", "OUT_CSR"));
						Files.deleteIfExists(out.toPath());
						ful.renameTo(out);
						LOG.info("Done request CA {}", ful.getName());
					} catch (Exception e) {
						File error = new File(ful.getAbsolutePath().replace("IN_CSR", "ERR_CSR"));
						try {
							Files.deleteIfExists(error.toPath());
						} catch (IOException e1) {/**/}
						ful.renameTo(error);
						LOG.error("Error request CA {}", ful.getName());
						LOG.error(e.getMessage(), e);
					}
					
				}
			}

		}, "REQUEST_CA");
		
		SchedulerHelper.scheduleJob("0/5 * * * * ? *", () -> {
			//refresh CID
			try {
				caRequestLogService.getCids(true);
			} catch (Exception e) {
				//
			}
			
		}, "REFRESH_CID");
		try {
			initS3();
		} catch (Exception e) {/**/}
		try {
			logRepository.createMIDSeq();
			Number lastValue = logRepository.nextvalMID();
			if (lastValue.longValue() < 10000l) {
				logRepository.nextvalMID(10000l);
			}
		} catch (Exception e) {/**/}
	}
	
	@SuppressWarnings("deprecation")
	private void initS3() {
		
		/**
		 * https://842807477657.signin.aws.amazon.com/console
		 * henry/P0wer!2
		 * esim
		 * https://904734893309.signin.aws.amazon.com/console
		 * password henry/P0wer!23
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
	
	public String getS3URL(String objectKey) {
		//java.util.Date expiration = new java.util.Date();
		//long expTimeMillis = Instant.now().toEpochMilli();
		//expTimeMillis += 1000 * 60 * expireTime;
		//expiration.setTime(expTimeMillis);

		// Generate the presigned URL.
		// GeneratePresignedUrlRequest generatePresignedUrlRequest = new GeneratePresignedUrlRequest(bucketName + "/" + firmwareService.getLatestFirmware().getVersion(),
		// 		objectKey).withMethod(com.amazonaws.HttpMethod.GET).withExpiration(expiration);
		//return s3Client.generatePresignedUrl(generatePresignedUrlRequest).toString();
		
		String bcName = bucketName + "/" + firmwareService.getLatestFirmware().getVersion() + "/" + objectKey;
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		CMD.exec("/usr/local/aws/bin/aws s3 presign s3://" + bcName + " --expires-in " + (60 * expireTime), null, bos);
		String rs = new String(bos.toByteArray(), StandardCharsets.UTF_8).replaceAll("[\n\r]", "");
		try {
			bos.close();
		} catch (IOException e) {
			LOG.error(e.getMessage(), e);
		}
		return rs;
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
		Map<String, Object> response = ApiUtils.getRestTemplate().exchange(caRequestUrl, HttpMethod.POST, entity, Map.class).getBody();
		String pem = (response.get("cas") + "").replaceAll(".*\"Certificate\": \"(-----BEGIN CERTIFICATE-----[\na-zA-Z0-9=\\\\/+]+-----END CERTIFICATE-----).*", "$1").replace("\\n", "\n");
		
		if (!pem.contains("-----BEGIN CERTIFICATE-----")) {
			throw new RuntimeException("CA request ERROR " + uuid + "\n" + response.get("cas"));
		}
		response.put("pem", pem);
		response.put("pemBase64", Base64.getEncoder().encodeToString(pem.getBytes(StandardCharsets.UTF_8)));
		response.put("uid", uuid);
		return response;
	}
	
    @Override
    public boolean upload(String fileName, String version, String hashCode, InputStream in) {
        boolean result=false;
        Date today = new Date();
        String keyName = String.format("%s", new SimpleDateFormat("yyyy/MM/dd").format(today));
        LOG.info("Upload Function. Bucket Name: {}, File Name: {}, Version: {}, KeyName: {}", bucketName, fileName, version, keyName);
        PutObjectRequest request = null;
        ObjectMetadata metadata = null;
        try {
            if (in != null) {
                String key = version + "/" + fileName;     
                metadata = new ObjectMetadata();
                metadata.setContentType("plain/text");
                request = new PutObjectRequest(bucketName, key, in, metadata);
                request.setMetadata(metadata);
                s3Client.putObject(request);

                LOG.info("{} Finished uploading File Object: {} to {}", DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss").format(LocalDateTime.now()), fileName, version);
                result = true;
            }
            else {
                LOG.info("File: {} does not exists", fileName);
                result = true;
            }
        } catch (AmazonServiceException e) {
            // The call was transmitted successfully, but Amazon S3 couldn't process
            // it, so it returned an error response.
            LOG.error("S3 Service Exception: " + e.getMessage(), e);
        } catch (Exception e) {
            LOG.error("Exception in uploading file: " + e.getLocalizedMessage());
        } finally {
              IdleConnectionReaper.shutdown();
              AwsSdkMetrics.unregisterMetricAdminMBean();
              LOG.info("Finished S3 Upload");
        }

        return result;
    }
    
    void updatePublishStatus(Log log) {
    	try {
    		Long status = log.getStatus();
        	if (status != null) {
        		Long mId = log.getOid();
        		logRepository.updateStatus(status, mId);
        	}
		} catch (Exception e) {
			LOG.error(e.getMessage(), e);
		}
    }
	
	public static void main(String[] args) throws Exception {
		/**System.out.println(requestCA("http://54.254.171.4:8880/api/evs-ca-request", new ClassPathResource("sv-ca/server.csr"), null));*/
		
		/*Mqtt.subscribe(null, "dev/evs/pa/data", QUALITY_OF_SERVICE, o -> {
			MqttMessage mqttMessage = (MqttMessage) o;
			LOG.info("1 -> " + new String(mqttMessage.getPayload()));
			return null;
		});*/
		
		//String json = "{\"header\":{\"mid\":1001,\"uid\":\"BIERWXAABMAB2AEBAA\",\"gid\":\"BIERWXAAA4AFBABABXX\",\"msn\":\"201906000032\",\"sig\":\"Base64(ECC_SIGN(payload))\"},\"payload\":{\"id\":\"BIERWXAABMAB2AEBAA\",\"type\":\"OBR\",\"data\":\"201906000137\"}}";
		//Mqtt.publish("dev/evs/pa/data", new ObjectMapper().readValue(json, Map.class), QUALITY_OF_SERVICE, false);
		//Mqtt.publish("dev/evs/pa/data", new ObjectMapper().readValue(json, Map.class), QUALITY_OF_SERVICE, false);
		//qtt.publish("dev/evs/pa/data", new ObjectMapper().readValue(json, Map.class), QUALITY_OF_SERVICE, false);
		/*Map<String, Object> map = new HashMap<>();
		map.put("id", "BIERWXAABMAGSAEAAA");
		map.put("cmd", "PW1");
		String payload = new ObjectMapper().writeValueAsString(map);
		System.out.println(payload);
		String sig = RSAUtil.initSignedRequest("D://server.key", payload);
		System.out.println(sig);*/

		String json = "{\n" +
				"    \"header\": {\n" +
				"        \"mid\": 888001,\n" +
				"        \"msn\": \"201906000020\"\n" +
				"    },\n" +
				"    \"payload\": {\n" +
				"        \"id\": \"201906000018\",\n" +
				"        \"cmd\": \"PW0\",\n" +
				"        \"p1\": \"0\",\n" +
				"        \"p2\": \"0\"\n" +
				"    }\n" +
				"}";

		Mqtt.publish(Mqtt.getInstance(evsPAMQTTAddress, mqttClientId), "Meter/Grp30/Req", new ObjectMapper().readValue(json, Map.class), QUALITY_OF_SERVICE, false);

		/*SimpleDateFormat sf = new SimpleDateFormat();
		sf.setTimeZone(UTC);
		sf.applyPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");
		Date newDate = sf.parse("2021-07-07T13:44:25Z");
		
		Calendar c = Calendar.getInstance();
		c.setTime(newDate);

		while(true) {
			sf.applyPattern("yyyy-MM-dd'T'HH:mm:ss");
			json = json.replaceAll("[0-9]{4}-[0-9]{2}-[0-9]{2}T[0-9]{2}\\:[0-9]{2}\\:[0-9]{2}", sf.format(newDate));
			Mqtt.publish(Mqtt.getInstance(evsPAMQTTAddress, mqttClientId), "dev/evs/pa/data", new ObjectMapper().readValue(json, Map.class), QUALITY_OF_SERVICE, false);
			c.add(Calendar.MINUTE, 30);
			newDate = c.getTime();
			if (newDate.getTime() > System.currentTimeMillis()) {
				break;
			}
			System.out.println(newDate);
		}*/
	}
}
