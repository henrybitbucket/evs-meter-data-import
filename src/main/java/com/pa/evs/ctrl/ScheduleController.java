package com.pa.evs.ctrl;

import com.pa.evs.converter.ExceptionConvertor;
import com.pa.evs.dto.GetGroupTaskResponseDto;
import com.pa.evs.dto.GetReportTaskResponseDto;
import com.pa.evs.dto.GroupTaskDto;
import com.pa.evs.dto.PaginDto;
import com.pa.evs.dto.ReportFileDto;
import com.pa.evs.dto.ReportScheduleDto;
import com.pa.evs.dto.ResponseDto;
import com.pa.evs.dto.ScheduleDto;
import com.pa.evs.enums.ResponseEnum;
import com.pa.evs.sv.ScheduleService;

import springfox.documentation.annotations.ApiIgnore;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;


import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@RestController
@ApiIgnore
public class ScheduleController {

    static final Logger logger = LogManager.getLogger(ScheduleController.class);

    @Autowired
    private ScheduleService scheduleService;

    @Autowired
    private ExceptionConvertor exceptionConvertor;

    @PostMapping("/api/schedule/group-task/create")
    public ResponseDto createSchedule(@RequestBody ScheduleDto data) {
        try {
            logger.info("invoke createSchedule, groupId: {}, type: {}, command: {} "
                    , data.getGroupId(), data.getType().name(), data.getCommand().name());
            scheduleService.createSchedule(data);
            return ResponseDto.builder().success(true).message(ResponseEnum.SUCCESS.getErrorDescription()).build();
        } catch (Exception ex) {
            return exceptionConvertor.createResponseDto(ex);
        }
    }

    @DeleteMapping("/api/schedule/group-task/{id}/remove")
    public ResponseDto removeSchedule(@PathVariable Long id) {
        try {
            logger.info("invoke removeSchedule, scheduleId: {} ", id);
            scheduleService.removeSchedule(id);
            return ResponseDto.builder().success(true).message(ResponseEnum.SUCCESS.getErrorDescription()).build();
        } catch (Exception ex) {
            return exceptionConvertor.createResponseDto(ex);
        }
    }

    @GetMapping("/api/schedule/group-task/{groupId}")
    public ResponseDto<?> getGroupTaskGroupId(@PathVariable(name = "groupId") Long groupId, HttpServletRequest request) {
        try {
            logger.info("invoke getGroupTaskGroupId, groupId: {} ", groupId);
            GetGroupTaskResponseDto dto = new GetGroupTaskResponseDto();
            dto.setResults(scheduleService.findAllByGroupId(groupId));
            return ResponseDto.<GetGroupTaskResponseDto>builder().success(true).response(dto).build();
        }catch (Exception ex) {
            return exceptionConvertor.createResponseDto(ex);
        }
    }
    
    @PostMapping("/api/schedule/group-task")
    public ResponseEntity<Object> getGroupTask(HttpServletRequest httpServletRequest, @RequestBody PaginDto<GroupTaskDto> pagin) throws Exception {
        try {
        	scheduleService.searchAllSchedule(pagin);
        } catch (Exception e) {
        	e.printStackTrace();
            return ResponseEntity.ok(ResponseDto.builder().success(false).message(e.getMessage()).build());
        }
        return ResponseEntity.ok(ResponseDto.builder().success(true).response(pagin).build());
    }
    
    @PostMapping("/api/schedule/report-task/create")
    public ResponseDto createReportSchedule(@RequestBody ReportScheduleDto data) {
        try {
            logger.info("invoke createReportSchedule, groupId: {}, type: {}, parameter: {}, format: {} "
                    , data.getReportId(), data.getType().name(), data.getParameter(), data.getFormat());
            scheduleService.createReportSchedule(data);
            return ResponseDto.builder().success(true).message(ResponseEnum.SUCCESS.getErrorDescription()).build();
        } catch (Exception ex) {
            return exceptionConvertor.createResponseDto(ex);
        }
    }
    
    @DeleteMapping("/api/schedule/report-task/{id}/remove")
    public ResponseDto removeReportTaskSchedule(@PathVariable Long id) {
        try {
            logger.info("invoke removeReportTaskSchedule, Report Task ID: {} ", id);
            scheduleService.removeReportTaskSchedule(id);
            return ResponseDto.builder().success(true).message(ResponseEnum.SUCCESS.getErrorDescription()).build();
        } catch (Exception ex) {
            return exceptionConvertor.createResponseDto(ex);
        }
    }
   
    
    @PostMapping("/api/schedule/report-task/{reportId}")
    public ResponseEntity<Object> getReports(HttpServletRequest httpServletRequest, @RequestBody PaginDto<GetReportTaskResponseDto> pagin, 
    		@PathVariable(name = "reportId") Long reportId) throws Exception {
        try {
        	logger.info("invoke getGroupTaskReportId, reportId: {} ", reportId);
        	scheduleService.getTaskReport(pagin, reportId);
        } catch (Exception e) {
            return ResponseEntity.ok(ResponseDto.builder().success(false).message(e.getMessage()).build());
        }
        return ResponseEntity.ok(ResponseDto.builder().success(true).response(pagin).build());
    }
    
    
    @PostMapping("/api/schedule/report-file")
    public ResponseEntity<Object> getReports(HttpServletRequest httpServletRequest, @RequestBody PaginDto<ReportFileDto> pagin) throws Exception {
        try {
        	scheduleService.getReportFiles(httpServletRequest, pagin);
        } catch (Exception e) {
            return ResponseEntity.ok(ResponseDto.builder().success(false).message(e.getMessage()).build());
        }
        return ResponseEntity.ok(ResponseDto.builder().success(true).response(pagin).build());
    }
    
    @PostMapping("/api/schedule/report-file/{reportTaskId}")
    public ResponseEntity<Object> getReportById(HttpServletRequest httpServletRequest, @RequestBody PaginDto<ReportFileDto> pagin, @PathVariable(name = "reportTaskId") Long reportTaskId) throws Exception {
        try {
        	scheduleService.getReportFileById(pagin, reportTaskId);
        } catch (Exception e) {
            return ResponseEntity.ok(ResponseDto.builder().success(false).message(e.getMessage()).build());
        }
        return ResponseEntity.ok(ResponseDto.builder().success(true).response(pagin).build());
    }
    
    @PostMapping("/api/schedule/report-file/download/{reportFileId}")
    public ResponseEntity<Object> getReportTaskFile(HttpServletRequest httpServletRequest, HttpServletResponse response, @PathVariable(name = "reportFileId") Long reportFileId) throws Exception {
        try {
            logger.debug("report task Id: " + reportFileId);
            
            scheduleService.downloadReportFileById(response, reportFileId);
            return ResponseEntity.ok(ResponseDto.builder().success(true).build());
        } catch (Exception e) {
            logger.error("", e);
            return ResponseEntity.ok(ResponseDto.builder().success(false).message(e.getMessage()).build());
        }
    }
    

}
