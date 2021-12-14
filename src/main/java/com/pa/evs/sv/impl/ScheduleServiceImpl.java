package com.pa.evs.sv.impl;

import com.pa.evs.dto.PaginDto;
import com.pa.evs.dto.ReportScheduleDto;
import com.pa.evs.dto.ScheduleDto;
import com.pa.evs.enums.JasperFormat;
import com.pa.evs.enums.ResponseEnum;
import com.pa.evs.exception.ApiException;
import com.pa.evs.model.Group;
import com.pa.evs.model.GroupTask;
import com.pa.evs.model.Report;
import com.pa.evs.model.ReportTask;
import com.pa.evs.repository.GroupRepository;
import com.pa.evs.repository.GroupTaskRepository;
import com.pa.evs.repository.ReportRepository;
import com.pa.evs.repository.ReportTaskRepository;
import com.pa.evs.schedule.GroupTaskSchedule;
import com.pa.evs.schedule.ReportTaskSchedule;
import com.pa.evs.schedule.WebSchedule;
import com.pa.evs.sv.EVSPAService;
import com.pa.evs.sv.ReportService;
import com.pa.evs.sv.ScheduleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
    private ReportService reportService;

    @Autowired
    private GroupTaskRepository groupTaskRepository;

    @Autowired
    private GroupRepository groupRepository;
    
    @Autowired
    private ReportRepository reportRepository;
    
    @Autowired
    private ReportTaskRepository reportTaskRepository;
    
    
    @Autowired
    EntityManager em;

    @Value("${evs.pa.mqtt.publish.topic.alias}") private String alias;

    @Value("${evs.pa.privatekey.path}")
    private String pkPath;

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
            webSchedule.removeSchedule(new GroupTaskSchedule(groupTask, evsPAService, alias, pkPath));
        }
        webSchedule.addSchedule(new GroupTaskSchedule(groupTask, evsPAService, alias, pkPath));
    }

    @Override
    public void removeSchedule(Long id) throws ApiException {
        Optional<GroupTask> groupTask = groupTaskRepository.findById(id);
        if (!groupTask.isPresent()) {
            throw new ApiException(ResponseEnum.TASK_IS_NOT_EXISTS);
        }
        webSchedule.removeSchedule(new GroupTaskSchedule(groupTask.get(), evsPAService, alias, pkPath));
        groupTaskRepository.delete(groupTask.get());
    }

    @Override
    public List<GroupTask> findAllByGroupId(Long groupId){
        return groupTaskRepository.findByGroupId(groupId);
    }

    @Override
    public void searchAllSchedule(PaginDto<?> pagin) {
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
    
    @Override
    public void createReportSchedule(ReportScheduleDto data) {
    	Optional<Report> report = reportRepository.findById(data.getReportId());
        ReportTask reportTask = data.getId() == null ? new ReportTask() : reportTaskRepository.findById(data.getId()).orElse(new ReportTask());
        String parameter = data.getParameter();
        JasperFormat format = data.getFormat();
        reportTask.setType(data.getType());
        reportTask.setReport(report.get());
        reportTask.setStartTime(data.getStartTime());
        reportTask.setCreateDate(Calendar.getInstance().getTime());
        boolean isNew = reportTask.getId() == null;
        reportTask = reportTaskRepository.save(reportTask);
        if (!isNew) {
            webSchedule.removeSchedule(new ReportTaskSchedule(reportTask, parameter, format, reportService, reportTaskRepository, evsPAService, alias, pkPath));
        }
        webSchedule.addSchedule(new ReportTaskSchedule(reportTask, parameter, format, reportService, reportTaskRepository, evsPAService, alias, pkPath));
    }
    
    @Override
    public void removeReportSchedule(Long id) throws ApiException {
        Optional<ReportTask> reportTask = reportTaskRepository.findById(id);
        String parameter = "";
        JasperFormat format = null;
        if (!reportTask.isPresent()) {
            throw new ApiException(ResponseEnum.TASK_IS_NOT_EXISTS);
        }
        webSchedule.removeSchedule(new ReportTaskSchedule(reportTask.get(), parameter, format, reportService, reportTaskRepository, evsPAService, alias, pkPath));
        reportTaskRepository.delete(reportTask.get());
    }
    
    @Override
    public void searchAllReportSchedule(PaginDto<?> pagin) {
        StringBuilder sqlBuilder = new StringBuilder(" ");
        StringBuilder sqlCountBuilder = new StringBuilder(" SELECT count(*) ");
        StringBuilder cmBuilder = new StringBuilder(" FROM ReportTask");
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
