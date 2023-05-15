package com.pa.evs.ctrl;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.pa.evs.converter.ExceptionConvertor;
import com.pa.evs.dto.BuildingDto;
import com.pa.evs.dto.PaginDto;
import com.pa.evs.dto.ResponseDto;
import com.pa.evs.enums.ResponseEnum;
import com.pa.evs.sv.BuildingService;
import com.pa.evs.utils.CsvUtils;


@SuppressWarnings("rawtypes")
@RestController
public class BuildingController {

	@Autowired
	BuildingService buildingService;
	
	@Autowired
    private ExceptionConvertor exceptionConvertor;
	
	@PostMapping("/api/building")
	public ResponseDto save(@RequestBody BuildingDto building) {
        try {
        	buildingService.save(building);
            return ResponseDto.builder().success(true).message(ResponseEnum.SUCCESS.getErrorDescription()).build();
        }catch (Exception ex) {
            return exceptionConvertor.createResponseDto(ex);
        }		
	}
	
	@PostMapping("/api/buildings")
	public ResponseEntity<?> search(@RequestBody PaginDto<BuildingDto> pagin, @RequestParam(required = false) String search, HttpServletResponse response) throws IOException {
		try {
			buildingService.search(pagin, search);
			if ("true".equals(pagin.getOptions().get("exportCSV") + "")) {
				String exportType = (String) pagin.getOptions().get("exportType");
				File file = CsvUtils.writeAddressCsv(pagin.getResults(), exportType, "address_" + System.currentTimeMillis() + ".csv");
	            String fileName = file.getName();
	            
	            try (FileInputStream fis = new FileInputStream(file)) {
	                response.setContentLengthLong(file.length());
	                response.setHeader(HttpHeaders.CONTENT_TYPE, "application/csv");
	                response.setHeader(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, "name");
	                response.setHeader("name", fileName);
	                response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"");
	                IOUtils.copy(fis, response.getOutputStream());
	            } finally {
	                FileUtils.deleteDirectory(file.getParentFile());
	            }
	        }
		} catch (Exception e) {
			e.printStackTrace();
		}
		return ResponseEntity.<Object>ok(pagin);
	}
	
	@DeleteMapping("/api/building/{id}")
	public ResponseDto delete(@PathVariable Long id) {
		try {
        	buildingService.delete(id);
            return ResponseDto.builder().success(true).message(ResponseEnum.SUCCESS.getErrorDescription()).build();
        }catch (Exception ex) {
            return exceptionConvertor.createResponseDto(ex);
        }	
	}
	
//	@PostConstruct
//    public void init() {
//    	new Thread(() -> buildingService.updateBuildingFullText()).start();
//    }
}
