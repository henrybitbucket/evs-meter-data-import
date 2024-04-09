package com.pa.evs.ctrl;

import java.util.List;

import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.pa.evs.dto.MenuItemsDto;
import com.pa.evs.dto.ResponseDto;
import com.pa.evs.sv.MenuItemsService;

import springfox.documentation.annotations.ApiIgnore;

@RestController
@ApiIgnore
public class MenuItemsController {
	
	@Autowired
	private MenuItemsService menuItemsService;

    @GetMapping("/api/menu-items")
	public ResponseEntity<Object> getMenuItems(HttpServletResponse response) {
    	try {
    		List<MenuItemsDto> result = menuItemsService.getMenuItems();
    		return ResponseEntity.<Object>ok(ResponseDto.<Object>builder().success(true).response(result).build());
    	} catch (Exception e) {
            return ResponseEntity.<Object>ok(ResponseDto.<Object>builder().success(false).message(e.getMessage()).build());
        }
    }
    
    @PutMapping("/api/menu-item")
	public ResponseDto<Object> updateMenuItem(HttpServletResponse response, @RequestParam String appCode, @RequestBody List<Object> menuItems) {
    	try {
    		menuItemsService.updateMenuItem(appCode, menuItems);
    		return ResponseDto.<Object>builder().success(true).build();
    	} catch (Exception e) {
    		return ResponseDto.<Object>builder().success(false).errorDescription(e.getMessage()).build();
        }
    }
	
}
