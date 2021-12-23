package com.pa.evs.sv.impl;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.jfree.util.Log;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pa.evs.dto.GetReportTaskResponseDto;
import com.pa.evs.dto.GroupTaskDto;
import com.pa.evs.dto.PaginDto;
import com.pa.evs.dto.ReportDto;
import com.pa.evs.dto.ReportFileDto;
import com.pa.evs.dto.ReportScheduleDto;
import com.pa.evs.dto.ScheduleDto;
import com.pa.evs.enums.JasperFormat;
import com.pa.evs.enums.ResponseEnum;
import com.pa.evs.exception.ApiException;
import com.pa.evs.model.Group;
import com.pa.evs.model.GroupTask;
import com.pa.evs.model.Report;
import com.pa.evs.model.ReportFile;
import com.pa.evs.model.ReportTask;
import com.pa.evs.model.Users;
import com.pa.evs.repository.GroupRepository;
import com.pa.evs.repository.GroupTaskRepository;
import com.pa.evs.repository.ReportFileRepository;
import com.pa.evs.repository.ReportRepository;
import com.pa.evs.repository.ReportTaskRepository;
import com.pa.evs.repository.UserRepository;
import com.pa.evs.schedule.GroupTaskSchedule;
import com.pa.evs.schedule.ReportTaskSchedule;
import com.pa.evs.schedule.WebSchedule;
import com.pa.evs.sv.EVSPAService;
import com.pa.evs.sv.ReportService;
import com.pa.evs.sv.ScheduleService;
import com.pa.evs.utils.SecurityUtils;

@Service
@Transactional
@SuppressWarnings("unchecked")
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
    private ReportFileRepository reportFileRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    
    @Autowired
    EntityManager em;

    @Value("${evs.pa.mqtt.publish.topic.alias}") private String alias;

    @Value("${evs.pa.privatekey.path}")
    private String pkPath;

    @Override
    public void createSchedule(ScheduleDto data) {
    	Users user = userRepository.findByEmail(SecurityUtils.getEmail());
        Optional<Group> group = groupRepository.findById(data.getGroupId());
        GroupTask groupTask = data.getId() == null ? new GroupTask() : groupTaskRepository.findById(data.getId()).orElse(new GroupTask());
        groupTask.setCommand(data.getCommand());
        groupTask.setType(data.getType());
        groupTask.setGroup(group.get());
        groupTask.setStartTime(data.getStartTime());
        groupTask.setCreateDate(Calendar.getInstance().getTime());
        groupTask.setUser(user);
        boolean isNew = groupTask.getId() == null;
        groupTask = groupTaskRepository.save(groupTask);
        if (!isNew) {
            webSchedule.removeSchedule(new GroupTaskSchedule(groupTask, user, evsPAService, alias, pkPath));
        }
        webSchedule.addSchedule(new GroupTaskSchedule(groupTask, user, evsPAService, alias, pkPath));
    }

    @Override
    public void removeSchedule(Long id) throws ApiException {
    	Users user = userRepository.findByEmail(SecurityUtils.getEmail());
        Optional<GroupTask> groupTask = groupTaskRepository.findById(id);
        if (!groupTask.isPresent()) {
            throw new ApiException(ResponseEnum.TASK_IS_NOT_EXISTS);
        }
        webSchedule.removeSchedule(new GroupTaskSchedule(groupTask.get(), user, evsPAService, alias, pkPath));
        groupTaskRepository.deleteTaskLog(groupTask.get().getId());
        groupTaskRepository.delete(groupTask.get());
    }

    @Override
    public List<GroupTask> findAllByGroupId(Long groupId){
        return groupTaskRepository.findByGroupId(groupId);
    }

    @Override
    public void searchAllSchedule(PaginDto<GroupTaskDto> pagin) {
    	StringBuilder sqlBuilder = new StringBuilder("FROM GroupTask");
        StringBuilder sqlCountBuilder = new StringBuilder("SELECT count(*) FROM GroupTask");

        StringBuilder sqlCommonBuilder = new StringBuilder();
        sqlCommonBuilder.append(" WHERE 1=1 ");
        sqlBuilder.append(sqlCommonBuilder).append(" ORDER BY createDate DESC");
        sqlCountBuilder.append(sqlCommonBuilder);

        if (pagin.getOffset() == null || pagin.getOffset() < 0) {
            pagin.setOffset(0);
        }

        if (pagin.getLimit() == null || pagin.getLimit() <= 0) {
            pagin.setLimit(100);
        }

        Query queryCount = em.createQuery(sqlCountBuilder.toString());

        Long count = ((Number)queryCount.getSingleResult()).longValue();
        pagin.setTotalRows(count);
        pagin.setResults(new ArrayList<>());
        if (count == 0l) {
            return;
        }

        Query query = em.createQuery(sqlBuilder.toString());
        query.setFirstResult(pagin.getOffset());
        query.setMaxResults(pagin.getLimit());

        List<GroupTask> list = query.getResultList();

        list.forEach(li -> {
        	if(Objects.isNull(li.getUser())) {
        		GroupTaskDto dto = GroupTaskDto.builder()
                        .id(li.getId())
                        .command(li.getCommand())     
                        .type(li.getType())
                        .startTime(li.getStartTime())                     
                        .groupId(li.getGroup().getId())
                        .groupName(li.getGroup().getName())
                        .build();
                pagin.getResults().add(dto);
        	} else {
            GroupTaskDto dto = GroupTaskDto.builder()
                    .id(li.getId())
                    .command(li.getCommand())     
                    .type(li.getType())
                    .startTime(li.getStartTime())
                    .userId(li.getUser().getUserId())
                    .groupId(li.getGroup().getId())
                    .groupName(li.getGroup().getName())
                    .build();
            pagin.getResults().add(dto);
        	}
        }); 
    }
    
    @Override
    public void createReportSchedule(ReportScheduleDto data) {
    	Optional<Report> report = reportRepository.findById(data.getReportId());
        ReportTask reportTask = new ReportTask();
        String parameter = data.getParameter();
        JasperFormat format = data.getFormat();
        reportTask.setType(data.getType());
        reportTask.setReport(report.get());
        reportTask.setStartTime(data.getStartTime());
        reportTask.setCreateDate(Calendar.getInstance().getTime());
        reportTask = reportTaskRepository.save(reportTask);
        webSchedule.addSchedule(new ReportTaskSchedule(reportTask, reportFileRepository, em, parameter, format, reportService, reportTaskRepository, evsPAService, alias, pkPath));
    }
    
    @Override
    public void removeReportTaskSchedule(Long id) throws ApiException {
        Optional<ReportTask> reportTask = reportTaskRepository.findById(id);
        List<ReportFile> reportFiles = reportFileRepository.findByReportTaskId(id);
        String parameter = "";
        JasperFormat format = null;
        if (!reportTask.isPresent()) {
            throw new ApiException(ResponseEnum.TASK_IS_NOT_EXISTS);
        }
        webSchedule.removeSchedule(new ReportTaskSchedule(reportTask.get(), reportFileRepository, em, parameter, format, reportService, reportTaskRepository, evsPAService, alias, pkPath));
        reportTaskRepository.delete(reportTask.get());
        reportFileRepository.deleteAll(reportFiles);   
    }
    
	@Override
    public void getTaskReport(PaginDto<GetReportTaskResponseDto> pagin,  Long reportId) {
        StringBuilder sqlBuilder = new StringBuilder("FROM ReportTask");
        StringBuilder sqlCountBuilder = new StringBuilder("SELECT count(*) FROM ReportTask");

        StringBuilder sqlCommonBuilder = new StringBuilder();
        sqlCommonBuilder.append(" WHERE report = " + reportId);
        sqlBuilder.append(sqlCommonBuilder).append(" ORDER BY createDate DESC");
        sqlCountBuilder.append(sqlCommonBuilder);

        if (pagin.getOffset() == null || pagin.getOffset() < 0) {
            pagin.setOffset(0);
        }

        if (pagin.getLimit() == null || pagin.getLimit() <= 0) {
            pagin.setLimit(30);
        }

        Query queryCount = em.createQuery(sqlCountBuilder.toString());

        Long count = ((Number)queryCount.getSingleResult()).longValue();
        pagin.setTotalRows(count);
        pagin.setResults(new ArrayList<>());
        if (count == 0l) {
            return;
        }

        Query query = em.createQuery(sqlBuilder.toString());
        query.setFirstResult(pagin.getOffset());
        query.setMaxResults(pagin.getLimit());

        List<ReportTask> list = query.getResultList();

        list.forEach(li -> {
        	GetReportTaskResponseDto dto = GetReportTaskResponseDto.builder()
                    .id(li.getId())
                    .type(li.getType()) 
                    .startTime(li.getStartTime())
                    .build();
            pagin.getResults().add(dto);
        });
    }
    
    @Override
    public void getReportFiles(HttpServletRequest httpServletRequest, PaginDto<ReportFileDto> pagin) {
		pagin.setKeyword(httpServletRequest.getParameter("search"));
		pagin.getOptions().put("reportTaskId", (httpServletRequest.getParameter("reportTaskId")));
    	
    	StringBuilder sqlBuilder = new StringBuilder("FROM ReportFile");
        StringBuilder sqlCountBuilder = new StringBuilder("SELECT count(*) FROM ReportFile");

        StringBuilder sqlCommonBuilder = new StringBuilder();
        
        if (pagin.getOptions().get("reportTaskId") != null) {
        	sqlCommonBuilder.append("  WHERE reportTask = " + pagin.getOptions().get("reportTaskId"));
		} else {
			sqlCommonBuilder.append(" WHERE 1=0 ");
		}
        sqlBuilder.append(sqlCommonBuilder).append(" ORDER BY reportTask DESC");
        sqlCountBuilder.append(sqlCommonBuilder);
        

        if (pagin.getOffset() == null || pagin.getOffset() < 0) {
            pagin.setOffset(0);
        }

        if (pagin.getLimit() == null || pagin.getLimit() <= 0) {
            pagin.setLimit(30);
        }

        Query queryCount = em.createQuery(sqlCountBuilder.toString());

        Long count = ((Number)queryCount.getSingleResult()).longValue();
        pagin.setTotalRows(count);
        pagin.setResults(new ArrayList<>());
        if (count == 0l) {
            return;
        }

        Query query = em.createQuery(sqlBuilder.toString());
        query.setFirstResult(pagin.getOffset());
        query.setMaxResults(pagin.getLimit());

        List<ReportFile> list = query.getResultList();

        list.forEach(li -> {
        	ReportFileDto dto = ReportFileDto.builder()
                    .id(li.getId())
                    .reportId(li.getReportTask().getReport().getReportName() + " (" +li.getReportTask().getReport().getId()+ ")")
                    .reportTaskId(li.getReportTask().getId())                 
                    .fileName(li.getFileName())
                    .createDate(li.getCreateDate())
                    .fileFormat(li.getFileFormat())
                    .build();
            pagin.getResults().add(dto);
        });
    }
    
    @Override
    public void getReportFileById(PaginDto<ReportFileDto> pagin, Long reportTaskId) {
    	StringBuilder sqlBuilder = new StringBuilder("FROM ReportFile");
        StringBuilder sqlCountBuilder = new StringBuilder("SELECT count(*) FROM ReportFile");

        StringBuilder sqlCommonBuilder = new StringBuilder();
        sqlCommonBuilder.append(" WHERE reportTask = " + reportTaskId);
        sqlBuilder.append(sqlCommonBuilder).append(" ORDER BY reportTask DESC");
        sqlCountBuilder.append(sqlCommonBuilder);

        if (pagin.getOffset() == null || pagin.getOffset() < 0) {
            pagin.setOffset(0);
        }

        if (pagin.getLimit() == null || pagin.getLimit() <= 0) {
            pagin.setLimit(30);
        }

        Query queryCount = em.createQuery(sqlCountBuilder.toString());

        Long count = ((Number)queryCount.getSingleResult()).longValue();
        pagin.setTotalRows(count);
        pagin.setResults(new ArrayList<>());
        if (count == 0l) {
            return;
        }

        Query query = em.createQuery(sqlBuilder.toString());
        query.setFirstResult(pagin.getOffset());
        query.setMaxResults(pagin.getLimit());

        List<ReportFile> list = query.getResultList();

        list.forEach(li -> {
        	ReportFileDto dto = ReportFileDto.builder()
                    .id(li.getId())
                    .fileName(li.getFileName())
                    .fileFormat(li.getFileFormat())
                    .build();
            pagin.getResults().add(dto);
        });
    }
    
    @Override
    public void downloadReportFileById (HttpServletResponse response, Long reportFileId) {
    	Optional<ReportFile> reportFile = reportFileRepository.findById(reportFileId);
    	reportFile.get().getBinBlob();
    	response.setContentType("application/octet-stream");
    	response.setHeader("Content-disposition", "attachment; filename=" + reportFile.get().getFileName());
    	response.setHeader("name", reportFile.get().getFileName());
    	try {
			IOUtils.copy(reportFile.get().getBinBlob().getBinaryStream(), response.getOutputStream());
		} catch (Exception e) {
			Log.error(e.getMessage(), e);
		}
    }

}
