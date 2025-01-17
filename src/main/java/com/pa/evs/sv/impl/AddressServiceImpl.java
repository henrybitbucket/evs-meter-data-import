package com.pa.evs.sv.impl;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.ibm.icu.math.BigDecimal;
import com.pa.evs.constant.Message;
import com.pa.evs.dto.AddressDto;
import com.pa.evs.enums.DeviceType;
import com.pa.evs.model.Address;
import com.pa.evs.model.AddressLog;
import com.pa.evs.model.Block;
import com.pa.evs.model.Building;
import com.pa.evs.model.BuildingUnit;
import com.pa.evs.model.CARequestLog;
import com.pa.evs.model.FloorLevel;
import com.pa.evs.model.MMSMeter;
import com.pa.evs.repository.AddressLogRepository;
import com.pa.evs.repository.AddressRepository;
import com.pa.evs.repository.BlockRepository;
import com.pa.evs.repository.BuildingRepository;
import com.pa.evs.repository.BuildingUnitRepository;
import com.pa.evs.repository.CARequestLogRepository;
import com.pa.evs.repository.FloorLevelRepository;
import com.pa.evs.repository.MMSMeterRepository;
import com.pa.evs.sv.AddressService;
import com.pa.evs.utils.AppProps;
import com.pa.evs.utils.SecurityUtils;
import com.pa.evs.utils.Utils;

@Service
public class AddressServiceImpl implements AddressService {

	@Autowired
	AddressRepository addressRepository;
	
	@Autowired
	CARequestLogRepository caRequestLogRepository;
	
	@Autowired
	MMSMeterRepository mmsMeterRepository;
	
	@Autowired
	BuildingRepository buildingRepository;
	
	@Autowired
	BlockRepository blockRepository;
	
	@Autowired
	FloorLevelRepository floorLevelRepository;
	
	@Autowired
	BuildingUnitRepository buildingUnitRepository;
	
	@Autowired
	AddressLogRepository addressLogRepository;

	@Override
	@Transactional
	public List<AddressDto> handleUpload(MultipartFile file, String importType) throws IOException {

		List<AddressDto> dtos = parseCsv(file.getInputStream(), "address");
		// updateNullBlock();
		Map<String, AddressDto> mapA = new LinkedHashMap<>();
		Map<String, CARequestLog> msnCA = new LinkedHashMap<>();
		dtos.forEach(a -> {
			 mapA.put((a.getPostalCode() + "__" + a.getCity() + "__" + a.getBuilding()).toUpperCase(), a);
			 if (StringUtils.isNotBlank(a.getCoupleMsn())) {
				 msnCA.put(a.getCoupleMsn(), null);	 
			 }
		});
		List<Building> buildings = buildingRepository.findAllByPostalCodeAndCityAndName(mapA.keySet());
		caRequestLogRepository.findByMsnIn(msnCA.keySet())
		.forEach(ca -> msnCA.put(ca.getMsn(), ca));
		
		Map<String, Address> mapAE = new LinkedHashMap<>();
		
		buildings.forEach(b -> {
			Address e = b.getAddress();
			mapAE.put((e.getPostalCode() + "__" + e.getCity() + "__" + b.getName()).trim().replaceAll(" *__ *", "__"), e);
		});
		dtos.forEach(a -> {
			if (StringUtils.isNotBlank(a.getMessage())) {
				// ignore
				return;
			}
			String combineKey = (a.getPostalCode() + "__" + a.getCity() + "__" + a.getBuilding()).trim().replaceAll(" *__ *", "__");
			Address add = mapAE.computeIfAbsent(combineKey, st -> new Address());
			
			add.setStreet(StringUtils.isBlank(a.getStreet()) ? "-" : a.getStreet());
			add.setCity(a.getCity());
			add.setUnitNumber("");
			add.setPostalCode(a.getPostalCode());
			add.setRemark(a.getRemark());
			add.setModifyDate(new Date());
			addressRepository.save(add);
			
			Building building = null;
			Block block = null;
			FloorLevel floor = null;
			BuildingUnit buildingUnit = null;
			
			String bd = a.getBuilding();
			String bl = a.getBlock();
			String lvl = a.getLevel();
			String unit = a.getUnitNumber();

			if (StringUtils.isNotBlank(lvl) || StringUtils.isNotBlank(bl)) {
				building = add.getId() == null ? new Building() : buildingRepository.findByAddressId(add.getId()).orElse(new Building());
				if (building.getId() == null) {
					building.setName(StringUtils.isBlank(bd) ? add.getStreet() : bd);
				}

				building.setAddress(add);
				buildingRepository.save(building);
				buildingRepository.flush();
				
				if (StringUtils.isNotBlank(bl)) {
					block = building.getId() == null ? new Block() : blockRepository.findByBuildingIdAndName(building.getId(), bl).orElse(new Block());
					block.setName(bl);
					block.setBuilding(building);
					add.setBlock(bl);
					add.setLevel(lvl);
					blockRepository.save(block);
					blockRepository.flush();
				}
				
				if (StringUtils.isNotBlank(lvl)) {
					if (building.getId() != null && block != null && block.getId() != null) {
						floor = floorLevelRepository.findByBuildingIdAndBlockIdAndName(building.getId(), block.getId(), lvl).orElse(new FloorLevel());
					} else if (building.getId() != null && (block == null || block.getId() == null)) {
						floor = floorLevelRepository.findByBuildingIdAndName(building.getId(), lvl).orElse(new FloorLevel());
					} else {
						floor = new FloorLevel();
					}
					floor.setBuilding(building);
					floor.setBlock(block);
					floor.setName(lvl);
					floorLevelRepository.save(floor);
					floorLevelRepository.flush();
				}
				
			}

			if (StringUtils.isNotBlank(unit) && floor != null) {
				buildingUnit = floor.getId() == null ? new BuildingUnit() : buildingUnitRepository.findByFloorLevelIdAndName(floor.getId(), unit).orElse(new BuildingUnit());
				buildingUnit.setName(unit);
				buildingUnit.setFloorLevel(floor);
				buildingUnit.setRemark(a.getRemark());
				buildingUnitRepository.save(buildingUnit);
				buildingUnitRepository.flush();
			}
			
			if (building != null) {
				building.setFullText1(building);
				String str1 = building.getFullText();
				if (StringUtils.isNotBlank(str1)) {
					String str2 = building.getName() 
							+ '-' + building.getAddress().getBlock()
							+ '-' + building.getAddress().getLevel()
							+ '-' + building.getAddress().getUnitNumber()
							+ '-' + building.getAddress().getStreet() 
							+ '-' + building.getAddress().getPostalCode() 
							+ '-' + building.getAddress().getCity();
					List<String> list1 = Arrays.asList(str1.split("-"));
					List<String> list2 = Arrays.asList(str2.toLowerCase().split("-"));
					List<String> unique = list2.stream().filter(l2 -> list1.indexOf(l2) == -1).collect(Collectors.toList());
					unique.forEach(uni -> {
						list1.remove(uni);
					});

					building.setFullText(Stream.concat(list1.stream(), unique.stream()).map(blo -> blo).collect(Collectors.joining("-")));
				} else {
					building.setFullText1(building);
				}
				if (buildingUnit != null) {
					buildingUnit.setFullText1(buildingUnit);
					buildingUnitRepository.save(buildingUnit);
				}
				buildingRepository.save(building);
			}

			if ("couple-with-msn".equalsIgnoreCase(importType)) {
				String msn = a.getCoupleMsn();
				if (StringUtils.isNotBlank(a.getCoupleMsn())) {

					CARequestLog caUnitCoupled = caRequestLogRepository.findByBuildingUnitId(buildingUnit.getId()).orElse(null);
					
					if (caUnitCoupled != null && "de-couple".equalsIgnoreCase(msn)) {
						
						// Add new log to address_log that this address is unlinked to this device
						AddressLog addrLog = AddressLog.build(caUnitCoupled);
						addrLog.setType(DeviceType.NOT_COUPLED);
						addrLog.setUpdatedBy(SecurityUtils.getUsername());
						addressLogRepository.save(addrLog);
						
						caUnitCoupled.setBuildingUnit(null);
						caUnitCoupled.setFloorLevel(null);
						caUnitCoupled.setBlock(null);
						caUnitCoupled.setBuilding(null);
						caUnitCoupled.setAddress(null);
						caRequestLogRepository.save(caUnitCoupled);
						return;
					}
					
					if (caUnitCoupled != null && caUnitCoupled.getBuildingUnit() != null) {
						a.setMessage(Message.ADDRESS_IS_ASSIGNED);
						return;
					}
					
					CARequestLog ca = msnCA.get(a.getCoupleMsn());
					if (ca == null) {
						a.setMessage("Meter SN doesn't exists!");
						return;
					}
					
					if (StringUtils.isNotBlank(a.getCoupleSn()) && !ca.getSn().equalsIgnoreCase(a.getCoupleSn())) {
						Optional<CARequestLog> caBySnOpt = caRequestLogRepository.findBySn(a.getCoupleSn());
						if (!caBySnOpt.isPresent()) {
							a.setMessage("MCU SN doesn't exists!");
							return;
						}
				    	
				    	CARequestLog caBySn = caBySnOpt.get();
				    	
				    	if (caBySn.getBuildingUnit() != null) {
				    		// Add new log to address_log that unlinked exists address to this device
							AddressLog addrLog = AddressLog.build(caBySn);
							addrLog.setType(DeviceType.NOT_COUPLED);
							addressLogRepository.save(addrLog);
				    	}
				    	
				    	caBySn.setBuilding(building);
				    	caBySn.setFloorLevel(floor);
				    	caBySn.setBlock(block);
				    	
				    	if (caBySn.getBuildingUnit() == null || caBySn.getBuildingUnit().getId().longValue() != buildingUnit.getId().longValue()) {
				    		caBySn.setBuildingUnit(buildingUnit);
							buildingUnit.setCoupledDate(new Date());
						}
						
						a.setCoupleTime(buildingUnit.getCoupledDate() == null ? buildingUnit.getCreateDate() : buildingUnit.getCoupledDate());
						buildingUnitRepository.save(buildingUnit);
						caBySn.setBuildingUnit(buildingUnit);
				    	caRequestLogRepository.save(caBySn);
				    	
				    	// Add new log to address_log that linked new address to this device
						AddressLog newAddrLog = AddressLog.build(caBySn);
						newAddrLog.setType(DeviceType.COUPLED);
						addressLogRepository.save(newAddrLog);
						
				    	return;
					}
					
					if (ca.getBuildingUnit() != null) {
			    		// Add new log to address_log that unlinked exists address to this device
						AddressLog addrLog = AddressLog.build(ca);
						addrLog.setType(DeviceType.NOT_COUPLED);
						addressLogRepository.save(addrLog);
			    	}

					ca.setBuilding(building);
					ca.setBlock(block);
					ca.setFloorLevel(floor);
					if (ca.getBuildingUnit() == null || ca.getBuildingUnit().getId().longValue() != buildingUnit.getId().longValue()) {
						ca.setBuildingUnit(buildingUnit);
						buildingUnit.setCoupledDate(new Date());
					}
					
					a.setCoupleTime(buildingUnit.getCoupledDate() == null ? buildingUnit.getCreateDate() : buildingUnit.getCoupledDate());
					buildingUnitRepository.save(buildingUnit);
					buildingUnitRepository.flush();
					ca.setBuildingUnit(buildingUnit);
					caRequestLogRepository.save(ca);
					
					// copy address to coupled Meter
					AppProps.getContext().getBean(CaRequestLogServiceImpl.class).updateMMSMeter(ca, ca.getMsn());
					
					// Add new log to address_log that linked new address to this device
					AddressLog newAddrLog = AddressLog.build(ca);
					newAddrLog.setType(DeviceType.COUPLED);
					addressLogRepository.save(newAddrLog);
				} else {
					a.setMessage("Meter SN is required!");
				}
			}
		});
		
		return dtos;
	}
	
	@Override
	@Transactional
	public List<AddressDto> handleUploadMeter(MultipartFile file, String importType) throws IOException {

		List<AddressDto> dtos = parseCsv(file.getInputStream(), "meter");
		Map<String, AddressDto> mapA = new LinkedHashMap<>();
		Map<String, CARequestLog> msnCA = new LinkedHashMap<>();
		dtos.forEach(a -> {
			 mapA.put((a.getPostalCode() + "__" + a.getCity() + "__" + a.getBuilding()).toUpperCase(), a);
			 if (StringUtils.isNotBlank(a.getCoupleMsn())) {
				 msnCA.put(a.getCoupleMsn(), null);	 
			 }
		});
		List<Building> buildings = buildingRepository.findAllByPostalCodeAndCityAndName(mapA.keySet());
		caRequestLogRepository.findByMsnIn(msnCA.keySet())
		.forEach(ca -> msnCA.put(ca.getMsn(), ca));
		
		Map<String, Address> mapAE = new LinkedHashMap<>();
		
		buildings.forEach(b -> {
			Address e = b.getAddress();
			mapAE.put((e.getPostalCode() + "__" + e.getCity() + "__" + b.getName()).trim().replaceAll(" *__ *", "__"), e);
		});
		dtos.forEach(a -> {
			if (!"Decouple-address".equalsIgnoreCase(importType) && StringUtils.isNotBlank(a.getMessage())) {
				// ignore
				return;
			}
			
			if ("Decouple-address".equalsIgnoreCase(importType)) {
				MMSMeter meter = mmsMeterRepository.findByMsn(a.getImportMsn());
				if (meter == null) {
					a.setMessage("Meter doesn't exist!");
					return;
				}
				meter.setBuilding(null);
				meter.setBlock(null);
				meter.setFloorLevel(null);
				meter.setBuildingUnit(null);
				meter.setHomeAddress(null);
				
				CARequestLog ca = caRequestLogRepository.findByMsn(a.getImportMsn()).orElse(null);
				if (ca != null) {
					ca.setBuilding(null);
					ca.setBlock(null);
					ca.setFloorLevel(null);
					ca.setBuildingUnit(null);
					ca.setHomeAddress(null);
					caRequestLogRepository.save(ca);
					caRequestLogRepository.flush();
				}
				
				checkChangeAddress(meter);
				mmsMeterRepository.save(meter);
				mmsMeterRepository.flush();
				a.setMessage(null);
				return;
			}
			
			String combineKey = (a.getPostalCode() + "__" + a.getCity() + "__" + a.getBuilding()).trim().replaceAll(" *__ *", "__");
			Address add = mapAE.computeIfAbsent(combineKey, st -> new Address());
			
			if (add.getId() == null || "Override".equalsIgnoreCase(importType)) {
				add.setStreet(StringUtils.isBlank(a.getStreet()) ? "-" : a.getStreet());	
			}
			if (StringUtils.isNotBlank(a.getCity())) {
				add.setCity(a.getCity());	
			}
			if (StringUtils.isNotBlank(a.getPostalCode())) {
				add.setPostalCode(a.getPostalCode());	
			}
			if (StringUtils.isNotBlank(a.getRemark())) {
				add.setRemark(a.getRemark());	
			}
			add.setUnitNumber("");
			add.setModifyDate(new Date());
			if (!a.isSkipAddress()) {
				addressRepository.save(add);
			}
			
			Building building = null;
			Block block = null;
			FloorLevel floor = null;
			BuildingUnit buildingUnit = null;
			
			String bd = a.getBuilding();
			String bl = a.getBlock();
			String lvl = a.getLevel();
			String unit = a.getUnitNumber();

			if (!a.isSkipAddress() && (StringUtils.isNotBlank(lvl) || StringUtils.isNotBlank(bl))) {
				building = add.getId() == null ? new Building() : buildingRepository.findByAddressId(add.getId()).orElse(new Building());
				if (building.getId() == null) {
					building.setName(StringUtils.isBlank(bd) ? add.getStreet() : bd);
				}

				building.setAddress(add);
				buildingRepository.save(building);
				buildingRepository.flush();
				
				if (StringUtils.isNotBlank(bl)) {
					block = building.getId() == null ? new Block() : blockRepository.findByBuildingIdAndName(building.getId(), bl).orElse(new Block());
					block.setName(bl);
					block.setBuilding(building);
					add.setBlock(bl);
					add.setLevel(lvl);
					blockRepository.save(block);
					blockRepository.flush();
				}
				
				if (StringUtils.isNotBlank(lvl)) {
					if (building.getId() != null && block != null && block.getId() != null) {
						floor = floorLevelRepository.findByBuildingIdAndBlockIdAndName(building.getId(), block.getId(), lvl).orElse(new FloorLevel());
					} else if (building.getId() != null && (block == null || block.getId() == null)) {
						floor = floorLevelRepository.findByBuildingIdAndName(building.getId(), lvl).orElse(new FloorLevel());
					} else {
						floor = new FloorLevel();
					}
					floor.setBuilding(building);
					floor.setBlock(block);
					floor.setName(lvl);
					floorLevelRepository.save(floor);
					floorLevelRepository.flush();
				}
				
			}

			if (StringUtils.isNotBlank(unit) && floor != null) {
				buildingUnit = floor.getId() == null ? new BuildingUnit() : buildingUnitRepository.findByFloorLevelIdAndName(floor.getId(), unit).orElse(new BuildingUnit());
				buildingUnit.setName(unit);
				buildingUnit.setFloorLevel(floor);
				buildingUnit.setRemark(a.getRemark());
				buildingUnitRepository.save(buildingUnit);
				buildingUnitRepository.flush();
			}
			
			if (building != null) {
				building.setFullText1(building);
				String str1 = building.getFullText();
				if (StringUtils.isNotBlank(str1)) {
					String str2 = building.getName() 
							+ '-' + building.getAddress().getBlock()
							+ '-' + building.getAddress().getLevel()
							+ '-' + building.getAddress().getUnitNumber()
							+ '-' + building.getAddress().getStreet() 
							+ '-' + building.getAddress().getPostalCode() 
							+ '-' + building.getAddress().getCity();
					List<String> list1 = Arrays.asList(str1.split("-"));
					List<String> list2 = Arrays.asList(str2.toLowerCase().split("-"));
					List<String> unique = list2.stream().filter(l2 -> list1.indexOf(l2) == -1).collect(Collectors.toList());
					unique.forEach(uni -> {
						list1.remove(uni);
					});

					building.setFullText(Stream.concat(list1.stream(), unique.stream()).map(blo -> blo).collect(Collectors.joining("-")));
				} else {
					building.setFullText1(building);
				}
				if (buildingUnit != null) {
					buildingUnit.setFullText1(buildingUnit);
					buildingUnitRepository.save(buildingUnit);
				}
				buildingRepository.save(building);
			}
			buildingRepository.flush();
			
//			Duplicate Handling when import:
//
//			Allow users to choose between "Force Overwrite" or "Skip" when duplicate entries are encountered.   
//
//			if the meter SN is dulicated, then to update the Remark for this meter if “Overwrite“ id ticked. 
//
//			if the City/zippostal/Building/Block/Floor/Unit  duplicate, to update the “Street” if the “Overwrite“ is ticked .
//
//			if the coupled of meter and address is different in import table compared with the couple in MMS, then skip with log this meter SNN and address .   
//
//			whether "Force Overwrite" or "Skip" is selected, the program should generate a log file documenting the action taken for each record.
//
//			if meter or addres did not coupled in table(either meter ID is blank or the City/zipPostal is blank), then just to import the available meter info or the available address info only.
			if (StringUtils.isNotBlank(a.getImportMsn())) {
			
				MMSMeter meter = mmsMeterRepository.findByMsn(a.getImportMsn());
				
				//if the meter SN is dulicated
				if (meter != null && "Override".equalsIgnoreCase(importType)) {
					meter.setRemark(a.getRemarkForMeter());
				}
				
				// if the coupled of meter and address is different in import table compared with the couple in MMS
				if (meter != null && meter.getBuildingUnit() != null && buildingUnit != null && meter.getBuildingUnit().getId().longValue() != buildingUnit.getId().longValue()) {
					a.setMessage("Meter SN has linked to another address (" + Utils.formatHomeAddress(meter) + ")!");
					return;
				}
				
				if (meter == null) {
					meter = new MMSMeter();
					meter.setMsn(a.getImportMsn());
					
					meter.setRemark(a.getRemarkForMeter());
					if (buildingUnit != null) {
						
						MMSMeter meterUnitCoupled = mmsMeterRepository.findByBuildingUnitId(buildingUnit.getId());
						if (meterUnitCoupled != null) {
							a.setMessage("Address has linked to another meter SN (" + meterUnitCoupled.getMsn() + ")!");
							return;
						}
						meter.setBuildingUnit(buildingUnit);
						meter.setBlock(block);
						meter.setFloorLevel(floor);
						meter.setBuilding(building);
					}
					
					checkChangeAddress(meter);
					mmsMeterRepository.save(meter);
					return;
				}
				if (buildingUnit != null) {
					meter.setBuildingUnit(buildingUnit);
					meter.setBlock(block);
					meter.setFloorLevel(floor);
					meter.setBuilding(building);				
				}

				checkChangeAddress(meter);
				mmsMeterRepository.save(meter);
			}
		});
		
		return dtos;
	}	
	
	public void checkChangeAddress(MMSMeter meter) {
		
		// check change mester address
		try {
			if (meter.getBuildingUnit() != null) {
				
				long lastestDecouple = StringUtils.isBlank(meter.getLatestDecoupleAddress()) ? -1l : Long.parseLong(meter.getLatestDecoupleAddress().split("@@")[1]);
				long lastestCoupled = StringUtils.isBlank(meter.getLatestCoupledAddress()) ? -1l : Long.parseLong(meter.getLatestCoupledAddress().split("@@")[1]);
				
				if (StringUtils.isBlank(meter.getLatestCoupledAddress()) || lastestCoupled < lastestDecouple) {
					meter.setLatestCoupledAddress(meter.getBuildingUnit().getId() + "@@" + System.currentTimeMillis() + "@@" + SecurityUtils.getEmail());	
				}
				
			}
			if (meter.getBuildingUnit() == null) {
				long lastestDecouple = StringUtils.isBlank(meter.getLatestDecoupleAddress()) ? -1l : Long.parseLong(meter.getLatestDecoupleAddress().split("@@")[1]);
				long lastestCoupled = StringUtils.isBlank(meter.getLatestCoupledAddress()) ? -1l : Long.parseLong(meter.getLatestCoupledAddress().split("@@")[1]);
				
				if (lastestCoupled > 0l && StringUtils.isBlank(meter.getLatestDecoupleAddress()) || lastestCoupled > lastestDecouple) {
					meter.setLatestDecoupleAddress("__@@" + System.currentTimeMillis() + "@@" + SecurityUtils.getEmail());
				}
			}
		} catch (Exception e) {
			//
		}
	}
	
	private static List<AddressDto> parseCsv(InputStream file, String importType) throws IOException {
		Map<String, Integer> head = new LinkedHashMap<>(); 
		List<AddressDto> rs = new ArrayList<>();
		for (String line: IOUtils.readLines(file, StandardCharsets.UTF_8)) {
			if (StringUtils.isBlank(line)) {
				continue;
			}
			boolean parseHead = head.isEmpty();
			int count = 0;
			AddressDto dto = null;
			for (String it: line.split(" *, *")) {
				if (parseHead) {
					head.put(it, count);
				} else {
					if (dto == null) {
						dto = new AddressDto();
					}
					if (head.computeIfAbsent("Building", k->-1) == count || head.computeIfAbsent("Building Name", k->-1) == count) {
						dto.setBuilding(StringUtils.isBlank(it) ? "NA" : it);
					} else if (head.computeIfAbsent("Block", k->-1) == count) {
						dto.setBlock(StringUtils.isBlank(it) ? "NA" : it);
					} else if (head.computeIfAbsent("Level", k->-1) == count) {
						dto.setLevel(StringUtils.isBlank(it) ? "NA" : it);
					} else if (head.computeIfAbsent("Unit", k->-1) == count || head.computeIfAbsent("\"Unit\"", k->-1) == count) {
						dto.setUnitNumber(it);
					} else if (head.computeIfAbsent("Postcode", k->-1) == count || head.computeIfAbsent("Postal", k->-1) == count) {
						dto.setPostalCode(it);
					} else if (head.computeIfAbsent("Street Address", k->-1) == count || head.computeIfAbsent("Street", k->-1) == count) {
						dto.setStreet(StringUtils.isBlank(it) ? "-" : it);
					} else if (head.computeIfAbsent("State.City", k->-1) == count || head.computeIfAbsent("City", k->-1) == count || head.computeIfAbsent("State", k->-1) == count) {
						dto.setCity(it);
					} else if (head.computeIfAbsent("Remark", k->-1) == count) {
						dto.setRemark(it);
					} else if (head.computeIfAbsent("Remark for meter", k->-1) == count || head.computeIfAbsent("remark for meter", k->-1) == count || head.computeIfAbsent("\"Remark for meter\"", k->-1) == count) {
						dto.setRemarkForMeter(it);
					} else if (head.computeIfAbsent("Coupled MCU SN", k->-1) == count) {
						dto.setCoupleSn(it);
					} else if (head.computeIfAbsent("Coupled Meter No.", k->-1) == count) {
						dto.setCoupleMsn(it.contains("+") ? (new BigDecimal(it).longValue() + "") : it);
					} else if (head.computeIfAbsent("MSN", k->-1) == count || head.computeIfAbsent("\"MSN\"", k->-1) == count) {
						dto.setImportMsn(it.contains("+") ? (new BigDecimal(it).longValue() + "") : it);
					}
				}
				count++;
			}
			head.computeIfAbsent("Message", k -> 100);
			if (dto != null) {
				dto.setLine(count);
				dto.setHead(head);
				rs.add(dto);				
			}

			if ("address".equalsIgnoreCase(importType)) {
				if (dto != null && StringUtils.isBlank(dto.getPostalCode())) {
					dto.setMessage("postalCode is required");
				} else if (dto != null && StringUtils.isBlank(dto.getCity())) {
					dto.setMessage("city is required");
				}
			}
			
			if ("meter".equalsIgnoreCase(importType) && dto != null) {
				dto.setBuilding("NA".equalsIgnoreCase(dto.getBuilding()) ? null : dto.getBuilding());
				dto.setBlock("NA".equalsIgnoreCase(dto.getBlock()) ? null : dto.getBlock());
				dto.setLevel("NA".equalsIgnoreCase(dto.getLevel()) ? null : dto.getLevel());
				dto.setUnitNumber("NA".equalsIgnoreCase(dto.getUnitNumber()) ? null : dto.getUnitNumber());
			}
			
			if ("meter".equalsIgnoreCase(importType) && dto != null) {
				if ((StringUtils.isBlank(dto.getImportMsn())
						|| StringUtils.isNotBlank(dto.getBuilding())
						|| StringUtils.isNotBlank(dto.getBlock())
						|| StringUtils.isNotBlank(dto.getLevel())
						|| StringUtils.isNotBlank(dto.getUnitNumber())
						|| StringUtils.isNotBlank(dto.getPostalCode())
						|| StringUtils.isNotBlank(dto.getCity())
						)) {
					if (StringUtils.isBlank(dto.getBuilding())) {
						dto.setMessage("building is required (" + (StringUtils.isNotBlank(dto.getImportMsn()) ? "or should be blank all address column" : "") + ")");
					} else if (StringUtils.isBlank(dto.getBlock())) {
						dto.setMessage("block is required (" + (StringUtils.isNotBlank(dto.getImportMsn()) ? "or should be blank all address column" : "") + ")");
					} else if (StringUtils.isBlank(dto.getLevel())) {
						dto.setMessage("level is required (" + (StringUtils.isNotBlank(dto.getImportMsn()) ? "or should be blank all address column" : "") + ")");
					} else if (StringUtils.isBlank(dto.getUnitNumber())) {
						dto.setMessage("unit is required (" + (StringUtils.isNotBlank(dto.getImportMsn()) ? "or should be blank all address column" : "") + ")");
					} else if (StringUtils.isBlank(dto.getPostalCode())) {
						dto.setMessage("postalCode is required (" + (StringUtils.isNotBlank(dto.getImportMsn()) ? "or should be blank all address column" : "") + ")");
					} else if (StringUtils.isBlank(dto.getCity())) {
						dto.setMessage("city is required (" + (StringUtils.isNotBlank(dto.getImportMsn()) ? "or should be blank all address column" : "") + ")");
					}
				}
				dto.setSkipAddress(StringUtils.isBlank(dto.getBuilding()) 
						|| StringUtils.isBlank(dto.getBlock())
						|| StringUtils.isBlank(dto.getLevel())
						|| StringUtils.isBlank(dto.getUnitNumber())
						);
			}
		}
		return rs;
	}
	
	public void updateNullBlock() {
		List<FloorLevel> fls = floorLevelRepository.findAllByBlockIsNull();
		fls.forEach(fl -> {
			Block block = blockRepository.findByBuildingIdAndName(fl.getBuilding().getId(), "NA").orElse(new Block());
			block.setName("NA");
			block.setBuilding(fl.getBuilding());
			blockRepository.save(block);
			blockRepository.flush();
			fl.setBlock(block);
			floorLevelRepository.save(fl);
		});
		buildingUnitRepository.findAll()
		.forEach(bu -> {
			if (StringUtils.isBlank(bu.getFullText())) {
				try {
					bu.setFullText1(bu);
				} catch (Exception e) {
					e.printStackTrace();
				}
				buildingUnitRepository.save(bu);				
			}
		});
	}
	
//	ID (Key),Building,Block,Level,Unit,Postcode,,Street Address,State.City,Coupled,UpdatedTime,Remark
//	1,Building21 Prince George’s Park,12,4,6,118426,,21 Prince George’s Park,Singapore,Y,,
//	2,Building21 Prince George’s Park,21,B2,K,118427,,22 Prince George’s Park,Singapore,,,
//	3,Building21 Prince George’s Park,123A,5,M,118428,,23 Prince George’s Park,Singapore,,,
//	4,Building21 Prince George’s Park,333,6,5,118429,,13 Prince George’s Park,Singapore,,,
//	5,Building21 Prince George’s Park,21,B1,L,118430,,29 Prince George’s Park,Singapore,,,
	public static void main(String[] args) throws FileNotFoundException, IOException {
		// parseCsv(new FileInputStream("address-upload-test.csv", null));
	}
}
