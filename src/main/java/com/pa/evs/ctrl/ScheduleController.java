package com.pa.evs.ctrl;

import com.pa.evs.converter.ExceptionConvertor;
import com.pa.evs.dto.GetGroupTaskResponseDto;
import com.pa.evs.dto.PaginDto;
import com.pa.evs.dto.ReportScheduleDto;
import com.pa.evs.dto.ResponseDto;
import com.pa.evs.dto.ScheduleDto;
import com.pa.evs.enums.ResponseEnum;
import com.pa.evs.sv.ScheduleService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

@RestController
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

    @GetMapping("/api/schedule/group-task")
    public Object getGroupTask(HttpServletRequest request) {
        try {
            PaginDto<?> pagin = new PaginDto<>();
            pagin.setOffset(request.getParameter("offset"));
            pagin.setLimit(request.getParameter("limit"));
            scheduleService.searchAllSchedule(pagin);
            return pagin;
        }catch (Exception ex) {
            return exceptionConvertor.createResponseDto(ex);
        }
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
    public ResponseDto removeReportSchedule(@PathVariable Long id) {
        try {
            logger.info("invoke removeReportSchedule, scheduleId: {} ", id);
            scheduleService.removeReportSchedule(id);
            return ResponseDto.builder().success(true).message(ResponseEnum.SUCCESS.getErrorDescription()).build();
        } catch (Exception ex) {
            return exceptionConvertor.createResponseDto(ex);
        }
    }
    
    @GetMapping("/api/schedule/report-task")
    public Object getReportTask(HttpServletRequest request) {
        try {
            PaginDto<?> pagin = new PaginDto<>();
            pagin.setOffset(request.getParameter("offset"));
            pagin.setLimit(request.getParameter("limit"));
            scheduleService.searchAllReportSchedule(pagin);
            return pagin;
        }catch (Exception ex) {
            return exceptionConvertor.createResponseDto(ex);
        }
    }


}
