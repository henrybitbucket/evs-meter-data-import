package com.pa.evs.sv.impl;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pa.evs.dto.AddressDto;
import com.pa.evs.dto.BuildingDto;
import com.pa.evs.dto.DMSLocationSiteDto;
import com.pa.evs.dto.DMSSiteDto;
import com.pa.evs.dto.DMSWorkOrdersDto;
import com.pa.evs.dto.GroupUserDto;
import com.pa.evs.dto.PaginDto;
import com.pa.evs.enums.ResponseEnum;
import com.pa.evs.exception.ApiException;
import com.pa.evs.model.DMSBlock;
import com.pa.evs.model.DMSBuilding;
import com.pa.evs.model.DMSBuildingUnit;
import com.pa.evs.model.DMSFloorLevel;
import com.pa.evs.model.DMSLocationSite;
import com.pa.evs.model.DMSSite;
import com.pa.evs.model.DMSWorkOrders;
import com.pa.evs.model.GroupUser;
import com.pa.evs.repository.DMSBlockRepository;
import com.pa.evs.repository.DMSBuildingRepository;
import com.pa.evs.repository.DMSBuildingUnitRepository;
import com.pa.evs.repository.DMSFloorLevelRepository;
import com.pa.evs.repository.DMSLocationSiteRepository;
import com.pa.evs.repository.DMSSiteRepository;
import com.pa.evs.repository.DMSWorkOrdersRepository;
import com.pa.evs.repository.GroupUserRepository;
import com.pa.evs.sv.DMSSiteService;
import com.pa.evs.utils.Utils;

@SuppressWarnings("rawtypes")
@Service
public class DMSSiteServiceImpl implements DMSSiteService {

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
	DMSSiteRepository dmsSiteRepository;
	
	@Autowired
	DMSWorkOrdersRepository dmsWorkOrdersRepository;
	
	@Autowired
	DMSLocationSiteRepository dmsLocationSiteRepository;
	
	@Autowired
	GroupUserRepository groupUserRepository;
	
	@Transactional
	@Override
	public void save(DMSSiteDto dto) {
		
		if (StringUtils.isBlank(dto.getLabel())) {
			throw new RuntimeException("Label is required!");
		}
		if (dto.getId() != null) {
			update(dto);
		} else {
			if (dmsSiteRepository.findByLabel(dto.getLabel().trim()).isPresent()) {
				throw new RuntimeException("Label exitst!");
			}
			dmsSiteRepository.save(
					DMSSite.builder()
					.label(dto.getLabel())
					.description(dto.getDescription())
					.remark(dto.getRemark())
					.lng(dto.getLng())
					.lat(dto.getLat())
					.radius(dto.getRadius())
					.build()
					);
		}
	}
	
	@Override
	public void search(PaginDto<DMSSiteDto> pagin) {

		if (pagin.getLimit() == null) {
			pagin.setLimit(10000);
		}
		if (pagin.getOffset() == null) {
			pagin.setOffset(0);
		}
		
		StringBuilder sqlBuilder = new StringBuilder(" select fl ");
		StringBuilder cmmBuilder = new StringBuilder(" FROM DMSSite fl where 1=1");
		if (pagin.getOptions().get("label") != null) {
			cmmBuilder.append(" AND upper(fl.label) like upper('%" + pagin.getOptions().get("label") + "%')");
		}
		
		if (pagin.getOptions().get("description") != null) {
			cmmBuilder.append(" AND upper(fl.description) like upper('%" + pagin.getOptions().get("description") + "%')");
		}
		
		sqlBuilder.append(cmmBuilder);
		sqlBuilder.append(" ORDER BY fl.modifyDate DESC ");
		
		Query q = em.createQuery(sqlBuilder.toString());
		q.setFirstResult(pagin.getOffset());
		q.setMaxResults(pagin.getLimit());
		
		StringBuilder sqlCountBuilder = new StringBuilder("SELECT COUNT(*) ");
		sqlCountBuilder.append(cmmBuilder);
		
		Query qCount = em.createQuery(sqlCountBuilder.toString());
		
		@SuppressWarnings("unchecked")
		List<DMSSite> list = q.getResultList();
		
		List<DMSSiteDto> dtos = new ArrayList<>();
		list.forEach(f -> {
			DMSSiteDto dto = new DMSSiteDto();
			dto.setId(f.getId());
			dto.setLabel(f.getLabel());
			dto.setDescription(f.getDescription());
			dto.setRadius(f.getRadius());
			dto.setLng(f.getLng());
			dto.setLat(f.getLat());
			dto.setRemark(f.getRemark());
			dto.setCreateDate(f.getCreateDate());
			dto.setModifyDate(f.getModifyDate());
			dtos.add(dto);
		});
		pagin.setResults(dtos);
		pagin.setTotalRows(((Number)qCount.getSingleResult()).longValue());
	}
	
	@Transactional
	@Override
	public void update(DMSSiteDto dto) {
		DMSSite entity = dmsSiteRepository.findById(dto.getId()).orElseThrow(() 
				-> new RuntimeException("site not found!"));
		
		if (!dto.getLabel().trim().equalsIgnoreCase(entity.getLabel()) && dmsSiteRepository.findByLabel(dto.getLabel()).isPresent()) {
			throw new RuntimeException("Label exitst!");
		}
		
		entity.setLabel(dto.getLabel());
		entity.setDescription(dto.getDescription());
		entity.setLng(dto.getLng());
		entity.setLat(dto.getLat());
		entity.setRadius(dto.getRadius());
		entity.setRemark(dto.getRemark());
		dmsSiteRepository.save(entity);
	}
	
	@Transactional
	@Override
	public void delete(Long id) {
		DMSSite entity = dmsSiteRepository.findById(id).orElseThrow(() -> new ApiException(ResponseEnum.FLOOR_LEVEL_NOT_FOUND));
		dmsSiteRepository.delete(entity);
	}
	
	@PostConstruct
	public void init() {
		if (!dmsSiteRepository.findByLabel("HENRY Site Test").isPresent()) {
			save(DMSSiteDto.builder()
					.label("HENRY Site Test")
					.radius("1234")
					.description("HENRY Site Test")
					.lng(new BigDecimal("103.98890742478571"))
					.lat(new BigDecimal("1.362897698151024"))
					.remark("HENRY Site Test")
					.build()
					);
			dmsSiteRepository.flush();
		}
		
		if (dmsWorkOrdersRepository.findBySiteLabel("HENRY Site Test").isEmpty()) {
			dmsWorkOrdersRepository.save(
					DMSWorkOrders.builder()
					.name("HENRY Site Test")
					.group(groupUserRepository.findByAppCodeName("DMS").get(0))
					.site(dmsSiteRepository.findByLabel("HENRY Site Test").get())
					.build()
			);
		}
		
//		if (!dmsLocationSiteRepository.findBySiteLabel("HENRY Site Test").isPresent()) {
//			dmsLocationSiteRepository.save(
//					DMSLocationSite.builder()
//					.building(buildingRepository.findById(1l).get())
//					.block(blockRepository.findById(1l).get())
//					.floorLevel(floorLevelRepository.findById(1l).get())
//					.buildingUnit(buildingUnitRepository.findById(1l).get())
//					.site(dmsSiteRepository.findByLabel("HENRY Site Test").get())
//					.build()
//			);
//		}		
		
	}

	@SuppressWarnings("unchecked")
	@Override
	public void searchWorkOrders(PaginDto pagin) {
		if (pagin.getLimit() == null) {
			pagin.setLimit(10000);
		}
		if (pagin.getOffset() == null) {
			pagin.setOffset(0);
		}
		
		StringBuilder sqlBuilder = new StringBuilder(" select fl ");
		StringBuilder cmmBuilder = new StringBuilder(" FROM DMSWorkOrders fl where 1=1");
		
		cmmBuilder.append(" AND fl.group.appCode.name = 'DMS' ");
		if (pagin.getOptions().get("siteId") != null) {
			cmmBuilder.append(" AND fl.site.id = " + pagin.getOptions().get("siteId") + " ");
		}
		
		sqlBuilder.append(cmmBuilder);
		sqlBuilder.append(" ORDER BY fl.modifyDate DESC ");
		
		Query q = em.createQuery(sqlBuilder.toString());
		q.setFirstResult(pagin.getOffset());
		q.setMaxResults(pagin.getLimit());
		
		StringBuilder sqlCountBuilder = new StringBuilder("SELECT COUNT(*) ");
		sqlCountBuilder.append(cmmBuilder);
		
		Query qCount = em.createQuery(sqlCountBuilder.toString());
		
		List<DMSWorkOrders> list = q.getResultList();
		
		List<DMSWorkOrdersDto> dtos = new ArrayList<>();
		list.forEach(wod -> {
			
			DMSWorkOrdersDto workOrdersDto = new DMSWorkOrdersDto();
			workOrdersDto.setId(wod.getId());
			workOrdersDto.setName(wod.getName());
			
			GroupUser f = wod.getGroup();
			GroupUserDto dto = new GroupUserDto();
			dto.setId(f.getId());
			dto.setName(f.getName());
			dto.setDescription(f.getDescription());
			workOrdersDto.setGroup(dto);
			
			dtos.add(workOrdersDto);
		});
		pagin.setResults(dtos);
		pagin.setTotalRows(((Number)qCount.getSingleResult()).longValue());
	}

	@SuppressWarnings("unchecked")
	@Override
	public void searchLocations(PaginDto pagin) {
		if (pagin.getLimit() == null) {
			pagin.setLimit(10000);
		}
		if (pagin.getOffset() == null) {
			pagin.setOffset(0);
		}
		
		StringBuilder sqlBuilder = new StringBuilder(" select fl ");
		StringBuilder cmmBuilder = new StringBuilder(" FROM DMSLocationSite fl where 1=1");
		
		if (pagin.getOptions().get("siteId") != null) {
			cmmBuilder.append(" AND fl.site.id = " + pagin.getOptions().get("siteId") + " ");
		}
		
		sqlBuilder.append(cmmBuilder);
		sqlBuilder.append(" ORDER BY fl.modifyDate DESC ");
		
		Query q = em.createQuery(sqlBuilder.toString());
		q.setFirstResult(pagin.getOffset());
		q.setMaxResults(pagin.getLimit());
		
		StringBuilder sqlCountBuilder = new StringBuilder("SELECT COUNT(*) ");
		sqlCountBuilder.append(cmmBuilder);
		
		Query qCount = em.createQuery(sqlCountBuilder.toString());
		
		List<DMSLocationSite> list = q.getResultList();
		
		List<BuildingDto> dtos = new ArrayList<>();
		list.forEach(wod -> {
			
			DMSBuilding building = wod.getBuilding();
			BuildingDto a = new BuildingDto();
			a.setId(building.getId());
			a.setName(building.getName());
			// a.setType(building.getType());
			a.setDescription(building.getDescription());
			a.setHasTenant(building.getHasTenant());
			a.setCreatedDate(building.getCreateDate());
			a.setModifiedDate(building.getModifyDate());
			
			AddressDto address = new AddressDto();
			address.setId(building.getAddress().getId());
			address.setCountry(building.getAddress().getCountry());
			address.setBuilding(building.getName());
			address.setCity(building.getAddress().getCity());
			address.setTown(building.getAddress().getTown());
			address.setStreet(building.getAddress().getStreet());
			address.setDisplayName(building.getAddress().getDisplayName());
			address.setPostalCode(building.getAddress().getPostalCode());
			
			address.setBlock(wod.getBlock().getName());
			address.setLevel(wod.getFloorLevel().getName());
			address.setLevelId(wod.getFloorLevel().getId());
			
			address.setUnitId(wod.getBuildingUnit().getId());
			address.setUnitNumber(wod.getBuildingUnit().getName());
			address.setRemark(wod.getBuildingUnit().getRemark());
			address.setLocationTag(wod.getBuildingUnit().getLocationTag());
			
			a.setAddress(address);
			a.setLabel(Utils.formatHomeAddress(a.getName(), address));
			
			a.setLocationSiteId(wod.getId());
			dtos.add(a);
		});
		pagin.setResults(dtos);
		pagin.setTotalRows(((Number)qCount.getSingleResult()).longValue());
	}

	@Transactional
	@Override
	public void linkLocation(DMSLocationSiteDto dto) {
		
		DMSBuildingUnit unit = buildingUnitRepository.findById(dto.getBuildingUnitId()).orElseThrow(() -> new RuntimeException("Building unit is required"));
		
		DMSFloorLevel level = unit.getFloorLevel();
		
		if (!level.getId().equals(dto.getFloorLevelId())) {
			throw new RuntimeException("FloorLevel invalid!");
		}

		DMSBlock block = level.getBlock();
		if (!block.getId().equals(dto.getBlockId())) {
			throw new RuntimeException("Block invalid!");
		}
		
		DMSBuilding building = block.getBuilding();
		if (!building.getId().equals(dto.getBuildingId())) {
			throw new RuntimeException("Building invalid!");
		}
		
		String locationKey = unit.getId() + "__" + level.getId() + "__" + block.getId() + "__" + building.getId();
		
		if (dmsLocationSiteRepository.findBySiteIdAndLocationKey(dto.getSiteId(), locationKey).isPresent()) {
			throw new RuntimeException("Location already exists in this site!");
		}
		
		dmsLocationSiteRepository.save(
				DMSLocationSite.builder()
				.building(building)
				.block(block)
				.floorLevel(level)
				.buildingUnit(unit)
				.locationKey(locationKey)
				.site(dmsSiteRepository.findById(dto.getSiteId()).orElseThrow(() -> new RuntimeException("Site not found!")))
				.build()
		);
	}

	@Transactional
	@Override
	public void unLinkLocation(Long linkSiteLocationId) {
		dmsLocationSiteRepository.deleteById(linkSiteLocationId);
	}
	
}
