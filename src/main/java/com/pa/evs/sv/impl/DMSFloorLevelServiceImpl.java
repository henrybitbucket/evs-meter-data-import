package com.pa.evs.sv.impl;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pa.evs.dto.BuildingDto;
import com.pa.evs.dto.FloorLevelDto;
import com.pa.evs.dto.PaginDto;
import com.pa.evs.enums.ResponseEnum;
import com.pa.evs.exception.ApiException;
import com.pa.evs.model.DMSBlock;
import com.pa.evs.model.DMSBuilding;
import com.pa.evs.model.DMSBuildingUnit;
import com.pa.evs.model.DMSFloorLevel;
import com.pa.evs.repository.DMSBlockRepository;
import com.pa.evs.repository.DMSBuildingRepository;
import com.pa.evs.repository.DMSBuildingUnitRepository;
import com.pa.evs.repository.DMSFloorLevelRepository;
import com.pa.evs.repository.DMSLocationLockRepository;
import com.pa.evs.repository.DMSLocationSiteRepository;
import com.pa.evs.sv.DMSFloorLevelService;



@Transactional
@Service
public class DMSFloorLevelServiceImpl implements DMSFloorLevelService {

	@Autowired
	EntityManager em;
	
	@Autowired
	DMSFloorLevelRepository floorLevelRepository;
	
	@Autowired
	DMSBuildingRepository buildingRepository;
	
	@Autowired
	DMSBlockRepository blockRepository;
	
	@Autowired
	DMSBuildingUnitRepository buildingUnitRepository;
	
	@Autowired
	DMSLocationSiteRepository dmsLocationSiteRepository;
	
	@Autowired
	DMSLocationLockRepository dmsLocationLockRepository;
	
	@Override
	public void save(FloorLevelDto dto) throws ApiException{
		if (dto.getId() != null) {
			update(dto);
		} else {
			
			DMSBuilding building = null;
			if (dto.getBuilding() != null) {
				building = buildingRepository.findById(dto.getBuilding().getId()).orElse(new DMSBuilding());
			}
			DMSBlock block = null;
			if (dto.getBlock() != null) {
				block = blockRepository.findById(dto.getBlock().getId()).orElse(new DMSBlock());
			}
			for (String name : dto.getNames()) {
				DMSFloorLevel entity = new DMSFloorLevel();
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
		StringBuilder cmmBuilder = new StringBuilder(" FROM DMSFloorLevel fl left join DMSBlock bl on fl.block.id = bl.id where 1=1");
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
		
		Query q = em.createQuery(sqlBuilder.toString(), DMSFloorLevel.class);
		q.setFirstResult(pagin.getOffset());
		q.setMaxResults(pagin.getLimit());
		
		StringBuilder sqlCountBuilder = new StringBuilder("SELECT COUNT(*) ");
		sqlCountBuilder.append(cmmBuilder);
		
		Query qCount = em.createQuery(sqlCountBuilder.toString());
		
		@SuppressWarnings("unchecked")
		List<DMSFloorLevel> list = q.getResultList();
		
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
		DMSFloorLevel entity = floorLevelRepository.findById(dto.getId()).orElseThrow(() 
				-> new ApiException(ResponseEnum.FLOOR_LEVEL_NOT_FOUND));
		DMSBuilding building = null;
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
		DMSFloorLevel entity = floorLevelRepository.findById(id).orElseThrow(() -> new ApiException(ResponseEnum.FLOOR_LEVEL_NOT_FOUND));
		List<String> sites = dmsLocationSiteRepository.findSiteByFloorLevelId(entity.getId());
		if (!sites.isEmpty()) {
			throw new RuntimeException("Floor is already linked to the site(Site label = " + sites.get(0) + ")");
		}
		List<String> locks = dmsLocationLockRepository.findLockByFloorLevelId(entity.getId());
		if (!locks.isEmpty()) {
			throw new RuntimeException("Floor is already linked to the lock(Lock Number = " + locks.get(0) + ")");
		}
		
		List<DMSBuildingUnit> buildingUnits = buildingUnitRepository.findAllByFloorLevel(entity);
		for (DMSBuildingUnit buildingUnit : buildingUnits) {
			sites = dmsLocationSiteRepository.findSiteByBuildingUnitId(buildingUnit.getId());
			if (!sites.isEmpty()) {
				throw new RuntimeException("Unit is already linked to the site(Site label = " + sites.get(0) + ")");
			}
			locks = dmsLocationLockRepository.findLockByBuildingUnitId(buildingUnit.getId());
			if (!locks.isEmpty()) {
				throw new RuntimeException("Unit is already linked to the lock(Lock Number = " + locks.get(0) + ")");
			}
			buildingUnitRepository.delete(buildingUnit);
		}
		floorLevelRepository.delete(entity);
	}
	
}
