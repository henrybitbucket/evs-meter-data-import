package com.pa.evs.sv.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.transaction.Transactional;

import org.apache.commons.lang3.StringUtils;
import org.json.simple.JSONArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pa.evs.dto.GroupUserDto;
import com.pa.evs.dto.MenuItemDto;
import com.pa.evs.dto.MenuItemsDto;
import com.pa.evs.dto.PermissionDto;
import com.pa.evs.dto.RoleDto;
import com.pa.evs.model.AppCode;
import com.pa.evs.model.Firmware;
import com.pa.evs.model.GroupUser;
import com.pa.evs.model.MenuItems;
import com.pa.evs.model.Permission;
import com.pa.evs.model.Role;
import com.pa.evs.repository.AppCodeRepository;
import com.pa.evs.repository.GroupUserRepository;
import com.pa.evs.repository.MenuItemsRepository;
import com.pa.evs.repository.PermissionRepository;
import com.pa.evs.repository.RoleRepository;
import com.pa.evs.sv.MenuItemsService;
import com.pa.evs.utils.SecurityUtils;

@Service
@Transactional
public class MenuItemsServiceImpl implements MenuItemsService {

	private static final Logger LOGGER = LoggerFactory.getLogger(ProjectTagServiceImpl.class);

	@Autowired
	private MenuItemsRepository menuItemsRepository;

	@Autowired
	private RoleRepository roleRepository;

	@Autowired
	private PermissionRepository permissionRepository;

	@Autowired
	private GroupUserRepository groupUserRepository;

	@Autowired
	private AppCodeRepository appCodeRepository;

	@PostConstruct
	public void init() {
    	new Thread(() -> {
    		try {
    			String user = SecurityUtils.getEmail();

    			List<AppCode> appCodes = appCodeRepository.findAll();

    			for (int i = 0; i < appCodes.size(); i++) {
    				AppCode appCode = appCodes.get(i);
    				String appCodeName = appCode.getName();
    				Optional<MenuItems> menuOpt = menuItemsRepository.findByAppCode(appCodeName);

    				List<Role> roles = roleRepository.findByAppCode(appCode);
    				List<RoleDto> rolesDto = roles.stream().map(role -> new RoleDto().builder().id(role.getId()).name(role.getName()).name(role.getDesc()).build()).collect(Collectors.toList());
    				List<String> rolesStr = roles.stream().map(role -> role.getName()).collect(Collectors.toList());
    				
    				List<Permission> permissions = permissionRepository.findByAppCode(appCode);
    				List<PermissionDto> permissionsDto = permissions.stream().map(permission -> new PermissionDto().builder().id(permission.getId()).description(permission.getDescription()).name(permission.getName()).build()).collect(Collectors.toList());
    				List<String> permissionsStr = permissions.stream().map(permission -> permission.getName()).collect(Collectors.toList());

    				List<GroupUser> groups = groupUserRepository.findByAppCode(appCode);
    				List<GroupUserDto> groupsDto = groups.stream().map(group -> new GroupUserDto().builder().id(group.getId()).name(group.getName()).description(group.getDescription()).build()).collect(Collectors.toList());
    				List<String> groupsStr = groups.stream().map(group -> group.getName()).collect(Collectors.toList());

    				if (!menuOpt.isPresent() || StringUtils.isBlank(menuOpt.get().getItems())) {
    					ClassPathResource resource = null;
    					if ("MMS".equalsIgnoreCase(appCodeName)) {
    						resource = new ClassPathResource("mms-default-menu-items.json");
    					} else if ("DMS".equalsIgnoreCase(appCodeName)) {
    						resource = new ClassPathResource("dms-default-menu-items.json");
    					}
    					ObjectMapper objectMapper = new ObjectMapper();
    					List<MenuItemDto> items = objectMapper.readValue(resource.getInputStream(), new TypeReference<List<MenuItemDto>>() {});
    					for (int j = 0; j < items.size(); j++) {
    						MenuItemDto item = items.get(j);
    						item.setRoles(rolesStr);
    						item.setPermissions(permissionsStr);
    						item.setGroups(groupsStr);
    						List<MenuItemDto> children = item.getChildren();
    						for (int k = 0; k < children.size(); k++) {
    							MenuItemDto child = children.get(k);
    							child.setRoles(rolesStr);
    							child.setPermissions(permissionsStr);
    							child.setGroups(groupsStr);
    						}
    					}
    					MenuItems menu;
    					if (!menuOpt.isPresent()) {
    						menu = new MenuItems();
    						menu.setAppCode(appCodeName);
    						menu.setCreatedBy(user);
    					} else {
    						menu = menuOpt.get();
    						menu.setUpdatedBy(user);
    					}
    					menu.setItems(objectMapper.writeValueAsString(items));
    					menuItemsRepository.save(menu);
    				}
    			}
    		} catch (IOException e) {
    			LOGGER.error("Error while init menu items");
    			e.printStackTrace();
    		}
    	}).start();
	}

	@Override
	public List<MenuItemsDto> getMenuItems() {
		List<MenuItems> items = menuItemsRepository.findAll();
		List<MenuItemsDto> result = new ArrayList<>();
		items.forEach(item -> {
			result.add(new MenuItemsDto().builder().id(item.getId()).items(item.getItems()).appCode(item.getAppCode()).build());
		});
		return result;
	}

	@Override
	public void updateMenuItem(String appCode, List<Object> menuItems) {
		Optional<MenuItems> menuOpt = menuItemsRepository.findByAppCode(appCode);
		if (!menuOpt.isPresent()) {
			throw new RuntimeException("Menu with app code = " + appCode + " not found!");
		}
		MenuItems menu = menuOpt.get();
		menu.setItems(new JSONArray().toJSONString(menuItems));
		menu.setUpdatedBy(SecurityUtils.getEmail());
		menuItemsRepository.save(menu);
	}

}
