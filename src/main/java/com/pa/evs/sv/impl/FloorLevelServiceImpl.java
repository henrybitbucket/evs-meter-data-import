package com.pa.evs.sv.impl;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pa.evs.dto.BuildingDto;
import com.pa.evs.dto.FloorLevelDto;
import com.pa.evs.dto.PaginDto;
import com.pa.evs.enums.ResponseEnum;
import com.pa.evs.exception.ApiException;
import com.pa.evs.model.Block;
import com.pa.evs.model.Building;
import com.pa.evs.model.BuildingUnit;
import com.pa.evs.model.FloorLevel;
import com.pa.evs.repository.BlockRepository;
import com.pa.evs.repository.BuildingRepository;
import com.pa.evs.repository.BuildingUnitRepository;
import com.pa.evs.repository.FloorLevelRepository;
import com.pa.evs.sv.FloorLevelService;



@Transactional
@Service
public class FloorLevelServiceImpl implements FloorLevelService {

	@Autowired
	EntityManager em;
	
	@Autowired
	FloorLevelRepository floorLevelRepository;
	
	@Autowired
	BuildingRepository buildingRepository;
	
	@Autowired
	BlockRepository blockRepository;
	
	@Autowired
	BuildingUnitRepository buildingUnitRepository;
	
	@Override
	public void save(FloorLevelDto dto) throws ApiException{
		if (dto.getId() != null) {
			update(dto);
		} else {
			
			Building building = null;
			if (dto.getBuilding() != null) {
				building = buildingRepository.findById(dto.getBuilding().getId()).orElse(new Building());
			}
			Block block = null;
			if (dto.getBlock() != null) {
				block = blockRepository.findById(dto.getBlock().getId()).orElse(new Block());
			}
			for (String name : dto.getNames()) {
				FloorLevel entity = new FloorLevel();
				entity.setId(dto.getId());
				entity.setBuilding(building);
				entity.setDisplayName(dto.getDisplayName());
				entity.setHasTenant(dto.getHasTenant());
				entity.setName(name);
				entity.setBlock(block);
				entity.setLevel(dto.getLevel());
				floorLevelRepository.save(entity);
			}
		}
	}
	
	@Override
	public void search(PaginDto<FloorLevelDto> pagin) {

		if (pagin.getLimit() == null) {
			pagin.setLimit(10000);
		}
		if (pagin.getOffset() == null) {
			pagin.setOffset(0);
		}
		
		StringBuilder sqlBuilder = new StringBuilder(" select fl ");
		StringBuilder cmmBuilder = new StringBuilder(" FROM FloorLevel fl left join Block bl on fl.block.id = bl.id where 1=1");
		if (StringUtils.isNotBlank(pagin.getKeyword())) {
			cmmBuilder.append(" AND fl.displayName like '%" + pagin.getKeyword() + "%'");
		}
		
		if (pagin.getOptions().get("buildingId") != null) {
			cmmBuilder.append(" AND fl.building.id = ").append(pagin.getOptions().get("buildingId"));
		}
		
		if (pagin.getOptions().get("blockId") != null) {
			cmmBuilder.append(" AND bl.id = ").append(pagin.getOptions().get("blockId"));
		} else {
			cmmBuilder.append(" AND (bl.id is null or bl.name = '-' or bl.name = '') ");
		}
		
		sqlBuilder.append(cmmBuilder);
		sqlBuilder.append(" ORDER BY fl.modifyDate DESC ");
		
		Query q = em.createQuery(sqlBuilder.toString(), FloorLevel.class);
		q.setFirstResult(pagin.getOffset());
		q.setMaxResults(pagin.getLimit());
		
		StringBuilder sqlCountBuilder = new StringBuilder("SELECT COUNT(*) ");
		sqlCountBuilder.append(cmmBuilder);
		
		Query qCount = em.createQuery(sqlCountBuilder.toString());
		
		@SuppressWarnings("unchecked")
		List<FloorLevel> list = q.getResultList();
		
		List<FloorLevelDto> dtos = new ArrayList<>();
		list.forEach(f -> {
			FloorLevelDto dto = new FloorLevelDto();
			dto.setName(f.getName());
			dto.setLevel(f.getLevel());
			dto.setDisplayName(f.getDisplayName());
			dto.setId(f.getId());
			dto.setHasTenant(f.getHasTenant());
			
			BuildingDto buildingDto = new BuildingDto();
			if (f.getBuilding() != null) {
				buildingDto.setId(f.getBuilding().getId());
				buildingDto.setName(f.getBuilding().getName());
			}
			dto.setBuilding(buildingDto);
			dtos.add(dto);
		});
		pagin.setResults(dtos);
		pagin.setTotalRows(((Number)qCount.getSingleResult()).longValue());
	}
	
	@Override
	public void update(FloorLevelDto dto) throws ApiException {
		FloorLevel entity = floorLevelRepository.findById(dto.getId()).orElseThrow(() 
				-> new ApiException(ResponseEnum.FLOOR_LEVEL_NOT_FOUND));
		Building building = null;
		if (dto.getBuilding() != null) {
			building = buildingRepository.findById(dto.getBuilding().getId()).orElseThrow(() 
					-> new ApiException(ResponseEnum.BUILDING_NOT_FOUND));
		}
		entity.setBuilding(building);
		entity.setDisplayName(dto.getDisplayName());
		entity.setHasTenant(dto.getHasTenant());
		entity.setName(dto.getName());
		entity.setLevel(dto.getLevel());
		floorLevelRepository.save(entity);
	}
	
	@Override
	public void delete(Long id) throws ApiException {
		FloorLevel entity = floorLevelRepository.findById(id).orElseThrow(() -> new ApiException(ResponseEnum.FLOOR_LEVEL_NOT_FOUND));
		List<String> sns = floorLevelRepository.linkedSN(id);
		if (!sns.isEmpty()) {
			throw new RuntimeException("Floor is already linked to the device(MCU SN = " + sns.get(0) + ")");
		}
		List<String> msns = floorLevelRepository.linkedMSN(id);
		if (!msns.isEmpty()) {
			throw new RuntimeException("Floor is already linked to the device(Meter SN = " + msns.get(0) + ")");
		}
		
		List<BuildingUnit> buildingUnits = buildingUnitRepository.findAllByFloorLevel(entity);
		for(BuildingUnit buildingUnit : buildingUnits) {
			buildingUnitRepository.delete(buildingUnit);
		}
		floorLevelRepository.delete(entity);
	}
	
}
