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
import com.pa.evs.model.Building;
import com.pa.evs.model.FloorLevel;
import com.pa.evs.repository.BuildingRepository;
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
	
	@Override
	public void save(FloorLevelDto dto) throws ApiException{
		if (dto.getId() != null) {
			update(dto);
		} else {
			
			FloorLevel entity = new FloorLevel();
			entity.setId(dto.getId());
			
			Building building = null;
			if (dto.getBuilding() != null) {
				building = buildingRepository.findById(dto.getBuilding().getId()).orElse(new Building());
			}
			entity.setBuilding(building);
			entity.setDisplayName(dto.getDisplayName());
			entity.setHasTenant(dto.getHasTenant());
			entity.setName(dto.getName());
			entity.setLevel(dto.getLevel());
			floorLevelRepository.save(entity);
		}
	}
	
	@Override
	public void search(PaginDto<FloorLevelDto> pagin) {

		if (pagin.getLimit() == null) {
			pagin.setLimit(10);
		}
		if (pagin.getOffset() == null) {
			pagin.setOffset(0);
		}
		
		StringBuilder sqlBuilder = new StringBuilder("FROM FloorLevel where 1=1");
		if (StringUtils.isNotBlank(pagin.getKeyword())) {
			sqlBuilder.append(" AND displayName like '%" + pagin.getKeyword() + "%'");
		}
		
		if (pagin.getOptions().get("buildingId") != null) {
			sqlBuilder.append(" AND building.id = ").append(pagin.getOptions().get("buildingId"));
		}
		
		sqlBuilder.append(" ORDER BY modifyDate DESC ");
		
		Query q = em.createQuery(sqlBuilder.toString(), FloorLevel.class);
		q.setFirstResult(pagin.getOffset());
		q.setMaxResults(pagin.getLimit());
		
		StringBuilder sqlCountBuilder = new StringBuilder("SELECT COUNT(*) FROM FloorLevel where 1=1");
		if (StringUtils.isNotBlank(pagin.getKeyword())) {
			sqlCountBuilder.append(" AND displayName like '%" + pagin.getKeyword() + "%'");
		}
		
		if (pagin.getOptions().get("buildingId") != null) {
			sqlCountBuilder.append(" AND building.id = ").append(pagin.getOptions().get("buildingId"));
		}
		
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
		floorLevelRepository.delete(entity);
	}
	
}
