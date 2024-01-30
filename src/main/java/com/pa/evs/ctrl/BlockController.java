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


@SuppressWarnings("rawtypes")
@RestController
public class BlockController {

	@Autowired
	BlockService blockService;

	@Autowired
    private ExceptionConvertor exceptionConvertor;		
	
	@PostMapping("/api/block")
	public ResponseDto save(@RequestBody BlockDto block) {
		try {
			blockService.save(block);
			return ResponseDto.builder().success(true).message(ResponseEnum.SUCCESS.getErrorDescription()).build();
		} catch (Exception ex) {
			return exceptionConvertor.createResponseDto(ex);
		}
	}
	
	@PostMapping("/api/blocks")
	public PaginDto<BlockDto> search(@RequestBody PaginDto<BlockDto> pagin) {
		blockService.search(pagin);
		return pagin;
	}
	
	@DeleteMapping("/api/block/{id}")
	public ResponseDto delete(@PathVariable Long id) {
		try {
			blockService.delete(id);
			return ResponseDto.builder().success(true).message(ResponseEnum.SUCCESS.getErrorDescription()).build();
		} catch (Exception ex) {
			return ResponseDto.builder().success(false).message(ex.getMessage()).build();
		}
	}
}
