package com.pa.evs.sv.impl;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pa.evs.dto.BuildingDto;
import com.pa.evs.dto.BuildingUnitDto;
import com.pa.evs.dto.FloorLevelDto;
import com.pa.evs.dto.PaginDto;
import com.pa.evs.enums.ResponseEnum;
import com.pa.evs.exception.ApiException;
import com.pa.evs.model.BuildingUnit;
import com.pa.evs.model.BuildingUnit.BuildingUnitType;
import com.pa.evs.model.FloorLevel;
import com.pa.evs.repository.BuildingUnitRepository;
import com.pa.evs.repository.FloorLevelRepository;
import com.pa.evs.sv.BuildingUnitService;


@Transactional
@Service
public class BuildingUnitServiceImpl implements BuildingUnitService {

	@Autowired
	EntityManager em;

	@Autowired
	BuildingUnitRepository buildingUnitRepository;

	@Autowired
	FloorLevelRepository floorLevelRepository;

	@Override
	public void save(BuildingUnitDto dto) throws ApiException {
		if (dto.getId() != null) {
			update(dto);
		} else {
			if (dto.getFloorLevel() == null || dto.getFloorLevel().getId() == null) {
				throw new ApiException(ResponseEnum.FLOOR_LEVEL_NOT_FOUND);
			}
			FloorLevel f = floorLevelRepository.findById(dto.getFloorLevel().getId())
					.orElseThrow(() -> new ApiException(ResponseEnum.FLOOR_LEVEL_NOT_FOUND));
			BuildingUnit entity = new BuildingUnit();
			entity.setDescription(dto.getDescription());
			entity.setHasTenant(dto.getHasTenant());
			entity.setId(dto.getId());
			entity.setName(dto.getName());
			entity.setType(BuildingUnitType.from(dto.getType()));
			entity.setFloorLevel(f);
			entity.setDisplayName(dto.getDisplayName());
			entity.setUnit(dto.getUnit());
			buildingUnitRepository.save(entity);
		}
	}

	@Override
	public void search(PaginDto<BuildingUnitDto> pagin) {

		StringBuilder sqlBuilder = new StringBuilder();
		sqlBuilder.append("SELECT bu, bu.floorLevel.id, bu.floorLevel.building.id FROM BuildingUnit bu ");
		sqlBuilder.append(" where 1=1 ");
		if (StringUtils.isNotBlank(pagin.getKeyword())) {
			sqlBuilder.append(" and (name like '%" + pagin.getKeyword() + "%') ");
		}
		
		if (pagin.getOptions().get("floorLevelId") != null) {
			sqlBuilder.append(" AND bu.floorLevel.id = ").append(pagin.getOptions().get("floorLevelId"));
		}
		
		sqlBuilder.append(" ORDER BY modifyDate DESC ");

		Query q = em.createQuery(sqlBuilder.toString());
		q.setFirstResult(pagin.getOffset());
		q.setMaxResults(pagin.getLimit());

		StringBuilder sqlCountBuilder = new StringBuilder();
		sqlCountBuilder.append(" SELECT count(*) FROM BuildingUnit ");
		sqlCountBuilder.append(" where 1=1 ");
		if (StringUtils.isNotBlank(pagin.getKeyword())) {
			sqlCountBuilder.append(" and lower(name) like lower('%" + pagin.getKeyword() + "%') ");
		}
		
		if (pagin.getOptions().get("floorLevelId") != null) {
			sqlCountBuilder.append(" AND floorLevel.id = ").append(pagin.getOptions().get("floorLevelId"));
		}
		
		Query qCount = em.createQuery(sqlCountBuilder.toString());

		Number totalRows = (Number) qCount.getSingleResult();
		pagin.setTotalRows(totalRows.longValue());
		List<BuildingUnitDto> results = new ArrayList<>();

		@SuppressWarnings("unchecked")
		List<Object[]> list = q.getResultList();
		for (int i = 0; i < list.size(); i++) {
			BuildingUnitDto a = new BuildingUnitDto();
			Object[] os = list.get(i);
			BuildingUnit o = (BuildingUnit) os[0];
			Long flId = (Long) os[1];
			Long bId = (Long) os[2];
			
			FloorLevelDto f = new FloorLevelDto();
			f.setId(flId);
			
			BuildingDto b = new BuildingDto();
			b.setId(bId);
			f.setBuilding(b);
			
			a.setId(o.getId());
			a.setName(o.getName());
			a.setType(o.getType() == null ? null : o.getType().name());
			a.setDescription(o.getDescription());
			a.setHasTenant(o.getHasTenant());
			a.setCreatedDate(o.getCreateDate());
			a.setModifiedDate(o.getModifyDate());
			a.setFloorLevel(f);
			a.setUnit(o.getUnit());
			a.setDisplayName(o.getDisplayName());
			results.add(a);
		}
		pagin.setResults(results);
	}

	@Override
	public void delete(Long id) throws ApiException {
		BuildingUnit entity = buildingUnitRepository.findById(id)
				.orElseThrow(() -> new ApiException(ResponseEnum.BUILDING_NOT_FOUND));
		buildingUnitRepository.delete(entity);
		em.createNativeQuery("delete from {h-schema}floor_level where id = " + entity.getFloorLevel().getId())
				.executeUpdate();
	}

	@Override
	public void update(BuildingUnitDto dto) throws ApiException {

		BuildingUnit en = buildingUnitRepository.findById(dto.getId())
				.orElseThrow(() -> new ApiException(ResponseEnum.BUILDING_NOT_FOUND));

		if (dto.getFloorLevel() == null || dto.getFloorLevel().getId() == null) {
			throw new ApiException(ResponseEnum.FLOOR_LEVEL_NOT_FOUND);
		}
		FloorLevel f = floorLevelRepository.findById(dto.getFloorLevel().getId())
				.orElseThrow(() -> new ApiException(ResponseEnum.FLOOR_LEVEL_NOT_FOUND));

		en.setModifyDate(new Date());
		en.setDescription(dto.getDescription());
		en.setHasTenant(dto.getHasTenant());
		en.setName(dto.getName());
		en.setType(BuildingUnitType.from(dto.getType()));
		en.setUnit(dto.getUnit());
		en.setDisplayName(dto.getDisplayName());
		en.setFloorLevel(f);
		buildingUnitRepository.save(en);
	}
}
