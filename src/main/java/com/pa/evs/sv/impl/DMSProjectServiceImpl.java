package com.pa.evs.sv.impl;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pa.evs.dto.AddressDto;
import com.pa.evs.dto.BuildingDto;
import com.pa.evs.dto.DMSProjectDto;
import com.pa.evs.dto.DMSWorkOrdersDto;
import com.pa.evs.dto.PaginDto;
import com.pa.evs.dto.UserDto;
import com.pa.evs.exception.ApiException;
import com.pa.evs.model.DMSBuilding;
import com.pa.evs.model.DMSLocationSite;
import com.pa.evs.model.DMSProject;
import com.pa.evs.model.DMSProjectPicUser;
import com.pa.evs.model.DMSProjectSite;
import com.pa.evs.model.DMSSite;
import com.pa.evs.model.DMSWorkOrders;
import com.pa.evs.model.Users;
import com.pa.evs.repository.DMSBlockRepository;
import com.pa.evs.repository.DMSBuildingRepository;
import com.pa.evs.repository.DMSBuildingUnitRepository;
import com.pa.evs.repository.DMSFloorLevelRepository;
import com.pa.evs.repository.DMSLocationSiteRepository;
import com.pa.evs.repository.DMSProjectPicUserRepository;
import com.pa.evs.repository.DMSProjectRepository;
import com.pa.evs.repository.DMSProjectSiteRepository;
import com.pa.evs.repository.DMSWorkOrdersRepository;
import com.pa.evs.repository.GroupUserRepository;
import com.pa.evs.sv.DMSProjectService;
import com.pa.evs.utils.AppCodeSelectedHolder;
import com.pa.evs.utils.SimpleMap;
import com.pa.evs.utils.Utils;

@SuppressWarnings("rawtypes")
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
	
	@Transactional
	@Override
	public void save(DMSProjectDto dto) {
		
		if (StringUtils.isBlank(dto.getName())) {
			throw new RuntimeException("Name is required!");
		}
		if (dto.getId() != null) {
			update(dto);
		} else {
			if (dmsProjectRepository.findByName(dto.getName().trim()).isPresent()) {
				throw new RuntimeException("name exitst!");
			}
			SimpleDateFormat sf = new SimpleDateFormat("yyMMdd-hhmmss");
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
		
		
		sqlBuilder.append(cmmBuilder);
		sqlBuilder.append(" ORDER BY fl.modifyDate DESC ");
		
		Query q = em.createQuery(sqlBuilder.toString());
		q.setFirstResult(pagin.getOffset());
		q.setMaxResults(pagin.getLimit());
		
		StringBuilder sqlCountBuilder = new StringBuilder("SELECT COUNT(fl) ");
		sqlCountBuilder.append(cmmBuilder);
		
		Query qCount = em.createQuery(sqlCountBuilder.toString());
		
		@SuppressWarnings("unchecked")
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
		dmsProjectRepository.delete(entity);
	}

	@SuppressWarnings("unchecked")
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

	@SuppressWarnings("unchecked")
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
	
	@SuppressWarnings("unchecked")
	Optional<DMSProjectPicUser> findPicUserByProjectId(Long projectId) {
		List<DMSProjectPicUser> rp = em.createQuery("FROM DMSProjectPicUser pic where pic.project.id = " + projectId + " and pic.isSubPic = false").getResultList();
		return rp.isEmpty() ? Optional.empty() : Optional.ofNullable(rp.get(0));
	}

	@SuppressWarnings("unchecked")
	List<DMSProjectPicUser> findSubPicUsersByProjectId(Long projectId) {
		return em.createQuery("FROM DMSProjectPicUser pic where pic.project.id = " + projectId + " and pic.isSubPic = true").getResultList();
	}
	
	@SuppressWarnings("unchecked")
	DMSProjectPicUser findSubPicUsersByProjectId(Long projectId, String email) {
		List<DMSProjectPicUser> rp = em.createQuery("FROM DMSProjectPicUser pic where pic.project.id = " + projectId + " and pic.isSubPic = true and pic.picUser.email = '" + email + "'").getResultList();
		return rp.isEmpty() ? null : rp.get(0);
	}
	
}
