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

import com.pa.evs.dto.PaginDto;
import com.pa.evs.dto.PermissionDto;
import com.pa.evs.dto.RoleDto;
import com.pa.evs.enums.ResponseEnum;
import com.pa.evs.exception.ApiException;
import com.pa.evs.model.Permission;
import com.pa.evs.model.Role;
import com.pa.evs.model.RolePermission;
import com.pa.evs.repository.PermissionRepository;
import com.pa.evs.repository.RolePermissionRepository;
import com.pa.evs.repository.RoleRepository;
import com.pa.evs.repository.UserRoleRepository;
import com.pa.evs.sv.RoleService;

@Service
@SuppressWarnings("unchecked")
public class RoleServiceImpl implements RoleService {

    static final Logger logger = LogManager.getLogger(RoleServiceImpl.class);

    @Autowired
    RoleRepository roleRepository;
    
    @Autowired
    RolePermissionRepository rolePermissionRepository;
    
    @Autowired
    UserRoleRepository userRoleRepository;
    
    @Autowired
    PermissionRepository permissionRepository;

    @Autowired
    EntityManager em;

    @Value("${evs.pa.report.jasperDir}")
    private String jasperDir;

	@Override
	public void getRoles(PaginDto<RoleDto> pagin) {
	
		StringBuilder sqlBuilder = new StringBuilder("FROM Role");
        StringBuilder sqlCountBuilder = new StringBuilder("SELECT count(*) FROM Role");

        StringBuilder sqlCommonBuilder = new StringBuilder();
        sqlCommonBuilder.append(" WHERE 1=1 ");
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

        List<Role> list = query.getResultList();
        Map<Long, List<PermissionDto>> mapRolePms = new LinkedHashMap<>();
        rolePermissionRepository.findByRoleNameIn(list.stream().map(r -> r.getName()).collect(Collectors.toList()))
        .forEach(rp -> {
        	List<PermissionDto> pms = mapRolePms.computeIfAbsent(rp.getRole().getId(), k -> new ArrayList<>());
        	Permission pm = rp.getPermission();
        	pms.add(PermissionDto.builder()
                    .id(pm.getId())
                    .name(pm.getName())
                    .description(pm.getDescription())
                    .build());
        });
        

        list.forEach(li -> {
        	RoleDto dto = RoleDto.builder()
                    .id(li.getId())
                    .name(li.getName())
                    .desc(li.getDesc())
                    .permissions(mapRolePms.get(li.getId()))
                    .build();
            pagin.getResults().add(dto);
        });
	}

	@Override
	public void createRole(RoleDto dto)throws ApiException {
		
		boolean check = false;
		List<Role> roles = roleRepository.findAll();
		for(Role role : roles) {
			if (role.getName() == dto.getName().toUpperCase()) {
				check = true;
				new ApiException(ResponseEnum.ROLE_EXIST);
			}
		}
		if(!check) {
			Role role = new Role();
			role.setName(dto.getName().toUpperCase());
			role.setDesc(dto.getDesc());
			roleRepository.save(role);
		}
	}

	@Transactional
	@Override
	public void updateRole(RoleDto dto) {
		
		Optional<Role> role = roleRepository.findById(dto.getId());
		permissionRepository.findAll();
		List<RolePermission> rolePermissions = rolePermissionRepository.findByRoleId(dto.getId());
		if (role.isPresent()) {
			role.get().setName(dto.getName());
			role.get().setDesc(dto.getDesc());
			roleRepository.save(role.get());
			rolePermissionRepository.deleteAll(rolePermissions);
			rolePermissionRepository.flush();
			if (!dto.getPermissions().isEmpty()) {
				List<Permission> newPrms = permissionRepository.findByNameIn(dto.getPermissions().stream().map(pm -> pm.getName()).collect(Collectors.toList()));
				for (Permission pms : newPrms) {
					RolePermission rolePer = new RolePermission();
					rolePer.setPermission(pms);
					rolePer.setRole(role.get());
					rolePermissionRepository.save(rolePer);
				}
				
			}
		}
	}

	@Transactional
	@Override
	public void deleteRole(Long id) {
		rolePermissionRepository.findByRoleId(id)
		.stream().forEach(rolePermissionRepository::delete);
		rolePermissionRepository.flush();
		
		userRoleRepository.findByRoleId(id)
		.stream().forEach(userRoleRepository::delete);
		
		userRoleRepository.flush();
		
		roleRepository.deleteById(id);
	}
	
	@Override
	public void getPermissions(PaginDto<PermissionDto> pagin) {
		StringBuilder sqlBuilder = new StringBuilder("FROM Permission");
        StringBuilder sqlCountBuilder = new StringBuilder("SELECT count(*) FROM Permission");

        StringBuilder sqlCommonBuilder = new StringBuilder();
        sqlCommonBuilder.append(" WHERE 1=1 ");
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

        List<Permission> list = query.getResultList();

        list.forEach(li -> {
        	PermissionDto dto = PermissionDto.builder()
                    .id(li.getId())
                    .name(li.getName())
                    .description(li.getDescription())
                    .build();
            pagin.getResults().add(dto);
        });
	}

}
