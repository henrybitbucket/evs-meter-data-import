package com.pa.evs.sv.impl;


import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pa.evs.dto.GroupUserDto;
import com.pa.evs.dto.PaginDto;
import com.pa.evs.dto.PermissionDto;
import com.pa.evs.dto.RoleDto;
import com.pa.evs.exception.ApiException;
import com.pa.evs.model.GroupUser;
import com.pa.evs.model.Permission;
import com.pa.evs.model.Role;
import com.pa.evs.model.RoleGroup;
import com.pa.evs.repository.AppCodeRepository;
import com.pa.evs.repository.GroupUserRepository;
import com.pa.evs.repository.RoleGroupRepository;
import com.pa.evs.repository.RoleRepository;
import com.pa.evs.repository.UserGroupRepository;
import com.pa.evs.sv.GroupUserService;
import com.pa.evs.utils.AppCodeSelectedHolder;

@Service
@SuppressWarnings("unchecked")
public class GroupUserServiceImpl implements GroupUserService {

    static final Logger logger = LogManager.getLogger(GroupUserServiceImpl.class);
    
    @Autowired
    GroupUserRepository groupUserRepository;
    
    @Autowired
    RoleGroupRepository roleGroupRepository;
    
    @Autowired
    AppCodeRepository appCodeRepository;
    
    @Autowired
    UserGroupRepository userGroupRepository;

    @Autowired
    RoleRepository roleRepository;

    @Autowired
    EntityManager em;

    @Value("${evs.pa.report.jasperDir}")
    private String jasperDir;

	@Override
	public void getGroupUser(PaginDto<GroupUserDto> pagin) {
		
		StringBuilder sqlBuilder = new StringBuilder("FROM GroupUser ");
        StringBuilder sqlCountBuilder = new StringBuilder("SELECT count(*) FROM GroupUser ");

        StringBuilder sqlCommonBuilder = new StringBuilder();
        sqlCommonBuilder.append(" WHERE 1=1 ");
        sqlCommonBuilder.append(" AND appCode.name = '" + AppCodeSelectedHolder.get()  + "' ");
        
        sqlBuilder.append(sqlCommonBuilder).append(" ORDER BY createDate DESC");
        sqlCountBuilder.append(sqlCommonBuilder);

        if (pagin.getOffset() == null || pagin.getOffset() < 0) {
            pagin.setOffset(0);
        }

        if (pagin.getLimit() == null || pagin.getLimit() <= 0) {
            pagin.setLimit(100);
        }

        Query queryCount = em.createQuery(sqlCountBuilder.toString());

        Long count = ((Number)queryCount.getSingleResult()).longValue();
        pagin.setTotalRows(count);
        pagin.setResults(new ArrayList<>());
        if (count == 0l) {
            return;
        }

        Query query = em.createQuery(sqlBuilder.toString());
        query.setFirstResult(pagin.getOffset());
        query.setMaxResults(pagin.getLimit());

        List<GroupUser> list = query.getResultList();
        Map<Long, List<RoleDto>> mapGroupRole = new LinkedHashMap<>();
        roleGroupRepository.findByGroupUserNameIn(list.stream().map(r -> r.getName()).collect(Collectors.toList()))
        .forEach(rp -> {
        	List<RoleDto> roles = mapGroupRole.computeIfAbsent(rp.getGroupUser().getId(), k -> new ArrayList<>());
        	Role role = rp.getRole();
        	roles.add(RoleDto.builder()
                    .id(role.getId())
                    .name(role.getName())
                    .desc(role.getDesc())
                    .build());
        });
        
        list.forEach(li -> {
        	GroupUserDto dto = GroupUserDto.builder()
                    .id(li.getId())
                    .name(li.getName())
                    .description(li.getDescription())
                    .roles(mapGroupRole.get(li.getId()))
                    .build();
            pagin.getResults().add(dto);
        });
	}

	@Transactional
	@Override
	public void createGroupUser(GroupUserDto dto) throws ApiException {
		
		dto.setName(dto.getName().toUpperCase());
		if (!dto.getName().startsWith("ALL_") && !dto.getName().startsWith(AppCodeSelectedHolder.get() + "_")) {
			throw new ApiException("Please use prefix: " + AppCodeSelectedHolder.get() + "_ " + " for name!");
		}
		GroupUser groupUser = new GroupUser();
		groupUser.setName(dto.getName().toUpperCase());
		groupUser.setDescription(dto.getDescription());
		groupUser.setAppCode(appCodeRepository.findByName(AppCodeSelectedHolder.get()));
		groupUserRepository.save(groupUser);
	
	}

	@Transactional
	@Override
	public void updateGroupUser(GroupUserDto dto) throws Exception {

		Optional<GroupUser> opt = groupUserRepository.findById(dto.getId());
		if (!opt.isPresent()) {
			throw new Exception(String.format("No groupUser with id %d found!", dto.getId()));
		}
		GroupUser group = opt.get();
		// group.setName(dto.getName());
		group.setDescription(dto.getDescription());
		groupUserRepository.save(group);
		if (dto.getRoles() != null) {

			List<Long> roleIds = dto.getRoles().stream().map(r -> r.getId()).collect(Collectors.toList());
			Map<Long, RoleGroup> mapExistsRole = new LinkedHashMap<>();
			groupUserRepository.findRolesInGroup(group.getId()).forEach(s -> mapExistsRole.put(s.getRole().getId(), s));

			Map<Long, Role> mapUpdateRole = new LinkedHashMap<>();
			groupUserRepository.findRolesIn(roleIds).forEach(s -> mapUpdateRole.put(s.getId(), s));

			for (Long existRoleId : mapExistsRole.keySet()) {
				if (!mapUpdateRole.containsKey(existRoleId)) {
					roleGroupRepository.delete(mapExistsRole.get(existRoleId));
				}
			}

			roleGroupRepository.flush();

			for (Long newRoleId : mapUpdateRole.keySet()) {
				if (!mapExistsRole.containsKey(newRoleId)) {
					roleGroupRepository
							.save(RoleGroup.builder().groupUser(group).role(mapUpdateRole.get(newRoleId)).build());
				}
			}
		}
	}

	@Transactional
	@Override
	public void deleteGroupUser(Long id) {
		roleGroupRepository.findByGroupUserId(id)
		.stream()
		.forEach(roleGroupRepository::delete);

		roleGroupRepository.flush();
		userGroupRepository.findByGroupUserId(id)
		.stream()
		.forEach(userGroupRepository::delete);

		userGroupRepository.flush();
		groupUserRepository.deleteById(id);
	}

}
