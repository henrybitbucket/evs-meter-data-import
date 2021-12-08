package com.pa.evs.sv.impl;

import com.pa.evs.dto.PaginDto;
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

import javax.persistence.EntityManager;
import javax.persistence.Query;
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

    @Autowired
    EntityManager em;

    @Override
    public void createSchedule(ScheduleDto data) {
        Optional<Group> group = groupRepository.findById(data.getGroupId());
        GroupTask groupTask = data.getId() == null ? new GroupTask() : groupTaskRepository.findById(data.getId()).orElse(new GroupTask());
        groupTask.setCommand(data.getCommand());
        groupTask.setType(data.getType());
        groupTask.setGroup(group.get());
        groupTask.setStartTime(data.getStartTime());
        groupTask.setCreateDate(Calendar.getInstance().getTime());
        boolean isNew = groupTask.getId() == null;
        groupTask = groupTaskRepository.save(groupTask);
        if (!isNew) {
            webSchedule.removeSchedule(new GroupTaskSchedule(groupTask, evsPAService));
        }
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

    @Override
    public void searchAllSchedule(PaginDto<?> pagin){
        StringBuilder sqlBuilder = new StringBuilder(" ");
        StringBuilder sqlCountBuilder = new StringBuilder(" SELECT count(*) ");
        StringBuilder cmBuilder = new StringBuilder(" FROM GroupTask");
        sqlBuilder.append(cmBuilder).append(" ORDER BY startTime DESC ");
        sqlCountBuilder.append(cmBuilder);

        Long count = ((Number)em.createQuery(sqlCountBuilder.toString()).getSingleResult()).longValue();

        Query query = em.createQuery(sqlBuilder.toString());
        query.setFirstResult(pagin.getOffset());
        query.setMaxResults(pagin.getLimit());
        pagin.getResults().clear();
        pagin.setResults(query.getResultList());
        pagin.setTotalRows(count);
    }

}
