package com.pa.evs.sv;

import com.pa.evs.dto.ScheduleDto;
import com.pa.evs.exception.ApiException;
import com.pa.evs.model.Group;
import com.pa.evs.model.GroupTask;

import java.util.List;

public interface ScheduleService {
    List<GroupTask> findAll();
    void createSchedule(ScheduleDto data);
    void removeSchedule(Long id) throws ApiException;
    List<GroupTask> findAllByGroupId(Long groupId);
}
