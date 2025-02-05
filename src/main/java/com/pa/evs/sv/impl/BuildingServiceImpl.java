package com.pa.evs.sv.impl;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pa.evs.dto.AddressDto;
import com.pa.evs.dto.BlockDto;
import com.pa.evs.dto.BuildingDto;
import com.pa.evs.dto.BuildingUnitDto;
import com.pa.evs.dto.FloorLevelDto;
import com.pa.evs.dto.PaginDto;
import com.pa.evs.enums.ResponseEnum;
import com.pa.evs.exception.ApiException;
import com.pa.evs.model.Address;
import com.pa.evs.model.Block;
import com.pa.evs.model.Building;
import com.pa.evs.model.Building.BuildingType;
import com.pa.evs.model.BuildingUnit;
import com.pa.evs.model.FloorLevel;
import com.pa.evs.model.MBoxBle;
import com.pa.evs.repository.AddressRepository;
import com.pa.evs.repository.BlockRepository;
import com.pa.evs.repository.BuildingRepository;
import com.pa.evs.repository.BuildingUnitRepository;
import com.pa.evs.repository.FloorLevelRepository;
import com.pa.evs.repository.MBoxBleRepository;
import com.pa.evs.sv.BuildingService;
import com.pa.evs.utils.Utils;


@Transactional
@Service
public class BuildingServiceImpl implements BuildingService {

	@Autowired
	EntityManager em;

	@Autowired
	BuildingRepository buildingRepository;

	@Autowired
	AddressRepository addressRepository;
	
	@Autowired
	BlockRepository blockRepository;
	
	@Autowired
	FloorLevelRepository floorLevelRepository;
	
	@Autowired
	BuildingUnitRepository buildingUnitRepository;
	
	@Autowired
	MBoxBleRepository mBoxBleRepository;

	@Override
	public void save(BuildingDto dto) throws ApiException {
		if (dto.getId() != null) {
			update(dto);
		} else {
			
			Building entity = new Building();
			Address address = new Address();
			List<BlockDto> blocks;
			List<FloorLevelDto> levels;
			List<BuildingUnitDto> units;
			AddressDto addressDto = dto.getAddress();
			address.setCity(addressDto.getCity());
			address.setCountry(addressDto.getCountry());
			address.setDisplayName(addressDto.getDisplayName());
			address.setBlock(addressDto.getBlock());
			address.setPostalCode(addressDto.getPostalCode());
			address.setStreet(addressDto.getStreet());
			address.setTown(addressDto.getTown());
			address.setUnitNumber(addressDto.getUnitNumber());
			address.setCreateDate(new Date());
			address.setModifyDate(new Date());
			addressRepository.save(address);

			entity.setAddress(address);
			entity.setDescription(dto.getDescription());
			entity.setHasTenant(dto.getHasTenant());
			entity.setId(dto.getId());
			entity.setName(dto.getName());
			entity.setType(BuildingType.from(dto.getType()));
			entity.setAddress(address);
			
			if(Objects.nonNull(dto.getBlocks())) {
				blocks = dto.getBlocks();
				String strBlocks = blocks.stream().map(bl -> bl.getName()).collect(Collectors.joining(", "));
				dto.getAddress().setBlock(strBlocks);
				for(BlockDto blockDto : blocks) {
					Block block = new Block();				
					block.setName(blockDto.getName());
					block.setBuilding(entity);
					blockRepository.save(block);
					if(Objects.nonNull(blockDto.getLevels())) {
						levels = blockDto.getLevels();
						String strLevels = levels.stream().map(bl -> bl.getName()).collect(Collectors.joining(", "));
						dto.getAddress().setLevel(strLevels);
						for(FloorLevelDto floorDto : levels) {
							FloorLevel floorLevel = new FloorLevel();
							floorLevel.setName(floorDto.getName());
							floorLevel.setBlock(block);;
							floorLevel.setBuilding(entity);
							floorLevelRepository.save(floorLevel);
							if(Objects.nonNull(floorDto.getUnits())) {
								units = floorDto.getUnits();
								String strUnits = units.stream().map(bl -> bl.getName()).collect(Collectors.joining(", "));
								dto.getAddress().setUnitNumber(strUnits);
								for(BuildingUnitDto buildingUnitDto : units) {
									BuildingUnit buildingUnit = new BuildingUnit();
									buildingUnit.setName(buildingUnitDto.getName());
									buildingUnit.setFloorLevel(floorLevel);
									buildingUnitRepository.save(buildingUnit);
								}
							}
						}
					}
				}
			}
			entity.setFullText1(dto);
			buildingRepository.save(entity);
		}
	}

	@Override
	@Transactional
	public void search(PaginDto<BuildingDto> pagin, String search) {

		if (pagin.getOptions() == null) {
			pagin.setOptions(new HashMap<>());
		}
		
		Map<String, Object> options = pagin.getOptions();
        String coupleState = (String) options.get("coupleState");
        String querySn = (String) options.get("querySn");
        String queryMsn = (String) options.get("queryMsn");
        String queryBuilding = (String) options.get("queryBuilding");
        String queryBlock = (String) options.get("queryBlock");
        String queryFloorLevel = (String) options.get("queryFloorLevel");
        String queryBuildingUnit = (String) options.get("queryBuildingUnit");
        String queryPostalCode = (String) options.get("queryPostalCode");
        String queryStreet = (String) options.get("queryStreet");
		boolean exportCsv = "true".equals(options.get("exportCSV"));
		boolean detailUnit = "true".equals(options.get("detailUnit"));
		
		StringBuilder sqlBuilder = new StringBuilder();
		sqlBuilder.append(" select b.id, b.name, b.type, b.description, b.has_tenant, ");
		sqlBuilder.append(" b.create_date, b.modify_date, ");
		sqlBuilder.append(" a.id as address_id, a.country, a.city, a.town, a.street, a.display_name, ");
		sqlBuilder.append(" a.postal_code, a.unit_number, ");
		
		if ((exportCsv || detailUnit)) {
			sqlBuilder.append(" (select crl.sn || '_' || coalesce(crl.msn, '') from {h-schema}ca_request_log crl where crl.building_unit_id = bu.id and crl.building_id = b.id limit 1) crlId ");
			sqlBuilder.append(" ,bl.name blName, fl.name fName, bu.name buName, b.id bId, fl.id fId, bu.id buId, bu.remark, bu.coupled_date, bu.modify_date bu_modify_date ");
		} else {
			sqlBuilder.append(" (select crl.sn || '_' || coalesce(crl.msn, '') from {h-schema}ca_request_log crl where crl.building_id = b.id limit 1) crlId ");
		}
		sqlBuilder.append(" from {h-schema}building b ");
		if ((exportCsv || detailUnit)) {
			sqlBuilder.append(" left join {h-schema}floor_level fl on (fl.building_id = b.id) ");
			sqlBuilder.append(" left join {h-schema}block bl on (fl.block_id = bl.id and (bl.id is null or bl.building_id = b.id)) ");
			sqlBuilder.append(" inner join {h-schema}building_unit bu on bu.floor_level_id = fl.id ");
		}
		sqlBuilder.append(" left join {h-schema}address a on b.address_id = a.id ");
		sqlBuilder.append(" where 1=1 ");
		if (StringUtils.isNotBlank(pagin.getKeyword())) {
			sqlBuilder.append(" and (b.name like '%" + pagin.getKeyword() + "%' or a.display_name like '%" + pagin.getKeyword() + "%')");
		}
		if (StringUtils.isNotBlank(search)) {
			for (String it : search.split(" *[,&] *")) {
				if ((exportCsv || detailUnit)) {
					sqlBuilder.append(" and (bu.full_text like '%" + it.trim().toLowerCase() + "%')");
				} else {
					sqlBuilder.append(" and (b.full_text like '%" + it.trim().toLowerCase() + "%')");					
				}
			}
		}
		if (StringUtils.isNotBlank(queryMsn)) {
			sqlBuilder.append(" and (LOWER((SELECT crl1.msn FROM pa_evs_db.ca_request_log crl1 WHERE crl1.building_unit_id = bu.id AND crl1.building_id = b.id LIMIT 1)) like '%" + queryMsn.toLowerCase() + "%')");
		}
		if (StringUtils.isNotBlank(querySn)) {
			sqlBuilder.append(" and (LOWER((SELECT crl2.sn FROM pa_evs_db.ca_request_log crl2 WHERE crl2.building_unit_id = bu.id AND crl2.building_id = b.id LIMIT 1)) like '%" + querySn.toLowerCase() + "%')");
		}
		if (StringUtils.isNotBlank(queryBuilding)) {
			sqlBuilder.append(" and b.id = " + queryBuilding);
		}
		if (StringUtils.isNotBlank(queryBlock)) {
			sqlBuilder.append(" and bl.id = " + queryBlock);
		}
		if (StringUtils.isNotBlank(queryFloorLevel)) {
			sqlBuilder.append(" and fl.id = " + queryFloorLevel);
		}
		if (StringUtils.isNotBlank(queryBuildingUnit)) {
			sqlBuilder.append(" and bu.id = " + queryBuildingUnit);
		}
		if (StringUtils.isNotBlank(queryPostalCode)) {
			sqlBuilder.append(" and a.postal_code like '%" + queryPostalCode + "%'");
		}
		if (StringUtils.isNotBlank(queryStreet)) {
			sqlBuilder.append(" and lower(a.street) like '%" + queryStreet.toLowerCase() + "%'");
		}
		if ("coupled".equalsIgnoreCase(coupleState)) {
			if ((exportCsv || detailUnit)) {
				sqlBuilder.append(" and exists (select 1 from {h-schema}ca_request_log crl where crl.building_unit_id = bu.id)");
			} else {
				sqlBuilder.append(" and exists (select 1 from {h-schema}ca_request_log crl where crl.building_id = b.id)");	
			}
		} else if ("not_couple".equalsIgnoreCase(coupleState)) {
			if ((exportCsv || detailUnit)) {
				sqlBuilder.append(" and not exists (select 1 from {h-schema}ca_request_log crl where crl.building_unit_id = bu.id)");
			} else {
				sqlBuilder.append(" and not exists (select 1 from {h-schema}ca_request_log crl where crl.building_id = b.id)");	
			}
		}
		
		if ((exportCsv || detailUnit)) {
			sqlBuilder.append(" order by b.name asc, bl.name asc, fl.name asc, bu.name asc");
		} else {
			sqlBuilder.append(" order by b.id asc  ");	
		}
		
		if (!exportCsv) {
			sqlBuilder.append(" offset " + pagin.getOffset() + " limit " + pagin.getLimit());	
		}
		
		Query q = em.createNativeQuery(sqlBuilder.toString());

		StringBuilder sqlCountBuilder = new StringBuilder();
		sqlCountBuilder.append(" select count(b.id)  ");
		sqlCountBuilder.append(" from {h-schema}building b ");
		if ((exportCsv || detailUnit)) {
			sqlCountBuilder.append(" left join {h-schema}floor_level fl on (fl.building_id = b.id) ");
			sqlCountBuilder.append(" left join {h-schema}block bl on (fl.block_id = bl.id and (bl.id is null or bl.building_id = b.id)) ");
			sqlCountBuilder.append(" inner join {h-schema}building_unit bu on bu.floor_level_id = fl.id ");
		}
		sqlCountBuilder.append(" left join {h-schema}address a on b.address_id = a.id ");
		sqlCountBuilder.append(" where 1=1 ");
		if (StringUtils.isNotBlank(pagin.getKeyword())) {
			sqlCountBuilder.append(" and (b.name like '%" + pagin.getKeyword() + "%' or a.display_name like '%" + pagin.getKeyword() + "%')");
		}
		if (StringUtils.isNotBlank(search)) {
			for (String it : search.split(" *[,&] *")) {
				if ((exportCsv || detailUnit)) {
					sqlCountBuilder.append(" and (bu.full_text like '%" + it.trim().toLowerCase() + "%')");
				} else {
					sqlCountBuilder.append(" and (b.full_text like '%" + it.trim().toLowerCase() + "%')");					
				}
			}
		}
		if ("coupled".equalsIgnoreCase(coupleState)) {
			if ((exportCsv || detailUnit)) {
				sqlCountBuilder.append(" and exists (select 1 from {h-schema}ca_request_log crl where crl.building_unit_id = bu.id)");
			} else {
				sqlCountBuilder.append(" and exists (select 1 from {h-schema}ca_request_log crl where crl.building_id = b.id)");	
			}
		} else if ("not_couple".equalsIgnoreCase(coupleState)) {
			if ((exportCsv || detailUnit)) {
				sqlCountBuilder.append(" and not exists (select 1 from {h-schema}ca_request_log crl where crl.building_unit_id = bu.id)");
			} else {
				sqlCountBuilder.append(" and not exists (select 1 from {h-schema}ca_request_log crl where crl.building_id = b.id)");
			}
		}
		if (StringUtils.isNotBlank(queryMsn)) {
			sqlCountBuilder.append(" and (LOWER((SELECT crl1.msn FROM pa_evs_db.ca_request_log crl1 WHERE crl1.building_unit_id = bu.id AND crl1.building_id = b.id LIMIT 1)) like '%" + queryMsn.toLowerCase() + "%')");
		}
		if (StringUtils.isNotBlank(querySn)) {
			sqlCountBuilder.append(" and (LOWER((SELECT crl2.sn FROM pa_evs_db.ca_request_log crl2 WHERE crl2.building_unit_id = bu.id AND crl2.building_id = b.id LIMIT 1)) like '%" + querySn.toLowerCase() + "%')");
		}
		if (StringUtils.isNotBlank(queryBuilding)) {
			sqlCountBuilder.append(" and b.id = " + queryBuilding);
		}
		if (StringUtils.isNotBlank(queryBlock)) {
			sqlCountBuilder.append(" and bl.id = " + queryBlock);
		}
		if (StringUtils.isNotBlank(queryFloorLevel)) {
			sqlCountBuilder.append(" and fl.id = " + queryFloorLevel);
		}
		if (StringUtils.isNotBlank(queryBuildingUnit)) {
			sqlCountBuilder.append(" and bu.id = " + queryBuildingUnit);
		}
		if (StringUtils.isNotBlank(queryPostalCode)) {
			sqlCountBuilder.append(" and a.postal_code like '%" + queryPostalCode + "%'");
		}
		if (StringUtils.isNotBlank(queryStreet)) {
			sqlCountBuilder.append(" and lower(a.street) like '%" + queryStreet.toLowerCase() + "%'");
		}
		
		Query qr = em.createNativeQuery(sqlCountBuilder.toString());
		Number totalRows = (Number) qr.getSingleResult();
		pagin.setTotalRows(totalRows.longValue());
		List<BuildingDto> results = calculateResults(q.getResultList(), exportCsv, detailUnit, coupleState);
		
		pagin.setResults(results);
	}
	
	@Transactional
	public List<BuildingDto> calculateResults(List<?> list, boolean exportCsv, boolean detailUnit, String coupleState) {
		List<BuildingDto> results = new ArrayList<>();
		
		for (int i = 0; i < list.size(); i++) {
			Object[] o = (Object[]) list.get(i);
			BuildingDto a = new BuildingDto();
			
			AddressDto address = new AddressDto();

			Number id = (Number) o[0];
			a.setId(id.longValue());
			a.setName((String) o[1]);
			a.setType((String) o[2]);
			a.setDescription((String) o[3]);
			a.setHasTenant((Boolean) o[4]);
			a.setCreatedDate((Date) o[5]);
			a.setModifiedDate((Date) o[6]);
			
			Number ida = (Number) o[7];
			address.setId(ida.longValue());
			address.setCountry((String) o[8]);
			address.setBuilding((String) o[1]);
			address.setCity((String) o[9]);
			address.setTown((String) o[10]);
			address.setStreet((String) o[11]);
			address.setDisplayName((String) o[12]);
			address.setPostalCode((String) o[13]);
			address.setUnitNumber((String) o[14]);

			Object crlSn = o[15];

			String coupleMsn = null;
			if (crlSn != null) {
				coupleMsn = (crlSn + "").replaceAll(".*_([^_]*)$", "$1");
				crlSn = (crlSn + "").replaceAll("(.*)_([^_]*)$", "$1");
			}
			
			address.setCoupleState(crlSn == null ? "N" : "Y");
			
			if ("null".equalsIgnoreCase(coupleMsn) || StringUtils.isBlank(coupleMsn)) {
				coupleMsn = null;
			}
			
			address.setCoupleMsn(coupleMsn);
			address.setCoupleSn((String) crlSn);
			
			if ((exportCsv || detailUnit)) {
				
				address.setBlock((String) o[16]);
				address.setLevel((String) o[17]);
				address.setUnitNumber((String) o[18]);
				address.setUnitId(((Number) o[21]).longValue());
				address.setLevelId(((Number) o[20]).longValue());
//				address.setCoupleState((o[19] + "_" + o[20] + "_" + o[21]).equals(crlId) ? "Y" : "N");
				
				address.setRemark((String) o[22]);
				address.setCoupleTime((Date) o[(crlSn == null || o[23] == null) ? 24 : 23]);
				if ("coupled".equalsIgnoreCase(coupleState) && !"Y".equals(address.getCoupleState()) 
						|| "not_couple".equalsIgnoreCase(coupleState) && !"N".equals(address.getCoupleState())) {
					continue;
				}
			}
			
			a.setAddress(address);
			a.setLabel(Utils.formatHomeAddress(a.getName(), address));
			
			results.add(a);
		}
		
		return results;
	}

	@Override
	public void delete(Long id) throws ApiException {
		Building entity = buildingRepository.findById(id)
				.orElseThrow(() -> new ApiException(ResponseEnum.BUILDING_NOT_FOUND));
		
		List<String> sns = new ArrayList<>();
		List<String> msns = new ArrayList<>();
		
		sns = buildingRepository.linkedSN(id);
		if (!sns.isEmpty()) {
			throw new RuntimeException("Building is already linked to the device(MCU SN = " + sns.get(0) + ")");
		}
		msns = buildingRepository.linkedMSN(id);
		if (!msns.isEmpty()) {
			throw new RuntimeException("Building is already linked to the device(Meter SN = " + msns.get(0) + ")");
		}
		
		List<Block> blocks = blockRepository.findAllByBuilding(entity);
		if (!blocks.isEmpty()) {
			for (Block block : blocks) {
	            deleteBlock(block);
	        }
		} else {
			List<FloorLevel> fls = floorLevelRepository.findAllByBuilding(entity);
			for(FloorLevel floorLevel : fls) {
				List<BuildingUnit> buildingUnits = buildingUnitRepository.findAllByFloorLevel(floorLevel);
				for(BuildingUnit buildingUnit : buildingUnits) {
					sns = buildingUnitRepository.linkedSN(buildingUnit.getId());
					if (!sns.isEmpty()) {
						throw new RuntimeException("Unit is already linked to the device(MCU SN = " + sns.get(0) + ")");
					}
					msns = buildingUnitRepository.linkedMSN(buildingUnit.getId());
					if (!msns.isEmpty()) {
						throw new RuntimeException("Unit is already linked to the device(Meter SN = " + msns.get(0) + ")");
					}
					
					buildingUnitRepository.delete(buildingUnit);
				}
				sns = floorLevelRepository.linkedSN(floorLevel.getId());
				if (!sns.isEmpty()) {
					throw new RuntimeException("Floor is already linked to the device(MCU SN = " + sns.get(0) + ")");
				}
				msns = floorLevelRepository.linkedMSN(floorLevel.getId());
				if (!msns.isEmpty()) {
					throw new RuntimeException("Floor is already linked to the device(Meter SN = " + msns.get(0) + ")");
				}			
				
				floorLevelRepository.delete(floorLevel);
			}
		}
		
		buildingRepository.delete(entity);
		em.createNativeQuery("delete from {h-schema}address where id = " + entity.getAddress().getId()).executeUpdate();
		em.flush();
	}
	
	
	private void deleteBlock(Block block) {
	    List<FloorLevel> floorLevels = floorLevelRepository.findAllByBlock(block);
	    for (FloorLevel floorLevel : floorLevels) {
	    	deleteFloorLevel(floorLevel);
	    }
	    validateAndDeleteBlock(block);
	}

	private void deleteFloorLevel(FloorLevel floorLevel) {
	    List<BuildingUnit> buildingUnits = buildingUnitRepository.findAllByFloorLevel(floorLevel);
	    for (BuildingUnit buildingUnit : buildingUnits) {
	        deleteBuildingUnitHierarchy(buildingUnit);
	    }
	    validateAndDeleteFloorLevel(floorLevel);
	}
	
	private void deleteBuildingUnitHierarchy(BuildingUnit buildingUnit) {
	    List<MBoxBle> mBoxBles = mBoxBleRepository.findAllByBuildingUnit(buildingUnit);
	    for (MBoxBle mBoxBle : mBoxBles) {
	        clearMBoxBle(mBoxBle);
	    }
	    validateAndDeleteBuildingUnit(buildingUnit);
	}
	
	private void clearMBoxBle(MBoxBle mBoxBle) {
	    mBoxBle.setAddress(null);
	    mBoxBle.setBlock(null);
	    mBoxBle.setBuilding(null);
	    mBoxBle.setBuildingUnit(null);
	    mBoxBle.setFloorLevel(null);
	    mBoxBleRepository.save(mBoxBle);
	    mBoxBleRepository.flush();
	}
	
	private void validateAndDeleteBuildingUnit(BuildingUnit buildingUnit) {
	    if (!buildingUnitRepository.linkedSN(buildingUnit.getId()).isEmpty()) {
	        throw new RuntimeException("Unit is already linked to the device (MCU SN = " + buildingUnitRepository.linkedSN(buildingUnit.getId()).get(0) + ")");
	    }
	    if (!buildingUnitRepository.linkedMSN(buildingUnit.getId()).isEmpty()) {
	        throw new RuntimeException("Unit is already linked to the device (Meter SN = " + buildingUnitRepository.linkedMSN(buildingUnit.getId()).get(0) + ")");
	    }
	    buildingUnitRepository.delete(buildingUnit);
	}
	
	private void validateAndDeleteFloorLevel(FloorLevel floorLevel) {
	    if (!floorLevelRepository.linkedSN(floorLevel.getId()).isEmpty()) {
	        throw new RuntimeException("Floor is already linked to the device (MCU SN = " + floorLevelRepository.linkedSN(floorLevel.getId()).get(0) + ")");
	    }
	    if (!floorLevelRepository.linkedMSN(floorLevel.getId()).isEmpty()) {
	        throw new RuntimeException("Floor is already linked to the device (Meter SN = " + floorLevelRepository.linkedMSN(floorLevel.getId()).get(0) + ")");
	    }
	    floorLevelRepository.delete(floorLevel);
	}

	private void validateAndDeleteBlock(Block block) {
	    if (!blockRepository.linkedSN(block.getId()).isEmpty()) {
	        throw new RuntimeException("Block is already linked to the device (MCU SN = " + blockRepository.linkedSN(block.getId()).get(0) + ")");
	    }
	    if (!blockRepository.linkedMSN(block.getId()).isEmpty()) {
	        throw new RuntimeException("Block is already linked to the device (Meter SN = " + blockRepository.linkedMSN(block.getId()).get(0) + ")");
	    }
	    blockRepository.delete(block);
	}

	@Override
	public void update(BuildingDto buildingDto) throws ApiException {

		Building building = buildingRepository.findById(buildingDto.getId())
				.orElseThrow(() -> new ApiException(ResponseEnum.BUILDING_NOT_FOUND));

		AddressDto addressDto = buildingDto.getAddress();
		Address address = addressRepository.findById(addressDto.getId()).orElse(new Address());
		address.setCity(addressDto.getCity());
		address.setCountry(addressDto.getCountry());
		address.setDisplayName(addressDto.getDisplayName());
		address.setBlock(addressDto.getBlock());
		address.setPostalCode(addressDto.getPostalCode());
		address.setStreet(addressDto.getStreet());
		address.setTown(addressDto.getTown());
		address.setUnitNumber(addressDto.getUnitNumber());
		address.setModifyDate(new Date());
		addressRepository.save(address);

		building.setModifyDate(new Date());
		building.setDescription(buildingDto.getDescription());
		building.setHasTenant(buildingDto.getHasTenant());
		building.setName(buildingDto.getName());
		building.setType(BuildingType.from(buildingDto.getType()));
		building.setAddress(address);
		
		List<BuildingUnit> units = new ArrayList<>();
		List<Block> blocks = blockRepository.findAllByBuilding(building);
		List<FloorLevel> levels = floorLevelRepository.findAllByBuilding(building);
		
		for (FloorLevel lvl : levels) {
			List<BuildingUnit> listUnit = buildingUnitRepository.findAllByFloorLevel(lvl);
			units = Stream.concat(units.stream(), listUnit.stream()).collect(Collectors.toList());
		};
		
		if (!blocks.isEmpty()) {
			String strBlocks = blocks.stream().map(bl -> bl.getName()).collect(Collectors.joining(", "));
			buildingDto.getAddress().setBlock(strBlocks);
		}
		
		if (!levels.isEmpty()) {
			String strLevels = levels.stream().map(bl -> bl.getName()).collect(Collectors.joining(", "));
			buildingDto.getAddress().setLevel(strLevels);
		}
		
		if (!units.isEmpty()) {
			String strUnits = units.stream().map(bl -> bl.getName()).collect(Collectors.joining(", "));
			buildingDto.getAddress().setUnitNumber(strUnits);
		}
		
		building.setFullText1(buildingDto);
		buildingRepository.save(building);
	}
	
//	@Override
//	public void updateBuildingFullText() {
//		buildingRepository.findAll().forEach(bd -> bd.setFullText1(bd));
//	}
}
