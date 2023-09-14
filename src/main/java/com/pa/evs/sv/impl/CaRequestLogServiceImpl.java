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

import javax.annotation.PostConstruct;
import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import com.pa.evs.LocalMapStorage;
import com.pa.evs.constant.Message;
import com.pa.evs.dto.CaRequestLogDto;
import com.pa.evs.dto.PaginDto;
import com.pa.evs.dto.ResponseDto;
import com.pa.evs.dto.ScreenMonitoringDto;
import com.pa.evs.enums.DeviceStatus;
import com.pa.evs.enums.DeviceType;
import com.pa.evs.enums.ScreenMonitorKey;
import com.pa.evs.enums.ScreenMonitorStatus;
import com.pa.evs.model.Address;
import com.pa.evs.model.AddressLog;
import com.pa.evs.model.BuildingUnit;
import com.pa.evs.model.CARequestLog;
import com.pa.evs.model.FloorLevel;
import com.pa.evs.model.Group;
import com.pa.evs.model.ScreenMonitoring;
import com.pa.evs.model.Users;
import com.pa.evs.model.Vendor;
import com.pa.evs.repository.AddressLogRepository;
import com.pa.evs.repository.AddressRepository;
import com.pa.evs.repository.BuildingRepository;
import com.pa.evs.repository.BuildingUnitRepository;
import com.pa.evs.repository.CARequestLogRepository;
import com.pa.evs.repository.FloorLevelRepository;
import com.pa.evs.repository.GroupRepository;
import com.pa.evs.repository.LogRepository;
import com.pa.evs.repository.ScreenMonitoringRepository;
import com.pa.evs.repository.UserRepository;
import com.pa.evs.repository.VendorRepository;
import com.pa.evs.security.user.JwtUser;
import com.pa.evs.sv.AuthenticationService;
import com.pa.evs.sv.CaRequestLogService;
import com.pa.evs.utils.CsvUtils;
import com.pa.evs.utils.Mqtt;
import com.pa.evs.utils.SecurityUtils;
import com.pa.evs.utils.Utils;

@Component
@SuppressWarnings("unchecked")
public class CaRequestLogServiceImpl implements CaRequestLogService {
	
	private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(EVSPAServiceImpl.class);
	
	@Value("${evs.pa.mqtt.address}") private String evsPAMQTTAddress;

	@Value("${evs.pa.mqtt.client.id}") private String mqttClientId;
	
	@Autowired
	private CARequestLogRepository caRequestLogRepository;

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
	FloorLevelRepository floorLevelRepository;
	
	@Autowired
	BuildingUnitRepository buildingUnitRepository;

	@Autowired
	LogRepository logRepository;
	
	@Autowired
	private VendorRepository vendorRepository;
	
	@Autowired
	EntityManager em;
	
	@Autowired LocalMapStorage localMap;

	@Autowired
	private ScreenMonitoringRepository screenMonitoringRepository;
	
	@Autowired
	private AddressLogRepository addressLogRepository;

    private List<String> cacheCids = Collections.EMPTY_LIST;

	@PostConstruct
    public void init() {
        LOG.debug("Loading CID into cache");
        cacheCids = caRequestLogRepository.getCids();
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
    public void save(CaRequestLogDto dto) throws Exception {
        CARequestLog ca = null;
        Calendar c = Calendar.getInstance();
        if (StringUtils.isNotBlank(dto.getMsn())) {
        	dto.setMsn(dto.getMsn().trim());
        }
        if (dto.getId() != null) {
            Optional<CARequestLog> opt = caRequestLogRepository.findById(dto.getId());
            if (opt.isPresent()) {
                ca = opt.get();
                
                if (StringUtils.isNotBlank(dto.getMsn()) && !dto.getMsn().equalsIgnoreCase(ca.getMsn()) && BooleanUtils.isTrue(caRequestLogRepository.existsByMsn(dto.getMsn()))) {
                    throw new Exception(Message.MSN_WAS_ASSIGNED);
                }
                ca.setModifyDate(c.getTime());
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
			AddressLog addrLog = AddressLog.build(ca);
			addrLog.setType(DeviceType.NOT_COUPLED);
			addressLogRepository.save(addrLog);
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
        
        if (isCoupledAddress) {

        	// Add new log to address_log that this new address is linked to this device
    		AddressLog addrLog = AddressLog.build(ca);
    		addrLog.setType(DeviceType.COUPLED);
    		addressLogRepository.save(addrLog);
        }
        
        updateCacheUidMsnDevice(ca.getUid(), "update");
    }

    @Override
    @Transactional(readOnly = true)
    public PaginDto<CARequestLog> search(PaginDto<CARequestLog> pagin) {
        StringBuilder sqlBuilder = new StringBuilder("FROM CARequestLog ca ");
        StringBuilder sqlCountBuilder = new StringBuilder("SELECT count(*) FROM CARequestLog ca");
        
        StringBuilder sqlCommonBuilder = new StringBuilder();
        if (CollectionUtils.isEmpty(pagin.getOptions()) || (
                pagin.getOptions().size() == 1 
                && BooleanUtils.isTrue(BooleanUtils.toBoolean((String) pagin.getOptions().get("queryAllDate")))
            )) {
            sqlCommonBuilder.append(" WHERE 1=1 ");
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
                    sqlCommonBuilder.append(" EXTRACT(EPOCH FROM createDate) * 1000 >= " + fromDate + "AND");
                }
                if (fromDate == null && toDate != null) {
                    sqlCommonBuilder.append(" EXTRACT(EPOCH FROM createDate) * 1000 <= " + toDate + "AND");
                }
                if (fromDate != null && toDate != null) {
                    sqlCommonBuilder.append(" ( EXTRACT(EPOCH FROM createDate) * 1000 >= " + fromDate);
                    sqlCommonBuilder.append(" AND EXTRACT(EPOCH FROM createDate) * 1000 <= " + toDate + ") AND ");
                }
            } else {
                sqlCommonBuilder.append(" ( ");
                if (BooleanUtils.isTrue(enrollmentDate)) {
                    if (fromDate != null && toDate == null) {
                        sqlCommonBuilder.append(" enrollmentDatetime >= " + fromDate + "OR");
                    }
                    if (fromDate == null && toDate != null) {
                        sqlCommonBuilder.append(" enrollmentDatetime <= " + toDate + "OR");
                    }
                    if (fromDate != null && toDate != null) {
                        sqlCommonBuilder.append(" ( enrollmentDatetime >= " + fromDate);
                        sqlCommonBuilder.append(" AND enrollmentDatetime <= " + toDate + ") OR");
                    }
                }
                if (BooleanUtils.isTrue(coupledDate)) {
                    if (fromDate != null && toDate == null) {
                        sqlCommonBuilder.append(" coupledDatetime >= " + fromDate + "OR");
                    }
                    if (fromDate == null && toDate != null) {
                        sqlCommonBuilder.append(" coupledDatetime <= " + toDate + "OR");
                    }
                    if (fromDate != null && toDate != null) {
                        sqlCommonBuilder.append(" ( coupledDatetime >= " + fromDate);
                        sqlCommonBuilder.append(" AND coupledDatetime <= " + toDate + ") OR");
                    }          
                                }
                if (BooleanUtils.isTrue(activationDate)) {
                    if (fromDate != null && toDate == null) {
                        sqlCommonBuilder.append(" activationDate >= " + fromDate + "OR");
                    }
                    if (fromDate == null && toDate != null) {
                        sqlCommonBuilder.append(" activationDate <= " + toDate + "OR");
                    }
                    if (fromDate != null && toDate != null) {
                        sqlCommonBuilder.append(" ( activationDate >= " + fromDate);
                        sqlCommonBuilder.append(" AND activationDate <= " + toDate + ") OR");
                    }
                }
                if (BooleanUtils.isTrue(deactivationDate)) {
                    if (fromDate != null && toDate == null) {
                        sqlCommonBuilder.append(" deactivationDate >= " + fromDate + "OR");
                    }
                    if (fromDate == null && toDate != null) {
                        sqlCommonBuilder.append(" deactivationDate <= " + toDate + "OR");
                    }
                    if (fromDate != null && toDate != null) {
                        sqlCommonBuilder.append(" ( deactivationDate >= " + fromDate);
                        sqlCommonBuilder.append(" AND deactivationDate <= " + toDate + ") OR");
                    }
                }
                if (BooleanUtils.isTrue(onboardingDate)) {
                    if (fromDate != null && toDate == null) {
                        sqlCommonBuilder.append(" lastOBRDate >= " + fromDate + "OR");
                    }
                    if (fromDate == null && toDate != null) {
                        sqlCommonBuilder.append(" lastOBRDate <= " + toDate + "OR");
                    }
                    if (fromDate != null && toDate != null) {
                        sqlCommonBuilder.append(" ( lastOBRDate >= " + fromDate);
                        sqlCommonBuilder.append(" AND lastOBRDate <= " + toDate + ") OR");
                    }
                }
                
                sqlCommonBuilder.delete(sqlCommonBuilder.length() - 2, sqlCommonBuilder.length());
                if (sqlCommonBuilder.length() >= 30) {
                    sqlCommonBuilder.append(" ) AND ");
                }
            }
            
            if (StringUtils.isNotBlank(querySn)) {
                sqlCommonBuilder.append(" upper(sn) like '%" + querySn.toUpperCase() + "%' AND ");
            }
            if (StringUtils.isNotBlank(queryMsn)) {
                sqlCommonBuilder.append(" msn like '%" + queryMsn + "%' AND ");
            }
            if (StringUtils.isNotBlank(querySnOrCid)) {
                sqlCommonBuilder.append(" (lower(sn) like '%" + querySnOrCid.toLowerCase().trim() + "%' or lower(cid) like '%" + querySnOrCid.toLowerCase().trim() + "%') AND ");
            }
                
            if (StringUtils.isNotBlank(status)) {
                sqlCommonBuilder.append(" status = '" + status + "' AND ");
            }
            
            if (StringUtils.isNotBlank(type)) {
                sqlCommonBuilder.append(" type = '" + type + "' AND ");
            }
            
            if (!CollectionUtils.isEmpty(cids)) {
                sqlCommonBuilder.append(" (cid = '" + cids.get(0) + "'");
                for (int i = 1; i < cids.size(); i++) {
                    sqlCommonBuilder.append(" OR cid = '" + cids.get(i) + "'");
                }
                sqlCommonBuilder.append(" ) AND ");
            }
            if (StringUtils.isNotBlank(queryUuid)) {
                sqlCommonBuilder.append(" upper(uid) like '%" + queryUuid.toUpperCase() + "%' AND ");
            }
            if (StringUtils.isNotBlank(queryEsimId)) {
                sqlCommonBuilder.append(" upper(cid) like '%" + queryEsimId.toUpperCase() + "%' AND ");
            }
            if (queryGroup != null) {
                sqlCommonBuilder.append(" group = " + queryGroup + " AND ");
            }
            if (StringUtils.isNotBlank(queryBuilding)) {
                sqlCommonBuilder.append(" building.id= '" + queryBuilding + "' AND ");
            }
            if (StringUtils.isNotBlank(queryBlock)) {
                sqlCommonBuilder.append(" block.id= '" + queryBlock + "' AND ");
            }
            if (StringUtils.isNotBlank(queryFloorLevel)) {
                sqlCommonBuilder.append(" floorLevel.id= '" + queryFloorLevel + "' AND ");
            }
            if (StringUtils.isNotBlank(queryBuildingUnit)) {
                sqlCommonBuilder.append(" buildingUnit.id= '" + queryBuildingUnit + "' AND ");
            }
            if (StringUtils.isNotBlank(queryPostalCode)) {
                sqlCommonBuilder.append(" ((exists (select 1 from Building bd where bd.id = ca.building.id and upper(bd.address.postalCode) = '" + queryPostalCode.toUpperCase() + "') ");
                sqlCommonBuilder.append(" or (exists (select 1 FROM Address add1 where add1.id = ca.address.id and upper(add1.postalCode) = '" + queryPostalCode.toUpperCase() + "') ))) AND ");
            }
            if (queryVendor != null) {
                sqlCommonBuilder.append(" vendor.id = " + queryVendor + " AND ");
            }
            
            sqlCommonBuilder.append(" sn is not null and sn <> ''  AND ");
            
            sqlCommonBuilder.delete(sqlCommonBuilder.length() - 4, sqlCommonBuilder.length());
        }
        
        if (Boolean.parseBoolean(pagin.getOptions().get("cidIsNotNull") + "")) {
//            if (StringUtils.isNotBlank((String) pagin.getOptions().get("querySnOrCid"))) {
//                sqlCommonBuilder.append(" AND ");
//            }
            sqlCommonBuilder.append(" AND cid is not null AND cid <> '' ");
        }
        
        if (sqlCommonBuilder.length() < 10) {
            sqlCommonBuilder.append(" 1 = 1 ");
        }
        
        sqlBuilder.append(sqlCommonBuilder).append(" ORDER BY id asc");
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
            return pagin;
        }
        
        Query query = em.createQuery(sqlBuilder.toString());
        query.setFirstResult(pagin.getOffset());
        query.setMaxResults(pagin.getLimit());
        
        List<CARequestLog> list = query.getResultList();
        list.forEach(ca -> {
        	ca.getVendor();
        	ca.setHomeAddress(Utils.formatHomeAddress(ca));
        });
        
        pagin.setResults(list);
        getRLSLog(list);
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
        			updateCacheUidMsnDevice(ca.getUid(), "update");
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
	public void removeDevice(String uId) {
		CARequestLog caRequestLog = caRequestLogRepository.findByUid(uId).orElse(null);
		if (caRequestLog != null && caRequestLog.getType() == DeviceType.NOT_COUPLED) {//8931070521315025237F
			caRequestLogRepository.delete(caRequestLog);
			updateCacheUidMsnDevice(caRequestLog.getUid(), "remove");
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
			updateCacheUidMsnDevice(caRequestLog.getUid(), "update");
		} else {
			throw new RuntimeException("Device doesn't exists!");
		}
	}
}
