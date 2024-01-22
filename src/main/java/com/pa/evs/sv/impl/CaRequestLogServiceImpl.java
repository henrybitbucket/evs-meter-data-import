package com.pa.evs.sv.impl;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.Hibernate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pa.evs.LocalMapStorage;
import com.pa.evs.constant.Message;
import com.pa.evs.dto.CaRequestLogDto;
import com.pa.evs.dto.DeviceRemoveLogDto;
import com.pa.evs.dto.PaginDto;
import com.pa.evs.dto.RelayStatusLogDto;
import com.pa.evs.dto.ResponseDto;
import com.pa.evs.dto.ScreenMonitoringDto;
import com.pa.evs.enums.DeviceStatus;
import com.pa.evs.enums.DeviceType;
import com.pa.evs.enums.ScreenMonitorKey;
import com.pa.evs.enums.ScreenMonitorStatus;
import com.pa.evs.model.Address;
import com.pa.evs.model.AddressLog;
import com.pa.evs.model.Block;
import com.pa.evs.model.Building;
import com.pa.evs.model.BuildingUnit;
import com.pa.evs.model.CARequestLog;
import com.pa.evs.model.DeviceProject;
import com.pa.evs.model.DeviceRemoveLog;
import com.pa.evs.model.FloorLevel;
import com.pa.evs.model.Group;
import com.pa.evs.model.Log;
import com.pa.evs.model.MMSMeter;
import com.pa.evs.model.ProjectTag;
import com.pa.evs.model.RelayStatusLog;
import com.pa.evs.model.ScreenMonitoring;
import com.pa.evs.model.Users;
import com.pa.evs.model.Vendor;
import com.pa.evs.repository.AddressLogRepository;
import com.pa.evs.repository.AddressRepository;
import com.pa.evs.repository.BlockRepository;
import com.pa.evs.repository.BuildingRepository;
import com.pa.evs.repository.BuildingUnitRepository;
import com.pa.evs.repository.CARequestLogRepository;
import com.pa.evs.repository.DeviceProjectRepository;
import com.pa.evs.repository.DeviceRemoveLogRepository;
import com.pa.evs.repository.FloorLevelRepository;
import com.pa.evs.repository.GroupRepository;
import com.pa.evs.repository.LogRepository;
import com.pa.evs.repository.MMSMeterRepository;
import com.pa.evs.repository.ProjectTagRepository;
import com.pa.evs.repository.RelayStatusLogRepository;
import com.pa.evs.repository.ScreenMonitoringRepository;
import com.pa.evs.repository.UserRepository;
import com.pa.evs.repository.VendorRepository;
import com.pa.evs.security.user.JwtUser;
import com.pa.evs.sv.AuthenticationService;
import com.pa.evs.sv.CaRequestLogService;
import com.pa.evs.sv.EVSPAService;
import com.pa.evs.utils.AppProps;
import com.pa.evs.utils.CsvUtils;
import com.pa.evs.utils.Mqtt;
import com.pa.evs.utils.RSAUtil;
import com.pa.evs.utils.SecurityUtils;
import com.pa.evs.utils.SimpleMap;
import com.pa.evs.utils.Utils;

@Component
@SuppressWarnings("unchecked")
public class CaRequestLogServiceImpl implements CaRequestLogService {
	
	private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(EVSPAServiceImpl.class);
	
	@Value("${evs.pa.mqtt.address}") private String evsPAMQTTAddress;

	@Value("${evs.pa.mqtt.client.id}") private String mqttClientId;
	
	@Value("${evs.pa.mqtt.publish.topic.alias}") private String alias;
	
	@Value("${evs.pa.privatekey.path}") private String pkPath;
	
	@Autowired
	private CARequestLogRepository caRequestLogRepository;
	
	@Autowired
	private MMSMeterRepository mmsMeterRepository;
	
	@Autowired
	private DeviceRemoveLogRepository deviceRemoveLogRepository;

	@Autowired
    private GroupRepository groupRepository;
	
    @Autowired
    private AuthenticationService authenticationService;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
	AddressRepository addressRepository;
	
	@Autowired
	BuildingRepository buildingRepository;

	@Autowired
	BlockRepository blockRepository;
	
	@Autowired
	FloorLevelRepository floorLevelRepository;
	
	@Autowired
	BuildingUnitRepository buildingUnitRepository;

	@Autowired
	LogRepository logRepository;
	
	@Autowired
	private VendorRepository vendorRepository;
	
	@Autowired
	private ProjectTagRepository projectTagRepository;
	
	@Autowired
	EntityManager em;
	
	@Autowired LocalMapStorage localMap;

	@Autowired
	private ScreenMonitoringRepository screenMonitoringRepository;
	
	@Autowired
	private AddressLogRepository addressLogRepository;
	
	@Autowired
	private DeviceProjectRepository deviceProjectRepository;
	
	@Autowired
	EVSPAService evsPAService;
	
	@Autowired
	private RelayStatusLogRepository relayStatusLogRepository;

    private List<String> cacheCids = Collections.EMPTY_LIST;

	@PostConstruct
    public void init() {
        LOG.debug("Loading CID into cache");
        cacheCids = caRequestLogRepository.getCids();
        
//        new Thread(() -> {
//        	List<CARequestLog> list = caRequestLogRepository.findAll();
//        	for (int i = 0; i < list.size(); i++) {
//        		CARequestLog ca = list.get(i);
//        		updateMMSMeter(ca, ca.getMsn());
//        		System.out.println("Done updateMMSMeter " + i + " / " + list.size());
//        	}
//        }).start();
    }

	@Override
	public Optional<CARequestLog> findByUid(String uid) {
		return caRequestLogRepository.findByUid(uid);
	}
	
	@Override
	public Optional<CARequestLog> findByMsn(String msn) {
		return caRequestLogRepository.findByMsn(msn);
	}
	
	@Override
	@Transactional
	public void updateMMSMeter(CARequestLog ca, String msn) {

		msn = StringUtils.isBlank(ca.getMsn()) ? msn : ca.getMsn();
		if (StringUtils.isBlank(msn)) {
			return;
		}
		
		MMSMeter mmsMeter = mmsMeterRepository.findByMsn(msn);
		if (mmsMeter == null) {
			mmsMeter = new MMSMeter();
		}
		
		boolean isCoupled = StringUtils.isNotBlank(mmsMeter.getUid());
		
		if (isCoupled && StringUtils.isBlank(ca.getMsn())) {
			mmsMeter.setLastestDecoupleTime(System.currentTimeMillis());
			mmsMeter.setLastestDecoupleUser(SecurityUtils.getEmail());
		}
		
		if (!isCoupled && StringUtils.isNotBlank(ca.getMsn())) {
			mmsMeter.setLastestCoupledTime(System.currentTimeMillis());
			mmsMeter.setLastestCoupledUser(SecurityUtils.getEmail());
		}
		
		mmsMeter.setMsn(msn);
		mmsMeter.setLastUid(ca.getUid());
		
		mmsMeter.setUid(StringUtils.isNotBlank(ca.getMsn()) ? ca.getUid() : null);
		
		mmsMeterRepository.save(mmsMeter);
	}

	@Override
	@Transactional
    public void updateVendor(String msn, Long vendorId) throws Exception {
    	LOG.info("NMM updateVendor " + msn + " / " + vendorId);
    	Vendor vendor = vendorRepository.findById(vendorId).orElseThrow(() -> new RuntimeException("vendor not found"));
    	CARequestLog dv = caRequestLogRepository.findByMsn(msn).orElseThrow(() -> new RuntimeException("vendor not found"));
    	if (dv.getVendor().getId().longValue() != vendor.getId().longValue()) {
    		dv.setVendor(vendor);
    		caRequestLogRepository.save(dv);
    	}
    }

    @Override
    @Transactional
    public void save(CaRequestLogDto dto) throws Exception {
    	
    	if (StringUtils.isNotBlank(dto.getMsn())) {
    		// update remark meter device
    		MMSMeter mmsMeter = mmsMeterRepository.findByMsn(dto.getMsn());
    		if (mmsMeter != null) {
    			mmsMeter.setRemark(dto.getRemark());
    			mmsMeterRepository.save(mmsMeter);
    		}
    		if (StringUtils.isBlank(dto.getUid())) {
    			return;
    		}
    	}
        CARequestLog ca = null;
        Calendar c = Calendar.getInstance();
        boolean isSetAddress = false;
        AddressLog addrLog = null;
        String msn = dto.getMsn();
        String caUid = null;
        String caSn = null;
        
        if (StringUtils.isNotBlank(dto.getMsn())) {
        	dto.setMsn(dto.getMsn().trim());
        }
        if (dto.getId() != null) {
            Optional<CARequestLog> opt = caRequestLogRepository.findById(dto.getId());
            if (opt.isPresent()) {
                ca = opt.get();
                if (BooleanUtils.isTrue(dto.getIsReplaced())) {
                	String newUid = dto.getUid();
                	String newSn = dto.getSn();
                	String newReplaceReason = dto.getReplaceReason();
                	String oldSn = ca.getOldSn();
                	String oldReplaceReason = ca.getReplaceReason();
                	caUid = ca.getUid();
                	caSn = ca.getSn();
                	
                	if (StringUtils.isBlank(newUid)) {
                		throw new Exception("New MCU UUID is required!");
                	}
                	if (StringUtils.isBlank(newSn)) {
                		throw new Exception("New MCU SN is required!");
                	}
                	if (StringUtils.isBlank(newReplaceReason)) {
                		throw new Exception("Replace reason is required!");
                	}
                	if (caRequestLogRepository.findBySn(newSn).isPresent()) {
                		throw new Exception(String.format("New MCU SN %s exists!", newSn));
                	}
                	if (StringUtils.isNotBlank(oldSn)) {
                		ca.setOldSn(oldSn + "," + caSn);
                	} else {
                		ca.setOldSn(caSn);
                	}
                	if (StringUtils.isNotBlank(oldReplaceReason)) {
                		ca.setReplaceReason(oldReplaceReason + "," + newReplaceReason);
                	} else {
                		ca.setReplaceReason(newReplaceReason);
                	}
                	
                	ca.setUid(newUid);
                	ca.setSn(newSn);
                	ca.setIsReplaced(true);
                }
                
            	if (StringUtils.isNotBlank(dto.getMsn()) && StringUtils.isNotBlank(ca.getMsn()) && !dto.getMsn().equalsIgnoreCase(ca.getMsn())) {
            		if (BooleanUtils.isTrue(caRequestLogRepository.existsByMsn(dto.getMsn()))) {
            			throw new Exception(Message.MSN_WAS_ASSIGNED);
            		}
            		throw new Exception(Message.MCU_ALREADY_COUPLED);
            	}
            	if (ca.getBuildingUnit() != null && dto.getBuildingUnitId() != null && !ca.getBuildingUnit().getId().equals(dto.getBuildingUnitId())) {
            		throw new Exception(Message.MCU_ALREADY_COUPLED_ADDRESS);
            	}
                ca.setModifyDate(c.getTime());
                if (StringUtils.isBlank(msn) && StringUtils.isNotBlank(ca.getMsn())) {
                	msn = ca.getMsn();
                }
            } else {
                ca = new CARequestLog();
                ca.setCreateDate(c.getTime());
            }
        } else {
            ca = new CARequestLog();
            ca.setCreateDate(c.getTime());
        }
        
        ca.setCertificate(dto.getCertificate());
        ca.setMsn(dto.getMsn());
        ca.setRequireRefresh(dto.getRequireRefresh());
        ca.setRaw(dto.getRaw());
        ca.setStartDate(dto.getStartDate());
        ca.setEndDate(dto.getEndDate());
        
        if (dto.getSendMDTToPi() != null) {
        	ca.setSendMDTToPi(dto.getSendMDTToPi());	
        }
        
        List<CARequestLog> list = null;
    	if (dto.getBuildingId() != null && dto.getFloorLevelId() != null && dto.getBuildingUnitId() != null) {
    		list = caRequestLogRepository.findByBuildingAndFloorLevelAndBuildingUnit(dto.getBuildingId(), dto.getFloorLevelId(), dto.getBuildingUnitId());
    	} else if (dto.getBuildingId() != null && dto.getFloorLevelId() != null && dto.getBuildingUnitId() == null) {
    		list = caRequestLogRepository.findByBuildingAndFloorLevel(dto.getBuildingId(), dto.getFloorLevelId());
    	} else if (dto.getBuildingId() != null && dto.getFloorLevelId() == null && dto.getBuildingUnitId() == null) {
    		list = caRequestLogRepository.findByBuilding(dto.getBuildingId());
    	}
    	
    	if (list != null && !list.isEmpty() && (list.size() > 1 || ca.getId() == null || (list.get(0).getId().longValue() != ca.getId().longValue()))) {
    		throw new Exception(Message.ADDRESS_IS_ASSIGNED);
    	}
    	
    	boolean isCoupledAddress = (ca.getBuildingUnit() != null && dto.getBuildingUnitId() != null && !ca.getBuildingUnit().getId().equals(dto.getBuildingUnitId()))
    			|| (ca.getBuildingUnit() == null && dto.getBuildingUnitId() != null);
    	
    	if ((ca.getBuildingUnit() != null && dto.getBuildingUnitId() != null && !ca.getBuildingUnit().getId().equals(dto.getBuildingUnitId()))
    			|| (ca.getBuildingUnit() != null && dto.getBuildingUnitId() == null)) {
    		
    		// Add new log to address_log that this address is unlinked to this device
			addrLog = AddressLog.build(ca);
			addrLog.setType(DeviceType.NOT_COUPLED);
			addressLogRepository.save(addrLog);
			isSetAddress = true;
    	}
        
        try {
        	
        	if(StringUtils.isNotEmpty(dto.getHomeAddress())) {
				ca.setHomeAddress(dto.getHomeAddress());
			}
			
			if (dto.getBuildingId() == null) {
				ca.setBuilding(null);
			} else {	
				ca.setBuilding(buildingRepository.findById(dto.getBuildingId()).orElse(null));
				ca.setAddress(null);
			}
			
			if (dto.getFloorLevelId() == null) {
				ca.setFloorLevel(null);
			} else {
				FloorLevel fl = floorLevelRepository.findById(dto.getFloorLevelId()).orElse(null);
				ca.setFloorLevel(fl);
				if (fl != null) {
					ca.setBlock(fl.getBlock());
				}
			}
			
			if (dto.getBuildingUnitId() == null) {
				if (ca.getBuildingUnit() != null) {
					ca.getBuildingUnit().setCoupledDate(null);
					buildingUnitRepository.save(ca.getBuildingUnit());
				}
				ca.setBuildingUnit(null);
			} else {
				
				BuildingUnit next = buildingUnitRepository.findById(dto.getBuildingUnitId()).orElse(null);
				BuildingUnit old = ca.getBuildingUnit();
				if (old != null && (next == null || next.getId().longValue() != old.getId().longValue())) {
					old.setCoupledDate(null);
					buildingUnitRepository.save(old);
				}
				if (next != null && (old == null || next.getId().longValue() != old.getId().longValue())) {
					next.setCoupledDate(new Date());
					buildingUnitRepository.save(next);
				}
				ca.setBuildingUnit(next);
			}
			
			if (dto.getAddress() == null) {
				ca.setAddress(null);
			} else if (dto.getBuildingId() == null && StringUtils.isNotBlank(dto.getAddress().getStreetNumber())
				&& StringUtils.isNotBlank(dto.getAddress().getStreet())
				&& StringUtils.isNotBlank(dto.getAddress().getCity())
				&& StringUtils.isNotBlank(dto.getAddress().getCountry())
				&& dto.getAddress().getPostalCode() != null
						) {
				
				Address address;
				if (dto.getAddress().getId() == null) {
					address = new Address();
				} else {
					address = addressRepository.findById(dto.getAddress().getId()).orElse(new Address());
				}
				address.setStreetNumber(dto.getAddress().getStreetNumber());
				address.setStreet(dto.getAddress().getStreet());
				address.setTown(dto.getAddress().getTown());
				address.setCity(dto.getAddress().getCity());
				address.setCountry(dto.getAddress().getCountry());
				address.setPostalCode(dto.getAddress().getPostalCode());
				addressRepository.save(address);

				ca.setAddress(address);
				ca.setBuilding(null);
				ca.setFloorLevel(null);
				ca.setBuildingUnit(null);

			}
		} catch (Exception e) {/**/}
        
        if (dto.getInstaller() != null) {
            Optional<Users> installer = userRepository.findById(dto.getInstaller().longValue());
            if (installer.isPresent()) {
                ca.setInstaller(installer.get());
            }
        } else {
            ca.setInstaller(null);
        }
        if (dto.getGroup() != null) {
            Optional<Group> group = groupRepository.findById(dto.getGroup().longValue());
            if (group.isPresent()) {
                ca.setGroup(group.get());
            }
        } else {
            ca.setGroup(null);
        }
        if (dto.getVendor() != null) {
            Optional<Vendor> vendorOpt = vendorRepository.findById(dto.getVendor().longValue());
            if (vendorOpt.isPresent()) {
                ca.setVendor(vendorOpt.get());
            }
        } else {
        	if (dto.getId() != null) {
        		Optional<CARequestLog> opt = caRequestLogRepository.findById(dto.getId());
                if (opt.isPresent()) {
                	ca.setVendor(opt.get().getVendor());
                } else {
                	Vendor vendor = vendorRepository.findByName("Default");
                	if (vendor != null) {
                		ca.setVendor(vendor);
                	} else {
                		throw new Exception("Default vendor not found!");
                	}
                }
        	} else {
        		Vendor vendor = vendorRepository.findByName("Default");
            	if (vendor != null) {
            		ca.setVendor(vendor);
            	} else {
            		throw new Exception("Default vendor not found!");
            	}
        	}
        }
        
        // status, type
        if (StringUtils.isBlank(ca.getMsn()) || StringUtils.isBlank(ca.getSn())) {
        	ca.setType(DeviceType.NOT_COUPLED);
        	ca.setCoupledDatetime(System.currentTimeMillis());
    		ca.setCoupledUser(SecurityUtils.getUsername());
        } else {
        	if (ca.getType() != DeviceType.COUPLED) {
        		ca.setCoupledDatetime(System.currentTimeMillis());
        		ca.setCoupledUser(SecurityUtils.getUsername());
        	}
        	ca.setType(DeviceType.COUPLED);
        }
        if (ca.getStatus() == null) {
        	ca.setStatus(DeviceStatus.OFFLINE);
        }
        caRequestLogRepository.save(ca);
        caRequestLogRepository.flush();
        
        Long id = ca.getId();
        
        if (dto.getProjectTags() != null && !dto.getProjectTags().isEmpty()) {
        	Set<Long> projectNames = new HashSet<>();
    		dto.getProjectTags().forEach(projectNames::add);
    		deviceProjectRepository.deleteNotInProjects(id, projectNames.isEmpty() ? new HashSet<>() : projectNames);
    		List<String> existsProjectNames = deviceProjectRepository.findProjectNameByDeviceId(id);
    		List<ProjectTag> projects = deviceProjectRepository.findProjectByProjectTagNameIn(projectNames);
    		for (ProjectTag project : projects) {
    			if (!existsProjectNames.contains(project.getName())) {
    				DeviceProject deviceProject = new DeviceProject();
    				deviceProject.setDevice(ca);
    				deviceProject.setProject(project);
    				deviceProjectRepository.save(deviceProject);
    			}
    		}
        } else {
            ca.setProjectTags(null);
        }
        
        if (isCoupledAddress) {

        	// Add new log to address_log that this new address is linked to this device
    		addrLog = AddressLog.build(ca);
    		addrLog.setType(DeviceType.COUPLED);
    		addressLogRepository.save(addrLog);
    		isSetAddress = true;
        }

        updateCacheUidMsnDevice(ca.getUid(), "update");
        
        // backup de-couple msn to search Meter
        updateMMSMeter(ca, msn);
        try {
			DeviceRemoveLog log = DeviceRemoveLog.build(ca);
			
			if (BooleanUtils.isTrue(dto.getIsReplaced())) {
				String details = "New MCU UUID: " + dto.getUid() + "\r\nNew MCU SN: " + dto.getSn() + "\r\nReplace reason: " + dto.getReplaceReason();
				log.setUid(caUid);
				log.setSn(caSn);
	        	AppProps.getContext().getBean(this.getClass()).updateDeviceLogs(log, details, "REPLACED", null);
			}
			
			log.setUid(ca.getUid());
			log.setSn(ca.getSn());
			log.setReason(null);
			if (isSetAddress) {
	        	String operation = isCoupledAddress ? "COUPLE ADDRESS" : "DE-COUPLE ADDRESS";
	        	AppProps.getContext().getBean(this.getClass()).updateDeviceLogs(log, null, operation, addrLog);
	        } else {
	        	String operation = ca.getType() == DeviceType.COUPLED ? "COUPLE MSN" : "DE-COUPLE MSN";
	        	log.setMsn(msn);
	        	AppProps.getContext().getBean(this.getClass()).updateDeviceLogs(log, null, operation, addrLog);
	        }
			
		} catch (Exception e) {
			LOG.error(e.getMessage(), e);
		}
    }


    // search MMSMeter
	@Override
    @Transactional(readOnly = true)
    public PaginDto<CARequestLog> searchMMSMeter(PaginDto<CARequestLog> pagin) {
		pagin.getOptions().put("searchMeter", true);
		return search(pagin);
    }
    
    // search MCU
    @Override
    @Transactional(readOnly = true)
    public PaginDto<CARequestLog> search(PaginDto<CARequestLog> pagin) {
    	
    	
    	boolean searchMeter = "true".equalsIgnoreCase(pagin.getOptions().get("searchMeter") + "");
    	
        StringBuilder sqlBuilder = new StringBuilder("SELECT ca, 1 as m FROM CARequestLog ca ");
        StringBuilder sqlCountBuilder = new StringBuilder("SELECT count(ca) FROM CARequestLog ca");
        
        if (searchMeter) {
        	sqlBuilder = new StringBuilder("SELECT ca, m FROM MMSMeter m join CARequestLog ca on ca.uid = m.lastUid ");
        	sqlCountBuilder = new StringBuilder("SELECT count(ca) FROM MMSMeter m join CARequestLog ca on ca.uid = m.lastUid ");
        }
        
        List<String> tags = SecurityUtils.getProjectTags();
        List<ProjectTag> projectTags = projectTagRepository.findByNameIn(tags);
        Set<Long> tagIds = projectTags.stream().map(tag -> tag.getId()).collect(Collectors.toSet());
        StringBuilder sqlCommonBuilder = new StringBuilder();
        
        if (tags.isEmpty()) {
        	return pagin;
        }
        
        if (CollectionUtils.isEmpty(pagin.getOptions()) || (
                pagin.getOptions().size() == 1 
                && BooleanUtils.isTrue(BooleanUtils.toBoolean((String) pagin.getOptions().get("queryAllDate")))
            )) {
        	if (!tags.contains("ALL")) {
        		sqlCommonBuilder.append(" WHERE (exists (select 1 from DeviceProject dp where dp.device.id = ca.id and dp.project.id in :tagIds))    ");
            } else {
            	sqlCommonBuilder.append(" WHERE 1 = 1     ");	
            }
            
        } else {
            Map<String, Object> options = pagin.getOptions();
            Long fromDate = (Long) options.get("fromDate");
            Long toDate = (Long) options.get("toDate");
            String status = (String) options.get("status");
            String type = (String) options.get("type");
            String querySn = (String) options.get("querySn");
            String queryMsn = (String) options.get("queryMsn");
            String querySnOrCid = (String) options.get("querySnOrCid");
            List<String> cids = (List<String>) options.get("selectedCids");
            String queryUuid = (String) options.get("queryUuid");
            String queryEsimId = (String) options.get("queryEsimId");
            Integer queryGroup = StringUtils.isNotBlank((String) options.get("queryGroup")) ? Integer.valueOf((String) options.get("queryGroup")) : null;
            Boolean enrollmentDate = BooleanUtils.toBoolean((String) options.get("queryEnrollmentDate"));
            Boolean coupledDate = BooleanUtils.toBoolean((String) options.get("queryCoupledDate"));
            Boolean activationDate = BooleanUtils.toBoolean((String) options.get("queryActivationDate"));
            Boolean deactivationDate = BooleanUtils.toBoolean((String) options.get("queryDeactivationDate"));
            Boolean onboardingDate = BooleanUtils.toBoolean((String) options.get("onboardingDate"));
            Boolean allDate = BooleanUtils.toBoolean((String) options.get("queryAllDate"));
            
            String queryBuilding = (String) options.get("queryBuilding");
            String queryBlock = (String) options.get("queryBlock");
            String queryFloorLevel = (String) options.get("queryFloorLevel");
            String queryBuildingUnit = (String) options.get("queryBuildingUnit");
            String queryPostalCode = (String) options.get("queryPostalCode");
            Long queryVendor = StringUtils.isNotBlank((String) options.get("queryVendor")) ? Long.parseLong((String) options.get("queryVendor")) : null;
            
            sqlCommonBuilder.append(" WHERE     ");
            
            if (BooleanUtils.isTrue(allDate)) {
                if (fromDate != null && toDate == null) {
                    sqlCommonBuilder.append(" EXTRACT(EPOCH FROM ca.createDate) * 1000 >= " + fromDate + "AND");
                }
                if (fromDate == null && toDate != null) {
                    sqlCommonBuilder.append(" EXTRACT(EPOCH FROM ca.createDate) * 1000 <= " + toDate + "AND");
                }
                if (fromDate != null && toDate != null) {
                    sqlCommonBuilder.append(" ( EXTRACT(EPOCH FROM ca.createDate) * 1000 >= " + fromDate);
                    sqlCommonBuilder.append(" AND EXTRACT(EPOCH FROM ca.createDate) * 1000 <= " + toDate + ") AND ");
                }
            } else {
                sqlCommonBuilder.append(" ( ");
                if (BooleanUtils.isTrue(enrollmentDate)) {
                    if (fromDate != null && toDate == null) {
                        sqlCommonBuilder.append(" ca.enrollmentDatetime >= " + fromDate + "OR");
                    }
                    if (fromDate == null && toDate != null) {
                        sqlCommonBuilder.append(" ca.enrollmentDatetime <= " + toDate + "OR");
                    }
                    if (fromDate != null && toDate != null) {
                        sqlCommonBuilder.append(" ( ca.enrollmentDatetime >= " + fromDate);
                        sqlCommonBuilder.append(" AND ca.enrollmentDatetime <= " + toDate + ") OR");
                    }
                }
                if (BooleanUtils.isTrue(coupledDate)) {
                    if (fromDate != null && toDate == null) {
                        sqlCommonBuilder.append(" ca.coupledDatetime >= " + fromDate + "OR");
                    }
                    if (fromDate == null && toDate != null) {
                        sqlCommonBuilder.append(" ca.coupledDatetime <= " + toDate + "OR");
                    }
                    if (fromDate != null && toDate != null) {
                        sqlCommonBuilder.append(" ( ca.coupledDatetime >= " + fromDate);
                        sqlCommonBuilder.append(" AND ca.coupledDatetime <= " + toDate + ") OR");
                    }          
                }
                if (BooleanUtils.isTrue(activationDate)) {
                    if (fromDate != null && toDate == null) {
                        sqlCommonBuilder.append(" ca.activationDate >= " + fromDate + "OR");
                    }
                    if (fromDate == null && toDate != null) {
                        sqlCommonBuilder.append(" ca.activationDate <= " + toDate + "OR");
                    }
                    if (fromDate != null && toDate != null) {
                        sqlCommonBuilder.append(" ( ca.activationDate >= " + fromDate);
                        sqlCommonBuilder.append(" AND ca.activationDate <= " + toDate + ") OR");
                    }
                }
                if (BooleanUtils.isTrue(deactivationDate)) {
                    if (fromDate != null && toDate == null) {
                        sqlCommonBuilder.append(" ca.deactivationDate >= " + fromDate + "OR");
                    }
                    if (fromDate == null && toDate != null) {
                        sqlCommonBuilder.append(" ca.deactivationDate <= " + toDate + "OR");
                    }
                    if (fromDate != null && toDate != null) {
                        sqlCommonBuilder.append(" ( ca.deactivationDate >= " + fromDate);
                        sqlCommonBuilder.append(" AND ca.deactivationDate <= " + toDate + ") OR");
                    }
                }
                if (BooleanUtils.isTrue(onboardingDate)) {
                    if (fromDate != null && toDate == null) {
                        sqlCommonBuilder.append(" ca.lastOBRDate >= " + fromDate + "OR");
                    }
                    if (fromDate == null && toDate != null) {
                        sqlCommonBuilder.append(" ca.lastOBRDate <= " + toDate + "OR");
                    }
                    if (fromDate != null && toDate != null) {
                        sqlCommonBuilder.append(" ( ca.lastOBRDate >= " + fromDate);
                        sqlCommonBuilder.append(" AND ca.lastOBRDate <= " + toDate + ") OR");
                    }
                }
                
                sqlCommonBuilder.delete(sqlCommonBuilder.length() - 2, sqlCommonBuilder.length());
                if (sqlCommonBuilder.length() >= 30) {
                    sqlCommonBuilder.append(" ) AND ");
                }
            }
            
            if (StringUtils.isNotBlank(querySn)) {
                sqlCommonBuilder.append(" upper(ca.sn) like '%" + querySn.toUpperCase() + "%' AND ");
            }
            if (StringUtils.isNotBlank(queryMsn)) {
            	if (searchMeter) {
            		sqlCommonBuilder.append(" m.msn like '%" + queryMsn + "%' AND ");
            	} else {
            		sqlCommonBuilder.append(" ca.msn like '%" + queryMsn + "%' AND ");	
            	}
                
            }
            if (StringUtils.isNotBlank(querySnOrCid)) {
                sqlCommonBuilder.append(" (lower(ca.sn) like '%" + querySnOrCid.toLowerCase().trim() + "%' or lower(ca.cid) like '%" + querySnOrCid.toLowerCase().trim() + "%') AND ");
            }
            if (StringUtils.isNotBlank(status)) {
                sqlCommonBuilder.append(" ca.status = '" + status + "' AND ");
            }
            if (StringUtils.isNotBlank(type)) {
                sqlCommonBuilder.append(" ca.type = '" + type + "' AND ");
            }
            if (!CollectionUtils.isEmpty(cids)) {
                sqlCommonBuilder.append(" (ca.cid = '" + cids.get(0) + "'");
                for (int i = 1; i < cids.size(); i++) {
                    sqlCommonBuilder.append(" OR ca.cid = '" + cids.get(i) + "'");
                }
                sqlCommonBuilder.append(" ) AND ");
            }
            if (StringUtils.isNotBlank(queryUuid)) {
                sqlCommonBuilder.append(" upper(ca.uid) like '%" + queryUuid.toUpperCase() + "%' AND ");
            }
            if (StringUtils.isNotBlank(queryEsimId)) {
                sqlCommonBuilder.append(" upper(ca.cid) like '%" + queryEsimId.toUpperCase() + "%' AND ");
            }
            if (queryGroup != null) {
                sqlCommonBuilder.append(" ca.group = " + queryGroup + " AND ");
            }
            if (StringUtils.isNotBlank(queryBuilding)) {
                sqlCommonBuilder.append(" ca.building.id= '" + queryBuilding + "' AND ");
            }
            if (StringUtils.isNotBlank(queryBlock)) {
                sqlCommonBuilder.append(" ca.block.id= '" + queryBlock + "' AND ");
            }
            if (StringUtils.isNotBlank(queryFloorLevel)) {
                sqlCommonBuilder.append(" ca.floorLevel.id= '" + queryFloorLevel + "' AND ");
            }
            if (StringUtils.isNotBlank(queryBuildingUnit)) {
                sqlCommonBuilder.append(" ca.buildingUnit.id= '" + queryBuildingUnit + "' AND ");
            }
            if (StringUtils.isNotBlank(queryPostalCode)) {
                sqlCommonBuilder.append(" ((exists (select 1 from Building bd where bd.id = ca.building.id and upper(bd.address.postalCode) = '" + queryPostalCode.toUpperCase() + "') ");
                sqlCommonBuilder.append(" or (exists (select 1 FROM Address add1 where add1.id = ca.address.id and upper(add1.postalCode) = '" + queryPostalCode.toUpperCase() + "') ))) AND ");
            }
            if (queryVendor != null) {
                sqlCommonBuilder.append(" ca.vendor.id = " + queryVendor + " AND ");
            }
        	if (!tags.contains("ALL")) {
        		sqlCommonBuilder.append(" (exists (select 1 from DeviceProject dp where dp.device.id = ca.id and dp.project.id in :tagIds))  AND ");
            }
        	
            sqlCommonBuilder.append(" ca.sn is not null and ca.sn <> ''  AND ");
            sqlCommonBuilder.delete(sqlCommonBuilder.length() - 4, sqlCommonBuilder.length());
        }
        
        if (Boolean.parseBoolean(pagin.getOptions().get("cidIsNotNull") + "")) {
//            if (StringUtils.isNotBlank((String) pagin.getOptions().get("querySnOrCid"))) {
//                sqlCommonBuilder.append(" AND ");
//            }
            sqlCommonBuilder.append(" AND ca.cid is not null AND ca.cid <> '' ");
        }
        
        if (sqlCommonBuilder.length() < 10) {
            sqlCommonBuilder.append(" 1 = 1 ");
        }
        
        sqlBuilder.append(sqlCommonBuilder).append(" ORDER BY ca.id asc");
        sqlCountBuilder.append(sqlCommonBuilder);
        
        if (pagin.getOffset() == null || pagin.getOffset() < 0) {
            pagin.setOffset(0);
        }
        
        if (pagin.getLimit() == null || pagin.getLimit() <= 0) {
            pagin.setLimit(100);
        }
        
    	Query queryCount = !tags.contains("ALL") ? em.createQuery(sqlCountBuilder.toString()).setParameter("tagIds", tagIds) : em.createQuery(sqlCountBuilder.toString());
        Long count = ((Number)queryCount.getSingleResult()).longValue();
        pagin.setTotalRows(count);
        pagin.setResults(new ArrayList<>());
        if (count == 0l) {
            return pagin;
        }
        
    	Query query = !tags.contains("ALL") ? em.createQuery(sqlBuilder.toString()).setParameter("tagIds", tagIds) : em.createQuery(sqlBuilder.toString());
        query.setFirstResult(pagin.getOffset());
        query.setMaxResults(pagin.getLimit());
        
        final List<Object[]> list = query.getResultList();
        List<CARequestLog> rp = new ArrayList<>();
        for (Object[] obj : list) {
        	CARequestLog ca = (CARequestLog) obj[0];
        	List<ProjectTag> pTags = ca.getDeviceProject().isEmpty() ? new ArrayList<>() : ca.getDeviceProject().stream().map(dp -> (ProjectTag) Hibernate.unproxy(dp.getProject())).collect(Collectors.toList());
        	ca.getVendor();
        	ca.setDeviceProject(null);
        	ca.setProjectTags(pTags);
        	ca.setHomeAddress(Utils.formatHomeAddress(ca));
        	
        	if (StringUtils.isBlank(ca.getDeviceCsrSignatureAlgorithm())) {
        		AppProps.getContext().getBean(EVSPAServiceImpl.class).updateDeviceCsrInfo(ca.getUid());
        	}
        	rp.add(ca);
        }
        
        pagin.setResults(rp);
        getRLSLog(rp);
        
        if (searchMeter) {
        	for (Object[] obj : list) {
            	CARequestLog ca = (CARequestLog) obj[0];
            	MMSMeter meter = (MMSMeter) obj[1];
            	if (searchMeter && meter != null && StringUtils.isBlank(ca.getMsn())) {
            		ca.setUid(null);
            		ca.setSn(null);
            		ca.setCid(null);
            		ca.setMsn(meter.getMsn());
            		ca.setLastestDecoupleUser(meter.getLastestCoupledUser());
            		ca.setLastestDecoupleTime(meter.getLastestDecoupleTime());;
            	}
            	if (searchMeter && meter != null) {
            		ca.setRemark(meter.getRemark());
            	}
        	}
        }
        return pagin;
        
    }
    
    private void getRLSLog(List<CARequestLog> list) {
    	try {
    		list.forEach(l -> {
        		l.setLogs(Arrays.asList(logRepository.findRawByUidAndPType(l.getUid(), "RLS")));
        	});
		} catch (Exception e) {
			e.printStackTrace();
		}
    	
    }

    @Transactional
	@Override
	public void linkMsn(Map<String, Object> map) {
    	
    	if (map.get("sn") != null) {
            if (BooleanUtils.isTrue(caRequestLogRepository.existsByMsn((String)map.get("msn")))) {
                throw new RuntimeException("Invalid MSN, MSN is being linked !");
            }
    		caRequestLogRepository.linkMsnBySn((String)map.get("sn"), (String)map.get("msn"));
    		try {
    			Optional<CARequestLog> opt = caRequestLogRepository.findBySn((String)map.get("sn"));
        		if (opt.isPresent()) {
        			CARequestLog ca = opt.get();
        			if (map.get("groupId") instanceof Number) {
        			    Optional<Group> group = groupRepository.findById(((Number)map.get("groupId")).longValue());
                        ca.setGroup(group.get());
        			}
        			ca.setMsn((String)map.get("msn"));
        			// ca.setStatus(DeviceStatus.COUPLED);
        			ca.setType(DeviceType.COUPLED);
    				ca.setCoupledDatetime(System.currentTimeMillis());
    				ca.setCoupledUser(SecurityUtils.getUsername());
        			//ca.setAddress((String)map.get("address"));
        			if (map.get("request") instanceof HttpServletRequest) {
        				ResponseDto<JwtUser> us = authenticationService.getUser((HttpServletRequest) map.get("request"));
        				if (us != null && us.getResponse() != null) {
        					LOG.info("link to user: " + us.getResponse().getUsername());
        					Users user = userRepository.findByUsername(us.getResponse().getUsername());
        					if (user == null) {
        						user = userRepository.findByEmail(us.getResponse().getUsername());
        					}
        					ca.setInstaller(user);
        				}
        			}
        			caRequestLogRepository.save(ca);
        			caRequestLogRepository.flush();
        			
        			updateMMSMeter(ca, (String)map.get("msn"));
        			updateCacheUidMsnDevice(ca.getUid(), "update");
        			
        			DeviceRemoveLog log = DeviceRemoveLog.build(ca);
    				AppProps.getContext().getBean(this.getClass()).updateDeviceLogs(log, null, "COUPLE MSN", null);
        			
        		} else {
        			throw new RuntimeException("MCU SN(QR Code) doesn't exist!");
        		}
			} catch (Exception e) {
				LOG.error("link error " + e.getMessage(), e);
				throw new RuntimeException(e.getMessage(), e);
			}
    		
    	} else if (map.get("uuid") != null) {
    		caRequestLogRepository.linkMsn((String)map.get("uuid"), (String)map.get("msn"));
    	}

	}

    @Override
    @Transactional(readOnly = true)
    public File downloadCsv(List<CARequestLog> listInput, Long activateDate) throws IOException {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
        String tag = sdf.format(new Date());
        String fileName = "ca_request_log-" + tag + ".csv";
        return CsvUtils.writeCaRequestLogCsv(listInput, fileName, activateDate);
    }
    
    @Override
    public List<String> getCids(boolean refresh) {
	    if (refresh) {
	        cacheCids = caRequestLogRepository.getCids();
        }
	    return cacheCids;
    }
    
	@Override
	public void updateCacheUidMsnDevice(String currentUid, String action) {
		LOG.info("updating cache device: currentUid = " + currentUid + " action = " + action);
		Map<String, String> cache = localMap.getUidMsnMap();
		if (StringUtils.isEmpty(currentUid)) {
			List<Object[]> dvs = em.createQuery("select uid,msn from CARequestLog").getResultList();
			Set<String> existDb = new HashSet<>();
			for (Object[] obj : dvs) {
				String uid = (String) obj[0];
				String msn = (String) obj[1];
				existDb.add(uid);
				cache.put(uid, msn == null ? "" : msn);
			}
			Set<String> existCache = cache.keySet();
			for (String uid : existCache) {
				if (!existDb.contains(uid)) {
					cache.remove(uid);
				}
			}
			return;
		}
		if ("remove".equalsIgnoreCase(action)) {
			cache.remove(currentUid);
			return;
		}
		CARequestLog ca = caRequestLogRepository.findByUid(currentUid).orElse(null);
		if (ca != null) {
			cache.put(ca.getUid(), ca.getMsn() == null ? "" : ca.getMsn());
		}
	}

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void setActivationDate(Long activationDate, Set<Long> ids) {
        caRequestLogRepository.setActivationDate(activationDate, ids);
    }

    @Override
    public void checkDevicesOffline() {
        caRequestLogRepository.checkDevicesOffline();
    }

	@Override
	public Number countAlarms() {
		return caRequestLogRepository.countAlarms();
	}
	
	@Override
	public ScreenMonitoringDto mqttStatusCheck() {
		Optional<ScreenMonitoring> opt = screenMonitoringRepository.findByKey(ScreenMonitorKey.MQTT_STATUS);
		boolean mqttCheck = Mqtt.getInstance(evsPAMQTTAddress, mqttClientId).isConnected();
		Long now = System.currentTimeMillis();
		ScreenMonitoringDto dto = new ScreenMonitoringDto();
		
		if (opt.isPresent()) {
			ScreenMonitoring sm = opt.get();
			dto.setKey(ScreenMonitorKey.MQTT_STATUS.name());

			if (mqttCheck) {
				sm.setValue("UP");
				sm.setStatus(ScreenMonitorStatus.OK);
				sm.setLastUpTime(now);
				screenMonitoringRepository.save(sm);
			} else {
				sm.setValue("DOWN");
				sm.setStatus(ScreenMonitorStatus.NOT_OK);
				if (sm.getLastDownTime() == null || (sm.getLastDownTime() != null && sm.getLastUpTime() != null
						&& sm.getLastDownTime() < sm.getLastUpTime())) {
					sm.setLastDownTime(now);
					screenMonitoringRepository.save(sm);
				}
			}
			dto.setValue(sm.getValue());
			dto.setLastUpTime(sm.getLastUpTime());
			dto.setLastDownTime(sm.getLastDownTime());
		} else {
			ScreenMonitoring sm = new ScreenMonitoring();
			sm.setKey(ScreenMonitorKey.MQTT_STATUS);
			sm.setStatus(mqttCheck ? ScreenMonitorStatus.OK : ScreenMonitorStatus.NOT_OK);
			sm.setValue(mqttCheck ? "UP" : "DOWN");
			if (mqttCheck) {
				sm.setLastUpTime(now);
			} else {
				sm.setLastDownTime(now);
			}
			dto.setKey(ScreenMonitorKey.MQTT_STATUS.name());
			dto.setValue(sm.getValue());
			dto.setLastUpTime(sm.getLastUpTime());
			dto.setLastDownTime(sm.getLastDownTime());
			screenMonitoringRepository.save(sm);
		}
		return dto;
	}
	
	@Override
	public void markViewAll() {
		caRequestLogRepository.markViewAll();
	}

    @Override
    public Map<String, Integer> getCountDevices() {
        Map<String, Integer> result = new LinkedHashMap<>();
        Integer totalDevices = caRequestLogRepository.findAll().size();
        result.put("totalDevices", totalDevices);
        Arrays.asList(DeviceStatus.values()).forEach(status -> {
            Integer count = caRequestLogRepository.getCountDevicesByStatus(status);
            result.put(status.name(), count);
        });
        Arrays.asList(DeviceType.values()).forEach(type -> {
            Integer count = caRequestLogRepository.getCountDevicesByType(type);
            result.put(type.name(), count);
        });
        return result;
    }

    @Override
    public void checkDatabase() {
        Optional<ScreenMonitoring> opt = screenMonitoringRepository.findByKey(ScreenMonitorKey.DB_CHECK);
        Long dbSize = caRequestLogRepository.getDatabaseSize();
        if (dbSize != null) {
            dbSize = dbSize / (1024 * 1024);
        }
        try {
            caRequestLogRepository.checkDatabase();
            if (dbSize != null) {
                checkAndSaveScreenMonitoring(opt, dbSize.toString(), ScreenMonitorStatus.OK, ScreenMonitorKey.DB_CHECK);
            } else {
                checkAndSaveScreenMonitoring(opt, "N/A", ScreenMonitorStatus.OK, ScreenMonitorKey.DB_CHECK);
            }
        } catch (Exception e) {
            checkAndSaveScreenMonitoring(opt, "N/A", ScreenMonitorStatus.NOT_OK, ScreenMonitorKey.DB_CHECK);
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
            sm.setKey(ScreenMonitorKey.DB_CHECK);
            sm.setStatus(status);
            sm.setValue(value);
            screenMonitoringRepository.save(sm);
        }
    }

    @Override
    public List<ScreenMonitoring> getDashboard() {
        List<ScreenMonitoring> result = screenMonitoringRepository.findAll();
        return result;
    }

    @Override
    public void checkServerCertificate() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
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

    @Override
    public PaginDto<CARequestLog> getDevicesInGroup(List<Long> listGroupId) {
        PaginDto<CARequestLog> pagin = new PaginDto<>();
        List<CARequestLog> listDevices = caRequestLogRepository.findDevicesInGroup(listGroupId);
        pagin.setTotalRows((long) listDevices.size());
        listDevices.forEach(li -> {
            Users user = li.getInstaller();
            Users installer = new Users();
            if (user != null) {
                installer.setUserId(user.getUserId());
                installer.setUsername(user.getUsername());
                li.setInstaller(installer);
            }
        });
        pagin.setResults(listDevices);
        return pagin;
    }
    
    @Override
    public void searchCaRequestLog (PaginDto<CaRequestLogDto> pagin) {
    	Map<String, Object> map = pagin.getOptions();
		
        Long fromDate = (Long) map.get("fromDate");
        Long toDate = (Long) map.get("toDate");
        String userName = (String) map.get("userName");
		
		StringBuilder sqlBuilder = new StringBuilder("select l FROM CARequestLog l  JOIN Users u ON l.installer.userId = u.userId ");
        StringBuilder sqlCountBuilder = new StringBuilder("SELECT count(*) FROM CARequestLog l JOIN Users u ON l.installer.userId = u.userId");

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
        
        sqlBuilder.append(sqlCommonBuilder).append(" ORDER BY l.coupledDatetime DESC");
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

        List<CARequestLog> list = query.getResultList();

        list.forEach(li -> {
        	CaRequestLogDto dto = CaRequestLogDto.builder()
                        .id(li.getId())
                        .uid(li.getUid())
                        .msn(li.getMsn())
                        .group(li.getGroup().getId())
                        .status(li.getStatus())
                        .installer(li.getInstaller().getUserId())
                        .installerName(li.getInstaller().getUsername())
                        .installerEmail(li.getInstaller().getEmail())
                        .build();
                pagin.getResults().add(dto);
        }); 
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
	public void removelog() {
		Calendar c = Calendar.getInstance();
		c.add(Calendar.MONTH, -1);
		Query query = em.createQuery("DELETE FROM Log where createDate < :cd");
		query.setParameter("cd", c.getTime());
		query.executeUpdate();
	}

    @Transactional
	@Override
	public void removePiLlog() {
    	removelog();
		Calendar c = Calendar.getInstance();
		c.add(Calendar.MONTH, -1);
		Query query = em.createQuery("DELETE FROM PiLog where createDate < :cd");
		query.setParameter("cd", c.getTime());
		query.executeUpdate();
		
		query = em.createQuery("DELETE FROM MeterLog where createDate < :cd");
		query.setParameter("cd", c.getTime());
		query.executeUpdate();
	}

    @Transactional
	@Override
	public void removeDevice(String uId, String reason) {
		CARequestLog caRequestLog = caRequestLogRepository.findByUid(uId).orElse(null);
		if (caRequestLog != null && caRequestLog.getType() == DeviceType.NOT_COUPLED) {//8931070521315025237F
			caRequestLogRepository.delete(caRequestLog);
			
			try {
				DeviceRemoveLog log = DeviceRemoveLog.build(caRequestLog);
				AppProps.getContext().getBean(this.getClass()).updateDeviceLogs(log, reason, "REMOVE", null);
			} catch (Exception e) {
				LOG.error(e.getMessage(), e);
			}
			updateCacheUidMsnDevice(caRequestLog.getUid(), "remove");
		}
	}

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateDeviceLogs(DeviceRemoveLog log, String reason, String operation, AddressLog addrLog) {
    	try {
			log.setId(null);
			log.setOperation(operation);
			if (StringUtils.isBlank(log.getOperationBy())) {
				log.setOperationBy(SecurityUtils.getEmail());	
			}
			if (StringUtils.isNotBlank(reason)) {
				log.setReason(reason);
			}
			if (addrLog != null) {
				log.setAddressLog(addrLog);
			}
			deviceRemoveLogRepository.save(log);
		} catch (Exception e) {
			LOG.error(e.getMessage(), e);
		}
    }
    
	@Transactional
	@Override
	public void unLinkMsn(String uId) {
		CARequestLog caRequestLog = caRequestLogRepository.findByUid(uId).orElse(null);
		if (caRequestLog != null) {
			// caRequestLog.setStatus(DeviceStatus.NOT_COUPLED);
			caRequestLog.setType(DeviceType.NOT_COUPLED);
			caRequestLog.setOldMsn(caRequestLog.getMsn());
			caRequestLog.setMsn(null);
			caRequestLog.setCoupledDatetime(null);
			caRequestLogRepository.save(caRequestLog);
			caRequestLogRepository.flush();
			updateMMSMeter(caRequestLog, null);
			updateCacheUidMsnDevice(caRequestLog.getUid(), "update");
			
			try {
				DeviceRemoveLog log = DeviceRemoveLog.build(caRequestLog);
				AppProps.getContext().getBean(this.getClass()).updateDeviceLogs(log, null, "DE-COUPLE MSN", null);
			} catch (Exception e) {
				LOG.error(e.getMessage(), e);
			}
			
		} else {
			throw new RuntimeException("Device doesn't exists!");
		}
	}

	@Override
	public PaginDto<DeviceRemoveLogDto> getDeviceRemoveLogs(PaginDto<DeviceRemoveLogDto> pagin) {
		StringBuilder sqlBuilder = new StringBuilder("FROM DeviceRemoveLog ca ");
		StringBuilder sqlCountBuilder = new StringBuilder("SELECT count(*) FROM DeviceRemoveLog ca");
		StringBuilder sqlCommonBuilder = new StringBuilder();

		Map<String, Object> options = pagin.getOptions();
		Long fromDate = (Long) options.get("fromDate");
		Long toDate = (Long) options.get("toDate");
		String querySn = (String) options.get("querySn");
		String queryMsn = (String) options.get("queryMsn");
		String queryUuid = (String) options.get("queryUuid");
		String queryEsimId = (String) options.get("queryEsimId");
		String queryRemark = (String) options.get("queryRemark");
		String queryOperation = (String) options.get("queryOperation");
		String queryOperationBy = (String) options.get("queryOperationBy");
		Boolean enrollmentDate = BooleanUtils.toBoolean((String) options.get("queryEnrollmentDate"));
		boolean queryOperationDate = "true".equalsIgnoreCase(options.get("queryOperationDate") + "");
		Long queryVendor = StringUtils.isNotBlank((String) options.get("queryVendor"))
				? Long.parseLong((String) options.get("queryVendor"))
				: null;

		sqlCommonBuilder.append(" WHERE     ");

		if (BooleanUtils.isTrue(enrollmentDate)) {
			if (fromDate != null && toDate == null) {
				sqlCommonBuilder.append(" enrollmentDatetime >= " + fromDate + " AND ");
			}
			if (fromDate == null && toDate != null) {
				sqlCommonBuilder.append(" enrollmentDatetime <= " + toDate + " AND ");
			}
			if (fromDate != null && toDate != null) {
				sqlCommonBuilder.append(" ( enrollmentDatetime >= " + fromDate);
				sqlCommonBuilder.append(" AND enrollmentDatetime <= " + toDate + ") AND ");
			}
		} 
		if (queryOperationDate) {
            if (fromDate != null && toDate == null) {
                sqlCommonBuilder.append(" EXTRACT(EPOCH FROM createDate) * 1000 >= " + fromDate + "AND");
            }
            if (fromDate == null && toDate != null) {
                sqlCommonBuilder.append(" EXTRACT(EPOCH FROM createDate) * 1000 <= " + toDate + "AND");
            }
            if (fromDate != null && toDate != null) {
                sqlCommonBuilder.append(" ( EXTRACT(EPOCH FROM createDate) * 1000 >= " + fromDate);
                sqlCommonBuilder.append(" AND EXTRACT(EPOCH FROM createDate) * 1000 <= " + toDate + ") AND ");
            }
		}
		
		if (StringUtils.isNotBlank(querySn)) {
			sqlCommonBuilder.append(" upper(sn) like '%" + querySn.toUpperCase() + "%' AND ");
		}
		if (StringUtils.isNotBlank(queryMsn)) {
			sqlCommonBuilder.append(" msn like '%" + queryMsn + "%' AND ");
		}
		if (StringUtils.isNotBlank(queryUuid)) {
			sqlCommonBuilder.append(" upper(uid) like '%" + queryUuid.toUpperCase() + "%' AND ");
		}
		if (StringUtils.isNotBlank(queryEsimId)) {
			sqlCommonBuilder.append(" upper(cid) like '%" + queryEsimId.toUpperCase() + "%' AND ");
		}
		if (StringUtils.isNotBlank(queryRemark)) {
			sqlCommonBuilder.append(" upper(remark) like '%" + queryRemark.toUpperCase() + "%' AND ");
		}
		if (StringUtils.isNotBlank(queryOperation)) {
			sqlCommonBuilder.append(" upper(operation) = '" + queryOperation.toUpperCase() + "' AND ");
		}
		if (StringUtils.isNotBlank(queryOperationBy)) {
			sqlCommonBuilder.append(" upper(operationBy) like '%" + queryOperationBy.toUpperCase().trim() + "%' AND ");
		}
		
		if (queryVendor != null) {
			sqlCommonBuilder.append(" vendor.id = " + queryVendor + " AND ");
		}
		sqlCommonBuilder.append(" sn is not null and sn <> ''  AND ");

		sqlCommonBuilder.delete(sqlCommonBuilder.length() - 4, sqlCommonBuilder.length());

		if (sqlCommonBuilder.length() < 10) {
			sqlCommonBuilder.append(" 1 = 1 ");
		}

		sqlBuilder.append(sqlCommonBuilder).append(" ORDER BY id desc");
		sqlCountBuilder.append(sqlCommonBuilder);

		if (pagin.getOffset() == null || pagin.getOffset() < 0) {
			pagin.setOffset(0);
		}

		if (pagin.getLimit() == null || pagin.getLimit() <= 0) {
			pagin.setLimit(100);
		}

		Query queryCount = em.createQuery(sqlCountBuilder.toString());

		Long count = ((Number) queryCount.getSingleResult()).longValue();
		pagin.setTotalRows(count);
		pagin.setResults(new ArrayList<>());
		if (count == 0l) {
			return pagin;
		}

		Query query = em.createQuery(sqlBuilder.toString());
		query.setFirstResult(pagin.getOffset());
		query.setMaxResults(pagin.getLimit());

		List<DeviceRemoveLog> list = query.getResultList();
		list.forEach(dv -> {
			try {
				DeviceRemoveLogDto dto = DeviceRemoveLogDto.build(dv);
				pagin.getResults().add(dto);
			} catch (Exception e) {
				e.printStackTrace();
			}
		});
		return pagin;
	}
	
	@Override
	public void sendRLSCommandForDevices(List<CARequestLog> listDevice, String command, Map<String, Object> options, String commandSendBy, String uuid) {
		LOG.info("Sending command: " + command + "to list devices. List device size: " + listDevice.size());
		
		String comment = (String) options.get("comment");
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
		if (StringUtils.isNotBlank(comment)) {
			options.remove("comment");	
		}
		
        Long fromDate = (Long) options.get("fromDate");
        Long toDate = (Long) options.get("toDate");
        Long queryGroup = StringUtils.isNotBlank((String) options.get("queryGroup")) ? Long.parseLong((String) options.get("queryGroup")) : null;
        Long queryBuilding = StringUtils.isNotBlank((String) options.get("queryBuilding")) ? Long.parseLong((String) options.get("queryBuilding")) : null;
        Long queryBlock = StringUtils.isNotBlank((String) options.get("queryBlock")) ? Long.parseLong((String) options.get("queryBlock")) : null;
        Long queryFloorLevel = StringUtils.isNotBlank((String) options.get("queryFloorLevel")) ? Long.parseLong((String) options.get("queryFloorLevel")) : null;
        Long queryBuildingUnit = StringUtils.isNotBlank((String) options.get("queryBuildingUnit")) ? Long.parseLong((String) options.get("queryBuildingUnit")) : null;
        Long queryVendor = StringUtils.isNotBlank((String) options.get("queryVendor")) ? Long.parseLong((String) options.get("queryVendor")) : null;
        
        if (fromDate != null) {
        	String strFromDate = sdf.format(new Date(fromDate));
        	options.put("fromDate", strFromDate);
        }
        if (toDate != null) {
        	String strToDate = sdf.format(new Date(toDate));
        	options.put("toDate", strToDate);
        }
        if (queryGroup != null) {
        	Optional<Group> opt = groupRepository.findById(queryGroup);
        	if (opt.isPresent()) {
        		options.put("queryGroup", opt.get().getName());
        	}
        }
        if (queryBuilding != null) {
        	Optional<Building> opt = buildingRepository.findById(queryBuilding);
        	if (opt.isPresent()) {
        		options.put("queryBuilding", opt.get().getName());
        	}
        }
        if (queryBlock != null) {
        	Optional<Block> opt = blockRepository.findById(queryBlock);
        	if (opt.isPresent()) {
        		options.put("queryBlock", opt.get().getName());
        	}
        }
        if (queryFloorLevel != null) {
        	Optional<FloorLevel> opt = floorLevelRepository.findById(queryFloorLevel);
        	if (opt.isPresent()) {
        		options.put("queryFloorLevel", opt.get().getName());
        	}
        }
        if (queryBuildingUnit != null) {
        	Optional<BuildingUnit> opt = buildingUnitRepository.findById(queryBuildingUnit);
        	if (opt.isPresent()) {
        		options.put("queryBuildingUnit", opt.get().getName());
        	}
        }
        if (queryVendor != null) {
        	Optional<Vendor> opt = vendorRepository.findById(queryVendor);
        	if (opt.isPresent()) {
        		options.put("queryVendor", opt.get().getName());
        	}
        }
		
		RelayStatusLog rl = new RelayStatusLog();
		rl.setBatchUuid(uuid);
		rl.setCommand(command);
		rl.setComment(comment);
		rl.setFilters(options.toString());
		rl.setCommandSendBy(commandSendBy);
		rl.setTotalCount(listDevice.size());
		rl.setCurrentCount(0);
		rl.setErrorCount(0);
		relayStatusLogRepository.save(rl);
		
		int successCount = 0;
		int errorCount = 0;
		for (CARequestLog ca : listDevice) {
			try {
				SimpleMap<String, Object> map = SimpleMap.init("id", ca.getUid()).more("cmd", command);
				Long mid = evsPAService.nextvalMID(ca.getVendor());
				String sig = BooleanUtils.isTrue(ca.getVendor().getEmptySig()) ? "" : RSAUtil.initSignedRequest(ca.getVendor().getKeyPath(), new ObjectMapper().writeValueAsString(map), ca.getVendor().getSignatureAlgorithm());
				Log resultLog = evsPAService.publish(alias + ca.getUid(), SimpleMap.init(
	                    "header", SimpleMap.init("uid", ca.getUid()).more("mid", mid).more("gid", ca.getUid()).more("msn", ca.getMsn()).more("sig", sig)
	                ).more("payload", map), command, null);
				
				if (resultLog != null) {
					resultLog.setRlsBatchUuid(uuid);
					logRepository.save(resultLog);
				}
				
				successCount++;
				rl.setCurrentCount(successCount);
				updateRLSStatus(rl);
			} catch (Exception e) {
				errorCount++;
				rl.setErrorCount(errorCount);
				updateRLSStatus(rl);
				LOG.info("Error while sending command: " + command + ", uid: " + ca.getUid() + ". Error: " + e.getMessage());
				e.printStackTrace();
			}
		}
	}
	
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void updateRLSStatus(RelayStatusLog rl) {
		relayStatusLogRepository.save(rl);
	}

	@Override
	@Transactional
	public void getRelayStatusLogs(PaginDto<RelayStatusLogDto> pagin) {
		StringBuilder sqlBuilder = new StringBuilder("FROM RelayStatusLog ca ");
        StringBuilder sqlCountBuilder = new StringBuilder("SELECT count(*) FROM RelayStatusLog ca");
        StringBuilder sqlCommonBuilder = new StringBuilder();
        
        Map<String, Object> options = pagin.getOptions();
        Long fromDate = (Long) options.get("queryFromDate");
        Long toDate = (Long) options.get("queryToDate");
        String commandBy = (String) options.get("queryCommandBy");
        String command = (String) options.get("queryCommand");
        String comment = (String) options.get("queryComment");
        String batchUuid = (String) options.get("queryBatchUuid");
        String filter = (String) options.get("queryFilter");
        
        sqlCommonBuilder.append(" WHERE 1 = 1 ");
        
        if (fromDate != null && toDate == null) {
            sqlCommonBuilder.append(" AND EXTRACT(EPOCH FROM createDate) * 1000 >= " + fromDate);
        }
        if (fromDate == null && toDate != null) {
            sqlCommonBuilder.append(" AND EXTRACT(EPOCH FROM createDate) * 1000 <= " + toDate);
        }
        if (fromDate != null && toDate != null) {
            sqlCommonBuilder.append(" AND ( EXTRACT(EPOCH FROM createDate) * 1000 >= " + fromDate);
            sqlCommonBuilder.append(" AND EXTRACT(EPOCH FROM createDate) * 1000 <= " + toDate + ")");
        }
        if (StringUtils.isNotBlank(comment)) {
        	sqlCommonBuilder.append(" AND upper(comment) like '%" + comment.toUpperCase() + "%'");
        }
        if (StringUtils.isNotBlank(commandBy)) {
        	sqlCommonBuilder.append(" AND upper(commandSendBy) like '%" + commandBy.toUpperCase() + "%'");
        }
        if (StringUtils.isNotBlank(command)) {
        	sqlCommonBuilder.append(" AND upper(command) = '" + command.toUpperCase() + "'");
        }
        if (StringUtils.isNotBlank(batchUuid)) {
        	sqlCommonBuilder.append(" AND batchUuid = '" + batchUuid + "'");
        }
        if (StringUtils.isNotBlank(filter)) {
        	sqlCommonBuilder.append(" AND upper(filters) like '%" + filter.toUpperCase() + "%' ");
        }
        
        sqlBuilder.append(sqlCommonBuilder).append(" ORDER BY id DESC");
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
        
        List<RelayStatusLog> list = query.getResultList();
        list.forEach(rl -> {
        	RelayStatusLogDto dto = new RelayStatusLogDto().build(rl);
        	pagin.getResults().add(dto);
        });
	}
	
	@Override
	@Transactional
	public List<Map<String, String>> batchCoupleDevices(List<Map<String, String>> listInput) {
		
		if (!listInput.isEmpty()) {
			listInput.forEach(item -> {
				String msn = item.get("msn");
				String sn = item.get("sn");
				
				if (StringUtils.isBlank(sn) || StringUtils.isBlank(msn)) {
					item.put("error", "Both MSN and SN are required!");
				} else {
					if (BooleanUtils.isTrue(caRequestLogRepository.existsByMsn(msn))) {
		                item.put("error", "Invalid MSN, MSN is being linked!");
		            } else {
		            	Optional<CARequestLog> caOpt = caRequestLogRepository.findBySn(sn);
						if (!caOpt.isPresent()) {
							item.put("error", "MCU SN(QR Code) doesn't exist!");
						} else {
							CARequestLog ca = caOpt.get();
		        			ca.setMsn(msn);
		        			ca.setType(DeviceType.COUPLED);
		    				ca.setCoupledDatetime(System.currentTimeMillis());
		    				ca.setCoupledUser(SecurityUtils.getUsername());
		        			caRequestLogRepository.save(ca);
		        			caRequestLogRepository.flush();
		        			updateMMSMeter(ca, msn);
		        			
							try {
			        			updateCacheUidMsnDevice(ca.getUid(), "update");
			        			DeviceRemoveLog log = DeviceRemoveLog.build(ca);
			    				AppProps.getContext().getBean(this.getClass()).updateDeviceLogs(log, null, "COUPLE MSN", null);
							} catch (Exception e) {
								e.printStackTrace();
							}
						}
		            }	
				}
			});
			return listInput.stream().filter(item -> item.get("error") != null).collect(Collectors.toList());
		}
		return Collections.emptyList();
	}
}
