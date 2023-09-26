package com.pa.evs.sv.impl;

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
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TimeZone;
import java.util.Vector;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import javax.annotation.PostConstruct;
import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.eclipse.paho.client.mqttv3.IMqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.CollectionUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.multipart.MultipartFile;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.ClientConfiguration;
import com.amazonaws.Protocol;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.http.IdleConnectionReaper;
import com.amazonaws.metrics.AwsSdkMetrics;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pa.evs.LocalMapStorage;
import com.pa.evs.ctrl.CommonController;
import com.pa.evs.dto.LogBatchDto;
import com.pa.evs.dto.PaginDto;
import com.pa.evs.dto.PiLogDto;
import com.pa.evs.enums.DeviceStatus;
import com.pa.evs.enums.DeviceType;
import com.pa.evs.enums.MqttCmdStatus;
import com.pa.evs.enums.ScreenMonitorKey;
import com.pa.evs.enums.ScreenMonitorStatus;
import com.pa.evs.model.CARequestLog;
import com.pa.evs.model.Firmware;
import com.pa.evs.model.GroupTask;
import com.pa.evs.model.Log;
import com.pa.evs.model.LogBatch;
import com.pa.evs.model.LogBatchGroupTask;
import com.pa.evs.model.MeterFileData;
import com.pa.evs.model.MeterLog;
import com.pa.evs.model.P1OnlineStatus;
import com.pa.evs.model.Pi;
import com.pa.evs.model.PiLog;
import com.pa.evs.model.ScreenMonitoring;
import com.pa.evs.model.Users;
import com.pa.evs.model.Vendor;
import com.pa.evs.repository.CARequestLogRepository;
import com.pa.evs.repository.FirmwareRepository;
import com.pa.evs.repository.GroupTaskRepository;
import com.pa.evs.repository.LogBatchGroupTaskRepository;
import com.pa.evs.repository.LogBatchRepository;
import com.pa.evs.repository.LogRepository;
import com.pa.evs.repository.MeterFileDataRepository;
import com.pa.evs.repository.MeterLogRepository;
import com.pa.evs.repository.P1OnlineStatusRepository;
import com.pa.evs.repository.PiLogRepository;
import com.pa.evs.repository.PiRepository;
import com.pa.evs.repository.ScreenMonitoringRepository;
import com.pa.evs.repository.UserRepository;
import com.pa.evs.repository.VendorRepository;
import com.pa.evs.sv.CaRequestLogService;
import com.pa.evs.sv.EVSPAService;
import com.pa.evs.sv.FirmwareService;
import com.pa.evs.sv.LogService;
import com.pa.evs.utils.ApiUtils;
import com.pa.evs.utils.AppProps;
import com.pa.evs.utils.CMD;
import com.pa.evs.utils.ExternalLogger;
import com.pa.evs.utils.Mqtt;
import com.pa.evs.utils.RSAUtil;
import com.pa.evs.utils.SchedulerHelper;
import com.pa.evs.utils.SecurityUtils;
import com.pa.evs.utils.SimpleMap;
import com.pa.evs.utils.ZipUtils;

import software.amazon.awssdk.services.sns.model.SnsException;

@Component
@SuppressWarnings("unchecked")
@EnableScheduling
@org.springframework.context.annotation.DependsOn({"ajms", "jmsTemplate", "connectionFactory", "jmsContainerFactory"})
public class EVSPAServiceImpl implements EVSPAService {

	private static final org.slf4j.Logger LOG = ExternalLogger.getLogger(EVSPAServiceImpl.class);// org.slf4j.LoggerFactory.getLogger(EVSPAServiceImpl.class);

	private static final ObjectMapper MAPPER = new ObjectMapper();
	
	static final TimeZone UTC = TimeZone.getTimeZone("UTC");
	
	private static final int QUALITY_OF_SERVICE = 2;

	@Autowired LocalMapStorage localMap;

	@Autowired LogService logService;
	
	@Autowired EntityManager em;
	
	@Autowired TransactionTemplate transactionTemplate;

	@Autowired CaRequestLogService caRequestLogService;

	@Autowired private LogRepository logRepository;
	
	@Autowired private SequenceService sequenceService;
	
	@Autowired private LogBatchRepository logBatchRepository;

	@Autowired private CARequestLogRepository caRequestLogRepository;
	
	@Autowired private P1OnlineStatusRepository p1OnlineStatusRepository;
	
	@Autowired private LogBatchGroupTaskRepository logBatchGroupTaskRepository;
	
	@Autowired private GroupTaskRepository groupTaskRepository;
	
	@Autowired private PiRepository piRepository;
	
	@Autowired private MeterFileDataRepository meterFileDataRepository;
	
	@Autowired private PiLogRepository piLogRepository;
	
	@Autowired private FirmwareService firmwareService;

	@Autowired private MeterLogRepository meterLogRepository;
	
	@Autowired private ScreenMonitoringRepository screenMonitoringRepository;
	
    @Autowired private UserRepository userRepository;
    
    @Autowired private VendorRepository vendorRepository;
    
    @Autowired private FirmwareRepository firmwareRepository;
    
	@Value("${evs.pa.data.folder}") private String evsDataFolder;
	
	@Value("${evs.pa.subscribe.send.topic}") private String evsPASubscribeTopic;

	@Value("${evs.pa.subscribe.resp.topic}") private String evsPARespSubscribeTopic;

    @Value("${evs.pa.local.subscribe.send.topic}") private String evsMeterLocalSubscribeTopic;

    @Value("${evs.pa.local.subscribe.resp.topic}") private String evsMeterLocalRespSubscribeTopic;

	@Value("${evs.pa.local.data.send.topic}") private String evsMeterLocalDataSendTopic;

	//@Value("${evs.pa.local.data.resp.topic}") private String evsMeterLocalDataRespTopic;

	@Value("${evs.pa.mqtt.address}") private String evsPAMQTTAddress;

	@Value("${evs.pa.mqtt.client.id}") private String mqttClientId;

	@Value("${evs.pa.mqtt.publish.topic.alias}") private String alias;
	
	@Value("${portal.pa.ca.request.url}") private String caRequestUrl;

	@Value("${evs.pa.privatekey.path}") private String pkPath;

	@Value("${evs.pa.master.privatekey.path}") private String masterPkPath;

	@Value("${evs.pa.csr.folder}") private String csrFolder;

	@Value("${s3.access.expireTime:15}") private long expireTime;

	@Value("${evs.pa.validateSign:true}") private boolean validateSign;

	@Value("${evs.pa.fakeS3Url:false}") private boolean fakeS3Url;

	@Value("${s3.bucket.name}") private String bucketName;
	
	@Value("${s3.photo.bucket.name}") private String photoBucketName;

	@Value("${s3.access.id}") private String accessID;

	@Value("${s3.access.key}") private String accessKey;

	private static final ExecutorService EX = Executors.newFixedThreadPool(10);
	
	private static final ExecutorService EX_POOL_SDB = Executors.newFixedThreadPool(500);
	
	private AmazonS3Client s3Client = null;
	
	software.amazon.awssdk.services.sns.SnsClient snsClient = null;
	
	com.amazonaws.services.simpleemail.AmazonSimpleEmailService sesClient = null;
	
	@Override
	public Object uploadDeviceCsr(MultipartFile file, Long vendor) {
		
		// validate
		Set<String> snSet = new LinkedHashSet<>();
		Set<String> uidSet = new LinkedHashSet<>();
		Set<String> cidSet = new LinkedHashSet<>();
		List<Map<String, Object>> reads = new ArrayList<>();
		try (ZipInputStream zipFile = new ZipInputStream(file.getInputStream())) {
		    ZipEntry entry = null;
	        while ((entry = zipFile.getNextEntry()) != null) {
	            String currentName = entry.getName();
	            if (currentName.endsWith(".csv")) {
	            	String[] lines = null;
	            	try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
	            		IOUtils.copy(zipFile, bos);
						lines = new String(bos.toByteArray(), StandardCharsets.UTF_8).split("\r*\n");
						if (StringUtils.isBlank(lines[0]) || !lines[0].toUpperCase().startsWith("SN,UUID,CID")) {
							LOG.info("Headers should start with: SN UUID CID");
							throw new RuntimeException("Headers should start with: SN UUID CID");
						}
	            	}
					for (int i = 1; i < lines.length; i++) {
						String[] details = lines[i].split(" *, *");
						if (details.length > 2) {
							
							String sn = details[0];
							String uid = details[1];
							String cid = details[2];
							Map<String, Object> map = SimpleMap.init("sn", sn)
									.more("uid", uid)
									.more("cid", cid);
							reads.add(map);
							
							String message = (String) map.computeIfAbsent("message", k -> "");
							
							if (StringUtils.isBlank(uid) || StringUtils.isBlank(details[0]) || StringUtils.isBlank(details[2])) {
								LOG.info("UUID, SN, CID are required!");
								message += "UUID, SN, CID are required!\n";
							}
							
							if (snSet.contains(sn)) {
								message += "Duplicate SN\n";
							}
							if (uidSet.contains(uid)) {
								message += "Duplicate UUID\n";
							}
							if (cidSet.contains(cid)) {
								message += "Duplicate CID\n";
							}
							snSet.add(sn);
							uidSet.add(uid);
							cidSet.add(cid);
							map.put("message", message);
							
						}
					}
	            }
	        }
		} catch (Exception e) {
			LOG.error(e.getMessage(), e);
			throw new RuntimeException(e.getMessage());
		}
		
		Set<String> existSn = caRequestLogRepository.findSnBySnIn(snSet);
		Set<String> existUid = caRequestLogRepository.findUidByUidIn(uidSet);
		Set<String> existCid = caRequestLogRepository.findCidByCidIn(cidSet);
		
		boolean hasError = false;
		for (Map<String, Object> item : reads) {
			String sn = (String) item.get("sn");
			String uid = (String) item.get("uid");
			String cid = (String) item.get("cid");
			String message = (String) item.computeIfAbsent("message", k -> "");
			if (existSn.contains(sn)) {
				message = message + "SN exists!\n"; 
			}
			if (existUid.contains(uid)) {
				message = message + "UUID exists!\n"; 
			}
			if (existCid.contains(cid)) {
				message = message + "CID exists!\n"; 
			}
			item.put("message", message);
			hasError = hasError || StringUtils.isNotBlank(message);
		}
		
		if (hasError) {
			return reads;
		}
		
		// end validate
		
		File f = new File(evsDataFolder + "/" + vendor + "-" + file.getOriginalFilename());
		try {
			Files.deleteIfExists(f.toPath());
			Files.createFile(f.toPath());
			IOUtils.copy(file.getInputStream(), new FileOutputStream(f));
		} catch (IOException e) {
			LOG.error(e.getMessage(), e);
		}
		
		return null;
	}

	@Override
	public Log publish(String topic, Object message, String type, String batchId) throws Exception {
		Log log = publish(topic, message, type);
    	Users user = userRepository.findByEmail(SecurityUtils.getEmail());
		if (log != null && StringUtils.isNotBlank(batchId)) {
			LogBatch batch = logBatchRepository.findByUuid(batchId).orElse(LogBatch.builder().email(SecurityUtils.getEmail()).uuid(batchId).user(user).build());
			logBatchRepository.save(batch);
			log.setBatchId(batchId);
			if(type == "PUBLISH") {
				log.setUser(user);
			}
			logRepository.save(log);
		}
		return log;
	}

	@Override
	public List<CARequestLog> findDevicesInGroup(List<Long> listGroupId) {
		return caRequestLogRepository.findDevicesInGroup(listGroupId);
	}

	@SuppressWarnings("rawtypes")
	@Override
	public Log publish(String topic, Object message, String type) throws Exception {
		try {
			Mqtt.publish(Mqtt.getInstance(evsPAMQTTAddress, mqttClientId), topic, message, QUALITY_OF_SERVICE, false);
			LOG.info("Publish " + topic + " -> " + new ObjectMapper().writeValueAsString(message));
			
			//save log
			Map<String, Object> publishData = new HashMap<>((Map) message);
			publishData.put("type", type);
			Log logP = Log.build(publishData, "PUBLISH");
			logP.setTopic(topic);
			logP.setMqttAddress(evsPAMQTTAddress);
			
			if (CommonController.CMD_OPTIONS.get() != null 
					&& !CommonController.CMD_OPTIONS.get().isEmpty()
					&& logP.getUid() == null
					) {
				logP.setUid((String) CommonController.CMD_OPTIONS.get().get("uid"));
				if (logP.getMid() == null) {
					logP.setMid((Long) CommonController.CMD_OPTIONS.get().get("mid"));
				}
			}
			logRepository.save(logP);
			
			handlePublishType(logP);
			
			if ("INF".equals(type) && CommonController.CMD_OPTIONS.get() != null && CommonController.CMD_OPTIONS.get().get("selectVersion") != null) {
				LOG.info("> INF request update firmwave " + logP.getUid() + " -> " + CommonController.CMD_OPTIONS.get().get("selectVersion"));
				Optional<CARequestLog> opt = caRequestLogRepository.findByUid(logP.getUid());
				if (opt.isPresent()) {
					opt.get().setLatestINFFirmwaveRequest(System.currentTimeMillis() + "_INF_" + logP.getMid() + "_" + CommonController.CMD_OPTIONS.get().get("selectVersion"));
					caRequestLogRepository.save(opt.get());
				}
			}
			CommonController.CMD_OPTIONS.remove();
			
			if (!"MDT".equalsIgnoreCase(type)) {
				//wait 2s
				LOG.debug("sleep 2s");
				TimeUnit.SECONDS.sleep(2);
			}
			
			return logP;
		} catch (Exception e) {
			LOG.error(e.getMessage(), e);
		}
		return null;
	}

	@SuppressWarnings("rawtypes")
	private void publish(String topic, Object message) {
		try {
			Mqtt.publish(Mqtt.getInstance(evsPAMQTTAddress, mqttClientId), topic, message, QUALITY_OF_SERVICE, false);
			LOG.info("Publish " + topic + " -> " + new ObjectMapper().writeValueAsString(message));

			//save log
			Map<String, Object> publishData = new HashMap<>((Map) message);
			Log logP = Log.build(publishData, "PUBLISH");
			logP.setTopic(topic);
			logP.setMqttAddress(evsPAMQTTAddress);
			logRepository.save(logP);
			
			//wait 2s
			LOG.debug("sleep 2s");
			TimeUnit.SECONDS.sleep(2);
			
		} catch (Exception e) {
			LOG.error(e.getMessage(), e);
		}
	}

	private void handlePublishType(Log log) {
		if (CommonController.CMD_DESC.get() != null) {
			// ECH_P1_ONLINE_TEST
			String cmdType = CommonController.CMD_DESC.get().get(log.getUid() + "_" + log.getMid()) + "";
			if (StringUtils.isNotBlank(cmdType)) {
				// "ECH_P1_ONLINE_TEST".equals(cmdType)
				log.setCmdDesc(cmdType);
				logRepository.save(log);
			}
		}
		CommonController.CMD_DESC.remove();
		
		if ("ECH_P1_ONLINE_TEST".equals(log.getCmdDesc())) {
			Optional<CARequestLog> opt = caRequestLogRepository.findByUid(log.getUid());
			if (opt.isPresent()) {
				Long now = System.currentTimeMillis();
				
				CARequestLog ca = opt.get();
				ca.setP1Online("Offline");
				ca.setP1OnlineLastUserSent(SecurityUtils.getEmail());
				ca.setP1OnlineLastSent(now);
				caRequestLogRepository.save(ca);
				
				P1OnlineStatus p1 = new P1OnlineStatus();
				p1.setCid(ca.getCid());
				p1.setMsn(ca.getMsn());
				p1.setUid(ca.getUid());
				p1.setSn(ca.getSn());
				p1.setP1Online("Offline");
				p1.setP1OnlineLastUserSent(SecurityUtils.getEmail());
				p1.setP1OnlineLastSent(now);
				p1.setIsLatest(true);
				p1.setType(ca.getType());
				p1.setVendor(ca.getVendor());
				p1.setVersion(ca.getVer());
				
				transactionTemplate.execute(transactionStatus -> {
			        em.createQuery("UPDATE P1OnlineStatus set isLatest = false where uid = '" + ca.getUid() + "'").executeUpdate();
			        transactionStatus.flush();
			        return null;
			    });
				
				p1OnlineStatusRepository.save(p1);
			}
		}
	}
	
	private void sendToMeterClient(Map<String, Object> src, String type, Log msgLog) throws Exception {

		SimpleDateFormat sf = new SimpleDateFormat();
		sf.applyPattern("yyyyMMdd");

		Map<String, Object> header = (Map<String, Object>) src.get("header");
		Map<String, Object> payload = (Map<String, Object>) src.get("payload");
		List<Map<String, Object>> data = (List<Map<String, Object>>) payload.get("data");

		File file = null;
		String fName = null;
		for (Map<String, Object> o : data) {
			sf.applyPattern("yyyy-MM-dd'T'HH:mm:ss");
			Date datetime = sf.parse(((String) o.get("dt")).replace(".000", ""));
			sf.applyPattern("yyyyMMdd");

			Map<String, Object> tmp = new HashMap<>(o);
			tmp.put("uid", payload.get("id"));
			tmp.put("dt", datetime.getTime());
			tmp.put("dtd", datetime);
			tmp.put("dtn", Integer.parseInt(sf.format(datetime)));
			tmp.put("rawdt", o.get("dt"));
			MeterLog log = MeterLog.build(tmp);
			
            try {
            	if (fName == null) {
            		fName = "evsv3ga100_" + header.get("msn") + "_" + sf.format(datetime) + ".txt";
                }
            	file = createFile(header, data);
			} catch (Exception e) {
				e.printStackTrace();
			}
            
            meterLogRepository.save(log);
		}
		
		// un publish
		Log publishLog = null; //publish(evsMeterLocalDataSendTopic, src, type);
		
		try {
			logMDTSent((String)header.get("msn"), (Integer)header.get("mid"), file, msgLog, publishLog, fName);
		} catch (Exception e) {
			LOG.error(e.getMessage(), e);
		}
	}
	
	private synchronized File createFile(Map<String, Object> header, List<Map<String, Object>> data) {
		SimpleDateFormat sf = new SimpleDateFormat();
		File file = null;
		try {
			for (Map<String, Object> o : data) {
				sf.applyPattern("yyyy-MM-dd'T'HH:mm:ss");
				Date datetime = sf.parse(((String) o.get("dt")).replace(".000", ""));
				sf.applyPattern("yyyyMMdd");
				file = new File(evsDataFolder + "/meter_file/evsv3ga100_" + header.get("msn") + "_" + sf.format(datetime) + ".txt");
				if (!file.exists()) {
					Files.createFile(file.toPath());
				}
				try (FileOutputStream fos = new FileOutputStream(file, true)) {
					sf.applyPattern("yyyy-MM-dd HH:mm:ss");
					fos.write((sf.format(datetime) + "," + o.get("msn") + "," + o.get("kwh") + "," + "\r\n")
							.getBytes(StandardCharsets.UTF_8));
				}
			}
		} catch (Exception e) {
			LOG.error(e.getMessage(), e);
		}
		return file;
	}
	
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	private void logMDTSent(String msn, Integer mid, File file, Log log, Log publishLog, String defaultFname) {
		piRepository.findExists()
		.forEach(pi -> {
			PiLog piLog = new PiLog();
			piLog.setPi(pi);
			piLog.setLogId(log.getId());
			if (publishLog != null) {
				piLog.setPublishLogId(publishLog.getId());
			}
			piLog.setMsn(msn);
			piLog.setMid(Long.valueOf(mid));
			piLog.setType("MDT");
			piLog.setFtpResStatus("NEW");
			if (file != null) {
				piLog.setFileName(file.getName());
				piLog.setPiDownloaded(false);
				piLog.setPiFileName("/home/pi/evs-data/FTP_LOG/" + file.getName());
			} else if (defaultFname != null) {
				piLog.setFileName(defaultFname);
				piLog.setPiDownloaded(false);
				piLog.setPiFileName("/home/pi/evs-data/FTP_LOG/" + defaultFname);
			}
			piLogRepository.save(piLog);
			piLogRepository.flush();
		});
	}

	@Override
	@Transactional
	public void updateMissingFileName() {
		SimpleDateFormat sf = new SimpleDateFormat();
		List<PiLog> piLogs = piLogRepository.findByFileNameIsNull();
		piLogs
		.forEach(piLog -> {
			Log log = logRepository.findById(piLog.getLogId()).orElse(null);
			if (log != null) {
				String raw = log.getRaw();
				try {
					Map<String, Object> src = MAPPER.readValue(raw, Map.class);
					Map<String, Object> header = (Map<String, Object>) src.get("header");
					Map<String, Object> payload = (Map<String, Object>) src.get("payload");
					List<Map<String, Object>> data = (List<Map<String, Object>>) payload.get("data");
					String fName = null;
					for (Map<String, Object> o : data) {
						sf.applyPattern("yyyy-MM-dd'T'HH:mm:ss");
						Date datetime = sf.parse(((String) o.get("dt")).replace(".000", ""));
						sf.applyPattern("yyyyMMdd");

			            try {
			            	if (fName == null) {
			            		fName = "evsv3ga100_" + header.get("msn") + "_" + sf.format(datetime) + ".txt";
			            		break;
			                }
						} catch (Exception e) {
							e.printStackTrace();
						}
			            
					}
					piLog.setFileName(fName);
					piLogRepository.save(piLog);
				} catch (Exception e) {
					e.printStackTrace();
				}
			} else {
				System.out.println("LOL");
			}
			
		});
	}
	
	@Override
	public Long nextvalMID(Vendor vendor) {
		Number mid = sequenceService.nextvalMID(vendor.getId()).longValue();
		// TC Module mid (message id) format is uint32, the max number is 4294967295
		if (vendor.getMaxMidValue() == null) {
			vendor.setMaxMidValue(65535l);
		}
		if (mid.longValue() >= vendor.getMaxMidValue()) {// 4294967295l
			sequenceService.nextvalMID(10000l, vendor.getId());
			mid = sequenceService.nextvalMID(vendor.getId()).longValue();
		}
		return mid.longValue();
	}

	private int validateUid(Log log) {
		// Optional<?> opt = caRequestLogRepository.findByUid(log.getUid());
		Optional<Object> opt = localMap.getUidMsnMap().containsKey(log.getUid()) ? Optional.<Object>of(true) : Optional.<Object>empty();
		if (!opt.isPresent()) {
			//Publish device not found
			Map<String, Object> data = new HashMap<>();
			Map<String, Object> header = new HashMap<>();
			data.put("header", header);
			header.put("oid", log.getMid());
			header.put("uid", log.getUid());
			header.put("gid", log.getGid());
			header.put("msn", log.getMsn());
			header.put("status", MqttCmdStatus.NOTFOUND_DEVICE.getStatus());
			publish(alias + log.getUid(), data);
		}
		
		return opt.isPresent() ? 0 : MqttCmdStatus.NOTFOUND_DEVICE.getStatus();
	}
	
	private int validateUidAndMsn(Log log) {
		//Optional<CARequestLog> opt = caRequestLogRepository.findByUidAndMsn(log.getUid(), log.getMsn());
		String cacheMsn = localMap.getUidMsnMap().get(log.getUid());
		Optional<Object> opt = ((StringUtils.isBlank(log.getMsn()) && StringUtils.isBlank(cacheMsn)) || cacheMsn.equals(log.getMsn())) ? Optional.<Object>of(true) : Optional.<Object>empty();

		if (!opt.isPresent()) {
			LOG.error("Not found binding of msn: {} for uuid: {}", log.getMsn(), log.getUid());
		}
		return opt.isPresent() ? 0 : -1;
	}
	
	private void handleMDT(Map<String, Object> data, String type, Log log, int status) throws Exception {

		if (status == 0) {
			status = validateUidAndMsn(log);
		}
		
		//Publish
		final Map<String, Object> dataRes = new HashMap<>();
		Map<String, Object> header = new HashMap<>();
		dataRes.put("header", header);
		header.put("oid", log.getMid());
		header.put("uid", log.getUid());
		header.put("gid", log.getGid());
		header.put("msn", log.getMsn());
		header.put("status", status);

		// publish(alias + log.getUid(), dataRes, type);
		// special publish only for MDT
		Mqtt.publish(Mqtt.getInstance(evsPAMQTTAddress, mqttClientId), alias + log.getUid(), dataRes, QUALITY_OF_SERVICE, false);
		
		// save subscribe/publish message log
		final int statusTmp = status;
		EX_POOL_SDB.submit(() -> {
			try {
				LOG.info("Publish " + alias + log.getUid() + " -> " + new ObjectMapper().writeValueAsString(dataRes));
				AppProps.getContext().getBean(this.getClass()).saveMDTMessage(data, type, log, statusTmp, dataRes);
			} catch (Exception e) {
				LOG.error(e.getMessage(), e);
			}
		});
		
	}
	
	@SuppressWarnings("rawtypes")
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void saveMDTMessage(Map<String, Object> data, String type, Log subscribeLog, int status, final Map<String, Object> dataRes) throws Exception {
		
		logRepository.save(subscribeLog);
		updatePublishStatus(subscribeLog);
		//
		if (status == 0) {
			sendToMeterClient(data, type, subscribeLog);
		}
		updateLastMDTSubscribe(subscribeLog);
		
		//save log publish log
		Map<String, Object> publishData = new HashMap<>((Map) dataRes);
		publishData.put("type", type);
		Log logP = Log.build(publishData, "PUBLISH");
		logP.setTopic(alias + subscribeLog.getUid());
		logP.setMqttAddress(evsPAMQTTAddress);
		logRepository.save(logP);
		
		handlePublishType(logP);
		
	}

	private void checkECH(Map<String, Object> data, String type, Log log, int status) throws Exception {

		if (!"true".equalsIgnoreCase(AppProps.get("ECH_P1_ONLINE_TEST", "false"))) {
			return;
		}
		try {
			// P1 Provisioning
			if ("ECH".equals(log.getPType()) && evsPASubscribeTopic.equalsIgnoreCase(log.getTopic())) {
				// receive from evs/pa/data
				// MCU -> MMS
				LOG.info("ECH MCU -> PA EVS -> MCU " + log.getUid() + " checking...");
				Optional<CARequestLog> opt = caRequestLogRepository.findByUid(log.getUid());
				if (opt.isPresent()) {
					CARequestLog ca = opt.get();
					
					// reply -> MCU
					// publish
					Map<String, Object> dataP = new HashMap<>();
					Map<String, Object> header = new HashMap<>();
					dataP.put("header", header);
					header.put("oid", log.getMid());
					header.put("uid", log.getUid());
					header.put("gid", log.getGid());
					
					int sttECH = 0;
					
					// check coupled msn
					if (StringUtils.isNotBlank(ca.getMsn()) && !ca.getMsn().equalsIgnoreCase(log.getMsn())) {
						LOG.info("ECH PA EVS -> MCU false, msn coupled but msn not match");
						sttECH = MqttCmdStatus.INVALID_DEVICE.getStatus();
					}
					// not couple msn always ok
					
					header.put("status", sttECH);
					publish(alias + log.getUid(), dataP, type);
					if (StringUtils.isBlank(ca.getMsn()) || sttECH == 0) {
						Long now = System.currentTimeMillis();
						
						ca.setP1Online("Online");
						ca.setP1OnlineLastUserSent("MCU");
						ca.setP1OnlineLastSent(now);
						ca.setP1OnlineLastReceived(now);
						
						P1OnlineStatus p1 = new P1OnlineStatus();
						p1.setCid(ca.getCid());
						p1.setMsn(ca.getMsn());
						p1.setUid(ca.getUid());
						p1.setSn(ca.getSn());
						p1.setP1Online("Online");
						p1.setP1OnlineLastUserSent("MCU");
						p1.setP1OnlineLastSent(now);
						p1.setP1OnlineLastReceived(now);
						p1.setIsLatest(true);
						p1.setType(ca.getType());
						p1.setVendor(ca.getVendor());
						p1.setVersion(ca.getVer());
						
						transactionTemplate.execute(transactionStatus -> {
					        em.createQuery("UPDATE P1OnlineStatus set isLatest = false where uid = '" + ca.getUid() + "'").executeUpdate();
					        transactionStatus.flush();
					        return null;
					    });
						
						p1OnlineStatusRepository.save(p1);
					}
					caRequestLogRepository.save(ca);
				} else {
					LOG.info("ECH PA EVS -> MCU false, uid not exists: " + log.getUid());
				}
				LOG.info("> check ECH evs/pa/data ECHP1OnlineTest for uuid=" + log.getUid() + " mid=" + (log.getMid() == null ? log.getOid() : log.getMid()));
			} else if (evsPARespSubscribeTopic.equalsIgnoreCase(log.getTopic())) {
				// check MMS send ECH
				// receive from evs/pa/resp
				// MMS -> MCU -> MMS
				List<Log> logs = logRepository.findByUidAndMid(log.getUid(), log.getMid() == null ? log.getOid() : log.getMid());
				boolean isECHP1OnlineTest = logs.stream().anyMatch(l -> "ECH_P1_ONLINE_TEST".equalsIgnoreCase(l.getCmdDesc()));
				LOG.info("> check ECH evs/pa/resp ECHP1OnlineTest for uuid=" + log.getUid() + " mid=" + (log.getMid() == null ? log.getOid() : log.getMid()) + " isECHP1OnlineTest=" + isECHP1OnlineTest);
				if (isECHP1OnlineTest) {
					Optional<CARequestLog> opt = caRequestLogRepository.findByUid(log.getUid());
					if (opt.isPresent()) {
						CARequestLog ca = opt.get();
						Long now = System.currentTimeMillis();
						
						ca.setP1Online("Online");
						ca.setP1OnlineLastReceived(now);
						caRequestLogRepository.save(ca);
						
						Optional<P1OnlineStatus> p1Opt = p1OnlineStatusRepository.findLatestByUid(log.getUid());
						if (p1Opt.isPresent()) {
							P1OnlineStatus p1 = p1Opt.get();
							p1.setP1Online("Online");
							p1.setP1OnlineLastReceived(now);
							p1.setModifyDate(new Date());
							p1OnlineStatusRepository.save(p1);
						} else {
							P1OnlineStatus p1 = new P1OnlineStatus();
							p1.setCid(ca.getCid());
							p1.setMsn(ca.getMsn());
							p1.setUid(ca.getUid());
							p1.setSn(ca.getSn());
							p1.setP1Online("Online");
							p1.setP1OnlineLastUserSent("MCU");
							p1.setP1OnlineLastSent(now);
							p1.setP1OnlineLastReceived(now);
							p1.setIsLatest(true);
							p1.setType(ca.getType());
							p1.setVendor(ca.getVendor());
							p1.setVersion(ca.getVer());
							
							transactionTemplate.execute(transactionStatus -> {
						        em.createQuery("UPDATE P1OnlineStatus set isLatest = false where uid = '" + ca.getUid() + "'").executeUpdate();
						        transactionStatus.flush();
						        return null;
						    });
							
							p1OnlineStatusRepository.save(p1);
						}
					}
				}				
			}

		} catch (Exception e) {
			LOG.error(e.getMessage(), e);
		}

	}
	
	private void handleRLSRes(Map<String, Object> data, String type, Log log) throws Exception {
		//Publish
		if (localMap.getLocalMap().get(log.getOid()) != null) {
			Map<String, Object> header = (Map<String, Object>) data.get("header");
			Map<String, Object> payload = (Map<String, Object>) data.get("payload");
			header.put("oid", localMap.getLocalMap().get(log.getOid()));
			header.remove("sig");
			header.remove("uid");
			payload.put("id", header.get("msn"));
			publish(evsMeterLocalRespSubscribeTopic, data, type);
			localMap.getLocalMap().remove(log.getOid());
		}
	}
	
	private boolean isReceivedINFBySentOTA(Log log) {
		if (!"INF".equalsIgnoreCase(log.getPType())) {
			return false;
		}
		List<Log> logs = new ArrayList<>();
		try {
			logs = logRepository.findByUidAndMidAndPType(log.getUid(), log.getMid() != null ? log.getMid() : log.getOid(), "OTA");
		} catch (Exception e) {
			LOG.error("isReceivedINFBySentOTA error", e);
		}
		LOG.info(">INF from OTA OTA length=" + logs.size());
		for (Log l : logs) {
			if ((System.currentTimeMillis() - l.getCreateDate().getTime()) < 1l * 60l * 60l * 1000l) {
				LOG.info(">INF from OTA oid=" + log.getOid());
				return true;
			}
		}
		return false;
	}
	
	private void handleINFRes(Map<String, Object> data, String type, Log log, int status) throws Exception {
		// chua validate resp signature
		Optional<CARequestLog> opt = caRequestLogRepository.findByUidAndMsn(log.getUid() + "", log.getMsn());
		
		if (status == 0) {
			Map<String, Object> payload1 = (Map<String, Object>) data.get("payload");
			Map<String, Object> data1 = (Map<String, Object>) payload1.get("data");
			
			if (opt.isPresent()) {
				Long vendor = opt.get().getVendor().getId();
				
				String[] latestINFFirmwaveRequest = (opt.get().getLatestINFFirmwaveRequest() + "").split("_");
				String nextVersion = firmwareService.getLatestFirmware().get(vendor).getVersion();
				if (latestINFFirmwaveRequest.length >= 4) {
					nextVersion = latestINFFirmwaveRequest[3];
					LOG.debug("handleINFRes selectVersion - uid: {}, vendor: {}, firmware: {}", opt.get().getUid(), vendor, nextVersion);
				}
				// System.currentTimeMillis() + "_INF_" + logP.getMid() + "_" + CommonController.CMD_OPTIONS.get().get("selectVersion")
				if (nextVersion.equals(data1.get("ver"))) {
					status = MqttCmdStatus.UPGRADED_FIRMWARE_VERSION.getStatus();
					log.setHandleSubscribeDesc(MqttCmdStatus.UPGRADED_FIRMWARE_VERSION.getDescription());
					logRepository.save(log);
				} else if (isReceivedINFBySentOTA(log)) {
					status = MqttCmdStatus.UPGRADE_FIRMWARE_VERSION_FAIL.getStatus();
					log.setHandleSubscribeDesc(MqttCmdStatus.UPGRADE_FIRMWARE_VERSION_FAIL.getDescription());
					logRepository.save(log);
					return;
				}
				LOG.debug("handleINFRes - uid: {}, vendor: {}, firmware: {}", opt.get().getUid(), vendor, nextVersion);
			}
			if (data1.get("ver") != null) {
				LOG.debug("handleINFRes saving resp");
				if (opt.isPresent()) {
					opt.get().setVer(data1.get("ver") + "");
					if (data1.get("rdti") != null) {
						opt.get().setReadInterval(Long.parseLong(data1.get("rdti") + ""));
					}
					if (data1.get("pdti") != null) {
						opt.get().setInterval(Long.parseLong(data1.get("pdti") + ""));
					}
					caRequestLogRepository.save(opt.get());
				}
			}
			if ("TCM_INFO".equalsIgnoreCase(CommonController.MID_TYPE.get(log.getOid()))) {
				LOG.debug("GET TCM_INFO: " + log.getOid() + " " + log.getMsn());
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
			String urlS3 = "";
			Map<String, Object> mapPl = new LinkedHashMap<>();
			if (opt.isPresent()) {
				Long vendor = opt.get().getVendor().getId();
				
				String[] latestINFFirmwaveRequest = (opt.get().getLatestINFFirmwaveRequest() + "").split("_");
				String nextVersion = firmwareService.getLatestFirmware().get(vendor).getVersion();
				if (latestINFFirmwaveRequest.length >= 4) {
					nextVersion = latestINFFirmwaveRequest[3];
					LOG.debug("handleINFRes selectVersion - uid: {}, vendor: {}, firmware: {}", opt.get().getUid(), vendor, nextVersion);
				}
				
				List<Firmware> firmwares = firmwareRepository.findByVersionAndVendorId(nextVersion, vendor);
				Firmware firmware = null;
				if (firmwares.isEmpty()) {
					firmware = firmwareService.getLatestFirmware().get(vendor);
				} else {
					firmware = firmwares.get(0);
				}
				
				urlS3 = getS3URL(vendor, firmware.getVersion() + "/" + firmware.getFileName());
				mapPl.put("ver", firmware.getVersion());
				mapPl.put("hash", firmware.getHashCode());
				mapPl.put("url", urlS3);
				LOG.debug("handleINFRes - uid: {}, url: {}, vendor: {}, firmware: {}", opt.get().getUid(), urlS3, vendor, firmware.getVersion());
			}
			
			if (log.getMid() == null && opt.isPresent()) {
				log.setMid(nextvalMID(opt.get().getVendor()));
			}
			//Publish
			data = new HashMap<>();
			Map<String, Object> header = new HashMap<>();
			data.put("header", header);
			header.put("mid", log.getMid() == null ? log.getOid() : log.getMid());
			header.put("uid", log.getUid());
			header.put("gid", log.getUid());
			header.put("msn", log.getMsn());
			
			Map<String, Object> payload = new HashMap<>();
			data.put("payload", payload);
			payload.put("id", log.getUid());
			payload.put("cmd", "OTA");
			payload.put("p1", mapPl);
			String sig = opt.isPresent() && BooleanUtils.isTrue(opt.get().getVendor().getEmptySig()) ? "" : RSAUtil.initSignedRequest(pkPath, new ObjectMapper().writeValueAsString(payload));
			header.put("sig", sig);
			publish(alias + log.getUid(), data, type);

			//update last OTA time
			if (opt.isPresent()) {
				opt.get().setIsOta(true);
				opt.get().setLastOtaDate(Calendar.getInstance().getTimeInMillis());
				caRequestLogRepository.save(opt.get());
			}
			localMap.getOtaMap().put(log.getMid(), Calendar.getInstance().getTimeInMillis());
		} else if (!isReceivedINFBySentOTA(log)) {
			data = new HashMap<>();
			Map<String, Object> header = new HashMap<>();
			data.put("header", header);
			header.put("mid", log.getMid() == null ? log.getOid() : log.getMid());
			header.put("uid", log.getUid());
			header.put("gid", log.getUid());
			header.put("msn", log.getMsn());
			header.put("status", status);
			
			Map<String, Object> payload = new HashMap<>();
			data.put("payload", payload);
			payload.put("id", log.getUid());
			payload.put("cmd", "OTA");
			publish(alias + log.getUid(), data, type);
		}
	}
	
	private void handleACTRes(Map<String, Object> data, String type, Log log, int status) throws Exception {

		Optional<CARequestLog> opt = caRequestLogRepository.findByUid(log.getUid());
		if (opt.isPresent()) {
			opt.get().setLastACTDate(System.currentTimeMillis());
			caRequestLogRepository.save(opt.get());
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

//		https://powerautomationsg.atlassian.net/browse/MMS-93
//		xuejiang.qing
//		Backgroud,
//		The OBR test should be done during Provision step 1, allow the MCU without Meter can send OBR to MMS to do virtual onboarding test(no couple with meter). 
//		MMS Server should reply the virtual onboarding message as normal OBR if the Meter ID with  “20200101001001” is in the OBR. just not check the if Meter ID is in system or not.  
		
//		if (status == 0) {
//			status = validateUidAndMsn(log);
//		}
 
		if (StringUtils.isBlank(log.getMsn())) {
			log.setMsn("20200101001001");
		}
//		https://powerautomationsg.atlassian.net/browse/MMS-93 END

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
			
			Optional<CARequestLog> opt = caRequestLogRepository.findByUid(log.getUid());
			String sig = "";
			
			try {
				sig = opt.isPresent() && BooleanUtils.isTrue(opt.get().getVendor().getEmptySig()) ? "" : RSAUtil.initSignedRequest(masterPkPath, new ObjectMapper().writeValueAsString(payload));
			} catch (Exception e) {
				LOG.error(e.getMessage(), e);
			}
			header.put("sig", sig);

			publish(alias + log.getUid(), data, type);
			//put mid to check receive response or not
			localMap.getOnboardingMap().put(log.getMid(), Calendar.getInstance().getTimeInMillis());
			if (opt.isPresent()) {
				opt.get().setLastOBRDate(System.currentTimeMillis());
				opt.get().setLastACTDate(System.currentTimeMillis());
				caRequestLogRepository.save(opt.get());
			}
		}
	}
	
	private void handleOnSubscribe(final MqttMessage mqttMessage) {
		try {
			Map<String, Object> data = MAPPER.readValue(mqttMessage.getPayload(), Map.class);
			Map<String, Object> header = (Map<String, Object>) data.get("header");
			Map<String, Object> payload = (Map<String, Object>) data.get("payload");
			String type = (String) payload.get("type");
			
			//save log
			Log log = Log.build(data, "SUBSCRIBE");
			log.setMqttAddress(evsPAMQTTAddress);
			log.setTopic(evsPASubscribeTopic);
			LOG.debug(">Subscribe " + log.getMid() + " " + log.getMsn() + " " + log.getPType() + " " + evsPASubscribeTopic);
			if (!"MDT".equalsIgnoreCase(type)) {
				logRepository.save(log);
				updatePublishStatus(log);				
			}

			if (validateUid(log) == MqttCmdStatus.NOTFOUND_DEVICE.getStatus()) {
				LOG.debug("Device notfound: " + log.getUid());
				return;
			}
			
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
			
			checkECH(data, type, log, status);
			
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
			
			if ("ACT".equalsIgnoreCase(type)) {
				handleACTRes(data, type, log, status);
			}
			
			checkECH(data, type, log, status);

			if (localMap.getLocalMap().get(log.getOid()) != null && !"RLS".equalsIgnoreCase(type)) {
				LOG.debug("Handle LocalMap resp .... ");
				header.put("oid", localMap.getLocalMap().get(log.getOid()));
				header.remove("uid");
				if(data.get("payload") != null) {
					Map<String, Object> payload = (Map<String, Object>) data.get("payload");
					if(payload.get("id") != null){
						payload.put("id", header.get("msn"));
					}
				}
				publish(evsMeterLocalRespSubscribeTopic, data, type);
				localMap.getLocalMap().remove(log.getOid());
			} else if (localMap.getOnboardingMap().get(log.getOid()) != null && !"RLS".equalsIgnoreCase(type)) {
				LOG.debug("Handle OnboardingMap resp .... ");
				Optional<CARequestLog> opt = caRequestLogRepository.findByUidAndMsn(log.getUid() + "", log.getMsn());
				if (log.getStatus() == 0) {
					if (opt.isPresent()) {
						opt.get().setOnboardingDatetime(Calendar.getInstance().getTimeInMillis());
						caRequestLogRepository.save(opt.get());
					}
				} else {
					LOG.debug("Onboarding process fail, MID = {}", log.getRmid());
				}
				localMap.getOnboardingMap().remove(log.getOid());
			} else if (localMap.getOtaMap().get(log.getOid()) != null && !"RLS".equalsIgnoreCase(type)) {
				LOG.debug("Handle OtaMap resp .... ");
				Optional<CARequestLog> opt = caRequestLogRepository.findByUidAndMsn(log.getUid() + "", log.getMsn());
				if (opt.isPresent()) {
					opt.get().setIsOta(false);
					opt.get().setLastOtaDate(Calendar.getInstance().getTimeInMillis());
					caRequestLogRepository.save(opt.get());
				}
				localMap.getOtaMap().remove(log.getOid());
			} else if (localMap.getCfgMap().get(log.getOid()) != null && !"RLS".equalsIgnoreCase(type)) {
				LOG.debug("Handle CfgMap resp .... ");
				Optional<CARequestLog> opt = caRequestLogRepository.findByUidAndMsn(log.getUid() + "", log.getMsn());
				if (log.getStatus() == 0) {
					if (localMap.getCfgMap().get(log.getOid()).get("rdti") != null) {
						opt.get().setReadInterval(Long.parseLong(localMap.getCfgMap().get(log.getOid()).get("rdti") + ""));
					}
					if (localMap.getCfgMap().get(log.getOid()).get("pdti") != null) {
						opt.get().setInterval(Long.parseLong(localMap.getCfgMap().get(log.getOid()).get("pdti") + ""));
					}
					caRequestLogRepository.save(opt.get());
				}
				localMap.getCfgMap().remove(log.getOid());
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

	private void updateLastMDTSubscribe(Log log) {
		Optional<CARequestLog> opt = caRequestLogRepository.findByUidAndMsn(log.getUid() + "", log.getMsn());
		if (opt.isPresent()) {
			opt.get().setStatus(DeviceStatus.ONLINE);
			opt.get().setLastSubscribeDatetime(Calendar.getInstance().getTimeInMillis());
			opt.get().setLastMdtDate(Calendar.getInstance().getTimeInMillis());
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
	
	private void handleOnM3ModuleRequestSubscribe(final String topic, final MqttMessage mqttMessage) {
		try {
//			/check_ac_status
//			/check_ac_lock_status
//			These 2 command has a response/reply after send the MQTT message to the pi, the pi will read the status and response back with topic pa/evs/ntu/<meter_sn> and with message 
//			'Read Aircon Status for 202206000520 (3), Status: AirCon_Status: read failed, Coil: None'
//			'Read Lock Status for 202206000520(3), Status: Lock_Status: read failed, Coil: None'
			String raw = new String(mqttMessage.getPayload());
			String msn = topic.replace(AppProps.get("m3.pa.evs.ntu", "pa/evs/ntu/"), "");
			CARequestLog dv = caRequestLogRepository.findByMsn(msn).orElse(new CARequestLog());
			//save log
			Log log = Log.builder()
					.msn(msn)
					.uid(dv.getUid())
					.topic(topic)
					.type("SUBSCRIBE")
					.raw(raw)
					.pType(raw.toLowerCase().contains("read aircon status") ? "airconstatus" : raw.toLowerCase().contains("read lock status") ? "lockstatus" : "unknown")
					.build();
			log.setMqttAddress(evsPAMQTTAddress);
			LOG.debug(">Subscribe 4M3Module " + topic + " -> " + raw);
			logRepository.save(log);
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
			Long nextMid = nextvalMID(caRequestLog.get().getVendor());
			localMap.getLocalMap().put(nextMid, mid.longValue());
			header.put("mid", nextMid);
			header.put("uid", caRequestLog.get().getUid());
			header.put("gid", caRequestLog.get().getUid());
			payload.put("id", caRequestLog.get().getUid());
			String sig = BooleanUtils.isTrue(caRequestLog.get().getVendor().getEmptySig()) ? "" : RSAUtil.initSignedRequest(pkPath, new ObjectMapper().writeValueAsString(payload));
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
				if ("true".equalsIgnoreCase(AppProps.get("mqtt.subscribe.use.threadpool", "false"))) {
					EX.submit(() -> handleOnSubscribe(mqttMessage));
				} else {
					new Thread(() -> {handleOnSubscribe(mqttMessage);}).start();
				}
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
				if ("true".equalsIgnoreCase(AppProps.get("mqtt.subscribe.use.threadpool", "false"))) {
					EX.submit(() -> handleOnRespSubscribe(mqttMessage));
				} else {
					new Thread(() -> {handleOnRespSubscribe(mqttMessage);}).start();
				}
				return null;
			});
		} catch (Exception e) {
			LOG.error(e.getMessage(), e);
		}

		try {
			Mqtt.subscribe(Mqtt.getInstance(evsPAMQTTAddress, mqttClientId), evsMeterLocalSubscribeTopic, QUALITY_OF_SERVICE, o -> {
				final MqttMessage mqttMessage = (MqttMessage) o;
				LOG.info(evsMeterLocalSubscribeTopic + " -> " + new String(mqttMessage.getPayload()));
				LOG.info(evsPARespSubscribeTopic + " -> " + new String(mqttMessage.getPayload()));
				if ("true".equalsIgnoreCase(AppProps.get("mqtt.subscribe.use.threadpool", "false"))) {
					EX.submit(() -> handleOnLocalRequestSubscribe(mqttMessage));
				} else {
					new Thread(() -> {handleOnLocalRequestSubscribe(mqttMessage);}).start();
				}
				return null;
			});
		} catch (Exception e) {
			LOG.error(e.getMessage(), e);
		}
		
		
		/// M3 Module subscribe 
		/// https://powerautomationsg.atlassian.net/browse/BE-251
		try {
			Mqtt.subscribe(Mqtt.getInstance(evsPAMQTTAddress, mqttClientId), AppProps.get("m3.pa.evs.ntu", "pa/evs/ntu/") + "+", QUALITY_OF_SERVICE, (o, topic) -> {
				final MqttMessage mqttMessage = (MqttMessage) o;
				LOG.info(topic + " -> " + new String(mqttMessage.getPayload()));
				if ("true".equalsIgnoreCase(AppProps.get("mqtt.subscribe.use.threadpool", "false"))) {
					EX.submit(() -> handleOnM3ModuleRequestSubscribe((String) topic, mqttMessage));
				} else {
					new Thread(() -> {handleOnM3ModuleRequestSubscribe((String) topic, mqttMessage);}).start();
				}
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
	
	@Scheduled(cron = "${cron.schedule.subscribe:0 0/5 * * * ?}")
	public void scheduleSubscribe() {
		LOG.info("cron subscribe");
		subscribe();
	}
	
	@PostConstruct
	public void init() {

		LOG.debug("Init EVSPAServiceImpl ... ");
		prepareFolder();
		
		try {
			subscribe();
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
			if (fs == null) return;
			for (File f : fs) {
				if (f.exists() && f.isFile() && f.getName().endsWith(".zip")) {
					File tempFile = null;
					try {
						tempFile = File.createTempFile("temp", ".zip");
					} catch (IOException e2) {
						e2.printStackTrace();
					}
					String name = f.getName();
					String vendor = name.substring(0, name.indexOf("-"));
					
					try (ZipFile zipFile = new ZipFile(f.getAbsolutePath())) {
					    Enumeration<? extends ZipEntry> entries = zipFile.entries();
					    try (ZipOutputStream out = new ZipOutputStream(new FileOutputStream(tempFile.getAbsolutePath()))) {
					        while (entries.hasMoreElements()) {
					            ZipEntry entry = entries.nextElement();
					            ZipEntry newEntry = null;
					            String currentName = entry.getName();
					            
					            if (currentName.endsWith(".csv")) {
					            	String uploadedZipRealName = name.substring(name.indexOf("-") + 1, name.length());
					            	newEntry = new ZipEntry(vendor + "-" + uploadedZipRealName + "---" + currentName);
					            } else {
					            	newEntry = new ZipEntry(vendor + "-" + currentName);
					            }
					            
				                InputStream in = zipFile.getInputStream(entry);
				                out.putNextEntry(newEntry);
				                byte[] buffer = new byte[1024];
				                int len;
				                while ((len = in.read(buffer)) > 0) {
				                    out.write(buffer, 0, len);
				                }
				                in.close();
					        }
					    } catch (IOException e) {
							e.printStackTrace();
						}
					} catch (IOException e1) {
						e1.printStackTrace();
					}
					
					ZipUtils.unzip(tempFile.getAbsolutePath(), evsDataFolder + "/IN_CSR");
					File out = new File(evsDataFolder + "/OUT_CSR/" + f.getName() + '.' + System.currentTimeMillis());
					f.renameTo(out);
				}
			}
		}, "UNZIP");
		
		SchedulerHelper.scheduleJob("0/1 * * * * ? *", () -> {
			File f = new File(evsDataFolder + "/IN_CSR");
			if (f.listFiles() == null) return;
			for (File ful : f.listFiles()) {
				if (ful.isFile() && ful.getName().endsWith(".csv")) {
					String name = ful.getName();						
					Long vId = Long.parseLong(name.substring(0, name.indexOf("-")));
					Optional<Vendor> vendorOpt = vendorRepository.findById(vId);
					
					if (!vendorOpt.isPresent()) {
						continue;
					}
					try {
						String zipFileName = name.substring(name.indexOf("-") + 1, name.indexOf("---"));
						String csvFileName = name.substring(name.indexOf("---") + 3, name.length());
						String[] lines = new String(Files.readAllBytes(ful.toPath()), StandardCharsets.UTF_8).split("\r*\n");
						if (StringUtils.isBlank(lines[0]) || !lines[0].toUpperCase().startsWith("SN,UUID,CID")) {
							LOG.info("Uploaded file has wrong format. Headers should start with: SN UUID CID. Uploaded zip file: {}, CSV file: {}, Headers: {}", zipFileName, csvFileName, lines[0]);
							Files.delete(ful.toPath());
							continue;
						}
						for (int i = 1; i < lines.length; i++) {
							String[] details = lines[i].split(" *, *");
							if (details.length > 2) {
								String uuid = details[1];
								
								if (StringUtils.isBlank(uuid) || StringUtils.isBlank(details[0]) || StringUtils.isBlank(details[2])) {
									LOG.info("UUID, SN, CID are required. Zip file: {}, CSV file: {}, UUID: {}, SN: {}, CID: {}.", zipFileName, csvFileName, uuid, details[0], details[2]);
									continue;
								}
								
								Optional<CARequestLog> opt = caRequestLogRepository.findByUid(uuid);
								CARequestLog caLog = !opt.isPresent() ? new CARequestLog() : opt.get();
								
								if (caLog.getId() == null && caRequestLogRepository.findByCid(details[2]).isPresent()) {
									LOG.info("eSIM Id exists: " + details[2] + " where insert new UID: " + uuid);
									continue;
								}
								if (caLog.getStatus() == null) {
									caLog.setStatus(DeviceStatus.OFFLINE);	
								}
								if (caLog.getType() == null) {
									caLog.setType(DeviceType.NOT_COUPLED);	
								}
								
								caLog.setUid(uuid);
								caLog.setSn(details[0]);
								caLog.setCid(details[2]);
								caLog.setRequireRefresh(false);
								caLog.setVendor(vendorOpt.get());
								caRequestLogRepository.save(caLog);
								caRequestLogRepository.flush();
								caRequestLogService.updateCacheUidMsnDevice(caLog.getUid(), "update");
							}
						}
						Files.delete(ful.toPath());
					} catch (Exception e) {
						LOG.error(e.getMessage(), e);
					}
					
				}
				if (ful.isFile() && ful.getName().endsWith(".csr")) {
					String name = ful.getName();						
					Long vId = Long.parseLong(name.substring(0, name.indexOf("-")));
					Optional<Vendor> vendorOpt = vendorRepository.findById(vId);
					
					if (!vendorOpt.isPresent()) {
						continue;
					}
					try {
						String uuid = ful.getName().replaceAll("\\.csr$", "").replaceAll("(^[0-9]+-)", "");
						
						Optional<CARequestLog> opt = caRequestLogRepository.findByUid(uuid);
						CARequestLog caLog = !opt.isPresent() ? new CARequestLog() : opt.get();
						//Map<String, Object> data = requestCA(caRequestUrl, new FileSystemResource(ful), uuid);
						caLog.setUid(uuid);
						//caLog.setCertificate((String)data.get("pemBase64"));
						//caLog.setRaw((String)data.get("cas"));
						//caLog.setStartDate((Long)data.get("startDate"));
						//caLog.setEndDate((Long)data.get("endDate"));
//						caLog.setMsn(null);
						if (caLog.getStatus() == null) {
							caLog.setStatus(DeviceStatus.OFFLINE);	
						}
						if (caLog.getType() == null) {
							caLog.setType(DeviceType.NOT_COUPLED);	
						}
						caLog.setEnrollmentDatetime(Calendar.getInstance().getTimeInMillis());
						caLog.setRequireRefresh(false);
						caLog.setVendor(vendorOpt.get());
						caRequestLogRepository.save(caLog);
						caRequestLogRepository.flush();
						caRequestLogService.updateCacheUidMsnDevice(caLog.getUid(), "update");
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
		
		SchedulerHelper.scheduleJob("0 0 1 * * ? *", () -> {
			//removePiLlog
			try {
				caRequestLogService.removePiLlog();
			} catch (Exception e) {
				//
			}
			
		}, "removePiLlog");
		
		caRequestLogService.updateCacheUidMsnDevice(null, null);
		SchedulerHelper.scheduleJob("0 0/2 * * * ? *", () -> {
			//REFRESH_CACHE_UID_MSN
			try {
				caRequestLogService.updateCacheUidMsnDevice(null, null);
				LOG.info("refresh map uid-msn -> " + localMap.getUidMsnMap().size() + " devices");
			} catch (Exception e) {
				//
			}
			
		}, "REFRESH_CACHE_UID_MSN");
		
		try {
			initS3();
		} catch (Exception e) {/**/}

		try {
			vendorRepository.findAll()
			.forEach(vendor -> {
				if (vendor.getMaxMidValue() == null) {
					vendor.setMaxMidValue(!"Default".equalsIgnoreCase(vendor.getName()) ? 65535l : 4294967295l);
					vendorRepository.save(vendor);
				}
				try {
					sequenceService.createMIDSeq(vendor.getId());
					Number lastValue = sequenceService.nextvalMID(vendor.getId());
					if (lastValue.longValue() < 10000l) {
						sequenceService.nextvalMID(10000l, vendor.getId());
					}
				} catch (Exception e) {
					
					e.printStackTrace();
				}
					
			});
        } catch (Exception e) {
            //
        }
		
		
		try {
            caRequestLogService.getCids(true);
        } catch (Exception e) {
            //
        }
		
		Optional<ScreenMonitoring> optSystem = screenMonitoringRepository.findByKey(ScreenMonitorKey.SYSTEM_START);
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		
		try {
		    checkAndSaveScreenMonitoring(optSystem, sdf.format(new Date()), ScreenMonitorStatus.OK, ScreenMonitorKey.SYSTEM_START);
		} catch (Exception e) {
		    checkAndSaveScreenMonitoring(optSystem, sdf.format(new Date()), ScreenMonitorStatus.NOT_OK, ScreenMonitorKey.SYSTEM_START);
		}
		
		Optional<CARequestLog> optCaServer = caRequestLogRepository.findByUid("server.csr");
		Optional<ScreenMonitoring> optServer = screenMonitoringRepository.findByKey(ScreenMonitorKey.SERVER_CERTIFICATE);
		try {
		    Long expiredDate = optCaServer.get().getEndDate();
		    if (expiredDate > System.currentTimeMillis()) {
		        checkAndSaveScreenMonitoring(optServer, sdf.format(new Date(expiredDate)), ScreenMonitorStatus.OK, ScreenMonitorKey.SERVER_CERTIFICATE);
		    } else {
		        checkAndSaveScreenMonitoring(optServer, sdf.format(new Date(expiredDate)), ScreenMonitorStatus.EXPIRED, ScreenMonitorKey.SERVER_CERTIFICATE);
		    }
        } catch (Exception e) {
            checkAndSaveScreenMonitoring(optServer, "N/A", ScreenMonitorStatus.NOT_OK, ScreenMonitorKey.SERVER_CERTIFICATE);
        }
	}
	
	private void checkAndSaveScreenMonitoring (Optional<ScreenMonitoring> smOpt, String value, ScreenMonitorStatus status, ScreenMonitorKey key) {
        if (smOpt.isPresent()) {
            ScreenMonitoring sm = smOpt.get();
            sm.setStatus(status);
            sm.setValue(value);
            screenMonitoringRepository.save(sm);
        } else {
            ScreenMonitoring sm = new ScreenMonitoring();
            sm.setKey(key);
            sm.setStatus(status);
            sm.setValue(value);
            screenMonitoringRepository.save(sm);
        }
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
		
        try {
    		software.amazon.awssdk.auth.credentials.AwsCredentialsProvider a = new software.amazon.awssdk.auth.credentials.AwsCredentialsProvider() {
    			@Override
    			public software.amazon.awssdk.auth.credentials.AwsCredentials resolveCredentials() {
    				return software.amazon.awssdk.auth.credentials.AwsBasicCredentials.create(accessID, accessKey);
    			}};
    		snsClient = software.amazon.awssdk.services.sns.SnsClient.builder().region(software.amazon.awssdk.regions.Region.AP_SOUTHEAST_1)
    				.credentialsProvider(a).build();
		} catch (Exception e) {
			LOG.error(e.getMessage(), e);
		}
        
        try {
        	com.amazonaws.auth.AWSCredentialsProvider a = new com.amazonaws.auth.AWSCredentialsProvider() {

				@Override
				public AWSCredentials getCredentials() {
					return new BasicAWSCredentials(accessID, accessKey);
				}

				@Override
				public void refresh() {
				}
        	};
        	sesClient = com.amazonaws.services.simpleemail.AmazonSimpleEmailServiceClientBuilder.standard()
        			.withCredentials(a)
        	        .withRegion(com.amazonaws.regions.Regions.AP_SOUTHEAST_1).build();
		} catch (Exception e) {
			LOG.error(e.getMessage(), e);
		}
        // pubTextSMS(snsClient, "OTP: 123456", "+84909123456");
        
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
	
	public String getS3URL(Long vendor, String objectKey) {
		if (fakeS3Url) {
			LOG.debug("Using fake s3 url");
			return "http://gridhutautomation.com/pa-meter-2.bin";
		}
		if (vendor == null) {
			vendor = 1l; //Default vendor
		}
		String bcName = bucketName + "/" + vendor + "/" + objectKey;
		LOG.info("getS3URL: " + bcName);
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		CMD.exec("/usr/local/aws/bin/aws s3 presign \"s3://" + bcName + "\" --expires-in " + (60 * expireTime), null, bos);
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
        		Long mId = log.getOid() == null ? log.getMid() : log.getOid();
        		logRepository.updateStatus(status, mId, log.getUid());
        	}
		} catch (Exception e) {
			LOG.error(e.getMessage(), e);
		}
    }
	
	public static void main4(String[] args) throws Exception {
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

		String json = "{\"header\":{\"mid\":10181,\"uid\":\"89049032000001000000128256736680\",\"gid\":\"89049032000001000000128256736680\",\"msn\":\"202206000057\",\"sig\":\"\"},\"payload\":{\"id\":\"89049032000001000000128256736680\",\"type\":\"OBR\",\"data\":\"202206000057\"}}";

		String evsPAMQTTAddress = null;
		String mqttClientId = System.currentTimeMillis() + "";
		evsPAMQTTAddress = "tcp://13.229.153.249:1883";
		
		String topic = "evs/pa/data";

		int[] counts = new int[] {0};
		ExecutorService ex = Executors.newFixedThreadPool(1);
		Mqtt.subscribe(Mqtt.getInstance(evsPAMQTTAddress, mqttClientId), "evs/pa/data", 2, o -> {
			final MqttMessage mqttMessage = (MqttMessage) o;
			LOG.info(topic + " -> " + new String(mqttMessage.getPayload()));
			ex.submit(() -> {
				counts[0] = counts[0] + 1;
				System.out.println("count->" + counts[0]);
			});
			return null;
		});
		System.out.println(1 << 30);
		// Mqtt.publish(Mqtt.getInstance(evsPAMQTTAddress, mqttClientId), topic, new ObjectMapper().readValue(json, Map.class), 2, false);

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
	
	public static void main(String[] args) throws Exception {
		String evsPAMQTTAddress = "ssl://3.1.87.138:8883";
		String mqttClientId = System.currentTimeMillis() + "";
		
		Mqtt.publish(Mqtt.getInstance(evsPAMQTTAddress, mqttClientId), "pa/evs/ntu/202206000056", "Read Aircon Status for 202206000520 (3), Status: AirCon_Status: read failed, Coil: false", 2, false);
		Mqtt.publish(Mqtt.getInstance(evsPAMQTTAddress, mqttClientId), "pa/evs/ntu/202206000056", "Read Lock Status for 202206000520(3), Status: Lock_Status: read failed, Coil: None", 2, false);
	}
	
	public static void main3(String[] args) throws Exception {
		String evsPAMQTTAddress = "tcp://18.142.166.146:1883";
		String mqttClientId = System.currentTimeMillis() + "";
		
		String topic = "dev/evs/pa/data";

		int[] counts = new int[] {0};
		ExecutorService ex = Executors.newFixedThreadPool(1);
		Mqtt.subscribe(Mqtt.getInstance(evsPAMQTTAddress, mqttClientId), "dev/evs/pa/JMETER001", 2, o -> {
			final MqttMessage mqttMessage = (MqttMessage) o;
			LOG.info("dev/evs/pa/JMETER001 -> " + new String(mqttMessage.getPayload()));
			ex.execute(() -> {
				counts[0] = counts[0] + 1;
				System.out.println("count->" + counts[0]);
			});
			return null;
		});
		
		String json = "{\"header\":{\"mid\":15246,\"uid\":\"JMETER001\",\"gid\":\"JMETER001\",\"msn\":\"20230906000001\",\"sig\":\"\"},\"payload\":{\"id\":\"JMETER001\",\"type\":\"MDT\",\"data\":[{\"uid\":\"JMETER001\",\"msn\":\"20230906000001\",\"kwh\":\"8.584\",\"kw\":\"0.0\",\"i\":\"0.0\",\"v\":\"235.0\",\"pf\":\"1.0000\",\"dt\":\"2023-09-07T16:40:13\"},{\"uid\":\"JMETER001\",\"msn\":\"20230906000001\",\"kwh\":\"8.584\",\"kw\":\"0.0\",\"i\":\"0.0\",\"v\":\"235.0\",\"pf\":\"1.0000\",\"dt\":\"2023-09-07T16:55:14\"},{\"uid\":\"JMETER001\",\"msn\":\"20230906000001\",\"kwh\":\"8.584\",\"kw\":\"0.0\",\"i\":\"0.0\",\"v\":\"236.0\",\"pf\":\"1.0000\",\"dt\":\"2023-09-07T17:10:15\"},{\"uid\":\"JMETER001\",\"msn\":\"20230906000001\",\"kwh\":\"8.584\",\"kw\":\"0.0\",\"i\":\"0.0\",\"v\":\"236.0\",\"pf\":\"1.0000\",\"dt\":\"2023-09-07T17:25:16\"},{\"uid\":\"JMETER001\",\"msn\":\"20230906000001\",\"kwh\":\"8.584\",\"kw\":\"0.0\",\"i\":\"0.0\",\"v\":\"236.0\",\"pf\":\"1.0000\",\"dt\":\"2023-09-07T17:40:17\"},{\"uid\":\"JMETER001\",\"msn\":\"20230906000001\",\"kwh\":\"8.584\",\"kw\":\"0.0\",\"i\":\"0.0\",\"v\":\"237.0\",\"pf\":\"1.0000\",\"dt\":\"2023-09-07T17:55:18\"},{\"uid\":\"JMETER001\",\"msn\":\"20230906000001\",\"kwh\":\"8.584\",\"kw\":\"0.0\",\"i\":\"0.0\",\"v\":\"236.0\",\"pf\":\"1.0000\",\"dt\":\"2023-09-07T18:10:18\"},{\"uid\":\"JMETER001\",\"msn\":\"20230906000001\",\"kwh\":\"8.584\",\"kw\":\"0.0\",\"i\":\"0.0\",\"v\":\"237.0\",\"pf\":\"1.0000\",\"dt\":\"2023-09-07T18:25:19\"},{\"uid\":\"JMETER001\",\"msn\":\"20230906000001\",\"kwh\":\"8.584\",\"kw\":\"0.0\",\"i\":\"0.0\",\"v\":\"237.0\",\"pf\":\"1.0000\",\"dt\":\"2023-09-07T18:40:20\"},{\"uid\":\"JMETER001\",\"msn\":\"20230906000001\",\"kwh\":\"8.584\",\"kw\":\"0.0\",\"i\":\"0.0\",\"v\":\"236.0\",\"pf\":\"1.0000\",\"dt\":\"2023-09-07T18:55:21\"},{\"uid\":\"JMETER001\",\"msn\":\"20230906000001\",\"kwh\":\"8.584\",\"kw\":\"0.0\",\"i\":\"0.0\",\"v\":\"236.0\",\"pf\":\"1.0000\",\"dt\":\"2023-09-07T19:10:21\"},{\"uid\":\"JMETER001\",\"msn\":\"20230906000001\",\"kwh\":\"8.584\",\"kw\":\"0.0\",\"i\":\"0.0\",\"v\":\"236.0\",\"pf\":\"1.0000\",\"dt\":\"2023-09-07T19:25:23\"},{\"uid\":\"JMETER001\",\"msn\":\"20230906000001\",\"kwh\":\"8.584\",\"kw\":\"0.0\",\"i\":\"0.0\",\"v\":\"236.0\",\"pf\":\"1.0000\",\"dt\":\"2023-09-07T19:40:24\"},{\"uid\":\"JMETER001\",\"msn\":\"20230906000001\",\"kwh\":\"8.584\",\"kw\":\"0.0\",\"i\":\"0.0\",\"v\":\"236.0\",\"pf\":\"1.0000\",\"dt\":\"2023-09-07T19:55:24\"},{\"uid\":\"JMETER001\",\"msn\":\"20230906000001\",\"kwh\":\"8.584\",\"kw\":\"0.0\",\"i\":\"0.0\",\"v\":\"237.0\",\"pf\":\"1.0000\",\"dt\":\"2023-09-07T20:10:26\"},{\"uid\":\"JMETER001\",\"msn\":\"20230906000001\",\"kwh\":\"8.584\",\"kw\":\"0.0\",\"i\":\"0.0\",\"v\":\"238.0\",\"pf\":\"1.0000\",\"dt\":\"2023-09-07T20:25:27\"},{\"uid\":\"JMETER001\",\"msn\":\"20230906000001\",\"kwh\":\"8.584\",\"kw\":\"0.0\",\"i\":\"0.0\",\"v\":\"236.0\",\"pf\":\"1.0000\",\"dt\":\"2023-09-07T20:40:27\"},{\"uid\":\"JMETER001\",\"msn\":\"20230906000001\",\"kwh\":\"8.584\",\"kw\":\"0.0\",\"i\":\"0.0\",\"v\":\"236.0\",\"pf\":\"1.0000\",\"dt\":\"2023-09-07T20:55:28\"},{\"uid\":\"JMETER001\",\"msn\":\"20230906000001\",\"kwh\":\"8.584\",\"kw\":\"0.0\",\"i\":\"0.0\",\"v\":\"237.0\",\"pf\":\"1.0000\",\"dt\":\"2023-09-07T21:10:29\"},{\"uid\":\"JMETER001\",\"msn\":\"20230906000001\",\"kwh\":\"8.584\",\"kw\":\"0.0\",\"i\":\"0.0\",\"v\":\"236.0\",\"pf\":\"1.0000\",\"dt\":\"2023-09-07T21:25:30\"},{\"uid\":\"JMETER001\",\"msn\":\"20230906000001\",\"kwh\":\"8.584\",\"kw\":\"0.0\",\"i\":\"0.0\",\"v\":\"237.0\",\"pf\":\"1.0000\",\"dt\":\"2023-09-07T21:40:31\"},{\"uid\":\"JMETER001\",\"msn\":\"20230906000001\",\"kwh\":\"8.584\",\"kw\":\"0.0\",\"i\":\"0.0\",\"v\":\"238.0\",\"pf\":\"1.0000\",\"dt\":\"2023-09-07T21:55:32\"},{\"uid\":\"JMETER001\",\"msn\":\"20230906000001\",\"kwh\":\"8.584\",\"kw\":\"0.0\",\"i\":\"0.0\",\"v\":\"237.0\",\"pf\":\"1.0000\",\"dt\":\"2023-09-07T22:10:33\"},{\"uid\":\"JMETER001\",\"msn\":\"20230906000001\",\"kwh\":\"8.584\",\"kw\":\"0.0\",\"i\":\"0.0\",\"v\":\"237.0\",\"pf\":\"1.0000\",\"dt\":\"2023-09-07T22:25:33\"}]}}";
		Map<String, Object> payload = new ObjectMapper().readValue(json, Map.class);
		
		System.out.println("start connect");
		// connect
		ExecutorService exPub = Executors.newFixedThreadPool(10000);
		List<Callable<String>> tasks = new ArrayList<>();
		for (int i = 1; i <= 10000; i++) {
			int idx = i;
			tasks.add(() -> {
				String prefix = "java." + idx;
				IMqttAsyncClient client = Mqtt.getInstance(evsPAMQTTAddress, prefix);
				System.out.println(prefix + " -> " + client.getClientId() + " -> " + client.isConnected());				
				Mqtt.publish(client, topic, payload, 2, false);
				return null;
			});
		}
		exPub.invokeAll(tasks);
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	@Override
	public void ping(Pi pi, Boolean isEdit, Boolean isFE) throws Exception {
		
		if (StringUtils.isBlank(pi.getIeiId())) {
			throw new Exception("IEI ID cannot be null!");
		}
		
		String email = SecurityUtils.getEmail();
		if (BooleanUtils.isTrue(isEdit)) {
			Optional<Pi> existingPiOpt = piRepository.findByIeiId(pi.getIeiId());
			if (existingPiOpt.isPresent()) {
				Pi existingPi = existingPiOpt.get();
				if (SecurityUtils.getEmail() != null) {
					existingPi.setLocation(StringUtils.isNotBlank(pi.getLocation()) ? pi.getLocation() : "");
					existingPi.setHide(BooleanUtils.isTrue(pi.getHide()));
					if (existingPi.getEmail() != null) {
						existingPi.setEmail(email);
					}
				} else {
					existingPi.setLastPing(System.currentTimeMillis());
					if (StringUtils.isNotBlank(pi.getUuid())) {
						existingPi.setUuid(pi.getUuid());
					}
				}
				
				piRepository.save(existingPi);
			}
		} else {
			Optional<Pi> existingPiOpt = piRepository.findByIeiId(pi.getIeiId());
			if (existingPiOpt.isPresent()) {
				throw new Exception(String.format("IEI ID: %s already exist!", pi.getIeiId()));
			}
			
			if (email != null) {
				pi.setEmail(email);
			} else {
				pi.setLastPing(System.currentTimeMillis());
			}
			pi.setHide(BooleanUtils.isTrue(pi.getHide()));
			piRepository.save(pi);
		}
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	@Override
	public void ftpRes(String msn, Long mid, String piUuid, String ieiId, String status, String fileName, Long logId) throws Exception {
		//logMDTSent(msn, mid);
		if (StringUtils.isBlank(fileName) || "null".equalsIgnoreCase(fileName)) {
			List<PiLog> logs;
			
			if (logId == null) {
				logs = piLogRepository.findByMsnAndMidAndPiIeiId(msn, mid, ieiId);
			} else {
				logs = piLogRepository.findByMsnAndMidAndPiIeiIdAndLogId(msn, mid, ieiId, logId);
			}
			
			LOG.info("PI Ping3: ftpRes,  " + logId + " " + ieiId + ", " + msn + ", " + status + ", " + mid + ", " + logs.size());
			logs.forEach(log -> {
				log.setFtpResStatus(status);
				piLogRepository.save(log);
			});
		} else {
			Pi pi = piRepository.findByIeiId(ieiId).orElse(null);
			if (pi != null) {
				MeterFileData m = MeterFileData.builder().filename(fileName).ftpResStatus(status).pi(pi).build();
				m.setCreateDate(new Date());
				meterFileDataRepository.save(m);
			}
		}
		Pi pi = new Pi();
 		pi.setIeiId(ieiId);
 		pi.setUuid(piUuid);
		pi.setHide(null);
		this.ping(pi, true, false);
	}
	
	@SuppressWarnings("rawtypes")
	@Override
	public void searchPi(PaginDto pagin) {
		StringBuilder sqlBuilder = new StringBuilder(" ");
		StringBuilder sqlCountBuilder = new StringBuilder(" SELECT count(*) ");
		StringBuilder cmBuilder = new StringBuilder(" FROM Pi WHERE email is not null and (hide is null or hide <> true)");
		sqlBuilder.append(cmBuilder).append(" ORDER BY createDate DESC ");
		sqlCountBuilder.append(cmBuilder);
		
		Long count = ((Number)em.createQuery(sqlCountBuilder.toString()).getSingleResult()).longValue();
		
		Query query = em.createQuery(sqlBuilder.toString());
		query.setFirstResult(pagin.getOffset());
		query.setMaxResults(pagin.getLimit());
		pagin.getResults().clear();
		pagin.setResults(query.getResultList());
		pagin.setTotalRows(count);
	}
	
	@SuppressWarnings("rawtypes")
	@Override
	public void searchBatchLog(PaginDto pagin) {
		StringBuilder sqlBuilder = new StringBuilder(" SELECT l ");
		StringBuilder sqlCountBuilder = new StringBuilder(" SELECT count(*) ");
		StringBuilder cmBuilder = new StringBuilder(" FROM LogBatch l ");
		
		if (pagin.getOptions().get("groupTaskId") != null) {
			cmBuilder.append(" JOIN LogBatchGroupTask lt on (lt.batch.id = l.id and lt.task.id = " + pagin.getOptions().get("groupTaskId") + ") ");
		}
		
		cmBuilder.append(" WHERE 1=1 ");
		
		if (StringUtils.isNotBlank(pagin.getKeyword())) {
			cmBuilder.append(" AND l.uuid like '%" + pagin.getKeyword().trim() + "%' ");
		}
		
		sqlBuilder.append(cmBuilder).append(" ORDER BY l.createDate DESC ");
		sqlCountBuilder.append(cmBuilder);
		
		Long count = ((Number)em.createQuery(sqlCountBuilder.toString()).getSingleResult()).longValue();
		
		Query query = em.createQuery(sqlBuilder.toString());
		query.setFirstResult(pagin.getOffset());
		query.setMaxResults(pagin.getLimit());
		pagin.getResults().clear();
		pagin.setResults(query.getResultList());
		pagin.setTotalRows(count);
	}
	
	@Override
	public void searchBatchLogsByUser (PaginDto<LogBatchDto> pagin) {
		
		Map<String, Object> map = pagin.getOptions();
		
        Long fromDate = (Long) map.get("fromDate");
        Long toDate = (Long) map.get("toDate");
        String userName = (String) map.get("userName");
		
		StringBuilder sqlBuilder = new StringBuilder("select l FROM LogBatch l JOIN Users u ON l.user.userId = u.userId");
        StringBuilder sqlCountBuilder = new StringBuilder("SELECT count(*) FROM LogBatch l JOIN Users u ON l.user.userId = u.userId");

        StringBuilder sqlCommonBuilder = new StringBuilder();
        
        if(userName != null) {
        	sqlCommonBuilder.append(" AND u.username like '%" + userName + "%' ");
        }
        if (fromDate != null) {
            sqlCommonBuilder.append(" AND EXTRACT(EPOCH FROM l.createDate) * 1000 >= " + fromDate);
        }
        if (toDate != null) {
            sqlCommonBuilder.append(" AND EXTRACT(EPOCH FROM l.createDate) * 1000 <= " + toDate);
        }
        
        if(userName == null && fromDate == null && toDate == null ) {
        	sqlCommonBuilder.append(" WHERE 1=1 ");
        }
        sqlBuilder.append(sqlCommonBuilder);
        sqlCountBuilder.append(sqlCommonBuilder);

        if (pagin.getOffset() == null || pagin.getOffset() < 0) {
            pagin.setOffset(0);
        }

        if (pagin.getLimit() == null || pagin.getLimit() <= 0) {
            pagin.setLimit(100);
        }

        Query queryCount = em.createQuery(sqlCountBuilder.toString());

        Long count = ((Number)queryCount.getSingleResult()).longValue();
        pagin.setTotalRows(count);
        pagin.setResults(new ArrayList<>());
        if (count == 0l) {
            return;
        }

        Query query = em.createQuery(sqlBuilder.toString());
        query.setFirstResult(pagin.getOffset());
        query.setMaxResults(pagin.getLimit());

        List<LogBatch> list = query.getResultList();

        list.forEach(li -> {
        	if(Objects.isNull(li.getUser())) {
                LogBatchDto dto = LogBatchDto.builder()
                        .id(li.getId())
                        .createDate(li.getCreateDate())
                        .uuid(li.getUuid())
                        .build();
                pagin.getResults().add(dto);               
        	} else {
        		LogBatchDto dto = LogBatchDto.builder()
                        .id(li.getId())
                        .createDate(li.getCreateDate())
                        .uuid(li.getUuid())
                        .userId(li.getUser().getUserId())
                        .userName(li.getUser().getUsername())
                        .userEmail(li.getUser().getEmail())
                        .build();
                pagin.getResults().add(dto);
        	}
        }); 
	}
	
	@Override
	public List<PiLogDto> searchPiLog(Long piId, String msn, Long mid) {
		List<PiLogDto> rp = new ArrayList<>();
		List<PiLog> data;
		if (piId == null) {
			data = piLogRepository.findByMsnAndMid(msn, mid);
		} else {
			data = piLogRepository.findByMsnAndMidAndPiId(msn, mid, piId);
		}
		data.forEach(d -> rp.add(PiLogDto.builder().type(d.getType()).ftpResStatus(d.getFtpResStatus()).msn(d.getMsn()).mid(d.getMid()).piUuid(d.getPi().getUuid()).build()));
		return rp;
	}
	
	@Override
	@Transactional
	public void createTaskLog(String uuid, Long groupTaskId, Users user) {
		LogBatch batch = LogBatch.builder().uuid(uuid).user(user).build();
		logBatchRepository.save(batch);
		GroupTask task = groupTaskRepository.findById(groupTaskId).orElse(new GroupTask());
		logBatchGroupTaskRepository.save(LogBatchGroupTask.builder().batch(batch).task(task).build());
	}
	
	@Override
	@Transactional
	public String getFileName(String ieiId) {
		String fileName = "";
		Query query = null;
		Pi pi = piRepository.findByIeiId(ieiId).orElse(null);
		
		if (pi != null) {
			Calendar cd1 = Calendar.getInstance();
			Calendar cd2 = Calendar.getInstance();
			cd1.add(Calendar.HOUR_OF_DAY, -2);
			cd2.add(Calendar.DAY_OF_YEAR, -10);
			cd2.set(Calendar.HOUR_OF_DAY, 0);
			cd2.set(Calendar.MINUTE, 0);
			cd2.set(Calendar.SECOND, 0);
			query = em.createQuery("FROM PiLog pl" 
					+ " WHERE pl.pi.id = :piId and pl.fileName is not null "
					+ " and pl.createDate >= :cd2 "
					+ " and pl.createDate <= :cd1 "
					+ " and pl.ftpResStatus <> 'SUCCESS' "
					+ " and pl.piDownloaded = false"
					+ " ORDER BY createDate ASC");
			query.setParameter("cd1", cd1.getTime());
			query.setParameter("cd2", cd2.getTime());
			query.setParameter("piId", pi.getId());
			query.setMaxResults(1);
			List<PiLog> piLogs = query.getResultList();
			if(!CollectionUtils.isEmpty(piLogs)) {
				if(StringUtils.isNotBlank(piLogs.get(0).getFileName())) {
					fileName = piLogs.get(0).getFileName().toString();
				}
			}
		}	
		return fileName;
	}
	
	@Override
	@Transactional(readOnly = true)
	public List<Log> getMDTMessage(Integer limit, String ieiId, String status) {
		StringBuilder sqlBuilder = new StringBuilder("Select l From Log l ");
		sqlBuilder.append(" join PiLog pl on (l.id = pl.logId) ");
		sqlBuilder.append(" where pl.pi.ieiId = '" + ieiId + "' and (pl.pi.hide = false or pl.pi.hide is null) ");
		sqlBuilder.append(" and l.pType = 'MDT' and l.type = 'SUBSCRIBE' ");
		if (StringUtils.isNotBlank(status)) {
			sqlBuilder.append(" and pl.ftpResStatus = '" + status + "' ");
		}
		sqlBuilder.append(" order by l.createDate asc");
		
		Query query = em.createQuery(sqlBuilder.toString());
		query.setFirstResult(0);
		query.setMaxResults(limit != null ? limit : 100);
		return query.getResultList();
	} 
	
	@Override
	@Transactional
	public File getMeterFile(String fileName) {
		File fileResult = null;
		File dir = new File(evsDataFolder + "/meter_file");
		File[] files = dir.listFiles();
		for(File file : files) {
			if(StringUtils.equals(file.getName(), fileName)) {
				fileResult = file;
			}
		}
		return fileResult;
	}
	
	public static void main2(String[] args) {

		args = new String[] {"Test aws sms", "+84933520892"};
		final String usage = "\n" + "Usage: " + "   <message> <phoneNumber>\n\n" + "Where:\n"
				+ "   message - The message text to send.\n\n"
				+ "   phoneNumber - The mobile phone number to which a message is sent (for example, +1XXX5550100). \n\n";

		if (args.length != 2) {
			System.out.println(usage);
			System.exit(1);
		}

		String message = args[0];
		String phoneNumber = args[1];
		software.amazon.awssdk.auth.credentials.AwsCredentialsProvider a = new software.amazon.awssdk.auth.credentials.AwsCredentialsProvider() {
			@Override
			public software.amazon.awssdk.auth.credentials.AwsCredentials resolveCredentials() {
				return software.amazon.awssdk.auth.credentials.AwsBasicCredentials.create("AKIA5FJTG4T6ZJXTTU5I", "+D8Z177y+t29cybFj4elx4D6l2dUUZyDWXFXPAw7");
			}};
		software.amazon.awssdk.services.sns.SnsClient snsClient = software.amazon.awssdk.services.sns.SnsClient.builder().region(software.amazon.awssdk.regions.Region.AP_SOUTHEAST_1)
				.credentialsProvider(a).build();
		EVSPAServiceImpl sv = new EVSPAServiceImpl();
		sv.snsClient = snsClient;
		sv.sendSMS(message, phoneNumber);
		snsClient.close();
	}
	
	public static void main1(String[] args) {

		String message = "<html><body>MMS-1234576</body></html>";
		String email = "ttx.pipo.uit@gmail.com";
		com.amazonaws.services.simpleemail.model.SendEmailRequest request = new com.amazonaws.services.simpleemail.model.SendEmailRequest()
				.withDestination(new com.amazonaws.services.simpleemail.model.Destination().withToAddresses(email))
				.withMessage(new com.amazonaws.services.simpleemail.model.Message()
						.withBody(new com.amazonaws.services.simpleemail.model.Body().withHtml(new com.amazonaws.services.simpleemail.model.Content().withCharset("UTF-8").withData(message)))
//		                  .withText(new com.amazonaws.services.simpleemail.model.Content().withCharset("UTF-8").withData(TEXTBODY)))
						.withSubject(new com.amazonaws.services.simpleemail.model.Content().withCharset("UTF-8").withData("MMS")))
		 .withSource("evs2ops@evs.com.sg")
		;
		
    	com.amazonaws.services.simpleemail.AmazonSimpleEmailService sesClient = com.amazonaws.services.simpleemail.AmazonSimpleEmailServiceClientBuilder.standard()
    			.withCredentials(new AWSStaticCredentialsProvider(new BasicAWSCredentials("AKIA5FJTG4T6ZJXTTU5I", "+D8Z177y+t29cybFj4elx4D6l2dUUZyDWXFXPAw7")))
    	        .withRegion(com.amazonaws.regions.Regions.AP_SOUTHEAST_1).build();
		sesClient.sendEmail(request).getSdkResponseMetadata().getRequestId();
	}

	// https://github.com/awsdocs/aws-doc-sdk-examples/blob/main/javav2/example_code/sns/src/main/java/com/example/sns/PublishTextSMS.java
	@Override
	public String sendSMS(String message, String phoneNumber) {
		try {
			software.amazon.awssdk.services.sns.model.PublishRequest request = software.amazon.awssdk.services.sns.model.PublishRequest.builder().message(message).phoneNumber(phoneNumber).build();
			software.amazon.awssdk.services.sns.model.PublishResponse result = snsClient.publish(request);
			LOG.info("SMS -> " + phoneNumber + " -> " + result.messageId() + " Message sent. Status was " + result.sdkHttpResponse().statusCode());
			return result.messageId();
		} catch (SnsException e) {
			LOG.info("SMS -> " + phoneNumber + " -> " + e.awsErrorDetails().errorMessage());
			LOG.error(e.getMessage(), e);
			throw e;
		}
	}
	
	@Override
	public String sendEmail(String message, String email, String subject) {
		try {
			com.amazonaws.services.simpleemail.model.SendEmailRequest request = new com.amazonaws.services.simpleemail.model.SendEmailRequest()
					.withDestination(new com.amazonaws.services.simpleemail.model.Destination().withToAddresses(email))
					.withMessage(new com.amazonaws.services.simpleemail.model.Message()
							.withBody(new com.amazonaws.services.simpleemail.model.Body().withHtml(new com.amazonaws.services.simpleemail.model.Content().withCharset("UTF-8").withData(message)))
//			                  .withText(new com.amazonaws.services.simpleemail.model.Content().withCharset("UTF-8").withData(TEXTBODY)))
							.withSubject(new com.amazonaws.services.simpleemail.model.Content().withCharset("UTF-8").withData(subject)))
			.withSource(AppProps.get("AWS_SES_FROM", "evs2ops@evs.com.sg"))
			;
			return sesClient.sendEmail(request).getSdkResponseMetadata().getRequestId();

		} catch (Exception e) {
			LOG.error(e.getMessage(), e);
			throw e;
		}
	}

	@Override
	public boolean upload(String fileName, InputStream in, String contentType) {
		boolean result = false;
		Date today = new Date();
		String keyName = String.format("%s", new SimpleDateFormat("yyyy/MM/dd").format(today));
		LOG.info("Upload Function. Bucket Name: {}, File Name: {}, KeyName: {}", photoBucketName, fileName, keyName);
		PutObjectRequest request = null;
		ObjectMetadata metadata = null;
		try {
			if (in != null) {
				String key = fileName;
				metadata = new ObjectMetadata();
				metadata.setContentType(contentType);
				request = new PutObjectRequest(photoBucketName, key, in, metadata);
				request.setMetadata(metadata);
				s3Client.putObject(request);

				LOG.info("{} Finished uploading File Object: {} to {}", DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss").format(LocalDateTime.now()), fileName);
				result = true;
			} else {
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
	
}
