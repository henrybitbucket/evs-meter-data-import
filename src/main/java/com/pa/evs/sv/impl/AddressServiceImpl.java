package com.pa.evs.sv.impl;

import java.io.FileInputStream;
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
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.pa.evs.dto.AddressDto;
import com.pa.evs.model.Address;
import com.pa.evs.model.Block;
import com.pa.evs.model.Building;
import com.pa.evs.model.BuildingUnit;
import com.pa.evs.model.FloorLevel;
import com.pa.evs.repository.AddressRepository;
import com.pa.evs.repository.BlockRepository;
import com.pa.evs.repository.BuildingRepository;
import com.pa.evs.repository.BuildingUnitRepository;
import com.pa.evs.repository.FloorLevelRepository;
import com.pa.evs.sv.AddressService;

@Service
public class AddressServiceImpl implements AddressService {

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
	
	@Override
	@Transactional
	public void handleUpload(MultipartFile file) throws IOException {
		
		List<AddressDto> dtos = parseCsv(file.getInputStream());
		Map<String, AddressDto> mapA = new LinkedHashMap<>();
		dtos.forEach(a -> mapA.put(a.getUnitNumber(), a));
		List<Address> ens = addressRepository.findAllByStreet(mapA.keySet());
		Map<String, Address> mapAE = new LinkedHashMap<>();
		ens.forEach(e -> mapAE.put(e.getStreet() + "__" + e.getPostalCode() + "__" + e.getCity(), e));
		mapA.forEach((k, a) -> {
			
			String combineKey = a.getStreet() + "__" + a.getPostalCode() + "__" + a.getCity();
			
			Address add = mapAE.computeIfAbsent(combineKey, st -> new Address());
			
			add.setStreet(a.getStreet());
			add.setCity(a.getCity());
			add.setUnitNumber(a.getUnitNumber());
			add.setPostalCode(a.getPostalCode());
			add.setRemark(a.getRemark());
			add.setModifyDate(new Date());
			
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
				add.setBlock(bl);
				add.setLevel(lvl);
				building.setAddress(add);
				
				if (StringUtils.isNotBlank(bl)) {
					block = building.getId() == null ? new Block() : blockRepository.findByBuildingIdAndName(building.getId(), bl).orElse(new Block());
					block.setName(bl);
					block.setBuilding(building);
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
				}
				
			}

			if (StringUtils.isNotBlank(unit) && floor != null) {
				buildingUnit = floor.getId() == null ? new BuildingUnit() : buildingUnitRepository.findByFloorLevelIdAndName(floor.getId(), unit).orElse(new BuildingUnit());
				buildingUnit.setName(unit);
				buildingUnit.setFloorLevel(floor);
			}
			
			addressRepository.save(add);
			if (building != null) {
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
				buildingRepository.save(building);
			}
			if (block != null) {
				blockRepository.save(block);
			}
			if (floor != null) {
				floorLevelRepository.save(floor);
			}
			if (buildingUnit != null) {
				buildingUnitRepository.save(buildingUnit);
			}
		});
	}
	
	private static List<AddressDto> parseCsv(InputStream file) throws IOException {
		Map<String, Integer> head = new LinkedHashMap<>(); 
		List<AddressDto> rs = new ArrayList<>();
		for (String line: IOUtils.readLines(file, StandardCharsets.UTF_8)) {
			
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
						dto.setBuilding(it);
					} else if (head.computeIfAbsent("Block", k->-1) == count) {
						dto.setBlock(it);
					} else if (head.computeIfAbsent("Level", k->-1) == count) {
						dto.setLevel(it);
					} else if (head.computeIfAbsent("Unit", k->-1) == count) {
						dto.setUnitNumber(it);
					} else if (head.computeIfAbsent("Postcode", k->-1) == count) {
						dto.setPostalCode(it);
					} else if (head.computeIfAbsent("Street Address", k->-1) == count) {
						dto.setStreet(it);
					} else if (head.computeIfAbsent("State.City", k->-1) == count) {
						dto.setCity(it);
					} else if (head.computeIfAbsent("Remark", k->-1) == count) {
						dto.setRemark(it);
					}
				}
				count++;
			}
			if (dto != null && StringUtils.isNotBlank(dto.getPostalCode()) && StringUtils.isNotBlank(dto.getCity())) {
				rs.add(dto);
			}
		}
		return rs;
	}
	
//	ID (Key),Building,Block,Level,Unit,Postcode,,Street Address,State.City,Coupled,UpdatedTime,Remark
//	1,Building21 Prince George’s Park,12,4,6,118426,,21 Prince George’s Park,Singapore,Y,,
//	2,Building21 Prince George’s Park,21,B2,K,118427,,22 Prince George’s Park,Singapore,,,
//	3,Building21 Prince George’s Park,123A,5,M,118428,,23 Prince George’s Park,Singapore,,,
//	4,Building21 Prince George’s Park,333,6,5,118429,,13 Prince George’s Park,Singapore,,,
//	5,Building21 Prince George’s Park,21,B1,L,118430,,29 Prince George’s Park,Singapore,,,
	public static void main(String[] args) throws FileNotFoundException, IOException {
		parseCsv(new FileInputStream("address-upload-test.csv"));
	}
}
