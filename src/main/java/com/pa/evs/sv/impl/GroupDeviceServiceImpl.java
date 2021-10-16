package com.pa.evs.sv.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.pa.evs.dto.GroupDto;
import com.pa.evs.dto.PaginDto;
import com.pa.evs.model.Group;
import com.pa.evs.repository.GroupRepository;
import com.pa.evs.sv.GroupService;

@Service
public class GroupDeviceServiceImpl implements GroupService {
    
    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(EVSPAServiceImpl.class);
    
    @Autowired EntityManager em;
    
    @Autowired GroupRepository groupRepository;

    @Override
    public void addGroupDevice(GroupDto dto) throws IOException {
        Group entity = new Group();
        entity.setName(dto.getName());
        entity.setRemark(dto.getRemark());
        groupRepository.save(entity);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void getGroupDevies(PaginDto<GroupDto> pagin) {
        StringBuilder sqlBuilder = new StringBuilder("FROM Group");
        StringBuilder sqlCountBuilder = new StringBuilder("SELECT count(*) FROM Group");
        
        StringBuilder sqlCommonBuilder = new StringBuilder();
        sqlCommonBuilder.append(" WHERE 1=1 ");
        sqlBuilder.append(sqlCommonBuilder).append(" ORDER BY id DESC");
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
        
        List<Group> list = query.getResultList();
        
        list.forEach(li -> {
            GroupDto dto = GroupDto.builder()
                    .id(li.getId())
                    .name(li.getName())
                    .remark(li.getRemark())
                    .build();
            pagin.getResults().add(dto);
        });        
    }

    @Override
    public void deleteGroupDevice(Long id) throws Exception {
        
        Optional<Group> opt = groupRepository.findById(id);
        if (!opt.isPresent()) {
            throw new Exception(String.format("No group device with id %d found!", id));
        }
        
        try {
            groupRepository.deleteById(id);
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        }
    }
}
