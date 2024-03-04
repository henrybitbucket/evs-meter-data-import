package com.pa.evs.ctrl;

import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.pa.evs.dto.CompanyDto;
import com.pa.evs.dto.PaginDto;
import com.pa.evs.dto.ResponseDto;
import com.pa.evs.sv.CompanyService;

import springfox.documentation.annotations.ApiIgnore;

@RestController
@ApiIgnore
public class CompanyController {
	
	@Autowired
	private CompanyService companyService;

    @PostMapping("/api/company")
	public ResponseEntity<Object> save(HttpServletResponse response, @RequestBody CompanyDto dto) {
    	try {
    		companyService.save(dto);
    	} catch (Exception e) {
            return ResponseEntity.<Object>ok(ResponseDto.<Object>builder().success(false).message(e.getMessage()).build());
        }
    	return ResponseEntity.<Object>ok(ResponseDto.<Object>builder().success(true).build());
    }
    
    @DeleteMapping("/api/company/{cpnId}")
	public ResponseEntity<Object> delete(HttpServletResponse response, @PathVariable Long cpnId) {
    	try {
    		companyService.delete(cpnId);
    	} catch (Exception e) {
            return ResponseEntity.<Object>ok(ResponseDto.<Object>builder().success(false).message(e.getMessage()).build());
        }
    	return ResponseEntity.<Object>ok(ResponseDto.<Object>builder().success(true).build());
    }
    
    @PostMapping("/api/companies")
	public ResponseEntity<Object> getCompanies(HttpServletResponse response, @RequestBody PaginDto<CompanyDto> pagin) {
    	try {
    		companyService.search(pagin);
    	} catch (Exception e) {
            return ResponseEntity.<Object>ok(ResponseDto.<Object>builder().success(false).message(e.getMessage()).build());
        }
    	return ResponseEntity.<Object>ok(ResponseDto.<Object>builder().success(true).response(pagin).build());
    }
	
}
