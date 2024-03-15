package com.pa.evs.sv;

import java.util.List;

import com.pa.evs.dto.MenuItemsDto;

public interface MenuItemsService {

	List<MenuItemsDto> getMenuItems();

	void updateMenuItem(String appCode, List<Object> menuItems);

}
