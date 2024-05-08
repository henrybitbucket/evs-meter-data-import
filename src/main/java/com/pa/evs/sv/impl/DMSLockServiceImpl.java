package com.pa.evs.sv.impl;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.persistence.EntityManager;
import javax.persistence.Query;

import com.pa.evs.dto.LockDto;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pa.evs.dto.BlockDto;
import com.pa.evs.dto.BuildingDto;
import com.pa.evs.dto.BuildingUnitDto;
import com.pa.evs.dto.DMSLocationDto;
import com.pa.evs.dto.DMSLocationLockDto;
import com.pa.evs.dto.DMSLocationSiteLockDto;
import com.pa.evs.dto.DMSLockDto;
import com.pa.evs.dto.DMSLockVendorDto;
import com.pa.evs.dto.DMSSiteDto;
import com.pa.evs.dto.FloorLevelDto;
import com.pa.evs.dto.PaginDto;
import com.pa.evs.dto.SaveLogReq;
import com.pa.evs.model.DMSBlock;
import com.pa.evs.model.DMSBuilding;
import com.pa.evs.model.DMSBuildingUnit;
import com.pa.evs.model.DMSFloorLevel;
import com.pa.evs.model.DMSLocationLock;
import com.pa.evs.model.DMSLocationSite;
import com.pa.evs.model.DMSLock;
import com.pa.evs.model.DMSLockEventLog;
import com.pa.evs.model.DMSLockVendor;
import com.pa.evs.model.DMSSite;
import com.pa.evs.model.DMSWorkOrders;
import com.pa.evs.repository.DMSBlockRepository;
import com.pa.evs.repository.DMSBuildingRepository;
import com.pa.evs.repository.DMSBuildingUnitRepository;
import com.pa.evs.repository.DMSFloorLevelRepository;
import com.pa.evs.repository.DMSLocationLockRepository;
import com.pa.evs.repository.DMSLockEventLogRepository;
import com.pa.evs.repository.DMSLockRepository;
import com.pa.evs.repository.DMSLockVendorRepository;
import com.pa.evs.repository.GroupUserRepository;
import com.pa.evs.repository.UserRepository;
import com.pa.evs.sv.DMSLockService;
import com.pa.evs.utils.ApiUtils;
import com.pa.evs.utils.AppProps;
import com.pa.evs.utils.DESUtil;
import com.pa.evs.utils.SchedulerHelper;
import com.pa.evs.utils.SecurityUtils;
import com.pa.evs.utils.TimeZoneHolder;
import com.pa.evs.utils.Utils;

@Service
@SuppressWarnings("unchecked")
public class DMSLockServiceImpl implements DMSLockService {

	static final Logger LOGGER = LoggerFactory.getLogger(DMSLockServiceImpl.class);
	
	static final ObjectMapper MAPPER = new ObjectMapper();
	String token;
	RestTemplate resttemplate = ApiUtils.getRestTemplate();

	@Value("${dms-key.url:https://dms-key.evs.com.sg/}")
	private String dmsKeyUrl;
	
	@Autowired
	DMSLockRepository dmsLockRepository;
	
	@Autowired
	DMSLockEventLogRepository dmsLockEventLogRepository;
	
	@Autowired
	DMSLockVendorRepository dmsLockVendorRepository;
	
	@Autowired
	private EntityManager em;
	
	@Autowired
	DMSFloorLevelRepository floorLevelRepository;
	
	@Autowired
	DMSBuildingRepository buildingRepository;
	
	@Autowired
	DMSBlockRepository blockRepository;
	
	@Autowired
	DMSBuildingUnitRepository buildingUnitRepository;
	
	@Autowired
	DMSLocationLockRepository dmsLocationLockRepository;
	
	@Autowired
	GroupUserRepository groupUserRepository;
	
	@Autowired
	UserRepository userRepository;
	
	@Transactional(readOnly = true)
	@Override
	public PaginDto<DMSLockDto> search(PaginDto<DMSLockDto> pagin) {
		LOGGER.info("Get DMS LOCK");
		
		Map<String, Object> options = pagin.getOptions();
        String queryBuilding = (String) options.get("queryBuilding");
        String queryBlock = (String) options.get("queryBlock");
        String queryFloorLevel = (String) options.get("queryFloorLevel");
        String queryBuildingUnit = (String) options.get("queryBuildingUnit");
        
        Long queryVendor = StringUtils.isNotBlank((String) options.get("queryVendor")) ? Long.parseLong((String) options.get("queryVendor")) : null;
		
		StringBuilder sqlBuilder = new StringBuilder(" SELECT lock, locationLock ");
		StringBuilder sqlCountBuilder = new StringBuilder("SELECT count(lock) ");

		StringBuilder sqlCommonBuilder = new StringBuilder(" FROM DMSLock lock LEFT JOIN DMSLocationLock locationLock on lock.id = locationLock.lock.id ");
		sqlCommonBuilder.append(" WHERE 1=1 ");
		
		
        if (StringUtils.isNotBlank(queryBuilding)) {
        	sqlCommonBuilder.append(" AND locationLock.building.id= '" + queryBuilding + "' ");	
        }
        if (StringUtils.isNotBlank(queryBlock)) {
        	sqlCommonBuilder.append(" AND locationLock.block.id= '" + queryBlock + "' ");	
        }
        if (StringUtils.isNotBlank(queryFloorLevel)) {
        	sqlCommonBuilder.append(" AND locationLock.floorLevel.id= '" + queryFloorLevel + "' ");
        }
        if (StringUtils.isNotBlank(queryBuildingUnit)) {
        	sqlCommonBuilder.append(" AND locationLock.buildingUnit.id= '" + queryBuildingUnit + "' ");
        }
		
        if (queryVendor != null) {
            sqlCommonBuilder.append(" AND lock.vendor.id = " + queryVendor + " ");
        }
        
		sqlBuilder.append(sqlCommonBuilder).append(" ORDER BY lock.id ASC");
		sqlCountBuilder.append(sqlCommonBuilder);

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

		List<Object[]> list = query.getResultList();
		
		list.forEach(li -> {
			DMSLockDto dto = DMSLockDto.build((DMSLock) li[0]);
			if (li[1] != null) {
				DMSLocationLock locationLock = (DMSLocationLock) li[1];
				dto.setHomeAddress(Utils.formatHomeAddress(locationLock));	
				if (locationLock.getBuilding() != null) {
					dto.setBuilding(BuildingDto.builder().id(locationLock.getBuilding().getId()).name(locationLock.getBuilding().getName()).build());
				}
				if (locationLock.getBlock() != null) {
					dto.setBlock(BlockDto.builder().id(locationLock.getBlock().getId()).name(locationLock.getBlock().getName()).build());
				}
				if (locationLock.getFloorLevel() != null) {
					dto.setFloorLevel(FloorLevelDto.builder().id(locationLock.getFloorLevel().getId()).name(locationLock.getFloorLevel().getName()).build());
				}
				if (locationLock.getBuildingUnit() != null) {
					dto.setBuildingUnit(BuildingUnitDto.builder().id(locationLock.getBuildingUnit().getId()).name(locationLock.getBuildingUnit().getName()).build());
				}
				dto.setLinkLockLocationId(locationLock.getId());
			}
			
            pagin.getResults().add(dto);
        });
		
		return pagin;
	}
	
	public Object getChinaLockPadLock() {
		try {
			
//			if (!SecurityUtils.hasSelectedAppCode("DMS")) {
//				throw new AccessDeniedException(HttpStatus.FORBIDDEN.getReasonPhrase());
//			}
			String url = DESUtil.getInstance().decrypt(AppProps.get("APP_PAS_LIST_LOCKS"));
			HttpEntity<Object> entity = new HttpEntity<Object>(null);
			return MAPPER.readValue(resttemplate.exchange(url.replace("${token}", token), HttpMethod.GET, entity, String.class).getBody(), Map.class);
		} catch (Exception e) {
			Map<String, Object> res = new LinkedHashMap<>();
			res.put("code", -1);
			res.put("data", null);
			res.put("info", e.getMessage());
			return res;
		}
	}
	
	@Override
	@Transactional
	public Object syncLock(Long vendorId) {
		
		// Vendors : ISRAALI_LOCK, CHINA_LOCK_PADLOCK
		// if vendorId -> get all vendor -> sync all
		
		if (vendorId != null) {
			Optional<DMSLockVendor> dmsLockVendorOpt = dmsLockVendorRepository.findById(vendorId);
			if (!dmsLockVendorOpt.isPresent()) {
				throw new RuntimeException("Vendor not found!");
			}
			
			DMSLockVendor dmsLockVendor = dmsLockVendorOpt.get();
			if ("CHINA_LOCK_PADLOCK".equalsIgnoreCase(dmsLockVendor.getName())) {
				getDefaultAndChinaLock();
			}
			
			//TODO implement other vendor
		} else {
			//Default if vendor is null --> get CHINA_LOCK_PADLOCK
			getDefaultAndChinaLock();
		}
		
		Map<String, Object> res = new LinkedHashMap<>();
		res.put("code", 0);
		res.put("data", null);
		return res;
	}
	
	public void getDefaultAndChinaLock() {
		Object data = getChinaLockPadLock();
		if (data instanceof Map) {
			Optional<DMSLockVendor> dmsLockVendorOpt = dmsLockVendorRepository.findByName("CHINA_LOCK_PADLOCK");
        	DMSLockVendor vendor = null;
        	if (!dmsLockVendorOpt.isPresent()) {
        		DMSLockVendor newVendor = new DMSLockVendor();
        		newVendor.setName("CHINA LOCK PADLOCK");
        		dmsLockVendorRepository.save(newVendor);
        		vendor = newVendor;
        	} else {
        		vendor = dmsLockVendorOpt.get();
        	}
        	
			Map<String, Object> dataMap = (Map<String, Object>) data;
			if (dataMap.containsKey("data")) {
                Map<String, Object> dataObject = (Map<String, Object>) dataMap.get("data");
                if (dataObject != null && dataObject.containsKey("arealocks")) {
                    List<Map<String, Object>> arealocks = (List<Map<String, Object>>) dataObject.get("arealocks");
                    for (Map<String, Object> lock : arealocks) {
                        String lockName = lock.get("lockname") != null ? (String) lock.get("lockname") : null;
                        String lockId = lock.get("id") != null ? (lock.get("id") + "") : null;
                        String lockEsn = lock.get("lockesn") != null ? (String) lock.get("lockesn") : null;
                        String lockBid = lock.get("lockbid") != null ? (String) lock.get("lockbid") : null;
                        String lngStr = lock.get("long") != null ? (String) lock.get("long") : null;
                        String latStr = lock.get("lat") != null ? (String) lock.get("lat") : null;
                        String secretKey = lock.get("secretkey") != null ? (String) lock.get("secretkey") : null;
                        String areaId = lock.get("areaid") != null ? (lock.get("areaid") + "") : null;
                        String lockNumber = lock.get("locknumber") != null ? (String) lock.get("locknumber") : null;
                        
                        if (lockId != null && lockNumber != null) {
                        	BigDecimal lat = StringUtils.isNotBlank(latStr) ? new BigDecimal(latStr) : null;
                        	BigDecimal lng = StringUtils.isNotBlank(lngStr) ? new BigDecimal(lngStr) : null;
                        	
                        	Optional<DMSLock> lockOpt = dmsLockRepository.findByOriginalIdAndLockNumber(lockId, lockNumber);

                            if (lockOpt.isPresent()) {
                            	DMSLock existingLock = lockOpt.get();
                            	existingLock.setAreaId(areaId);
                            	existingLock.setLat(lat);
                            	existingLock.setLng(lng);
                            	existingLock.setLockBid(lockBid);
                            	existingLock.setLockEsn(lockEsn);
                            	existingLock.setLockName(lockName);
                            	existingLock.setModifyDate(new Date());
                            	existingLock.setSecretKey(secretKey);
                            	existingLock.setVendor(vendor);
                            	dmsLockRepository.save(existingLock);
                            } else {
                            	DMSLock newLock = new DMSLock();
                            	newLock.setLockNumber(lockNumber);
                            	newLock.setOriginalId(lockId);
                            	newLock.setAreaId(areaId);
                            	newLock.setLat(lat);
                            	newLock.setLng(lng);
                            	newLock.setLockBid(lockBid);
                            	newLock.setLockEsn(lockEsn);
                            	newLock.setLockName(lockName);
                            	newLock.setModifyDate(new Date());
                            	newLock.setSecretKey(secretKey);
                            	newLock.setVendor(vendor);
                            	dmsLockRepository.save(newLock);
                            }
                        }
                    }
                }
			}
		}
	}
	
	@Override
	public List<DMSLockVendorDto> getDMSLockVendors() {
		List<DMSLockVendorDto> res = new ArrayList<>();
		List<DMSLockVendor> verdors = dmsLockVendorRepository.findAll();
		for (DMSLockVendor vendor : verdors) {
			DMSLockVendorDto dto = new DMSLockVendorDto();
			dto.setId(vendor.getId());
			dto.setName(vendor.getName());
			res.add(dto);
		}
		return res;
	}
	
	@PostConstruct
	public void init() {
		
        new Thread(() -> {
    		try {
    			loginPAS();
    		} finally {
    			SchedulerHelper.scheduleJob("0 0/3 * * * ? *", () -> {
    				loginPAS();
    			}, "APP_PAS_LOGIN");
    		}
    		
    		// sync on init
    		AppProps.getContext().getBean(this.getClass()).syncLock(null);
        }).start();

	}
	
	void loginPAS() {
		try {
			HttpEntity<Object> entity = new HttpEntity<Object>(null);
			String pasLoginUrl = AppProps.get("APP_PAS_LOGIN");
			if (StringUtils.isBlank(pasLoginUrl)) {
				return;
			}
			String url = DESUtil.getInstance().decrypt(AppProps.get("APP_PAS_LOGIN"));
			Map<String, Object> res = MAPPER.readValue(resttemplate.exchange(url, HttpMethod.GET, entity, String.class).getBody(), Map.class);
			Map<String, Object> data = (Map<String, Object>) res.get("data");
			token = (String) data.get("token");
			System.out.println(token);
		} catch (Exception e) {
			LOGGER.error(e.getMessage(), e);
		}
	}

	@Transactional
	@Override
	public void linkLocation(DMSLocationLockDto dto) {
		DMSBuildingUnit unit = buildingUnitRepository.findById(dto.getBuildingUnitId()).orElseThrow(() -> new RuntimeException("Building unit is required"));
		
		DMSFloorLevel level = unit.getFloorLevel();
		
		if (!level.getId().equals(dto.getFloorLevelId())) {
			throw new RuntimeException("FloorLevel invalid!");
		}

		DMSBlock block = level.getBlock();
		if (!block.getId().equals(dto.getBlockId())) {
			throw new RuntimeException("Block invalid!");
		}
		
		DMSBuilding building = block.getBuilding();
		if (!building.getId().equals(dto.getBuildingId())) {
			throw new RuntimeException("Building invalid!");
		}
		
		String locationKey = unit.getId() + "__" + level.getId() + "__" + block.getId() + "__" + building.getId();
		
		DMSLocationLock locationLock = dmsLocationLockRepository.findByLockId(dto.getLockId()).orElse(new DMSLocationLock());
		locationLock.setBuilding(building);
		locationLock.setBlock(block);
		locationLock.setFloorLevel(level);
		locationLock.setBuildingUnit(unit);
		locationLock.setLock(dmsLockRepository.findById(dto.getLockId()).orElseThrow(() -> new RuntimeException("Lock not found!")));
		locationLock.setLocationKey(locationKey);
		dmsLocationLockRepository.save(locationLock);
	}

	@Transactional
	@Override
	public void unLinkLocation(Long linkLockLocationId) {
		dmsLocationLockRepository.deleteById(linkLockLocationId);
	}

	@Override
	@Transactional(readOnly = true)
	public Object getSecretCode2(String userMobile, Long dmsLockId) {
		
		return getSecretCode(userMobile, dmsLockId);
	}
	
	@Override
	@Transactional(readOnly = true)
	public Object getSecretCode(String userMobile, Long dmsLockId) {
	
		DMSLocationLock dmsLocationLock = dmsLocationLockRepository.findByLockId(dmsLockId).orElseThrow(() -> new RuntimeException("Lock not found or not link location!"));
		
		List<Long> appOfUser = em.createQuery(" SELECT app.id FROM DMSApplicationUser appUser where appUser.app.status = 'APPROVAL' and appUser.phoneNumber = :userPhone " )
				.setParameter("userPhone", userMobile)
				.getResultList();
		if (appOfUser.isEmpty()) {
			throw new RuntimeException("user not in any app");
		}
		
		// find site of lock
		List<Long> siteIds = em.createQuery("SELECT locationSite.site.id FROM DMSLocationSite locationSite where locationSite.locationKey = '" + dmsLocationLock.getLocationKey() + "' ")
		.getResultList();
		
		if (siteIds.isEmpty()) {
			throw new RuntimeException("user not in any site");
		}
		
		// get ApplicationSite by site in application of user and in site of lock => list appSite => list wod
		List<DMSWorkOrders> dmsWorkOrders = em.createQuery(new StringBuilder(" SELECT appSite.workOrder FROM DMSApplicationSite appSite where appSite.app.id in (:appId)  " )
				.append(" AND appSite.app.status = 'APPROVAL' ")
				.append(" AND appSite.site.id in :siteIds ")
				.append(" order by appSite.site.id desc ").toString())
				
		.setParameter("siteIds", siteIds)
		.setParameter("appId", appOfUser)
		.getResultList();
		
		for (DMSWorkOrders workOrders : dmsWorkOrders) {
			if (isMatch(dmsLocationLock.getLock(), workOrders)) {
				return dmsLocationLock.getLock().getSecretKey();
			}
		}
		
		throw new RuntimeException("not match any time period");
	}

	@Override
	@Transactional(readOnly = true)
	public Object getSecretCode(String userMobile, LockDto lockDto) {

		Optional<DMSLock> dmsLock = null;
		if (StringUtils.isNotBlank(lockDto.getLockNumber())) {
			dmsLock = dmsLockRepository.findByLockNumber(lockDto.getLockNumber());
		} else if (StringUtils.isNotBlank(lockDto.getBid())) {
			dmsLock = dmsLockRepository.findByLockBid(lockDto.getBid());
		}
		if (!dmsLock.isPresent() || dmsLock.get() == null) {
			throw new RuntimeException("Lock not found");
		}

		DMSLocationLock dmsLocationLock = dmsLocationLockRepository.findByLockId(dmsLock.get().getId()).orElseThrow(() -> new RuntimeException("Lock not found or not link location!"));

		List<Long> appOfUser = em.createQuery(" SELECT app.id FROM DMSApplicationUser appUser where appUser.app.status = 'APPROVAL' and appUser.phoneNumber = :userPhone " )
				.setParameter("userPhone", userMobile)
				.getResultList();
		if (appOfUser.isEmpty()) {
			throw new RuntimeException("user not in any app");
		}

		// find site of lock
		List<Long> siteIds = em.createQuery("SELECT locationSite.site.id FROM DMSLocationSite locationSite where locationSite.locationKey = '" + dmsLocationLock.getLocationKey() + "' ")
				.getResultList();

		if (siteIds.isEmpty()) {
			throw new RuntimeException("user not in any site");
		}

		// get ApplicationSite by site in application of user and in site of lock => list appSite => list wod
		List<DMSWorkOrders> dmsWorkOrders = em.createQuery(new StringBuilder(" SELECT appSite.workOrder FROM DMSApplicationSite appSite where appSite.app.id in (:appId)  " )
						.append(" AND appSite.app.status = 'APPROVAL' ")
						.append(" AND appSite.site.id in :siteIds ")
						.append(" order by appSite.site.id desc ").toString())

				.setParameter("siteIds", siteIds)
				.setParameter("appId", appOfUser)
				.getResultList();

		for (DMSWorkOrders workOrders : dmsWorkOrders) {
			if (isMatch(dmsLocationLock.getLock(), workOrders)) {
				//apply sv token
				String svcToken = "";
				String secKey = "";
				try {
					String url = dmsKeyUrl + "auth/apply_svc_token";
					HttpHeaders headers = new HttpHeaders();
					headers.setContentType(MediaType.APPLICATION_JSON);
					String json = "{\n" +
							"    \"api_key\": \"3Z6k1t9gII0yc\",\n" +
							"    \"username\": \"dms.henry\",\n" +
							"    \"svc_name\": \"dms.keysvc\",\n" +
							"    \"endpoint\": \"/key/get_device_sec_key\",\n" +
							"    \"scope\": \"\",\n" +
							"    \"target\": \"\",\n" +
							"    \"operation\": \"\"\n" +
							"}";

					HttpEntity<Object> entity = new HttpEntity<>(json, headers);
					ResponseEntity<Map> response = ApiUtils.getRestTemplate().exchange(url, HttpMethod.POST, entity, Map.class);
					if (response != null && response.getBody() != null && response.getBody().get("svc_token") != null) {
						svcToken = response.getBody().get("svc_token") + "";
					} else if (response != null && response.getBody() != null && response.getBody().get("error") != null) {
						throw new RuntimeException(response.getBody().get("error") + "");
					} else {
						throw new RuntimeException("Fail to generate svc token");
					}
				} catch (Exception e) {
					throw new RuntimeException("Fail to generate svc token");
				}

				//get sec_key
				if (StringUtils.isNotBlank(svcToken)) {
					try {
						String url = dmsKeyUrl + "key/get_device_sec_key";
						HttpHeaders headers = new HttpHeaders();
						headers.setContentType(MediaType.APPLICATION_JSON);
						headers.add("Authorization", "Bearer " + svcToken);
						String json = "{\n" +
								"    \"svcClaimDto\":{\n" +
								"        \"api_key\":\"3Z6k1t9gII0yc\",\n" +
								"        \"username\":\"dms.henry\",\n" +
								"        \"svc_name\":\"dms.keysvc\",\n" +
								"        \"endpoint\":\"/key/get_device_sec_key\",\n" +
								"        \"scope\":\"\",\n" +
								"        \"target\":\"\",\n" +
								"        \"operation\":\"\"\n" +
								"    },\n" +
								"    \"request\":\n" +
								"    {\n" +
								"        \"api_key\":\"3Z6k1t9gII0yc\",\n" +
								"        \"tracer\":\"" + lockDto.getTracer() + "\",\n" +
								"        \"vendor_id\":\"10\",\n" ;

						if (StringUtils.isNotBlank(lockDto.getLockNumber())) {
							json = json + "        \"item_number\":\"" + lockDto.getLockNumber()+ "\"    \n" ;
						} else if (StringUtils.isNotBlank(lockDto.getBid())) {
							json = json + "        \"bid\":\"" + lockDto.getBid() + "\"    \n" ;
						}
						json = json +
								"    }\n" +
								"}";

						HttpEntity<Object> entity = new HttpEntity<>(json, headers);
						ResponseEntity<Map> response = ApiUtils.getRestTemplate().exchange(url, HttpMethod.POST, entity, Map.class);
						if (response != null && response.getBody() != null && response.getBody().get("sec_key") != null) {
							secKey = response.getBody().get("sec_key") + "";
						} else if (response != null && response.getBody() != null && response.getBody().get("error") != null) {
							throw new RuntimeException(response.getBody().get("error") + "");
						} else {
							throw new RuntimeException("Fail to secret key");
						}
					} catch (Exception e) {
						throw new RuntimeException("Fail to secret key");
					}
				}
				return secKey;
			}
		}

		throw new RuntimeException("not match any time period");
	}

	@Transactional(readOnly = true)
	@Override
	public Object getAssignedLocks2(String userMobile, Boolean lockOnly) {
		
		return getAssignedLocks(userMobile, lockOnly);
	}
	
	@Transactional(readOnly = true)
	@Override
	public Object getAssignedLocks(String userMobile, Boolean lockOnly) {
		
		DMSLocationSiteLockDto rs = DMSLocationSiteLockDto.builder().build();
//		List<Long> groupIdOfUsers = em.createQuery(" SELECT groupUser.id FROM UserGroup where user.email = :email and groupUser.appCode.name = 'DMS' " )
//				.setParameter("email", email)
//				.getResultList();
//		
//		if (groupIdOfUsers.isEmpty()) {
//			return rs;
//		}
		
		//List<DMSSite> sitesOfUser = em.createQuery(" SELECT wod.site FROM DMSWorkOrders wod where wod.group.id in (:groupIds) order by wod.site.id desc " )
		
		List<Long> appOfUser = em.createQuery(" SELECT app.id FROM DMSApplicationUser appUser where appUser.app.status = 'APPROVAL' and appUser.phoneNumber = :userPhone " )
				.setParameter("userPhone", userMobile)
				.getResultList();
		if (appOfUser.isEmpty()) {
			return rs;
		}
		
		List<DMSSite> sitesOfUser = em.createQuery(" SELECT appSite.site FROM DMSApplicationSite appSite where appSite.app.id in (:appId) order by appSite.site.id desc " )
		//.setParameter("groupIds", groupIdOfUsers)
		.setParameter("appId", appOfUser)
		.getResultList();
		
		if (sitesOfUser.isEmpty()) {
			return rs;
		}
		
		List<Object[]> locationLockSites = em.createQuery("SELECT locationLock, locationSite FROM DMSLocationLock locationLock join DMSLocationSite locationSite on locationLock.locationKey = locationSite.locationKey where locationSite.site.id in (:siteIdOfUsers) " )
		.setParameter("siteIdOfUsers", sitesOfUser.stream().map(s -> s.getId()).collect(Collectors.toSet()))
		.getResultList();
		
		Map<Long, DMSSiteDto> siteMap = new LinkedHashMap<>();
		
		for (DMSSite site : sitesOfUser) {
			DMSSiteDto siteDto = siteMap.computeIfAbsent(site.getId(), k -> DMSSiteDto.builder().build());
			siteDto.setId(site.getId());
			siteDto.setLabel(site.getLabel());
			siteDto.setDescription(site.getDescription());
			siteDto.setRadius(site.getRadius());
			siteDto.setLng(site.getLng());
			siteDto.setLat(site.getLat());
			siteDto.setRemark(site.getRemark());
			siteDto.setCreateDate(site.getCreateDate());
			siteDto.setModifyDate(site.getModifyDate());
		}
		
		
		Map<Long, Map<String, DMSLocationDto>> siteLocationKeyMap = new LinkedHashMap<>();
		Map<Long, Map<String, Map<Long, DMSLockDto>>> siteLocationKeyLockMap = new LinkedHashMap<>();
		
		for (Object[] obj : locationLockSites) {
			DMSLocationLock locationLock = (DMSLocationLock) obj[0];
			DMSLocationSite locationSite = (DMSLocationSite) obj[1];
			DMSLock lock = locationLock.getLock();

			Long siteId = locationSite.getSite().getId();
			String locationKey = locationSite.getLocationKey();
			
			// prepare location
			Map<String, DMSLocationDto> locationKeyMap = siteLocationKeyMap.computeIfAbsent(siteId, k -> new LinkedHashMap<>());
			DMSLocationDto locationDto = locationKeyMap.computeIfAbsent(locationKey, k -> DMSLocationDto.builder().build());
			
			locationDto.setBuilding(locationLock.getBuilding().getName());
			locationDto.setBlock(locationLock.getBlock().getName());
			locationDto.setLevel(locationLock.getFloorLevel().getName());
			locationDto.setUnitNumber(locationLock.getBuildingUnit().getName());
			
			locationDto.setCountry(locationLock.getBuilding().getAddress().getCountry());
			locationDto.setCity(locationLock.getBuilding().getAddress().getCity());
			locationDto.setTown(locationLock.getBuilding().getAddress().getTown());
			locationDto.setStreet(locationLock.getBuilding().getAddress().getStreet());
			locationDto.setDisplayName(locationLock.getBuilding().getAddress().getDisplayName());
			locationDto.setPostalCode(locationLock.getBuilding().getAddress().getPostalCode());

			// prepare lock
			Map<String, Map<Long, DMSLockDto>> locationKeyLockMap = siteLocationKeyLockMap.computeIfAbsent(siteId, k -> new LinkedHashMap<>());
			Map<Long, DMSLockDto> lockMap = locationKeyLockMap.computeIfAbsent(locationKey, k -> new LinkedHashMap<>());
			DMSLockDto lockDto = lockMap.computeIfAbsent(lock.getId(), k -> DMSLockDto.build(lock));
			lockDto.setHomeAddress(Utils.formatHomeAddress(locationLock));
			
			lockDto.setSiteLabel(locationSite.getSite().getLabel());
			lockDto.setSiteId(siteId);
			
			if (locationLock.getBuilding() != null) {
				lockDto.setBuilding(BuildingDto.builder().id(locationLock.getBuilding().getId())
						.name(locationLock.getBuilding().getName()).build());
			}
			if (locationLock.getBlock() != null) {
				lockDto.setBlock(BlockDto.builder().id(locationLock.getBlock().getId())
						.name(locationLock.getBlock().getName()).build());
			}
			if (locationLock.getFloorLevel() != null) {
				lockDto.setFloorLevel(FloorLevelDto.builder().id(locationLock.getFloorLevel().getId())
						.name(locationLock.getFloorLevel().getName()).build());
			}
			if (locationLock.getBuildingUnit() != null) {
				lockDto.setBuildingUnit(BuildingUnitDto.builder().id(locationLock.getBuildingUnit().getId())
						.name(locationLock.getBuildingUnit().getName()).build());
			}
		}
		
		Set<String> checkLockKey = new LinkedHashSet<>();
		
		// map location to site
		for (Entry<Long, DMSSiteDto> en : siteMap.entrySet()) {
			DMSSiteDto siteDto = en.getValue();
			rs.getSites().add(siteDto);
			
			Map<String, DMSLocationDto> locationKeyMap = siteLocationKeyMap.computeIfAbsent(en.getKey(), k -> new LinkedHashMap<>());
			for (Entry<String, DMSLocationDto> enLocation : locationKeyMap.entrySet()) {
				siteDto.getLocations().add(enLocation.getValue());
				
				// map lock to location
				DMSLocationDto locationDto = enLocation.getValue();
				Map<String, Map<Long, DMSLockDto>> locationKeyLockMap = siteLocationKeyLockMap.computeIfAbsent(siteDto.getId(), k -> new LinkedHashMap<>());
				Map<Long, DMSLockDto> lockMap = locationKeyLockMap.computeIfAbsent(enLocation.getKey(), k -> new LinkedHashMap<>());
				for (Entry<Long, DMSLockDto> lockEn : lockMap.entrySet()) {
					
					DMSLockDto lockDto = lockEn.getValue();
					locationDto.getLocks().add(lockDto);
					
					if (!checkLockKey.contains(lockDto.getId() + "-|-" + lockDto.getSiteLabel())) {
						checkLockKey.add(lockDto.getId() + "-|-" + lockDto.getSiteLabel());
						rs.getLocks().add(lockDto);
					}
				}
			}
		}
		
		if (lockOnly == Boolean.TRUE) {
			rs.setSites(null);
		} else {
			rs.setLocks(null);
		}
		
		return rs;
	}
	
	boolean isMatch(DMSLock lock, DMSWorkOrders workOrder) {
		Calendar c = Calendar.getInstance(TimeZoneHolder.get());
		LOGGER.info("lockId = " + lock.getId() + "check time timePeriod " + workOrder.getName() + " at " + c.getTime() + " tz: " + TimeZoneHolder.get());
		
		// check dates
		long dateStart = workOrder.getTimePeriodDatesStart() == null ? 0 : workOrder.getTimePeriodDatesStart();
		long dateEnd = workOrder.getTimePeriodDatesEnd() == null ? Long.MAX_VALUE : workOrder.getTimePeriodDatesEnd();
		if (!workOrder.isTimePeriodDatesIsAlways() && !(c.getTimeInMillis() >= dateStart && c.getTimeInMillis() <= dateEnd)) {
			LOGGER.info("lockId = " + lock.getId() + " workOrder " + workOrder.getName() + " not match dates at " + c.getTime() + " tz: " + TimeZoneHolder.get());
			return false;
		}
		
		// check day in week
		int dayOfWeek = c.get(Calendar.DAY_OF_WEEK);
		if (!workOrder.isTimePeriodDayInWeeksIsAlways()
				&& !(
						dayOfWeek == 1 && workOrder.isTimePeriodDayInWeeksIsSun() ||
						dayOfWeek == 2 && workOrder.isTimePeriodDayInWeeksIsMon() ||
						dayOfWeek == 3 && workOrder.isTimePeriodDayInWeeksIsTue() ||
						dayOfWeek == 4 && workOrder.isTimePeriodDayInWeeksIsWed() ||
						dayOfWeek == 5 && workOrder.isTimePeriodDayInWeeksIsThu() ||
						dayOfWeek == 6 && workOrder.isTimePeriodDayInWeeksIsFri() ||
						dayOfWeek == 7 && workOrder.isTimePeriodDayInWeeksIsSat()
				)
		) {
			LOGGER.info("lockId = " + lock.getId() + " workOrder " + workOrder.getName() + " not match day of week at dayOfWeek = " + dayOfWeek + " tz: " + TimeZoneHolder.get());
			return false;
		}
		
		// check time in day
		int h = c.get(Calendar.HOUR_OF_DAY);
		int m = c.get(Calendar.MINUTE);
		if (!workOrder.isTimePeriodTimeInDayIsAlways()) {
			
			long hStart = workOrder.getTimePeriodTimeInDayHourStart() == null ? 0 : workOrder.getTimePeriodTimeInDayHourStart();
			long mStart = workOrder.getTimePeriodTimeInDayMinuteStart() == null ? 0 : workOrder.getTimePeriodTimeInDayMinuteStart();

			long hEnd = workOrder.getTimePeriodTimeInDayHourEnd() == null ? 23 : workOrder.getTimePeriodTimeInDayHourEnd();
			long mEnd = workOrder.getTimePeriodTimeInDayMinuteEnd() == null ? 59 : workOrder.getTimePeriodTimeInDayMinuteEnd();
			
			if (h < hStart || h == hStart && m < mStart) {
				LOGGER.info("lockId = " + lock.getId() + " workOrder " + workOrder.getName() + " not match time in day start at hh:mm = " + h + ":" + m + " tz: " + TimeZoneHolder.get());
				return false;				
			}
			
			if (h > hEnd || h == hEnd && m > mEnd) {
				LOGGER.info(" workOrder " + workOrder.getName() + " not match time in day end at hh:mm = " + h + ":" + m + " tz: " + TimeZoneHolder.get());
				return false;				
			}
		}
		
		LOGGER.info("lockId = " + lock.getId() + " workOrder " + workOrder.getName() + " is full match at dayOfWeek=" + dayOfWeek + " hh:mm = " + h + ":" + m + " tz: " + TimeZoneHolder.get());
		return true;
	}

	@Transactional
	@Override
	public void saveLog(SaveLogReq dto) {
		
		if (!dmsLockRepository.findByLockBid(dto.getBid()).isPresent()) {
			throw new RuntimeException("Lock not found!");
		}
		DMSLockEventLog entity = DMSLockEventLog.from(dto);
		entity.setCreatedBy(SecurityUtils.getEmail());
		dmsLockEventLogRepository.save(entity);
	}
}
