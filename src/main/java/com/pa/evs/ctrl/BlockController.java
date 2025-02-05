package com.pa.evs.ctrl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.pa.evs.converter.ExceptionConvertor;
import com.pa.evs.dto.BlockDto;
import com.pa.evs.dto.PaginDto;
import com.pa.evs.dto.ResponseDto;
import com.pa.evs.enums.ResponseEnum;
import com.pa.evs.sv.BlockService;
import com.pa.evs.sv.DMSBlockService;
import com.pa.evs.utils.AppCodeSelectedHolder;
import com.pa.evs.utils.AppProps;

import io.swagger.v3.oas.annotations.Hidden;


@SuppressWarnings("rawtypes")
@RestController
@Hidden
public class BlockController {

	@Autowired
	BlockService blockService;

	@Autowired
    private ExceptionConvertor exceptionConvertor;		
	
	@PostMapping("/api/block")
	public ResponseDto save(@RequestBody BlockDto block) {
		try {
			if ("DMS".equals(AppCodeSelectedHolder.get())) {
				AppProps.context.getBean(DMSBlockService.class).save(block);
			} else {
				blockService.save(block);
			}
			
			return ResponseDto.builder().success(true).message(ResponseEnum.SUCCESS.getErrorDescription()).build();
		} catch (Exception ex) {
			return exceptionConvertor.createResponseDto(ex);
		}
	}
	
	@PostMapping("/api/blocks")
	public PaginDto<BlockDto> search(@RequestBody PaginDto<BlockDto> pagin) {
		if ("DMS".equals(AppCodeSelectedHolder.get())) {
			AppProps.context.getBean(DMSBlockService.class).search(pagin);
		} else {
			blockService.search(pagin);
		}
		
		return pagin;
	}
	
	@DeleteMapping("/api/block/{id}")
	public ResponseDto delete(@PathVariable Long id) {
		try {
			if ("DMS".equals(AppCodeSelectedHolder.get())) {
				AppProps.context.getBean(DMSBlockService.class).delete(id);
			} else {
				blockService.delete(id);
			}
			
			return ResponseDto.builder().success(true).message(ResponseEnum.SUCCESS.getErrorDescription()).build();
		} catch (Exception ex) {
			return ResponseDto.builder().success(false).message(ex.getMessage()).build();
		}
	}
}
