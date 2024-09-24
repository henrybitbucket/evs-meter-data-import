package com.pa.evs.sv.impl;

import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pa.evs.dto.ApplicationRequestDto;
import com.pa.evs.dto.CreateDMSAppUserDto;
import com.pa.evs.dto.DMSApplicationDto;
import com.pa.evs.dto.DMSApplicationGuestSaveReqDto;
import com.pa.evs.dto.DMSApplicationSaveReqDto;
import com.pa.evs.dto.DMSApplicationSiteItemReqDto;
import com.pa.evs.dto.DMSApplicationUserGuestReqDto;
import com.pa.evs.dto.DMSProjectDto;
import com.pa.evs.dto.DMSSiteDto;
import com.pa.evs.dto.DMSTimePeriodDto;
import com.pa.evs.dto.DMSWorkOrdersDto;
import com.pa.evs.dto.LoginRequestDto;
import com.pa.evs.dto.LoginResponseDto;
import com.pa.evs.dto.PaginDto;
import com.pa.evs.dto.ResponseDto;
import com.pa.evs.dto.UserDto;
import com.pa.evs.exception.ApiException;
import com.pa.evs.model.DMSApplication;
import com.pa.evs.model.DMSApplicationHistory;
import com.pa.evs.model.DMSApplicationSite;
import com.pa.evs.model.DMSApplicationUser;
import com.pa.evs.model.DMSProject;
import com.pa.evs.model.DMSProjectPicUser;
import com.pa.evs.model.DMSProjectSite;
import com.pa.evs.model.DMSSite;
import com.pa.evs.model.DMSWorkOrders;
import com.pa.evs.model.OTP;
import com.pa.evs.model.Users;
import com.pa.evs.repository.DMSApplicationHistoryRepository;
import com.pa.evs.repository.DMSApplicationRepository;
import com.pa.evs.repository.DMSApplicationSiteRepository;
import com.pa.evs.repository.DMSApplicationUserRepository;
import com.pa.evs.repository.DMSBlockRepository;
import com.pa.evs.repository.DMSBuildingRepository;
import com.pa.evs.repository.DMSBuildingUnitRepository;
import com.pa.evs.repository.DMSFloorLevelRepository;
import com.pa.evs.repository.DMSLocationSiteRepository;
import com.pa.evs.repository.DMSProjectPicUserRepository;
import com.pa.evs.repository.DMSProjectRepository;
import com.pa.evs.repository.DMSProjectSiteRepository;
import com.pa.evs.repository.DMSSiteRepository;
import com.pa.evs.repository.DMSWorkOrdersRepository;
import com.pa.evs.repository.GroupUserRepository;
import com.pa.evs.repository.PlatformUserLoginRepository;
import com.pa.evs.repository.UserRepository;
import com.pa.evs.security.jwt.JwtTokenUtil;
import com.pa.evs.security.user.JwtUser;
import com.pa.evs.sv.AuthenticationService;
import com.pa.evs.sv.DMSProjectService;
import com.pa.evs.sv.NotificationService;
import com.pa.evs.sv.WorkOrdersService;
import com.pa.evs.utils.ApiUtils;
import com.pa.evs.utils.AppCodeSelectedHolder;
import com.pa.evs.utils.AppProps;
import com.pa.evs.utils.SchedulerHelper;
import com.pa.evs.utils.SecurityUtils;
import com.pa.evs.utils.SimpleMap;

import io.jsonwebtoken.Claims;

@SuppressWarnings({"rawtypes", "unchecked"})
@Service
public class DMSProjectServiceImpl implements DMSProjectService {

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
	DMSProjectRepository dmsProjectRepository;
	
	@Autowired
	DMSWorkOrdersRepository dmsWorkOrdersRepository;
	
	@Autowired
	DMSLocationSiteRepository dmsLocationSiteRepository;
	
	@Autowired
	GroupUserRepository groupUserRepository;
	
	@Autowired
	DMSProjectPicUserRepository dmsProjectPicUserRepository;
	
	@Autowired
	DMSProjectSiteRepository dmsProjectSiteRepository;
	
	@Autowired
	DMSSiteRepository dmsSiteRepository;
	
	@Autowired
	DMSApplicationRepository dmsApplicationRepository;
	
	@Autowired
	DMSApplicationHistoryRepository dmsApplicationHistoryRepository;
	
	@Autowired
	DMSApplicationSiteRepository dmsApplicationSiteRepository;
	
	@Autowired
	DMSApplicationUserRepository dmsApplicationUserRepository;
	
	@Autowired
	WorkOrdersService workOrdersService;
	
	@Autowired
	UserRepository userRepository;
	
	@Autowired
	AuthenticationService authenticationService;
	
	@Autowired
	NotificationService notificationService;
	
	@Autowired
	PlatformUserLoginRepository platformUserLoginRepository;
	
	@Autowired
	PasswordEncoder passwordEncoder;
	
	ObjectMapper mapper = new ObjectMapper();
	
	@Transactional
	@Override
	public synchronized void save(DMSProjectDto dto) {
		
		if (StringUtils.isBlank(dto.getName())) {
			throw new RuntimeException("Name is required!");
		}
		if (dto.getId() != null) {
			update(dto);
		} else {
			if (dmsProjectRepository.findByName(dto.getName().trim()).isPresent()) {
				throw new RuntimeException("name exitst!");
			}
			SimpleDateFormat sf = new SimpleDateFormat("yyMMdd-hhmmssSSS");
			Random random = new Random();
			// "ptwp-240315-35678"
			DMSProject entity = dmsProjectRepository.save(
					DMSProject.builder()
					.name(dto.getName())
					.displayName("ptwp-" + sf.format(new Date()) + "" + random.nextInt(9))
					.start(dto.getStart() == null ? System.currentTimeMillis() : dto.getStart())
					.end(dto.getEnd() == null ? System.currentTimeMillis() : dto.getEnd())
					.build()
					);
			dmsProjectRepository.flush();
			Optional<DMSProjectPicUser> currentPicUserOpt = findPicUserByProjectId(entity.getId());
			if (StringUtils.isBlank(dto.getPicUser()) && currentPicUserOpt.isPresent()) {
				dmsProjectPicUserRepository.delete(currentPicUserOpt.get());
			}
			if (StringUtils.isNotBlank(dto.getPicUser()) && (!currentPicUserOpt.isPresent() || !dto.getPicUser().equalsIgnoreCase(currentPicUserOpt.get().getPicUser().getEmail()))) {
				linkPicUser(dto.getPicUser(), entity.getId());
			}
			linkSubPicUsers(dto.getSubPicUsers(), entity.getId());
		}
	}
	
	@Override
	public void search(PaginDto<DMSProjectDto> pagin) {

		if (pagin.getLimit() == null) {
			pagin.setLimit(10000);
		}
		if (pagin.getOffset() == null) {
			pagin.setOffset(0);
		}
		
		StringBuilder sqlBuilder = new StringBuilder(" select fl, picUser ");
		
		StringBuilder cmmBuilder = new StringBuilder(" FROM DMSProject fl left join DMSProjectPicUser picUser on (picUser.isSubPic = false and picUser.project.id = fl.id) where 1=1");
		if (pagin.getOptions().get("queryName") != null) {
			cmmBuilder.append(" AND upper(fl.name) like upper('%" + pagin.getOptions().get("queryName") + "%')");
		}
		
		if (pagin.getOptions().get("queryDisplayName") != null) {
			cmmBuilder.append(" AND upper(fl.displayName) like upper('%" + pagin.getOptions().get("queryDisplayName") + "%')");
		}
		
		if (pagin.getOptions().get("projectId") != null) {
			cmmBuilder.append(" AND fl.id = " + pagin.getOptions().get("projectId") + " ");
		}
		
		sqlBuilder.append(cmmBuilder);
		sqlBuilder.append(" ORDER BY fl.modifyDate DESC ");
		
		Query q = em.createQuery(sqlBuilder.toString());
		q.setFirstResult(pagin.getOffset());
		q.setMaxResults(pagin.getLimit());
		
		StringBuilder sqlCountBuilder = new StringBuilder("SELECT COUNT(fl) ");
		sqlCountBuilder.append(cmmBuilder);
		
		Query qCount = em.createQuery(sqlCountBuilder.toString());
		
		List<Object[]> list = q.getResultList();
		
		List<DMSProjectDto> dtos = new ArrayList<>();
		list.forEach(obj -> {
			DMSProject f = (DMSProject) obj[0];
			DMSProjectPicUser user = (DMSProjectPicUser) obj[1];
			DMSProjectDto dto = new DMSProjectDto();
			dto.setId(f.getId());
			dto.setName(f.getName());
			dto.setDisplayName(f.getDisplayName());
			dto.setStart(f.getStart());
			dto.setEnd(f.getEnd());
			dto.setCreateDate(f.getCreateDate());
			dto.setModifyDate(f.getModifyDate());
			if (user != null && user.getPicUser() != null) {
				dto.setPicUser(user.getPicUser().getEmail());
			}
			dtos.add(dto);
		});
		
		if (pagin.getOptions().get("projectId") != null && dtos.size() == 1) {
			List<DMSProjectPicUser> subs = findSubPicUsersByProjectId(Long.parseLong(pagin.getOptions().get("projectId") + ""));
			subs.forEach(s -> dtos.get(0).getSubPicUsers().add(s.getPicUser().getEmail()));
		}
		
		pagin.setResults(dtos);
		pagin.setTotalRows(((Number)qCount.getSingleResult()).longValue());
	}
	
	@Transactional
	@Override
	public void update(DMSProjectDto dto) {
		DMSProject entity = dmsProjectRepository.findById(dto.getId()).orElseThrow(() 
				-> new RuntimeException("project not found!"));
		
		if (!dto.getName().trim().equalsIgnoreCase(entity.getName()) && dmsProjectRepository.findByName(dto.getName()).isPresent()) {
			throw new RuntimeException("Name exitst!");
		}
		
		entity.setName(dto.getName());
		if (dto.getStart() != null) {
			entity.setStart(dto.getStart());	
		}
		
		if (dto.getEnd() != null) {
			entity.setEnd(dto.getEnd());	
		}
		
		dmsProjectRepository.save(entity);
		dmsProjectRepository.flush();
		Optional<DMSProjectPicUser> currentPicUserOpt = findPicUserByProjectId(entity.getId());
		if (StringUtils.isBlank(dto.getPicUser()) && currentPicUserOpt.isPresent()) {
			dmsProjectPicUserRepository.delete(currentPicUserOpt.get());
		}
		if (StringUtils.isNotBlank(dto.getPicUser()) && (!currentPicUserOpt.isPresent() || !dto.getPicUser().equalsIgnoreCase(currentPicUserOpt.get().getPicUser().getEmail()))) {
			linkPicUser(dto.getPicUser(), entity.getId());
		}
		linkSubPicUsers(dto.getSubPicUsers(), entity.getId());
	}
	
	@Transactional
	@Override
	public void delete(Long id) {
		DMSProject entity = dmsProjectRepository.findById(id).orElseThrow(() -> new ApiException("Project not found"));
		
		Optional<DMSProjectPicUser> picUserOpt = findPicUserByProjectId(entity.getId());
		if (picUserOpt.isPresent()) {
			dmsProjectPicUserRepository.delete(picUserOpt.get());
		}
		findSubPicUsersByProjectId(entity.getId())
		.forEach(dmsProjectPicUserRepository::delete);
		dmsProjectPicUserRepository.flush();
		
		dmsApplicationRepository.findByProjectId(id)
		.forEach(a -> {
			a.setProjectName(entity.getName() + " - " + entity.getDisplayName());
			a.setProject(null);
			a.setStatus("DELETED");
			dmsApplicationRepository.save(a);
		});
		dmsApplicationRepository.flush();
		
		dmsProjectSiteRepository.findByProjectId(id)
		.forEach(dmsProjectSiteRepository::delete);
		dmsProjectSiteRepository.flush();
		
		dmsProjectRepository.delete(entity);
	}

	@Override
	public void searchPicUsers(PaginDto pagin) {
		if (pagin.getLimit() == null) {
			pagin.setLimit(10000);
		}
		if (pagin.getOffset() == null) {
			pagin.setOffset(0);
		}
		
		StringBuilder sqlBuilder = new StringBuilder(" select fl ");
		
		StringBuilder cmmBuilder = new StringBuilder(" FROM DMSSite fl where 1=1");
		
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
			
//			GroupUser f = wod.getGroup();
//			GroupUserDto dto = new GroupUserDto();
//			dto.setId(f.getId());
//			dto.setName(f.getName());
//			dto.setDescription(f.getDescription());
//			workOrdersDto.setGroup(dto);
			
			dtos.add(workOrdersDto);
		});
		pagin.setResults(dtos);
		pagin.setTotalRows(((Number)qCount.getSingleResult()).longValue());
	}

	@Override
	public void searchApplications(PaginDto pagin) {
		if (pagin.getLimit() == null) {
			pagin.setLimit(10000);
		}
		if (pagin.getOffset() == null) {
			pagin.setOffset(0);
		}
		
		StringBuilder sqlBuilder = new StringBuilder(" select fl ");
		StringBuilder cmmBuilder = new StringBuilder(" FROM DMSApplication fl where 1=1 and fl.status <> 'DELETED'");
		
		if (pagin.getOptions().get("queryName") != null) {
			cmmBuilder.append(" AND upper(fl.name) like upper('%" + pagin.getOptions().get("queryName") + "%') ");
		}
		
		if (pagin.getOptions().get("queryCreatedBy") != null) {
			cmmBuilder.append(" AND upper(fl.createdBy) like upper('%" + pagin.getOptions().get("queryCreatedBy") + "%') ");
		}
		
		if (pagin.getOptions().get("projectId") != null) {
			cmmBuilder.append(" AND fl.project.id = " + pagin.getOptions().get("projectId") + " ");
		}
		
		if (pagin.getOptions().get("applicationId") != null) {
			cmmBuilder.append(" AND fl.id = " + pagin.getOptions().get("applicationId") + " ");
		}	
		
		if (pagin.getOptions().get("applicationId") == null || !SecurityUtils.hasAnyRole("DMS_SUPER_ADMIN")) {
			if (!SecurityUtils.hasAnyRole("DMS_R_APPROVE_APPLICATION", "DMS_R_REJECT_APPLICATION")) {
				cmmBuilder.append(" AND fl.createdBy = '" + SecurityUtils.getPhoneNumber() + "' ");
			} else {
				cmmBuilder.append(" AND (fl.createdBy = '" + SecurityUtils.getPhoneNumber() + "' OR  exists (select 1 from DMSProjectPicUser pic where pic.project.id = fl.project.id and pic.picUser.email = '" + SecurityUtils.getEmail() + "')) ");	
			}			
		}
		
		sqlBuilder.append(cmmBuilder);
		sqlBuilder.append(" ORDER BY fl.modifyDate DESC ");
		
		Query q = em.createQuery(sqlBuilder.toString());
		q.setFirstResult(pagin.getOffset());
		q.setMaxResults(pagin.getLimit());
		
		StringBuilder sqlCountBuilder = new StringBuilder("SELECT COUNT(*) ");
		sqlCountBuilder.append(cmmBuilder);
		
		Query qCount = em.createQuery(sqlCountBuilder.toString());
		
		List<DMSApplication> list = q.getResultList();
		
		List<DMSApplicationDto> dtos = new ArrayList<>();
		
		list.forEach(it -> {
			DMSApplicationDto dto = new DMSApplicationDto();
			dto.setId(it.getId());
			dto.setName(it.getName());
			dto.setApprovalBy(it.getApprovalBy());
			dto.setCreatedBy(it.getCreatedBy());
			dto.setRejectBy(it.getRejectBy());
			dto.setStatus(it.getStatus());
			dto.setIsGuest(it.getIsGuest());
			dto.setProject(new DMSProjectDto());
			dto.getProject().setId(it.getProject().getId());
			dto.getProject().setName(it.getProject().getName());
			dto.getProject().setDisplayName(it.getProject().getDisplayName());
			dto.setCreateDate(it.getCreateDate());
			dto.setTimeTerminate(it.getTimeTerminate());
			dto.setTerminatedBy("TERMINATED".equalsIgnoreCase(it.getStatus()) ? (StringUtils.isBlank(it.getTerminatedBy()) ? "SYSTEM" : it.getTerminatedBy()) : null);
			
			dto.setTimePeriod(DMSTimePeriodDto.builder()
					.override(true)
					.timePeriodDatesIsAlways(it.isTimePeriodDatesIsAlways())
					.timePeriodDatesStart(it.getTimePeriodDatesStart())
					.timePeriodDatesEnd(it.getTimePeriodDatesEnd())
					.timePeriodDayInWeeksIsAlways(it.isTimePeriodDayInWeeksIsAlways())
					.timePeriodDayInWeeksIsMon(it.isTimePeriodDayInWeeksIsMon())
					.timePeriodDayInWeeksIsTue(it.isTimePeriodDayInWeeksIsTue())
					.timePeriodDayInWeeksIsWed(it.isTimePeriodDayInWeeksIsWed())
					.timePeriodDayInWeeksIsThu(it.isTimePeriodDayInWeeksIsThu())
					.timePeriodDayInWeeksIsFri(it.isTimePeriodDayInWeeksIsFri())
					.timePeriodDayInWeeksIsSat(it.isTimePeriodDayInWeeksIsSat())
					.timePeriodDayInWeeksIsSun(it.isTimePeriodDayInWeeksIsSun())
					.timePeriodTimeInDayIsAlways(it.isTimePeriodTimeInDayIsAlways())
					.timePeriodTimeInDayHourStart(it.getTimePeriodTimeInDayHourStart())
					.timePeriodTimeInDayHourEnd(it.getTimePeriodTimeInDayHourEnd())
					.timePeriodTimeInDayMinuteStart(it.getTimePeriodTimeInDayMinuteStart())
					.timePeriodTimeInDayMinuteEnd(it.getTimePeriodTimeInDayMinuteEnd())
					.build());
			
			dtos.add(dto);
		});
		
		
		if (dtos.size() == 1 && pagin.getOptions().get("applicationId") != null) {
			
			List<DMSApplicationHistory> applicationHis = em.createQuery("FROM DMSApplicationHistory where app.id = " + pagin.getOptions().get("applicationId") + " order by id desc ").getResultList();
			dtos.get(0).setAllHis(applicationHis);
			
			boolean isPIC = SecurityUtils.hasAnyRole("DMS_R_APPROVE_APPLICATION") && findPicUserOrSubPicUsersByProjectId(dtos.get(0).getProject().getId(), SecurityUtils.getEmail()) != null;
			dtos.get(0).setCurrentUserIsPICUser(isPIC);
			
			List<DMSApplicationSite> applicationSites = em.createQuery("FROM DMSApplicationSite where app.id = " + pagin.getOptions().get("applicationId")).getResultList();
			
			for (DMSApplicationSite appSite : applicationSites) {
				DMSSite site = appSite.getSite();
				if (site != null) {
					dtos.get(0).getSites().add(DMSTimePeriodDto.builder()
							.override(appSite.isOverrideTimePeriod())
							.siteId(site.getId())
							.siteLabel(site.getLabel())
							.timePeriodDatesIsAlways(appSite.isTimePeriodDatesIsAlways())
							.timePeriodDatesStart(appSite.getTimePeriodDatesStart())
							.timePeriodDatesEnd(appSite.getTimePeriodDatesEnd())
							.timePeriodDayInWeeksIsAlways(appSite.isTimePeriodDayInWeeksIsAlways())
							.timePeriodDayInWeeksIsMon(appSite.isTimePeriodDayInWeeksIsMon())
							.timePeriodDayInWeeksIsTue(appSite.isTimePeriodDayInWeeksIsTue())
							.timePeriodDayInWeeksIsWed(appSite.isTimePeriodDayInWeeksIsWed())
							.timePeriodDayInWeeksIsThu(appSite.isTimePeriodDayInWeeksIsThu())
							.timePeriodDayInWeeksIsFri(appSite.isTimePeriodDayInWeeksIsFri())
							.timePeriodDayInWeeksIsSat(appSite.isTimePeriodDayInWeeksIsSat())
							.timePeriodDayInWeeksIsSun(appSite.isTimePeriodDayInWeeksIsSun())
							.timePeriodTimeInDayIsAlways(appSite.isTimePeriodTimeInDayIsAlways())
							.timePeriodTimeInDayHourStart(appSite.getTimePeriodTimeInDayHourStart())
							.timePeriodTimeInDayHourEnd(appSite.getTimePeriodTimeInDayHourEnd())
							.timePeriodTimeInDayMinuteStart(appSite.getTimePeriodTimeInDayMinuteStart())
							.timePeriodTimeInDayMinuteEnd(appSite.getTimePeriodTimeInDayMinuteEnd())
							.build());
				}
			}
			
			List<DMSApplicationUser> applicationUsers = em.createQuery("FROM DMSApplicationUser where app.id = " + pagin.getOptions().get("applicationId")).getResultList();
			applicationUsers.forEach(au -> {
				dtos.get(0).getUsers().add(SimpleMap.init("type", au.getIsGuest() == Boolean.TRUE ? "guest" : "dms_user")
						.more("phoneNumber", au.getPhoneNumber())
						.more("email", au.getEmail())
						.more("name", au.getName())
						.more("isRequestCreateNew", au.getIsRequestCreateNew())
						.more("isRequestCreateSuccess", au.getIsRequestCreateSuccess())
						);
			});
		}
		
		pagin.setResults(dtos);
		pagin.setTotalRows(((Number)qCount.getSingleResult()).longValue());
	}
	
	@Transactional(readOnly = true)
	@Override
	public void searchApplicationUsers(PaginDto pagin) {
		if (pagin.getLimit() == null) {
			pagin.setLimit(10000);
		}
		if (pagin.getOffset() == null) {
			pagin.setOffset(0);
		}
		
		StringBuilder sqlBuilder = new StringBuilder(" select fl ");
		StringBuilder cmmBuilder = new StringBuilder(" FROM DMSApplicationUser fl where 1=1 ");

		if (!SecurityUtils.hasAnyRole("DMS_R_APPROVE_APPLICATION", "DMS_R_REJECT_APPLICATION")) {
			cmmBuilder.append(" AND fl.app.createdBy = '" + SecurityUtils.getPhoneNumber() + "' ");
		}
		
		if (pagin.getOptions().get("applicationId") != null) {
			cmmBuilder.append(" AND fl.app.id = " + pagin.getOptions().get("applicationId") + " ");
		}
		
		sqlBuilder.append(cmmBuilder);
		sqlBuilder.append(" ORDER BY fl.modifyDate DESC ");
		
		Query q = em.createQuery(sqlBuilder.toString());
		q.setFirstResult(pagin.getOffset());
		q.setMaxResults(pagin.getLimit());
		
		StringBuilder sqlCountBuilder = new StringBuilder("SELECT COUNT(*) ");
		sqlCountBuilder.append(cmmBuilder);
		
		Query qCount = em.createQuery(sqlCountBuilder.toString());
		
		List<DMSApplicationUser> list = q.getResultList();
		
		Map<String, Users> mapPhoneUsers = new LinkedHashMap<>();
		userRepository.findByPhoneNumberIn(list.stream().map(u -> u.getPhoneNumber()).collect(Collectors.toList()))
				.forEach(it -> mapPhoneUsers.put(it.getPhoneNumber(), it));
		
		list.forEach(it -> {
			it.setApplicationId(it.getApp().getId());
			it.setApp(null);
			it.setId(null);
			Users u = mapPhoneUsers.get(it.getPhoneNumber());
			if (u != null) {
				it.setUserId(u.getUserId());
				it.setEmail(u.getEmail());
			}
		});
		pagin.setResults(list);
		pagin.setTotalRows(((Number)qCount.getSingleResult()).longValue());
	}	

	@Transactional
	@Override
	public void linkPicUser(String email, Long projectId) {
		
		DMSProject project = dmsProjectRepository.findById(projectId).orElseThrow(() -> new RuntimeException("Project not found!"));
		List<Users> us = groupUserRepository.findUserByEmailAndGroupName(Arrays.asList(email), "DMS_G_PIC", AppCodeSelectedHolder.get());
		if (us.isEmpty()) {
			throw new RuntimeException("User Pic not found!");
		}
		
		// can pic or sub pic
		Optional<DMSProjectPicUser> currentLinkUserOpt = dmsProjectPicUserRepository.findByPicUserEmailAndProjectId(email, projectId);
		
		// pic user
		Optional<DMSProjectPicUser> currentPicUserOpt = findPicUserByProjectId(projectId);
		
		DMSProjectPicUser sub = findSubPicUsersByProjectId(projectId).stream().filter(p -> email.equalsIgnoreCase(p.getPicUser().getEmail())).findFirst().orElse(null);
		
		// check change pic
		if (currentPicUserOpt.isPresent() && !email.equals(currentPicUserOpt.get().getPicUser().getEmail())) {
			
			if (currentLinkUserOpt.isPresent()) {
				dmsProjectPicUserRepository.delete(currentLinkUserOpt.get());
			}

			if (sub != null) {
				dmsProjectPicUserRepository.delete(sub);
				dmsProjectPicUserRepository.flush();
			}
			currentPicUserOpt.get().setPicUser(us.get(0));
			dmsProjectPicUserRepository.save(currentPicUserOpt.get());
		} 
		
		// check new pic
		if (!currentPicUserOpt.isPresent()) {
			
			if (currentLinkUserOpt.isPresent()) {
				dmsProjectPicUserRepository.delete(currentLinkUserOpt.get());
			}
			
			if (sub != null) {
				dmsProjectPicUserRepository.delete(sub);
				dmsProjectPicUserRepository.flush();
			}
			
			dmsProjectPicUserRepository.save(
					DMSProjectPicUser.builder()
					.project(project)
					.picUser(us.get(0))
					.isSubPic(false)
					.build()
			);
		}
	}

	@Transactional
	@Override
	public void linkSubPicUsers(List<String> emails, Long projectId) {
		
		DMSProject project = dmsProjectRepository.findById(projectId).orElseThrow(() -> new RuntimeException("Project not found!"));
		List<Users> us = groupUserRepository.findUserByEmailAndGroupName(emails, "DMS_G_PIC", AppCodeSelectedHolder.get());
		
		Map<String, Users> emailsExists = new LinkedHashMap<>();
		us.forEach(it -> emailsExists.put(it.getEmail(), it));
		
		for (String email: emails) {
			if (!emailsExists.containsKey(email)) {
				throw new RuntimeException("User Pic not found! (" + email + ")");
			}
		}
		
		Optional<DMSProjectPicUser> currentPicUserOpt = findPicUserByProjectId(projectId);
		String emailPic = currentPicUserOpt.isPresent() ? currentPicUserOpt.get().getPicUser().getEmail() : null;

		Map<String, DMSProjectPicUser> mapCurrentSubPic = new LinkedHashMap<>();
		findSubPicUsersByProjectId(projectId)
		.forEach(it -> mapCurrentSubPic.put(it.getPicUser().getEmail(), it));
		
		for (String email : mapCurrentSubPic.keySet()) {
		
			if (!emails.contains(email)) {
				dmsProjectPicUserRepository.delete(mapCurrentSubPic.get(email));
			}
		}
		
		dmsProjectPicUserRepository.flush();
		
		for (String email: emails) {
			if (emailsExists.get(email) != null && !mapCurrentSubPic.containsKey(email)) {
				
				if (email.equals(emailPic)) {
					currentPicUserOpt.get().setSubPic(true);
					dmsProjectPicUserRepository.save(currentPicUserOpt.get());
				} else {
					dmsProjectPicUserRepository.save(
							DMSProjectPicUser.builder()
							.project(project)
							.picUser(emailsExists.get(email))
							.isSubPic(true)
							.build()
					);
				}					
			}
		}
	}

	@Override
	public void searchSubPicUserInProject(PaginDto pagin, Long projectId) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void searchDMSPicUsers(PaginDto pagin) {
		
		StringBuilder sqlBuilder = new StringBuilder("FROM Users us ");
		StringBuilder sqlCountBuilder = new StringBuilder("SELECT count(*) FROM Users us ");
		
		Map<String, Object> options = pagin.getOptions();
        String queryUserName = (String) options.get("queryUserName");
        String queryFirstName = (String) options.get("queryFirstName");
        String queryLastName = (String) options.get("queryLastName");

		StringBuilder sqlCommonBuilder = new StringBuilder();
		sqlCommonBuilder.append(" WHERE 1=1 ");
		
		if (StringUtils.isNotBlank(queryUserName)) {
			sqlCommonBuilder.append(" AND lower(username) like '%" + queryUserName.toLowerCase() + "%' ");
		}
		if (StringUtils.isNotBlank(queryFirstName)) {
			sqlCommonBuilder.append(" AND lower(firstName) like '%" + queryFirstName.toLowerCase() + "%' ");
		}
		if (StringUtils.isNotBlank(queryLastName)) {
			sqlCommonBuilder.append(" AND lower(lastName) like '%" + queryLastName.toLowerCase() + "%' ");
		}
		
		sqlCommonBuilder.append(" AND (exists (select 1 from UserGroup ug where ug.groupUser.name = 'DMS_G_PIC' and ug.user.userId = us.userId)) ");
		
		if (pagin.getOptions().get("projectId") != null) {
			sqlCommonBuilder.append(" AND exists (select 1 from DMSProjectPicUser ps where ps.isSubPic = true and ps.picUser.id = us.userId and ps.project.id = " + pagin.getOptions().get("projectId") + ") ");
		}
		
		sqlBuilder.append(sqlCommonBuilder).append(" ORDER BY us.userId asc");
		sqlCountBuilder.append(sqlCommonBuilder);

		if (pagin.getOffset() == null || pagin.getOffset() < 0) {
			pagin.setOffset(0);
		}

		if (pagin.getLimit() == null || pagin.getLimit() <= 0) {
			pagin.setLimit(100);
		}

		Query queryCount = em.createQuery(sqlCountBuilder.toString());

		Long count = ((Number) queryCount.getSingleResult()).longValue();
		pagin.setTotalRows(count);
		pagin.setResults(new ArrayList<>());
		if (count == 0l) {
			return;
		}

		Query query = em.createQuery(sqlBuilder.toString());
		query.setFirstResult(pagin.getOffset());
		query.setMaxResults(pagin.getLimit());

		List<Users> users = query.getResultList();
		users.forEach(user -> {

			List<String> roles = new ArrayList<>();
			UserDto dto = UserDto.builder().id(user.getUserId()).username(user.getUsername()).email(user.getEmail())
					.fullName(user.getFullName()).firstName(user.getFirstName()).lastName(user.getLastName())
					.phoneNumber(user.getPhoneNumber()).avatar(user.getAvatar())
					.fullName(user.getFirstName() + " " + user.getLastName()).status(user.getStatus())
					.changePwdRequire(user.getChangePwdRequire())
					.loginOtpRequire(user.getLoginOtpRequire())
					.identification(user.getIdentification())
					.roleDescs(user.getRoles().stream().filter(r -> r.getRole().getAppCode().getName().equals(AppCodeSelectedHolder.get())).map(authority -> {
						roles.add(authority.getRole().getName());
						return SimpleMap.init("name", authority.getRole().getName()).more("desc",
								authority.getRole().getDesc());
					}).collect(Collectors.toList())).build();
			dto.setRoles(roles);
			pagin.getResults().add(dto);
		});
	}

	@Transactional
	@Override
	public void linkSite(Long projectId, List<Long> siteIds) {
		
		DMSProject project = dmsProjectRepository.findById(projectId).orElseThrow(() -> new RuntimeException("Project not found!"));
		
		Map<Long, DMSProjectSite> mapExistsSite = new LinkedHashMap<>();
		dmsProjectRepository.findSitesInProject(projectId)
		.forEach(s -> mapExistsSite.put(s.getSite().getId(), s));
		
		Map<Long, DMSSite> mapUpdateSite = new LinkedHashMap<>();
		dmsProjectRepository.findSitesIn(siteIds)
		.forEach(s -> mapUpdateSite.put(s.getId(), s));
		
		for (Long existSiteId: mapExistsSite.keySet()) {
			if (!mapUpdateSite.containsKey(existSiteId)) {
				dmsProjectSiteRepository.delete(mapExistsSite.get(existSiteId));
			}
		}
		
		dmsProjectSiteRepository.flush();
		
		for (Long newSiteId: mapUpdateSite.keySet()) {
			if (!mapExistsSite.containsKey(newSiteId)) {
				dmsProjectSiteRepository.save(
					DMSProjectSite.builder()
					.project(project)
					.site(mapUpdateSite.get(newSiteId))
					.build()		
				);
			}
		}
	}
	
	Optional<DMSProjectPicUser> findPicUserByProjectId(Long projectId) {
		List<DMSProjectPicUser> rp = em.createQuery("FROM DMSProjectPicUser pic where pic.project.id = " + projectId + " and pic.isSubPic = false").getResultList();
		return rp.isEmpty() ? Optional.empty() : Optional.ofNullable(rp.get(0));
	}

	List<DMSProjectPicUser> findSubPicUsersByProjectId(Long projectId) {
		return em.createQuery("FROM DMSProjectPicUser pic where pic.project.id = " + projectId + " and pic.isSubPic = true").getResultList();
	}
	
	DMSProjectPicUser findSubPicUsersByProjectId(Long projectId, String email) {
		List<DMSProjectPicUser> rp = em.createQuery("FROM DMSProjectPicUser pic where pic.project.id = " + projectId + " and pic.isSubPic = true and pic.picUser.email = '" + email + "'").getResultList();
		return rp.isEmpty() ? null : rp.get(0);
	}
	
	DMSProjectPicUser findPicUserOrSubPicUsersByProjectId(Long projectId, String email) {
		List<DMSProjectPicUser> rp = em.createQuery("FROM DMSProjectPicUser pic where pic.project.id = " + projectId + " and pic.picUser.email = '" + email + "'").getResultList();
		return rp.isEmpty() ? null : rp.get(0);
	}

	@Override
	@Transactional
	public void approveApplication(Long applicationId) {
		DMSApplication application = dmsApplicationRepository.findById(applicationId).orElseThrow(() -> new RuntimeException("application not found!"));
		if (!SecurityUtils.hasAnyRole("DMS_R_APPROVE_APPLICATION") || findPicUserOrSubPicUsersByProjectId(application.getProject().getId(), SecurityUtils.getEmail()) == null) {
			throw new RuntimeException("Access denied!");
		}
		
		if (!"NEW".equals(application.getStatus())) {
			throw new RuntimeException("Application status invalid!");
		}
		application.setApprovalBy(SecurityUtils.getEmail());
		application.setStatus("APPROVAL");
		
		DMSProject project = application.getProject();
		
		int count = 1;
		List<DMSApplicationSite> appSites = dmsApplicationSiteRepository.findByAppId(applicationId);
		for (DMSApplicationSite appSite : appSites) {
			DMSWorkOrders workOrder;
			if (appSite.isOverrideTimePeriod()) {
				// get time period from app-site;
				workOrder = workOrdersService.save(DMSWorkOrdersDto.builder()
						.name("wod-p-" + project.getDisplayName() + "-a-" + application.getId() + "." + (count++) + "-s-" + appSite.getSite().getLabel())
						.site(DMSSiteDto.builder().id(appSite.getSite().getId()).build())
						.applicationId(application.getId())
						.timePeriodDatesIsAlways(appSite.isTimePeriodDatesIsAlways())
						.timePeriodDatesStart(appSite.getTimePeriodDatesStart())
						.timePeriodDatesEnd(appSite.getTimePeriodDatesEnd())
						.timePeriodDayInWeeksIsAlways(appSite.isTimePeriodDayInWeeksIsAlways())
						.timePeriodDayInWeeksIsMon(appSite.isTimePeriodDayInWeeksIsMon())
						.timePeriodDayInWeeksIsTue(appSite.isTimePeriodDayInWeeksIsTue())
						.timePeriodDayInWeeksIsWed(appSite.isTimePeriodDayInWeeksIsWed())
						.timePeriodDayInWeeksIsThu(appSite.isTimePeriodDayInWeeksIsThu())
						.timePeriodDayInWeeksIsFri(appSite.isTimePeriodDayInWeeksIsFri())
						.timePeriodDayInWeeksIsSat(appSite.isTimePeriodDayInWeeksIsSat())
						.timePeriodDayInWeeksIsSun(appSite.isTimePeriodDayInWeeksIsSun())
						.timePeriodTimeInDayIsAlways(appSite.isTimePeriodTimeInDayIsAlways())
						.timePeriodTimeInDayHourStart(appSite.getTimePeriodTimeInDayHourStart())
						.timePeriodTimeInDayHourEnd(appSite.getTimePeriodTimeInDayHourEnd())
						.timePeriodTimeInDayMinuteStart(appSite.getTimePeriodTimeInDayMinuteStart())
						.timePeriodTimeInDayMinuteEnd(appSite.getTimePeriodTimeInDayMinuteEnd())				
						.build());
			} else {
				// get time period from app;
				workOrder = workOrdersService.save(DMSWorkOrdersDto.builder()
						.name("wod-p-" + project.getDisplayName() + "-a-" + application.getId() + "." + (count++) + "-s-" + appSite.getSite().getLabel())
						.site(DMSSiteDto.builder().id(appSite.getSite().getId()).build())
						.applicationId(application.getId())
						.timePeriodDatesIsAlways(application.isTimePeriodDatesIsAlways())
						.timePeriodDatesStart(application.getTimePeriodDatesStart())
						.timePeriodDatesEnd(application.getTimePeriodDatesEnd())
						.timePeriodDayInWeeksIsAlways(application.isTimePeriodDayInWeeksIsAlways())
						.timePeriodDayInWeeksIsMon(application.isTimePeriodDayInWeeksIsMon())
						.timePeriodDayInWeeksIsTue(application.isTimePeriodDayInWeeksIsTue())
						.timePeriodDayInWeeksIsWed(application.isTimePeriodDayInWeeksIsWed())
						.timePeriodDayInWeeksIsThu(application.isTimePeriodDayInWeeksIsThu())
						.timePeriodDayInWeeksIsFri(application.isTimePeriodDayInWeeksIsFri())
						.timePeriodDayInWeeksIsSat(application.isTimePeriodDayInWeeksIsSat())
						.timePeriodDayInWeeksIsSun(application.isTimePeriodDayInWeeksIsSun())
						.timePeriodTimeInDayIsAlways(application.isTimePeriodTimeInDayIsAlways())
						.timePeriodTimeInDayHourStart(application.getTimePeriodTimeInDayHourStart())
						.timePeriodTimeInDayHourEnd(application.getTimePeriodTimeInDayHourEnd())
						.timePeriodTimeInDayMinuteStart(application.getTimePeriodTimeInDayMinuteStart())
						.timePeriodTimeInDayMinuteEnd(application.getTimePeriodTimeInDayMinuteEnd())				
						.build());				
			}

			appSite.setWorkOrder(workOrder);
			dmsApplicationSiteRepository.save(appSite);
		}

		dmsApplicationRepository.save(application);
		createAppGuestUser(application.getId());
		new Thread(() -> {
			try {
				notificationService.sendSMS("Your PTW application has been approved.", application.getCreatedBy());
				Users applicant = userRepository.findByPhoneNumber(application.getCreatedBy());
				if (applicant != null) {
					String email = applicant.getEmail();
					notificationService.sendEmail("Your PTW application has been approved.", email, "PTW application");
				}
			} catch (Exception e) {
				//
			}
		}).start();
	}
	
	@Transactional
	public void createAppGuestUser(Long applicationId) {
		
		DMSApplication application = dmsApplicationRepository.findById(applicationId).orElseThrow(() -> new RuntimeException("application not found!"));
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		String appCode = AppCodeSelectedHolder.get();
		// check create new user
		List<Long> applicationUserIds = application.getUsers().stream().map(u -> u.getId()).collect(Collectors.toList());
		new Thread(() -> {
			try {
				SecurityContextHolder.getContext().setAuthentication(authentication);
				AppCodeSelectedHolder.set(appCode);
				for (Long appUserId : applicationUserIds) {
					try {
						AppProps.getContext().getBean(this.getClass()).createUserGuestApplication(appUserId);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			} finally {
				SecurityContextHolder.clearContext();
				AppCodeSelectedHolder.remove();
			}

		}).start();
	}
	
	@Override
	@Transactional
	public void rejectApplication(Long applicationId) {
		DMSApplication application = dmsApplicationRepository.findById(applicationId).orElseThrow(() -> new RuntimeException("application not found!"));
		if (!SecurityUtils.hasAnyRole("DMS_R_REJECT_APPLICATION") || findPicUserOrSubPicUsersByProjectId(application.getProject().getId(), SecurityUtils.getEmail()) == null) {
			throw new RuntimeException("Access denied!");
		}
		
		if (!"NEW".equals(application.getStatus())) {
			throw new RuntimeException("Application status invalid!");
		}
		application.setRejectBy(SecurityUtils.getEmail());
		application.setStatus("REJECT");
		dmsApplicationRepository.save(application);
	}
	
	@Override
	@Transactional
	public void terminateApplication(Long applicationId) {
		DMSApplication application = dmsApplicationRepository.findById(applicationId).orElseThrow(() -> new RuntimeException("application not found!"));
		if (!SecurityUtils.hasAnyRole("DMS_R_APPROVE_APPLICATION") || findPicUserOrSubPicUsersByProjectId(application.getProject().getId(), SecurityUtils.getEmail()) == null) {
			throw new RuntimeException("Access denied!");
		}
		
		if (!"APPROVAL".equals(application.getStatus())) {
			throw new RuntimeException("Application status invalid!");
		}
		application.setTerminatedBy(SecurityUtils.getEmail());
		application.setStatus("TERMINATED");
		dmsApplicationRepository.save(application);
	}
	
	@Override
	@Transactional
	public void deleteApplication(Long applicationId) {
		DMSApplication application = dmsApplicationRepository.findById(applicationId).orElseThrow(() -> new RuntimeException("application not found!"));
		if (!application.getCreatedBy().equals(SecurityUtils.getPhoneNumber())) {
			throw new RuntimeException("Access denied!");
		}
		if (!"NEW".equals(application.getStatus())) {
			throw new RuntimeException("Application status invalid!");
		}
		application.setStatus("DELETED");
		dmsApplicationRepository.save(application);
	}
	
	@Override
	@Transactional
	public Object submitApplication(Long projectId, DMSApplicationGuestSaveReqDto dto) {
		dto.setGuestSubmit(true);
		String phone = dto.getSubmittedBy();
		List<OTP> otps = em.createQuery("FROM OTP where actionType = 'ptw_submit' AND phone = '" + phone + "' AND otp = '" + dto.getOtp() + "' AND endTime > " + System.currentTimeMillis() + "l  ORDER BY id DESC ").getResultList();
		if (otps.isEmpty() || otps.get(0).getStartTime() > System.currentTimeMillis() || otps.get(0).getEndTime() < System.currentTimeMillis()) {
			throw new RuntimeException("otp invalid!");
		}
		authenticationService.invalidOtp(phone, dto.getOtp());
		Users user = userRepository.findByPhoneNumber(phone);
		
		String email = user != null ? user.getEmail() : ("guestuser-" + phone + "@" + phone + "-guest-pa.evs.sg");
		String pwd = phone + "P@assW0rd";
		
		Long maxPeriod = null;
		for (DMSApplicationSiteItemReqDto site : dto.getSites()) {
			if (site.getTimePeriod().isOverride() && site.getTimePeriod().isTimePeriodDatesIsAlways()) {
				maxPeriod = null;
				break;
			}
			if (site.getTimePeriod().isOverride() && !site.getTimePeriod().isTimePeriodDatesIsAlways() && (maxPeriod == null || maxPeriod < site.getTimePeriod().getTimePeriodDatesEnd())) {
				maxPeriod = site.getTimePeriod().getTimePeriodDatesEnd();
			}
			if (!site.getTimePeriod().isOverride() && dto.getTimePeriod().isTimePeriodDatesIsAlways()) {
				maxPeriod = null;
				break;
			}
			if (!site.getTimePeriod().isOverride() && !dto.getTimePeriod().isTimePeriodDatesIsAlways() && (maxPeriod == null || maxPeriod < dto.getTimePeriod().getTimePeriodDatesEnd())) {
				maxPeriod = dto.getTimePeriod().getTimePeriodDatesEnd();
			}
		}

		ResponseDto<LoginResponseDto> credential = null;
		JwtUser guest = null;
		try {
			if (user == null) {
				guest = JwtUser.builder()
		                .id(-1l)
		                .email(email)
		                .fullName("User " + phone)
		                .firstName("User")
		                .lastName(phone)
		                .phone(phone)
		                .avatar(null)
		                .password(passwordEncoder.encode(pwd))
		                .appCodes(Arrays.asList("DMS"))
		                .authorities(Arrays.asList(new SimpleGrantedAuthority("DMS_GUEST")))
		                .permissions(Arrays.asList("DMS_GUEST"))
		                .enabled(true)
		                .changePwdRequire(false)
		                .phoneNumber(phone)
		                .lastPasswordResetDate(new Date())
		                .build();
				
			} else {
				guest = JwtUser.builder()
		                .id(user.getUserId())
		                .email(user.getEmail())
		                .fullName(user.getFullName())
		                .firstName(user.getFirstName())
		                .lastName(user.getLastName())
		                .phone(user.getPhoneNumber())
		                .avatar(null)
		                .password(passwordEncoder.encode(pwd))
		                .appCodes(Arrays.asList("DMS"))
		                .authorities(Arrays.asList(new SimpleGrantedAuthority("DMS_GUEST")))
		                .permissions(Arrays.asList("DMS_GUEST"))
		                .enabled(true)
		                .changePwdRequire(false)
		                .phoneNumber(phone)
		                .lastPasswordResetDate(new Date())
		                .build();
			}
			
			if (maxPeriod == null) {
				try {
					maxPeriod = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse("2100-01-01 00:00:00").getTime();
				} catch (Exception e) {
					//
				}
			}
			guest.setTokenExpireDate(new Date(maxPeriod));
			SecurityUtils.setByPassUser(guest);
			credential = authenticationService.login(LoginRequestDto.builder().email(email).password(pwd).build());	
		} finally {
			SecurityUtils.removeByPassUser();
		}
		
		if (credential != null) {
			if (user == null) {
				boolean hasGuestIn = dto.getGuests().stream().anyMatch(s -> s != null && phone.equalsIgnoreCase(s.getPhone()));
				if (!hasGuestIn) {
					dto.getGuests().add(DMSApplicationUserGuestReqDto.builder().phone(phone).createNewUser(false).name(guest.getFullName()).build());
				}
			} else {
                SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(guest, null, guest.getAuthorities()));
			}
			String guestToken = credential.getResponse().getToken();
			String tokenId = AppProps.getContext().getBean(JwtTokenUtil.class).getClaimFromToken(guestToken, Claims::getAudience);
			dto.setTokenStartTime(System.currentTimeMillis());
			dto.setTokenEndTime(guest.getTokenExpireDate().getTime());
			dto.setTokenId(tokenId);
			submitApplication(projectId, (DMSApplicationSaveReqDto) dto);
		}
		return credential;
	}
	
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void createUserGuestApplication(Long applicationUserId) {
		DMSApplicationUser appUser = dmsApplicationUserRepository.findById(applicationUserId).orElse(null);
		if (appUser != null && appUser.getIsRequestCreateNew() == Boolean.TRUE) {
			String arr[] = appUser.getName().split(" +");
			Map<String, Object> obj = (Map<String, Object>) authenticationService.getCredentialType(appUser.getEmail());
			
			if (obj != null) {
				List<String> appCodes = (List<String>) obj.get("app");
				if (!appCodes.contains(AppCodeSelectedHolder.get())) {
					authenticationService.assignAppCodeForEmail(AppCodeSelectedHolder.get(), appUser.getEmail());
				}
				appUser.setIsRequestCreateSuccess(true);
				dmsApplicationUserRepository.save(appUser);
			} else {
				obj = (Map<String, Object>) authenticationService.getCredentialType(appUser.getPhoneNumber());
				if (obj != null) {
					List<String> appCodes = (List<String>) obj.get("app");
					if (!appCodes.contains(AppCodeSelectedHolder.get())) {
						authenticationService.assignAppCodeForPhone(AppCodeSelectedHolder.get(), appUser.getPhoneNumber());
					}
					appUser.setIsRequestCreateSuccess(true);
					dmsApplicationUserRepository.save(appUser);
				}				
			}

			if (obj == null) {
				authenticationService.saveDMSAppUser(CreateDMSAppUserDto.builder()
						.email(appUser.getEmail())
						.firstName(arr[0])
						.lastName(arr.length == 1 ? arr[0] : appUser.getName().substring(arr[0].length()).trim())
						.phoneNumber(appUser.getPhoneNumber())
						.hPwd(appUser.getPassword())
						.loginOtpRequire(false)
						.firstLoginOtpRequire(true)
						.permissions(Arrays.asList("DMS_PAGE_APPLICATION_PERM"))
						.build());	
				appUser.setIsRequestCreateSuccess(true);
				dmsApplicationUserRepository.save(appUser);
			}

		}
	}
	
	// https://powerautomationsg.atlassian.net/browse/LOCKS-21
	@Transactional
	@Override
	public Object submitApplication(Long projectId, DMSApplicationSaveReqDto dto) {
		
		DMSProject project = dmsProjectRepository.findById(projectId).orElseThrow(() 
				-> new RuntimeException("project not found!"));
		
		if (StringUtils.isBlank(dto.getSubmittedBy()) || !dto.getSubmittedBy().trim().matches("^\\+[1-9][0-9]{7,}$")) {
			throw new RuntimeException(" SubmittedBy invalid!");
		}
		for (DMSApplicationSiteItemReqDto item : dto.getSites()) {
			Optional<DMSProjectSite> projectSiteOpt = dmsProjectSiteRepository.findByProjectIdAndSiteId(projectId, item.getId());
			if (!projectSiteOpt.isPresent()) {
				throw new RuntimeException("Site with id: " + item.getId() + " is not assigned to project: " + projectId);
			}
		}
		
		dto.setUpdatedDate(System.currentTimeMillis());
		dto.setUpdatedBy(SecurityUtils.getPhoneNumber());
		
		if (StringUtils.isNotBlank(SecurityUtils.getPhoneNumber())) {
			String phone = SecurityUtils.getPhoneNumber();
			boolean hasLoggedIn = dto.getUserPhones().stream().anyMatch(s -> phone.equalsIgnoreCase(s));
			if (!hasLoggedIn) {
				dto.getUserPhones().add(phone);
			}
		}
		
		DMSApplication application = dmsApplicationRepository.save(DMSApplication.builder()
				.name("app-" + new SimpleDateFormat("yyyyMMdd'-'HHmmss").format(new Date()) + "-" + "p-" + project.getDisplayName())
				.createdBy(dto.getSubmittedBy())
				.project(project)
				.status("NEW")
				// 1.d set time period he wants to access sites
				.timePeriodDatesIsAlways(dto.getTimePeriod().isTimePeriodDatesIsAlways())
				.timePeriodDatesStart(dto.getTimePeriod().getTimePeriodDatesStart())
				.timePeriodDatesEnd(dto.getTimePeriod().getTimePeriodDatesEnd())
				.timePeriodDayInWeeksIsAlways(dto.getTimePeriod().isTimePeriodDayInWeeksIsAlways())
				.timePeriodDayInWeeksIsMon(dto.getTimePeriod().isTimePeriodDayInWeeksIsMon())
				.timePeriodDayInWeeksIsTue(dto.getTimePeriod().isTimePeriodDayInWeeksIsTue())
				.timePeriodDayInWeeksIsWed(dto.getTimePeriod().isTimePeriodDayInWeeksIsWed())
				.timePeriodDayInWeeksIsThu(dto.getTimePeriod().isTimePeriodDayInWeeksIsThu())
				.timePeriodDayInWeeksIsFri(dto.getTimePeriod().isTimePeriodDayInWeeksIsFri())
				.timePeriodDayInWeeksIsSat(dto.getTimePeriod().isTimePeriodDayInWeeksIsSat())
				.timePeriodDayInWeeksIsSun(dto.getTimePeriod().isTimePeriodDayInWeeksIsSun())
				.timePeriodTimeInDayIsAlways(dto.getTimePeriod().isTimePeriodTimeInDayIsAlways())
				.timePeriodTimeInDayHourStart(dto.getTimePeriod().getTimePeriodTimeInDayHourStart())
				.timePeriodTimeInDayHourEnd(dto.getTimePeriod().getTimePeriodTimeInDayHourEnd())
				.timePeriodTimeInDayMinuteStart(dto.getTimePeriod().getTimePeriodTimeInDayMinuteStart())
				.timePeriodTimeInDayMinuteEnd(dto.getTimePeriod().getTimePeriodTimeInDayMinuteEnd())	
				.guestTokenId(dto.getTokenId())
				.guestTokenStartTime(dto.getTokenStartTime())
				.guestTokenEndTime(dto.getTokenEndTime())
				// for highlight changed
				.build());
		
		application.setName(application.getName() + "-" + application.getId());
		if (SecurityUtils.getPhoneNumber() == null || !SecurityUtils.getPhoneNumber().equalsIgnoreCase(dto.getSubmittedBy())) {
			application.setIsGuest(true);
		}
		
		dmsApplicationRepository.save(application);
		
		boolean existsSiteNotOpen = false;
		for (DMSApplicationSiteItemReqDto item : dto.getSites()) {
			DMSSite site = dmsSiteRepository.findById(item.getId()).orElseThrow(() 
					-> new RuntimeException("site not found!"));
			
			existsSiteNotOpen = existsSiteNotOpen || site.getIsOpen() != Boolean.TRUE;
			
			if (item.getTimePeriod() == null || !item.getTimePeriod().isOverride()) {
				item.setTimePeriod(ApiUtils.json2Object(ApiUtils.toStringJson(dto.getTimePeriod()), DMSTimePeriodDto.class));
				item.getTimePeriod().setOverride(false);
			}
			
			boolean override = item.getTimePeriod().isOverride();
			
			item.getTimePeriod().setSiteId(site.getId());
			item.getTimePeriod().setSiteLabel(site.getLabel());
			item.getTimePeriod().setOverride(override);
			
			dmsApplicationSiteRepository.save(DMSApplicationSite.builder()
					.app(application)
					.site(site)
					.overrideTimePeriod(override)
					//.workOrder(workOrder)
					.timePeriodDatesIsAlways(item.getTimePeriod().isTimePeriodDatesIsAlways())
					.timePeriodDatesStart(item.getTimePeriod().getTimePeriodDatesStart())
					.timePeriodDatesEnd(item.getTimePeriod().getTimePeriodDatesEnd())
					.timePeriodDayInWeeksIsAlways(item.getTimePeriod().isTimePeriodDayInWeeksIsAlways())
					.timePeriodDayInWeeksIsMon(item.getTimePeriod().isTimePeriodDayInWeeksIsMon())
					.timePeriodDayInWeeksIsTue(item.getTimePeriod().isTimePeriodDayInWeeksIsTue())
					.timePeriodDayInWeeksIsWed(item.getTimePeriod().isTimePeriodDayInWeeksIsWed())
					.timePeriodDayInWeeksIsThu(item.getTimePeriod().isTimePeriodDayInWeeksIsThu())
					.timePeriodDayInWeeksIsFri(item.getTimePeriod().isTimePeriodDayInWeeksIsFri())
					.timePeriodDayInWeeksIsSat(item.getTimePeriod().isTimePeriodDayInWeeksIsSat())
					.timePeriodDayInWeeksIsSun(item.getTimePeriod().isTimePeriodDayInWeeksIsSun())
					.timePeriodTimeInDayIsAlways(item.getTimePeriod().isTimePeriodTimeInDayIsAlways())
					.timePeriodTimeInDayHourStart(item.getTimePeriod().getTimePeriodTimeInDayHourStart())
					.timePeriodTimeInDayHourEnd(item.getTimePeriod().getTimePeriodTimeInDayHourEnd())
					.timePeriodTimeInDayMinuteStart(item.getTimePeriod().getTimePeriodTimeInDayMinuteStart())
					.timePeriodTimeInDayMinuteEnd(item.getTimePeriod().getTimePeriodTimeInDayMinuteEnd())
					
					// for highlight
					.build());
			
		}
		
		Map<String, Users> mapPhoneUsers = new LinkedHashMap<>();
		userRepository.findByPhoneNumberIn(dto.getUserPhones())
		.forEach(us -> mapPhoneUsers.put(us.getPhoneNumber(), us));
		
		for (String phone: dto.getUserPhones()) {
			if (StringUtils.isBlank(phone) || !phone.trim().matches("^\\+[1-9][0-9]{7,}$")) {
				throw new RuntimeException("Phone invalid(" + phone + ")! (ex: +65909123456)");
			}
			
			Users us = mapPhoneUsers.get(phone);
			if (us == null) {
				throw new RuntimeException("User with phone " + phone + " not found!");
			}
			dmsApplicationUserRepository.save(DMSApplicationUser.builder()
					.app(application)
					.email(us.getEmail())
					.phoneNumber(phone.trim())
					.name(us.getFirstName() + " " + us.getLastName())
					.isGuest(false)
					.build());
		}
		
		Map<String, Users> mapPhoneGuestUsers = new LinkedHashMap<>();
		userRepository.findByPhoneNumberIn(dto.getGuests().stream().map(g -> g.getPhone() == null ? null : g.getPhone().trim()).collect(Collectors.toList()))
		.forEach(us -> mapPhoneGuestUsers.put(us.getPhoneNumber(), us));
		
		Map<String, Users> mapEmailGuestUsers = new LinkedHashMap<>();
		userRepository.findByEmailIn(dto.getGuests().stream().filter(g -> g.getEmail() != null).map(g -> g.getEmail() == null ? null : g.getEmail().toLowerCase().trim()).collect(Collectors.toList()))
		.forEach(us -> mapEmailGuestUsers.put(us.getEmail(), us));
		
		for (DMSApplicationUserGuestReqDto guest: dto.getGuests()) {
			if (StringUtils.isBlank(guest.getPhone()) || !guest.getPhone().trim().matches("^\\+[1-9][0-9]{7,}$")) {
				throw new RuntimeException("Guest phone invalid! (ex: +65909123456)");
			}
			if (guest.getCreateNewUser() == Boolean.TRUE && (StringUtils.isBlank(guest.getEmail()) || !guest.getEmail().trim().matches("^[a-zA-Z0-9_!#$%&'*+/=?`{|}~^.-]+@[a-zA-Z0-9.-]+$"))) {
				throw new RuntimeException("Guest email invalid (ex: example@example.com)!");
			}			
			if (mapPhoneGuestUsers.get(guest.getPhone().toLowerCase().trim()) != null) {
				throw new RuntimeException("User with guest phone already exists(" + guest.getPhone().trim() + ")");
			}
			if (guest.getCreateNewUser() == Boolean.TRUE && mapEmailGuestUsers.get(guest.getEmail().toLowerCase().trim()) != null) {
				throw new RuntimeException("User with guest email already exists(" + guest.getEmail().trim() + ")");
			}
			if (guest.getCreateNewUser() == Boolean.TRUE && guest.getPassword() == null) {
				throw new RuntimeException("Password required!");
			}
			
			if (guest.getCreateNewUser() == Boolean.TRUE && guest.getPassword() != null && (guest.getPassword().length() < 8 || !guest.getPassword().matches(".*[a-z].*") || !guest.getPassword().matches(".*[A-Z].*") || !guest.getPassword().matches(".*[0-9].*") || !guest.getPassword().matches(".*[!@#\\$%^&*\\(\\)\\|\\[\\]].*"))) {
				throw new RuntimeException(AppProps.get("MSG_PWD_ERROR_FORMAT", "password invalid(password must contain lowercase, uppercase, numeric, special characters and at least 8 characters, ex: aA1!@#$%^&*()[])!"));
			}
			dmsApplicationUserRepository.save(DMSApplicationUser.builder()
					.app(application)
					.phoneNumber(guest.getPhone().trim())
					.name(guest.getName())
					.email(guest.getCreateNewUser() == Boolean.TRUE ? guest.getEmail().toLowerCase() : null)
					.password((guest.getCreateNewUser() == Boolean.TRUE && guest.getPassword() != null) ? passwordEncoder.encode(guest.getPassword()) : null)
					.isRequestCreateNew(guest.getCreateNewUser() == Boolean.TRUE)
					.isGuest(true)
					.build());
			guest.setPassword(null);
		}
		
		// for highlight changed
		dmsApplicationHistoryRepository.save(DMSApplicationHistory.builder()
				.app(application)
				.updatedDate(System.currentTimeMillis())
				.updatedBy(SecurityUtils.getPhoneNumber())
				.content(ApiUtils.toStringJson(dto))
				.build());
		handleAutoApprove(dto.isGuestSubmit(), projectId, application.getId(), existsSiteNotOpen);
		
		return application.getName();
	}
	
	private void handleAutoApprove(boolean isGuestSubmit, final Long projectId, final Long applicationId, final boolean existsSiteNotOn) {
		if (!isGuestSubmit) {
			return;
		}
		new Thread(() -> {
			try {
				Thread.sleep(1000);
			} catch (Exception e) {
				//
			}
			AppProps.getContext().getBean(this.getClass()).handleAutoApprove0(projectId, applicationId, existsSiteNotOn);
		}).start();
	}
	
	@Transactional
	public void handleAutoApprove0(final Long projectId, final Long applicationId, final boolean existsSiteNotOn) {
		try {
			DMSProject project = dmsProjectRepository.findById(projectId).orElseThrow(() -> new RuntimeException("project not found"));
			DMSApplication application = dmsApplicationRepository.findById(applicationId).orElseThrow(() -> new RuntimeException("application not found"));
			List<DMSProjectPicUser> picUsers = project.getPicUsers();
			System.out.println(picUsers);
			if (picUsers.isEmpty()) {
				return;
			}
			if (!existsSiteNotOn) {
				DMSProjectPicUser picUser = picUsers.stream().filter(u -> !u.isSubPic()).findFirst().orElse(picUsers.get(0));
				final String picEmail = picUser.getPicUser().getEmail();
				UserDetails userDetails = AppProps.getContext().getBean(UserDetailsService.class).loadUserByUsername(picEmail);
				SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities()));
				AppProps.getContext().getBean(this.getClass()).approveApplication(applicationId);
			} else {
				// has site off -> send notify to PIC
				for (DMSProjectPicUser picUser : picUsers) {
					Users u = picUser.getPicUser();
					String email = u.getEmail();
					String phone = u.getPhoneNumber();
					if (StringUtils.isNotBlank(phone)) {
						notificationService.sendSMS("New PTW application has been created by " + application.getCreatedBy(), phone);
					}
					notificationService.sendEmail("New PTW application has been created by " + application.getCreatedBy(), email, "PTW application");
				}
			}
		} finally {
			SecurityContextHolder.clearContext();
		}
	}
	
	@Override
	@Transactional
	public Object updateApplication(Long applicationId, DMSApplicationSaveReqDto dto) {
		
		DMSApplication application = dmsApplicationRepository.findById(applicationId).orElseThrow(() -> new RuntimeException("Application notfound!"));
		
		if (!SecurityUtils.hasAnyRole("DMS_R_APPROVE_APPLICATION") || findPicUserOrSubPicUsersByProjectId(application.getProject().getId(), SecurityUtils.getEmail()) == null) {
			throw new RuntimeException("Access denied!");
		}
		
		if (!"NEW".equals(application.getStatus())) {
			throw new RuntimeException("Application status invalid!");
		}
		
		if (dto.getTimePeriod() == null) {
			
			throw new RuntimeException("Application time period is require!");

		}
		
		dto.setUpdatedDate(System.currentTimeMillis());
		dto.setUpdatedBy(SecurityUtils.getPhoneNumber());
		application.setUpdatedBy(SecurityUtils.getEmail());
		application.setTimePeriodDatesIsAlways(dto.getTimePeriod().isTimePeriodDatesIsAlways());
		application.setTimePeriodDatesStart(dto.getTimePeriod().getTimePeriodDatesStart());
		application.setTimePeriodDatesEnd(dto.getTimePeriod().getTimePeriodDatesEnd());
		application.setTimePeriodDayInWeeksIsAlways(dto.getTimePeriod().isTimePeriodDayInWeeksIsAlways());
		application.setTimePeriodDayInWeeksIsMon(dto.getTimePeriod().isTimePeriodDayInWeeksIsMon());
		application.setTimePeriodDayInWeeksIsTue(dto.getTimePeriod().isTimePeriodDayInWeeksIsTue());
		application.setTimePeriodDayInWeeksIsWed(dto.getTimePeriod().isTimePeriodDayInWeeksIsWed());
		application.setTimePeriodDayInWeeksIsThu(dto.getTimePeriod().isTimePeriodDayInWeeksIsThu());
		application.setTimePeriodDayInWeeksIsFri(dto.getTimePeriod().isTimePeriodDayInWeeksIsFri());
		application.setTimePeriodDayInWeeksIsSat(dto.getTimePeriod().isTimePeriodDayInWeeksIsSat());
		application.setTimePeriodDayInWeeksIsSun(dto.getTimePeriod().isTimePeriodDayInWeeksIsSun());
		application.setTimePeriodTimeInDayIsAlways(dto.getTimePeriod().isTimePeriodTimeInDayIsAlways());
		application.setTimePeriodTimeInDayHourStart(dto.getTimePeriod().getTimePeriodTimeInDayHourStart());
		application.setTimePeriodTimeInDayHourEnd(dto.getTimePeriod().getTimePeriodTimeInDayHourEnd());
		application.setTimePeriodTimeInDayMinuteStart(dto.getTimePeriod().getTimePeriodTimeInDayMinuteStart());
		application.setTimePeriodTimeInDayMinuteEnd(dto.getTimePeriod().getTimePeriodTimeInDayMinuteEnd());

		if (dto.getTimeTerminate() != null && dto.getTimeTerminate().longValue() > 0) {
			application.setTimeTerminate(dto.getTimeTerminate());
		}
		dmsApplicationRepository.save(application);
		
		Map<Long, DMSApplicationSite> mapSiteIdAppSite = new LinkedHashMap<>(); 
		dmsApplicationSiteRepository.findByAppId(application.getId())
		.forEach(as -> mapSiteIdAppSite.put(as.getSite().getId(), as));
		
		Set<Long> updatingSiteIds = new HashSet<>();
		for (DMSApplicationSiteItemReqDto item : dto.getSites()) {
			DMSSite site = dmsSiteRepository.findById(item.getId()).orElseThrow(() 
					-> new RuntimeException("site not found!"));
			updatingSiteIds.add(site.getId());
			
			DMSApplicationSite appSite = mapSiteIdAppSite.get(site.getId()) == null ? new DMSApplicationSite() : mapSiteIdAppSite.get(site.getId());
			
			appSite.setApp(application);
			appSite.setSite(site);
			
			if (item.getTimePeriod() == null || !item.getTimePeriod().isOverride()) {
				item.setTimePeriod(ApiUtils.json2Object(ApiUtils.toStringJson(dto.getTimePeriod()), DMSTimePeriodDto.class));
				item.getTimePeriod().setOverride(false);
			}
			
			boolean override = item.getTimePeriod().isOverride();
			
			item.getTimePeriod().setSiteId(site.getId());
			item.getTimePeriod().setSiteLabel(site.getLabel());
			item.getTimePeriod().setOverride(override);

			appSite.setTimePeriodDatesIsAlways(item.getTimePeriod().isTimePeriodDatesIsAlways());
			appSite.setTimePeriodDatesStart(item.getTimePeriod().getTimePeriodDatesStart());
			appSite.setTimePeriodDatesEnd(item.getTimePeriod().getTimePeriodDatesEnd());
			appSite.setTimePeriodDayInWeeksIsAlways(item.getTimePeriod().isTimePeriodDayInWeeksIsAlways());
			appSite.setTimePeriodDayInWeeksIsMon(item.getTimePeriod().isTimePeriodDayInWeeksIsMon());
			appSite.setTimePeriodDayInWeeksIsTue(item.getTimePeriod().isTimePeriodDayInWeeksIsTue());
			appSite.setTimePeriodDayInWeeksIsWed(item.getTimePeriod().isTimePeriodDayInWeeksIsWed());
			appSite.setTimePeriodDayInWeeksIsThu(item.getTimePeriod().isTimePeriodDayInWeeksIsThu());
			appSite.setTimePeriodDayInWeeksIsFri(item.getTimePeriod().isTimePeriodDayInWeeksIsFri());
			appSite.setTimePeriodDayInWeeksIsSat(item.getTimePeriod().isTimePeriodDayInWeeksIsSat());
			appSite.setTimePeriodDayInWeeksIsSun(item.getTimePeriod().isTimePeriodDayInWeeksIsSun());
			appSite.setTimePeriodTimeInDayIsAlways(item.getTimePeriod().isTimePeriodTimeInDayIsAlways());
			appSite.setTimePeriodTimeInDayHourStart(item.getTimePeriod().getTimePeriodTimeInDayHourStart());
			appSite.setTimePeriodTimeInDayHourEnd(item.getTimePeriod().getTimePeriodTimeInDayHourEnd());
			appSite.setTimePeriodTimeInDayMinuteStart(item.getTimePeriod().getTimePeriodTimeInDayMinuteStart());
			appSite.setTimePeriodTimeInDayMinuteEnd(item.getTimePeriod().getTimePeriodTimeInDayMinuteEnd());
			
			// for highlight
			appSite.setOverrideTimePeriod(appSite.isOverrideTimePeriod() || override);
			
			dmsApplicationSiteRepository.save(appSite);
		}
		
		// remove appSite
		for (Entry<Long, DMSApplicationSite> en : mapSiteIdAppSite.entrySet()) {
			if (!updatingSiteIds.contains(en.getKey())) {
				dmsApplicationSiteRepository.delete(en.getValue());
			}
		}
		
		Map<String, Users> mapPhoneUsers = new LinkedHashMap<>();
		userRepository.findByPhoneNumberIn(dto.getUserPhones())
		.forEach(us -> mapPhoneUsers.put(us.getPhoneNumber(), us));
		
		Map<String, DMSApplicationUser> mapAppUserPhoneAppUser = new LinkedHashMap<>(); 
		dmsApplicationUserRepository.findByAppId(application.getId())
		.stream()
		.filter(as -> as.getIsGuest() != Boolean.TRUE)
		.forEach(as -> mapAppUserPhoneAppUser.put(as.getPhoneNumber(), as));
		
		Set<String> updatingPhoneUsers = new HashSet<>();
		for (String phone: dto.getUserPhones()) {
			if (StringUtils.isBlank(phone) || !phone.trim().matches("^\\+[1-9][0-9]{7,}$")) {
				throw new RuntimeException("Phone invalid(" + phone + ")! (ex: +65909123456)");
			}
			
			Users us = mapPhoneUsers.get(phone);
			if (us == null) {
				throw new RuntimeException("User with phone " + phone + " not found!");
			}
			
			updatingPhoneUsers.add(phone.trim());
			
			DMSApplicationUser appUser = mapAppUserPhoneAppUser.get(phone.trim()) == null ? new DMSApplicationUser() : mapAppUserPhoneAppUser.get(phone.trim());
			appUser.setApp(application);
			appUser.setEmail(us.getEmail());
			appUser.setPhoneNumber(phone.trim());
			appUser.setName(us.getFirstName() + " " + us.getLastName());
			appUser.setIsGuest(false);
			
			dmsApplicationUserRepository.save(appUser);
		}
		
		// remove appUser
		for (Entry<String, DMSApplicationUser> en : mapAppUserPhoneAppUser.entrySet()) {
			if (!updatingPhoneUsers.contains(en.getKey())) {
				dmsApplicationUserRepository.delete(en.getValue());
			}
		}
		
		Map<String, Users> mapPhoneGuestUsers = new LinkedHashMap<>();
		userRepository.findByPhoneNumberIn(dto.getGuests()
				.stream()
				.map(g -> g.getPhone() == null ? null : g.getPhone().trim())
				.collect(Collectors.toList())
		)
		.forEach(us -> mapPhoneGuestUsers.put(us.getPhoneNumber(), us));
		
		Map<String, Users> mapEmailGuestUsers = new LinkedHashMap<>();
		userRepository.findByEmailIn(dto.getGuests()
				.stream()
				.filter(g -> g.getEmail() != null)
				.map(g -> g.getEmail() == null ? null : g.getEmail().toLowerCase().trim())
				.collect(Collectors.toList())
		)
		.forEach(us -> mapEmailGuestUsers.put(us.getEmail(), us));
		
		
		Map<String, DMSApplicationUser> mapAppUserPhoneAppGuest = new LinkedHashMap<>(); 
		dmsApplicationUserRepository.findByAppId(application.getId())
		.stream()
		.filter(as -> as.getIsGuest() == Boolean.TRUE)
		.forEach(as -> mapAppUserPhoneAppGuest.put(as.getPhoneNumber(), as));
		
		Set<String> updatingPhoneGuests = new HashSet<>();
		for (DMSApplicationUserGuestReqDto guest: dto.getGuests()) {
			if (StringUtils.isBlank(guest.getPhone()) || !guest.getPhone().trim().matches("^\\+[1-9][0-9]{7,}$")) {
				throw new RuntimeException("Guest phone invalid! (ex: +65909123456)");
			}
			if (guest.getCreateNewUser() == Boolean.TRUE && (StringUtils.isBlank(guest.getEmail()) || !guest.getEmail().trim().matches("^[a-zA-Z0-9_!#$%&'*+/=?`{|}~^.-]+@[a-zA-Z0-9.-]+$"))) {
				throw new RuntimeException("Guest email invalid! (ex: example@example.com)");
			}			
			if (mapPhoneGuestUsers.get(guest.getPhone().toLowerCase().trim()) != null) {
				throw new RuntimeException("User with guest phone already exists(" + guest.getPhone().trim() + ")");
			}
			if (guest.getCreateNewUser() == Boolean.TRUE && mapEmailGuestUsers.get(guest.getEmail().toLowerCase().trim()) != null) {
				throw new RuntimeException("User with guest email already exists(" + guest.getEmail().trim() + ")");
			}
			
			updatingPhoneGuests.add(guest.getPhone().trim());
			DMSApplicationUser appGuest = mapAppUserPhoneAppGuest.get(guest.getPhone().trim()) == null ? new DMSApplicationUser() : mapAppUserPhoneAppGuest.get(guest.getPhone().trim());
			appGuest.setApp(application);
			appGuest.setPhoneNumber(guest.getPhone().trim());
			appGuest.setName(guest.getName());
			appGuest.setEmail(guest.getCreateNewUser() == Boolean.TRUE ? guest.getEmail().toLowerCase() : null);
			if (guest.getCreateNewUser() != Boolean.TRUE) {
				appGuest.setPassword(null);
			} else if (StringUtils.isNotBlank(guest.getPassword())) {
				appGuest.setPassword(passwordEncoder.encode(guest.getPassword()));
			}
			appGuest.setIsRequestCreateNew(guest.getCreateNewUser() == Boolean.TRUE);
			appGuest.setIsGuest(true);
			
			dmsApplicationUserRepository.save(appGuest);
		}
		
		// remove appGuest
		for (Entry<String, DMSApplicationUser> en : mapAppUserPhoneAppGuest.entrySet()) {
			if (!updatingPhoneGuests.contains(en.getKey())) {
				dmsApplicationUserRepository.delete(en.getValue());
			}
		}

		// for highlight changed
		dmsApplicationHistoryRepository.save(DMSApplicationHistory.builder()
				.app(application)
				.updatedDate(System.currentTimeMillis())
				.updatedBy(SecurityUtils.getPhoneNumber())
				.content(ApiUtils.toStringJson(dto))
				.build());
		
		return application.getName();
	}	

	@Transactional
	@Override
	public void deleteSiteOfApplication(Long applicationId, Long siteId) {
		DMSApplication application = dmsApplicationRepository.findById(applicationId).orElseThrow(() -> new RuntimeException("application not found!"));
		if (!SecurityUtils.hasAnyRole("DMS_R_APPROVE_APPLICATION") || findPicUserOrSubPicUsersByProjectId(application.getProject().getId(), SecurityUtils.getEmail()) == null) {
			throw new RuntimeException("Access denied!");
		}
		
		if (!"NEW".equals(application.getStatus())) {
			throw new RuntimeException("Application status invalid!");
		}
		
		DMSApplicationSite applicationSite = dmsApplicationSiteRepository.findByAppIdAndSiteId(applicationId, siteId).orElseThrow(() -> new RuntimeException("application and site not found!"));
		dmsWorkOrdersRepository.delete(applicationSite.getWorkOrder());
		dmsApplicationSiteRepository.delete(applicationSite);
	}
	
	@Transactional
	public void checkTerminateApplication() {
		em.createQuery("UPDATE DMSApplication SET status = 'TERMINATED' where status = 'APPROVAL' and timeTerminate is not null and timeTerminate <= " + System.currentTimeMillis()).executeUpdate();
	}
	
	@Transactional
	public void checkTerminateApplicationGuest() {
		
		long newGuestAppTimeExpiry = 30l * 60 * 1000;
		try {
			newGuestAppTimeExpiry = Long.parseLong(AppProps.get("NEW_GUEST_APP_TIME_EXPIRY_MINUTE", "30")) * 60l * 1000l;
		} catch (Exception e) {
			newGuestAppTimeExpiry = 30l * 60 * 1000;
		}
		List<DMSApplication> apps = em.createQuery(" FROM DMSApplication WHERE isGuest = true and guestTokenId is not null and status <> 'APPROVAL' and guestTokenStartTime < " + (System.currentTimeMillis() - newGuestAppTimeExpiry)).getResultList();
		
		List<String> tokenIds = apps.stream().map(DMSApplication::getGuestTokenId).collect(Collectors.toList());
		if (!tokenIds.isEmpty()) {
			em.createQuery("UPDATE Login SET endTime = startTime where tokenId in (:tokenIds)")
			.setParameter("tokenIds", tokenIds)
			.executeUpdate();
		}
		
		for (DMSApplication a : apps) {
			em.createQuery("DELETE FROM DMSApplicationUser u where u.app.id = " + a.getId()).executeUpdate();
			em.createQuery("DELETE FROM DMSApplicationSite u where u.app.id = " + a.getId()).executeUpdate();
			em.createQuery("DELETE FROM DMSApplicationHistory u where u.app.id = " + a.getId()).executeUpdate();
			em.flush();
			dmsApplicationRepository.delete(a);
		}
	}

	@Transactional
	@PostConstruct
	public void init() {
		SchedulerHelper.scheduleJob("0/30 * * * * ? *", () -> {
			
			try {
				AppProps.getContext().getBean(this.getClass()).checkTerminateApplication();
			} catch (Exception e) {
				
			}
			try {
				AppProps.getContext().getBean(this.getClass()).checkTerminateApplicationGuest();
			} catch (Exception e) {
				
			}
			
		}, "checkTerminateApplication");
	}

	// get public applications as request https://powerautomationsg.atlassian.net/browse/LOCKS-38
	@Override
	public Object getAllApplications(ApplicationRequestDto dto) {
		
		StringBuilder sql = new StringBuilder(" SELECT app.id, app.name, app.status, app.createDate, app.modifyDate, app.project.name ");
		sql.append(" FROM DMSApplication app where app.status <> 'DELETED' ");
		
		if (dto != null && dto.getRequest() != null && dto.getRequest().getFrom() != null) {
			sql.append(" AND app.createDate >= :from ");
		}
		
		if (dto != null && dto.getRequest() != null && dto.getRequest().getTo() != null) {
			sql.append(" AND app.createDate <= :to ");
		}		
		Query query = em.createQuery(sql.append(" ORDER BY app.modifyDate DESC ").toString());

		if (dto != null && dto.getRequest() != null && dto.getRequest().getFrom() != null) {
			query.setParameter("from", new Date(dto.getRequest().getFrom().toEpochMilli()));
		}
		if (dto != null && dto.getRequest() != null && dto.getRequest().getTo() != null) {
			query.setParameter("to", new Date(dto.getRequest().getTo().toEpochMilli()));
		}	
		
		List<Object[]> ens = query.setMaxResults(20000).getResultList();
		List<Map<String, Object>> rs = new ArrayList<>();
		for (Object[] app : ens) {
			Map<String, Object> it = new LinkedHashMap<>();
			it.put("id", app[0]);
			it.put("name", app[1]);
			it.put("status", app[2]);
			it.put("createdDate", Instant.ofEpochMilli(((Date) app[3]).getTime()));
			it.put("lastModifiedDate", Instant.ofEpochMilli(((Date) app[4]).getTime()));
			it.put("projectName", app[5]);
			rs.add(it);
		}
		return rs;
	}
}
