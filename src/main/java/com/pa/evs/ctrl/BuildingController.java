package com.pa.evs.ctrl;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
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
	public PaginDto<BuildingDto> search(@RequestBody PaginDto<BuildingDto> pagin, @RequestParam(required = false) String search) {
		buildingService.search(pagin, search);
		return pagin;
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
	
	@PostConstruct
    public void init() {
    	new Thread(() -> buildingService.updateBuildingFullText()).start();
    }
}
