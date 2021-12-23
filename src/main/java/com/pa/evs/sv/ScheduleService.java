package com.pa.evs.sv;

import com.pa.evs.dto.GetReportTaskResponseDto;
import com.pa.evs.dto.GroupTaskDto;
import com.pa.evs.dto.PaginDto;
import com.pa.evs.dto.ReportFileDto;
import com.pa.evs.dto.ReportScheduleDto;
import com.pa.evs.dto.ScheduleDto;
import com.pa.evs.exception.ApiException;
import com.pa.evs.model.GroupTask;

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public interface ScheduleService {
    void createSchedule(ScheduleDto data);
    void removeSchedule(Long id) throws ApiException;
    List<GroupTask> findAllByGroupId(Long groupId);
    void searchAllSchedule(PaginDto<GroupTaskDto> pagin);
    void createReportSchedule(ReportScheduleDto data);
    void removeReportTaskSchedule(Long id);
    void getTaskReport(PaginDto<GetReportTaskResponseDto> pagin, Long reportId);
    void getReportFiles(HttpServletRequest httpServletRequest, PaginDto<ReportFileDto> pagin);
    void downloadReportFileById(HttpServletResponse response, Long reportFileId);
    void getReportFileById(PaginDto<ReportFileDto> pagin, Long reportTaskId);
}
