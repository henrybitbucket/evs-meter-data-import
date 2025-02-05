package com.pa.evs.ctrl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.pa.evs.converter.ExceptionConvertor;
import com.pa.evs.dto.BuildingUnitDto;
import com.pa.evs.dto.PaginDto;
import com.pa.evs.dto.ResponseDto;
import com.pa.evs.enums.ResponseEnum;
import com.pa.evs.sv.BuildingUnitService;
import com.pa.evs.sv.DMSBuildingUnitService;
import com.pa.evs.utils.AppCodeSelectedHolder;
import com.pa.evs.utils.AppProps;

import io.swagger.v3.oas.annotations.Hidden;


@SuppressWarnings("rawtypes")
@RestController
@Hidden
public class BuildingUnitController {

	@Autowired
	BuildingUnitService buildingUnitService;
	
	@Autowired
    private ExceptionConvertor exceptionConvertor;	
	
	@PostMapping("/api/buildingunit")
	public ResponseDto save(@RequestBody BuildingUnitDto dto) {
		try {
			if ("DMS".equals(AppCodeSelectedHolder.get())) {
				AppProps.context.getBean(DMSBuildingUnitService.class).save(dto);
			} else {
				buildingUnitService.save(dto);
			}
			
			return ResponseDto.builder().success(true).message(ResponseEnum.SUCCESS.getErrorDescription()).build();
		} catch (Exception ex) {
			return exceptionConvertor.createResponseDto(ex);
		}
	}
	
	@PostMapping("/api/buildingunits")
	public PaginDto<BuildingUnitDto> search(@RequestBody PaginDto<BuildingUnitDto> pagin) {
		if ("DMS".equals(AppCodeSelectedHolder.get())) {
			AppProps.context.getBean(DMSBuildingUnitService.class).search(pagin);
		} else {
			buildingUnitService.search(pagin);
		}
		
		return pagin;
	}
	
	@DeleteMapping("/api/buildingunit/{id}")
	public ResponseDto delete(@PathVariable Long id) {
		try {
			if ("DMS".equals(AppCodeSelectedHolder.get())) {
				AppProps.context.getBean(DMSBuildingUnitService.class).delete(id);
			} else {
				buildingUnitService.delete(id);
			}
			
			return ResponseDto.builder().success(true).message(ResponseEnum.SUCCESS.getErrorDescription()).build();
		} catch (Exception ex) {
			return ResponseDto.builder().success(false).message(ex.getMessage()).build();
		}
	}
}
