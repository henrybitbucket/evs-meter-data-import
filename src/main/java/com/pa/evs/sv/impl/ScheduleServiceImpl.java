package com.pa.evs.sv.impl;

import com.pa.evs.dto.ScheduleDto;
import com.pa.evs.enums.ResponseEnum;
import com.pa.evs.exception.ApiException;
import com.pa.evs.model.Group;
import com.pa.evs.model.GroupTask;
import com.pa.evs.repository.GroupRepository;
import com.pa.evs.repository.GroupTaskRepository;
import com.pa.evs.schedule.GroupTaskSchedule;
import com.pa.evs.schedule.WebSchedule;
import com.pa.evs.sv.EVSPAService;
import com.pa.evs.sv.ScheduleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Calendar;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class ScheduleServiceImpl implements ScheduleService {

    @Autowired
    private WebSchedule webSchedule;

    @Autowired
    EVSPAService evsPAService;

    @Autowired
    private GroupTaskRepository groupTaskRepository;

    @Autowired
    private GroupRepository groupRepository;

    @Override
    public List<GroupTask> findAll() {
        return groupTaskRepository.findAll();
    }

    @Override
    public void createSchedule(ScheduleDto data) {
        Optional<Group> group = groupRepository.findById(data.getGroupId());
        GroupTask groupTask = new GroupTask();
        groupTask.setCommand(data.getCommand());
        groupTask.setType(data.getType());
        groupTask.setGroup(group.get());
        groupTask.setStartTime(data.getStartTime());
        groupTask.setCreateDate(Calendar.getInstance().getTime());
        groupTask = groupTaskRepository.save(groupTask);
        webSchedule.addSchedule(new GroupTaskSchedule(groupTask, evsPAService));
    }

    @Override
    public void removeSchedule(Long id) throws ApiException {
        Optional<GroupTask> groupTask = groupTaskRepository.findById(id);
        if (!groupTask.isPresent()) {
            throw new ApiException(ResponseEnum.TASK_IS_NOT_EXISTS);
        }
        webSchedule.removeSchedule(new GroupTaskSchedule(groupTask.get(), evsPAService));
        groupTaskRepository.delete(groupTask.get());
    }

    @Override
    public List<GroupTask> findAllByGroupId(Long groupId){
        return groupTaskRepository.findByGroupId(groupId);
    }

}
