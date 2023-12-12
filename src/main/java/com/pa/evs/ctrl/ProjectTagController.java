package com.pa.evs.ctrl;

import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.pa.evs.dto.PaginDto;
import com.pa.evs.dto.ProjectTagDto;
import com.pa.evs.dto.ResponseDto;
import com.pa.evs.sv.ProjectTagService;

@RestController
public class ProjectTagController {
	
	@Autowired
	private ProjectTagService projectTagService;

    @PostMapping("/api/project-tag")
	public ResponseEntity<Object> save(HttpServletResponse response, @RequestBody ProjectTagDto dto) {
    	try {
    		projectTagService.save(dto);
    	} catch (Exception e) {
            return ResponseEntity.<Object>ok(ResponseDto.<Object>builder().success(false).message(e.getMessage()).build());
        }
    	return ResponseEntity.<Object>ok(ResponseDto.<Object>builder().success(true).build());
    }
    
    @DeleteMapping("/api/project-tag/{tagId}")
	public ResponseEntity<Object> delete(HttpServletResponse response, @PathVariable Long tagId) {
    	try {
    		projectTagService.delete(tagId);
    	} catch (Exception e) {
            return ResponseEntity.<Object>ok(ResponseDto.<Object>builder().success(false).message(e.getMessage()).build());
        }
    	return ResponseEntity.<Object>ok(ResponseDto.<Object>builder().success(true).build());
    }
    
    @PostMapping("/api/project-tags")
	public ResponseEntity<Object> getP1Report(HttpServletResponse response, @RequestBody PaginDto<ProjectTagDto> pagin) {
    	try {
    		projectTagService.search(pagin);
    	} catch (Exception e) {
            return ResponseEntity.<Object>ok(ResponseDto.<Object>builder().success(false).message(e.getMessage()).build());
        }
    	return ResponseEntity.<Object>ok(ResponseDto.<Object>builder().success(true).response(pagin).build());
    }
	
}
