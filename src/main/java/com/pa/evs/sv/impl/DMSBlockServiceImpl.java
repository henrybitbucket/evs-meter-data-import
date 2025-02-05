package com.pa.evs.sv.impl;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pa.evs.dto.BlockDto;
import com.pa.evs.dto.BuildingDto;
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
import com.pa.evs.sv.DMSBlockService;


@Transactional
@Service
public class DMSBlockServiceImpl implements DMSBlockService {

	@Autowired
	EntityManager em;
	
	@Autowired
	DMSBlockRepository blockRepository;
	
	@Autowired
	DMSBuildingRepository buildingRepository;
	
	@Autowired
	DMSFloorLevelRepository floorLevelRepository;
	
	@Autowired
	DMSBuildingUnitRepository buildingUnitRepository;
	
	@Autowired
	DMSLocationSiteRepository dmsLocationSiteRepository;
	
	@Autowired
	DMSLocationLockRepository dmsLocationLockRepository;
	
	@Override
	public void save(BlockDto dto) throws ApiException{
		if (dto.getId() != null) {
			update(dto);
		} else {
			DMSBuilding building = null;
			if (dto.getBuilding() != null) {
				building = buildingRepository.findById(dto.getBuilding().getId()).orElse(new DMSBuilding());
			}
			for (String name : dto.getNames()) {
				DMSBlock entity = new DMSBlock();
				entity.setId(dto.getId());
				entity.setBuilding(building);
				entity.setDisplayName(dto.getDisplayName());
				entity.setHasTenant(dto.getHasTenant());
				entity.setName(name);
				entity.setBlock(dto.getBlock());
				blockRepository.save(entity);
			}
		}
	}
	
	@Override
	public void search(PaginDto<BlockDto> pagin) {

		if (pagin.getLimit() == null) {
			pagin.setLimit(10000);
		}
		if (pagin.getOffset() == null) {
			pagin.setOffset(0);
		}
		
		StringBuilder sqlBuilder = new StringBuilder("FROM DMSBlock where 1=1");
		if (StringUtils.isNotBlank(pagin.getKeyword())) {
			sqlBuilder.append(" AND displayName like '%" + pagin.getKeyword() + "%'");
		}
		
		if (pagin.getOptions().get("buildingId") != null) {
			sqlBuilder.append(" AND building.id = ").append(pagin.getOptions().get("buildingId"));
		}
		
		sqlBuilder.append(" ORDER BY modifyDate DESC ");
		
		Query q = em.createQuery(sqlBuilder.toString(), DMSBlock.class);
		q.setFirstResult(pagin.getOffset());
		q.setMaxResults(pagin.getLimit());
		
		StringBuilder sqlCountBuilder = new StringBuilder("SELECT COUNT(*) FROM DMSBlock where 1=1");
		if (StringUtils.isNotBlank(pagin.getKeyword())) {
			sqlCountBuilder.append(" AND displayName like '%" + pagin.getKeyword() + "%'");
		}
		
		if (pagin.getOptions().get("buildingId") != null) {
			sqlCountBuilder.append(" AND building.id = ").append(pagin.getOptions().get("buildingId"));
		}
		
		Query qCount = em.createQuery(sqlCountBuilder.toString());
		
		@SuppressWarnings("unchecked")
		List<DMSBlock> list = q.getResultList();
		
		List<BlockDto> dtos = new ArrayList<>();
		list.forEach(f -> {
			BlockDto dto = new BlockDto();
			dto.setName(f.getName());
			dto.setBlock(f.getBlock());
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
	public void update(BlockDto dto) throws ApiException {
		DMSBlock entity = blockRepository.findById(dto.getId()).orElseThrow(() 
				-> new ApiException(ResponseEnum.BLOCK_NOT_FOUND));
		entity.setDisplayName(dto.getDisplayName());
		entity.setHasTenant(dto.getHasTenant());
		entity.setName(dto.getName());
		entity.setBlock(dto.getBlock());
		blockRepository.save(entity);
	}
	
	@Override
	public void delete(Long id) throws ApiException {
		DMSBlock entity = blockRepository.findById(id).orElseThrow(() -> new ApiException(ResponseEnum.BLOCK_NOT_FOUND));
		
		List<String> sites = dmsLocationSiteRepository.findSiteByBlockId(entity.getId());
		if (!sites.isEmpty()) {
			throw new RuntimeException("Block is already linked to the site(Site label = " + sites.get(0) + ")");
		}
		List<String> locks = dmsLocationLockRepository.findLockByBlockId(entity.getId());
		if (!locks.isEmpty()) {
			throw new RuntimeException("Block is already linked to the lock(Lock Number = " + locks.get(0) + ")");
		}
		
		List<DMSFloorLevel> fls = floorLevelRepository.findAllByBlock(entity);
		for (DMSFloorLevel floorLevel : fls) {
			List<DMSBuildingUnit> buildingUnits = buildingUnitRepository.findAllByFloorLevel(floorLevel);
			for (DMSBuildingUnit buildingUnit : buildingUnits) {
				buildingUnitRepository.delete(buildingUnit);
			}
			floorLevelRepository.delete(floorLevel);
		}
		blockRepository.delete(entity);
	}
	
}
