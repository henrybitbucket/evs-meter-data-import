package com.pa.evs.ctrl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.pa.evs.converter.ExceptionConvertor;
import com.pa.evs.dto.FloorLevelDto;
import com.pa.evs.dto.PaginDto;
import com.pa.evs.dto.ResponseDto;
import com.pa.evs.enums.ResponseEnum;
import com.pa.evs.sv.DMSFloorLevelService;
import com.pa.evs.sv.FloorLevelService;
import com.pa.evs.utils.AppCodeSelectedHolder;
import com.pa.evs.utils.AppProps;

import io.swagger.v3.oas.annotations.Hidden;


@SuppressWarnings("rawtypes")
@RestController
@Hidden
public class FloorLevelController {

	@Autowired
	FloorLevelService floorLevelService;

	@Autowired
    private ExceptionConvertor exceptionConvertor;		
	
	@PostMapping("/api/floorlevel")
	public ResponseDto save(@RequestBody FloorLevelDto floorLevel) {
		try {
			if ("DMS".equals(AppCodeSelectedHolder.get())) {
				AppProps.context.getBean(DMSFloorLevelService.class).save(floorLevel);
			} else {
				floorLevelService.save(floorLevel);
			}
			return ResponseDto.builder().success(true).message(ResponseEnum.SUCCESS.getErrorDescription()).build();
		} catch (Exception ex) {
			return exceptionConvertor.createResponseDto(ex);
		}
	}
	
	@PostMapping("/api/floorlevels")
	public PaginDto<FloorLevelDto> search(@RequestBody PaginDto<FloorLevelDto> pagin) {
		if ("DMS".equals(AppCodeSelectedHolder.get())) {
			AppProps.context.getBean(DMSFloorLevelService.class).search(pagin);
		} else {
			floorLevelService.search(pagin);
		}
		return pagin;
	}
	
	@DeleteMapping("/api/floorlevel/{id}")
	public ResponseDto delete(@PathVariable Long id) {
		try {
			if ("DMS".equals(AppCodeSelectedHolder.get())) {
				AppProps.context.getBean(DMSFloorLevelService.class).delete(id);
			} else {
				floorLevelService.delete(id);
			}
			return ResponseDto.builder().success(true).message(ResponseEnum.SUCCESS.getErrorDescription()).build();
		} catch (Exception ex) {
			return ResponseDto.builder().success(false).message(ex.getMessage()).build();
		}
	}
}
