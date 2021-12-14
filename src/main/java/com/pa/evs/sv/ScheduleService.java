package com.pa.evs.sv;

import com.pa.evs.dto.PaginDto;
import com.pa.evs.dto.ReportScheduleDto;
import com.pa.evs.dto.ScheduleDto;
import com.pa.evs.exception.ApiException;
import com.pa.evs.model.GroupTask;

import java.util.List;

public interface ScheduleService {
    void createSchedule(ScheduleDto data);
    void removeSchedule(Long id) throws ApiException;
    List<GroupTask> findAllByGroupId(Long groupId);
    void searchAllSchedule(PaginDto<?> pagin);
    void createReportSchedule(ReportScheduleDto data);
    void removeReportSchedule(Long id);
    void searchAllReportSchedule(PaginDto<?> pagin);
}
