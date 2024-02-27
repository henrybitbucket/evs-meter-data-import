package com.pa.evs.sv.impl;

import java.math.BigDecimal;
import java.util.ArrayList;
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

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
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
import com.pa.evs.model.DMSBlock;
import com.pa.evs.model.DMSBuilding;
import com.pa.evs.model.DMSBuildingUnit;
import com.pa.evs.model.DMSFloorLevel;
import com.pa.evs.model.DMSLocationLock;
import com.pa.evs.model.DMSLocationSite;
import com.pa.evs.model.DMSLock;
import com.pa.evs.model.DMSLockVendor;
import com.pa.evs.model.DMSSite;
import com.pa.evs.repository.DMSBlockRepository;
import com.pa.evs.repository.DMSBuildingRepository;
import com.pa.evs.repository.DMSBuildingUnitRepository;
import com.pa.evs.repository.DMSFloorLevelRepository;
import com.pa.evs.repository.DMSLocationLockRepository;
import com.pa.evs.repository.DMSLockRepository;
import com.pa.evs.repository.DMSLockVendorRepository;
import com.pa.evs.repository.GroupUserRepository;
import com.pa.evs.sv.DMSLockService;
import com.pa.evs.utils.ApiUtils;
import com.pa.evs.utils.AppProps;
import com.pa.evs.utils.DESUtil;
import com.pa.evs.utils.SchedulerHelper;
import com.pa.evs.utils.Utils;

@Service
@SuppressWarnings("unchecked")
public class DMSLockServiceImpl implements DMSLockService {

	static final Logger LOGGER = LoggerFactory.getLogger(DMSLockServiceImpl.class);
	
	static final ObjectMapper MAPPER = new ObjectMapper();
	String token;
	RestTemplate resttemplate = ApiUtils.getRestTemplate();
	
	@Autowired
	DMSLockRepository dmsLockRepository;
	
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
	
	@Transactional(readOnly = true)
	@Override
	public PaginDto<DMSLockDto> search(PaginDto<DMSLockDto> pagin) {
		LOGGER.info("Get DMS LOCK");
		
		StringBuilder sqlBuilder = new StringBuilder(" SELECT lock, locationLock ");
		StringBuilder sqlCountBuilder = new StringBuilder("SELECT count(lock) ");

		StringBuilder sqlCommonBuilder = new StringBuilder(" FROM DMSLock lock LEFT JOIN DMSLocationLock locationLock on lock.id = locationLock.lock.id ");
		sqlCommonBuilder.append(" WHERE 1=1 ");
		
		Map<String, Object> options = pagin.getOptions();
        Long queryVendor = StringUtils.isNotBlank((String) options.get("queryVendor")) ? Long.parseLong((String) options.get("queryVendor")) : null;
		
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
                if (dataObject.containsKey("arealocks")) {
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
		try {
			loginPAS();
		} finally {
			SchedulerHelper.scheduleJob("0 0/3 * * * ? *", () -> {
				loginPAS();
			}, "APP_PAS_LOGIN");
		}
		
		// sync on init
		syncLock(null);
	}
	
	void loginPAS() {
		try {
			HttpEntity<Object> entity = new HttpEntity<Object>(null);
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
	public Object getAssignedLocks(String email, Boolean lockOnly) {
		
		List<Long> groupIdOfUsers = em.createQuery(" SELECT groupUser.id FROM UserGroup where user.email = :email and groupUser.appCode.name = 'DMS' " )
				.setParameter("email", email)
				.getResultList();
		
		List<DMSSite> sitesOfUser = em.createQuery(" SELECT wod.site FROM DMSWorkOrders wod where wod.group.id in (:groupIds) order by wod.site.id desc " )
		.setParameter("groupIds", groupIdOfUsers)
		.getResultList();
		
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
		
		DMSLocationSiteLockDto rs = DMSLocationSiteLockDto.builder().build();
		Set<String> checkLockKey = new LinkedHashSet<>();
		
		// map location to site
		for (Entry<Long, DMSSiteDto> en : siteMap.entrySet()) {
			DMSSiteDto siteDto = en.getValue();
			rs.getSites().add(siteDto);
			
			Map<String, DMSLocationDto> locationKeyMap = siteLocationKeyMap.get(en.getKey());
			for (Entry<String, DMSLocationDto> enLocation : locationKeyMap.entrySet()) {
				siteDto.getLocations().add(enLocation.getValue());
				
				// map lock to location
				DMSLocationDto locationDto = enLocation.getValue();
				Map<String, Map<Long, DMSLockDto>> locationKeyLockMap = siteLocationKeyLockMap.get(siteDto.getId());
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
}
