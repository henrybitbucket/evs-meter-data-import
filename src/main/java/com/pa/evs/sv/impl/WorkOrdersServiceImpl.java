package com.pa.evs.sv.impl;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pa.evs.dto.DMSApplicationDto;
import com.pa.evs.dto.DMSProjectDto;
import com.pa.evs.dto.DMSSiteDto;
import com.pa.evs.dto.DMSWorkOrdersDto;
import com.pa.evs.dto.PaginDto;
import com.pa.evs.model.DMSApplication;
import com.pa.evs.model.DMSApplicationSite;
import com.pa.evs.model.DMSSite;
import com.pa.evs.model.DMSWorkOrders;
import com.pa.evs.repository.DMSApplicationRepository;
import com.pa.evs.repository.DMSBlockRepository;
import com.pa.evs.repository.DMSBuildingRepository;
import com.pa.evs.repository.DMSBuildingUnitRepository;
import com.pa.evs.repository.DMSFloorLevelRepository;
import com.pa.evs.repository.DMSLocationSiteRepository;
import com.pa.evs.repository.DMSSiteRepository;
import com.pa.evs.repository.DMSWorkOrdersRepository;
import com.pa.evs.repository.GroupUserRepository;
import com.pa.evs.sv.WorkOrdersService;

@Service
public class WorkOrdersServiceImpl implements WorkOrdersService {

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
	
	@Autowired
	DMSApplicationRepository dmsApplicationRepository;
	
	@Transactional
	@Override
	public DMSWorkOrders save(DMSWorkOrdersDto dto) {
		
		if (StringUtils.isBlank(dto.getName())) {
			throw new RuntimeException("Name is required!");
		}
		if (dto.getId() != null) {
			return update(dto);
		} else {
			if (dmsWorkOrdersRepository.findByName(dto.getName().trim()).isPresent()) {
				throw new RuntimeException("Name exitst!");
			}
//			Optional<DMSWorkOrders> opt = dmsWorkOrdersRepository.findByGroupIdAndSiteId(dto.getGroup().getId(), dto.getSite().getId());
//			if (opt.isPresent()) {
//				throw new RuntimeException("Work order with group and site exist!");
//			}
			return dmsWorkOrdersRepository.save(
					DMSWorkOrders.builder()
					.name(dto.getName())
					.status("WAITING_APPROVAL")
					.timePeriodDatesIsAlways(dto.isTimePeriodDatesIsAlways())
					.timePeriodDatesStart(dto.getTimePeriodDatesStart())
					.timePeriodDatesEnd(dto.getTimePeriodDatesEnd())
					.timePeriodDayInWeeksIsAlways(dto.isTimePeriodDayInWeeksIsAlways())
					.timePeriodDayInWeeksIsMon(dto.isTimePeriodDayInWeeksIsMon())
					.timePeriodDayInWeeksIsTue(dto.isTimePeriodDayInWeeksIsTue())
					.timePeriodDayInWeeksIsWed(dto.isTimePeriodDayInWeeksIsWed())
					.timePeriodDayInWeeksIsThu(dto.isTimePeriodDayInWeeksIsThu())
					.timePeriodDayInWeeksIsFri(dto.isTimePeriodDayInWeeksIsFri())
					.timePeriodDayInWeeksIsSat(dto.isTimePeriodDayInWeeksIsSat())
					.timePeriodDayInWeeksIsSun(dto.isTimePeriodDayInWeeksIsSun())
					
					.timePeriodTimeInDayIsAlways(dto.isTimePeriodTimeInDayIsAlways())
					.timePeriodTimeInDayHourStart(dto.getTimePeriodTimeInDayHourStart())
					.timePeriodTimeInDayHourEnd(dto.getTimePeriodTimeInDayHourEnd())
					.timePeriodTimeInDayMinuteStart(dto.getTimePeriodTimeInDayMinuteStart())
					.timePeriodTimeInDayMinuteEnd(dto.getTimePeriodTimeInDayMinuteEnd())
					//.group(groupUserRepository.findById(dto.getGroup().getId()).orElseThrow(() -> new RuntimeException("Group is reuired!")))
					.site(dto.getSite() == null ? null : dmsSiteRepository.findById(dto.getSite().getId()).orElse(null)) // all site
//                    .appicationEmail(dto.getApplicationEmail())
					.app(dmsApplicationRepository.findById(dto.getApplicationId()).orElse(null))
					.build()
					);
		}
	}
	
	@Override
	public void search(PaginDto<DMSWorkOrdersDto> pagin) {

		if (pagin.getLimit() == null) {
			pagin.setLimit(10000);
		}
		if (pagin.getOffset() == null) {
			pagin.setOffset(0);
		}
		
		StringBuilder sqlBuilder = new StringBuilder(" select fl ");
		StringBuilder cmmBuilder = new StringBuilder(" FROM DMSApplicationSite fl where fl.workOrder is not null and app.status = 'APPROVAL' ");
		
//		cmmBuilder.append(" AND fl.group.appCode.name = 'DMS' ");
		if (pagin.getOptions().get("name") != null) {
			cmmBuilder.append(" AND upper(fl.workOrder.name) like upper('%" + pagin.getOptions().get("name") + "%')");
		}
		
		if (pagin.getOptions().get("siteId") != null) {
			cmmBuilder.append(" AND fl.site.id = " + pagin.getOptions().get("siteId") + " ");
		}
		
		sqlBuilder.append(cmmBuilder);
		sqlBuilder.append(" ORDER BY fl.workOrder.modifyDate DESC ");
		
		Query q = em.createQuery(sqlBuilder.toString());
		q.setFirstResult(pagin.getOffset());
		q.setMaxResults(pagin.getLimit());
		
		StringBuilder sqlCountBuilder = new StringBuilder("SELECT COUNT(*) ");
		sqlCountBuilder.append(cmmBuilder);
		
		Query qCount = em.createQuery(sqlCountBuilder.toString());
		
		@SuppressWarnings("unchecked")
		List<DMSApplicationSite> list = q.getResultList();
		
		List<DMSWorkOrdersDto> dtos = new ArrayList<>();
		list.forEach(as -> {
			DMSWorkOrders f = as.getWorkOrder();
			DMSWorkOrdersDto dto = new DMSWorkOrdersDto();
			dto.setId(f.getId());
			dto.setName(f.getName());
			dto.setStatus(f.getStatus());
			dto.setTimePeriodDatesIsAlways(f.isTimePeriodDatesIsAlways());
			dto.setTimePeriodDatesStart(f.getTimePeriodDatesStart());
			dto.setTimePeriodDatesEnd(f.getTimePeriodDatesEnd());
			dto.setTimePeriodDayInWeeksIsAlways(f.isTimePeriodDayInWeeksIsAlways());
			dto.setTimePeriodDayInWeeksIsMon(f.isTimePeriodDayInWeeksIsMon());
			dto.setTimePeriodDayInWeeksIsTue(f.isTimePeriodDayInWeeksIsTue());
			dto.setTimePeriodDayInWeeksIsWed(f.isTimePeriodDayInWeeksIsWed());
			dto.setTimePeriodDayInWeeksIsThu(f.isTimePeriodDayInWeeksIsThu());
			dto.setTimePeriodDayInWeeksIsFri(f.isTimePeriodDayInWeeksIsFri());
			dto.setTimePeriodDayInWeeksIsSat(f.isTimePeriodDayInWeeksIsSat());
			dto.setTimePeriodDayInWeeksIsSun(f.isTimePeriodDayInWeeksIsSun());
			
			dto.setTimePeriodTimeInDayIsAlways(f.isTimePeriodTimeInDayIsAlways());
			dto.setTimePeriodTimeInDayHourStart(f.getTimePeriodTimeInDayHourStart() == null ? 0 : f.getTimePeriodTimeInDayHourStart());
			dto.setTimePeriodTimeInDayHourEnd(f.getTimePeriodTimeInDayHourEnd() == null ? 0 : f.getTimePeriodTimeInDayHourEnd());
			dto.setTimePeriodTimeInDayMinuteStart(f.getTimePeriodTimeInDayMinuteStart() == null ? 0 : f.getTimePeriodTimeInDayMinuteStart());
			dto.setTimePeriodTimeInDayMinuteEnd(f.getTimePeriodTimeInDayMinuteEnd() == null ? 0 : f.getTimePeriodTimeInDayMinuteEnd());

			DMSSite dmsSite = as.getSite();
			DMSSiteDto dmsSiteDto = new DMSSiteDto();
			dmsSiteDto.setId(dmsSite.getId());
			dmsSiteDto.setLabel(dmsSite.getLabel());
			dmsSiteDto.setDescription(dmsSite.getDescription());
			dmsSiteDto.setRemark(dmsSite.getRemark());
			dto.setSite(dmsSiteDto);

			DMSApplication app = as.getApp();
			DMSApplicationDto appDto = new DMSApplicationDto();
			
			appDto.setId(app.getId());
			appDto.setName(app.getName());
			appDto.setApprovalBy(app.getApprovalBy());
			appDto.setCreatedBy(app.getCreatedBy());
			appDto.setRejectBy(app.getRejectBy());
			appDto.setStatus(app.getStatus());
			appDto.setIsGuest(app.getIsGuest());
			appDto.setProject(new DMSProjectDto());
			appDto.getProject().setId(app.getProject().getId());
			appDto.getProject().setName(app.getProject().getName());
			appDto.getProject().setDisplayName(app.getProject().getDisplayName());
			
			dto.setApp(appDto);
			dtos.add(dto);
		});
		pagin.setResults(dtos);
		pagin.setTotalRows(((Number)qCount.getSingleResult()).longValue());
	}
	
	@Transactional
	@Override
	public DMSWorkOrders update(DMSWorkOrdersDto dto) {
		DMSWorkOrders entity = dmsWorkOrdersRepository.findById(dto.getId()).orElseThrow(() 
				-> new RuntimeException("work order not found!"));
		
		if (!dto.getName().trim().equalsIgnoreCase(entity.getName()) && dmsWorkOrdersRepository.findByName(dto.getName()).isPresent()) {
			throw new RuntimeException("Name exitst!");
		}
		
//		Optional<DMSWorkOrders> opt = dmsWorkOrdersRepository.findByGroupIdAndSiteId(dto.getGroup().getId(), dto.getSite().getId());
//		if (opt.isPresent() && opt.get().getId().longValue() != entity.getId().longValue()) {
//			throw new RuntimeException("Work order with group and site exitst!");
//		}
		
		entity.setName(dto.getName());
		entity.setTimePeriodDatesIsAlways(dto.isTimePeriodDatesIsAlways());
		entity.setTimePeriodDatesStart(dto.getTimePeriodDatesStart());
		entity.setTimePeriodDatesEnd(dto.getTimePeriodDatesEnd());
		entity.setTimePeriodDayInWeeksIsAlways(dto.isTimePeriodDayInWeeksIsAlways());
		entity.setTimePeriodDayInWeeksIsMon(dto.isTimePeriodDayInWeeksIsMon());
		entity.setTimePeriodDayInWeeksIsTue(dto.isTimePeriodDayInWeeksIsTue());
		entity.setTimePeriodDayInWeeksIsWed(dto.isTimePeriodDayInWeeksIsWed());
		entity.setTimePeriodDayInWeeksIsThu(dto.isTimePeriodDayInWeeksIsThu());
		entity.setTimePeriodDayInWeeksIsFri(dto.isTimePeriodDayInWeeksIsFri());
		entity.setTimePeriodDayInWeeksIsSat(dto.isTimePeriodDayInWeeksIsSat());
		entity.setTimePeriodDayInWeeksIsSun(dto.isTimePeriodDayInWeeksIsSun());
		
		entity.setTimePeriodTimeInDayIsAlways(dto.isTimePeriodTimeInDayIsAlways());
		entity.setTimePeriodTimeInDayHourStart(dto.getTimePeriodTimeInDayHourStart());
		entity.setTimePeriodTimeInDayHourEnd(dto.getTimePeriodTimeInDayHourEnd());
		entity.setTimePeriodTimeInDayMinuteStart(dto.getTimePeriodTimeInDayMinuteStart());
		entity.setTimePeriodTimeInDayMinuteEnd(dto.getTimePeriodTimeInDayMinuteEnd());
		// entity.setGroup(groupUserRepository.findById(dto.getGroup().getId()).orElseThrow(() -> new RuntimeException("Group is reuired!")));
		entity.setSite(dmsSiteRepository.findById(dto.getSite().getId()).orElseThrow(() -> new RuntimeException("Site is reuired!")));
		return dmsWorkOrdersRepository.save(entity);
	}
	
	@Transactional
	@Override
	public void delete(Long id) {
		DMSWorkOrders entity = dmsWorkOrdersRepository.findById(id).orElseThrow(() -> new RuntimeException("Work order not found!"));
		dmsWorkOrdersRepository.delete(entity);
	}
}
