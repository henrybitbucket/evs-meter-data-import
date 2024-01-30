package com.pa.evs.sv.impl;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pa.evs.dto.BlockDto;
import com.pa.evs.dto.BuildingDto;
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
import com.pa.evs.sv.BlockService;


@Transactional
@Service
public class BlockServiceImpl implements BlockService {

	@Autowired
	EntityManager em;
	
	@Autowired
	BlockRepository blockRepository;
	
	@Autowired
	BuildingRepository buildingRepository;
	
	@Autowired
	FloorLevelRepository floorLevelRepository;
	
	@Autowired
	BuildingUnitRepository buildingUnitRepository;
	
	@Override
	public void save(BlockDto dto) throws ApiException{
		if (dto.getId() != null) {
			update(dto);
		} else {
			Building building = null;
			if (dto.getBuilding() != null) {
				building = buildingRepository.findById(dto.getBuilding().getId()).orElse(new Building());
			}
			for (String name : dto.getNames()) {
				Block entity = new Block();
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
		
		StringBuilder sqlBuilder = new StringBuilder("FROM Block where 1=1");
		if (StringUtils.isNotBlank(pagin.getKeyword())) {
			sqlBuilder.append(" AND displayName like '%" + pagin.getKeyword() + "%'");
		}
		
		if (pagin.getOptions().get("buildingId") != null) {
			sqlBuilder.append(" AND building.id = ").append(pagin.getOptions().get("buildingId"));
		}
		
		sqlBuilder.append(" ORDER BY modifyDate DESC ");
		
		Query q = em.createQuery(sqlBuilder.toString(), Block.class);
		q.setFirstResult(pagin.getOffset());
		q.setMaxResults(pagin.getLimit());
		
		StringBuilder sqlCountBuilder = new StringBuilder("SELECT COUNT(*) FROM Block where 1=1");
		if (StringUtils.isNotBlank(pagin.getKeyword())) {
			sqlCountBuilder.append(" AND displayName like '%" + pagin.getKeyword() + "%'");
		}
		
		if (pagin.getOptions().get("buildingId") != null) {
			sqlCountBuilder.append(" AND building.id = ").append(pagin.getOptions().get("buildingId"));
		}
		
		Query qCount = em.createQuery(sqlCountBuilder.toString());
		
		@SuppressWarnings("unchecked")
		List<Block> list = q.getResultList();
		
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
		Block entity = blockRepository.findById(dto.getId()).orElseThrow(() 
				-> new ApiException(ResponseEnum.BLOCK_NOT_FOUND));
		entity.setDisplayName(dto.getDisplayName());
		entity.setHasTenant(dto.getHasTenant());
		entity.setName(dto.getName());
		entity.setBlock(dto.getBlock());
		blockRepository.save(entity);
	}
	
	@Override
	public void delete(Long id) throws ApiException {
		Block entity = blockRepository.findById(id).orElseThrow(() -> new ApiException(ResponseEnum.BLOCK_NOT_FOUND));
		List<String> sns = blockRepository.linkedSN(id);
		if (!sns.isEmpty()) {
			throw new RuntimeException("Block is already linked to the device(MCU SN = " + sns.get(0) + ")");
		}
		List<String> msns = blockRepository.linkedMSN(id);
		if (!msns.isEmpty()) {
			throw new RuntimeException("Block is already linked to the device(Meter SN = " + msns.get(0) + ")");
		}
		
		List<FloorLevel> fls = floorLevelRepository.findAllByBlock(entity);
		for(FloorLevel floorLevel : fls) {
			List<BuildingUnit> buildingUnits = buildingUnitRepository.findAllByFloorLevel(floorLevel);
			for(BuildingUnit buildingUnit : buildingUnits) {
				buildingUnitRepository.delete(buildingUnit);
			}
			floorLevelRepository.delete(floorLevel);
		}
		blockRepository.delete(entity);
	}
	
}
