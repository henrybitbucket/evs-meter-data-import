package com.pa.evs.sv.impl;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import javax.annotation.PostConstruct;
import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.transaction.Transactional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.pa.evs.dto.PaginDto;
import com.pa.evs.dto.ProjectTagDto;
import com.pa.evs.exception.ApiException;
import com.pa.evs.model.ProjectTag;
import com.pa.evs.repository.ProjectTagRepository;
import com.pa.evs.sv.ProjectTagService;
import com.pa.evs.utils.SecurityUtils;

@Service
public class ProjectTagServiceImpl implements ProjectTagService {
	
	private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger(ProjectTagServiceImpl.class);
	
	@Autowired
	private ProjectTagRepository projectTagRepository;
	
	@Autowired
	private EntityManager em;

	@Override
	public void save(ProjectTagDto dto) {
		if (dto.getId() != null) {
			LOGGER.info("Edit Project Tag");
			
			if (dto.getName().equalsIgnoreCase("all") || dto.getName().equalsIgnoreCase("na")) {
				throw new ApiException("This tag can't be edited!");
			}
			Optional<ProjectTag> tagOpt = projectTagRepository.findById(dto.getId());
			if (!tagOpt.isPresent()) {
				throw new ApiException("Project tag not found!");
			}
			
			ProjectTag tag = tagOpt.get();
			List<ProjectTag> otherTags = projectTagRepository.findByNameAndDifferenceId(dto.getName(), tag.getId());
			if (!otherTags.isEmpty()) {
				throw new ApiException("Project tag name already exists!");
			}
			
			tag.setName(dto.getName());
			tag.setDescription(dto.getDescription());
			tag.setModifyDate(new Date());		
			tag.setUpdatedBy(SecurityUtils.getEmail());
			projectTagRepository.save(tag);
		} else {
			LOGGER.info("Create new Project Tag");
			
			Optional<ProjectTag> tagOpt = projectTagRepository.findByName(dto.getName());
			if (tagOpt.isPresent() || dto.getName().equalsIgnoreCase("all") || dto.getName().equalsIgnoreCase("na")) {
				throw new ApiException("Project tag name already exists!");
			}
			
			ProjectTag tag = new ProjectTag();
			tag.setName(dto.getName());
			tag.setDescription(dto.getDescription());
			projectTagRepository.save(tag);
		}
		
	}

	@Override
	@Transactional
	public void delete(Long tagId) {
		LOGGER.info("Delete Project Tag");
		
		Optional<ProjectTag> tagOpt = projectTagRepository.findById(tagId);
		if (!tagOpt.isPresent()) {
			throw new ApiException("Project tag not found!");
		}
		if (tagOpt.get().getName().equalsIgnoreCase("all") || tagOpt.get().getName().equalsIgnoreCase("na")) {
			throw new ApiException("This tag can't be deleted!");
		}
		
		projectTagRepository.deleteById(tagId);
	}

	@Override
	public PaginDto<ProjectTagDto> search(PaginDto<ProjectTagDto> pagin) {
		LOGGER.info("Get Project Tag List");
		
		StringBuilder sqlBuilder = new StringBuilder("FROM ProjectTag tag ");
		StringBuilder sqlCountBuilder = new StringBuilder("SELECT count(*) FROM ProjectTag tag");

		StringBuilder sqlCommonBuilder = new StringBuilder();
		sqlCommonBuilder.append(" WHERE 1=1 ");
		
		sqlBuilder.append(sqlCommonBuilder).append(" ORDER BY id ASC");
		sqlCountBuilder.append(sqlCommonBuilder);

		Query queryCount = em.createQuery(sqlCountBuilder.toString());

		Long count = ((Number) queryCount.getSingleResult()).longValue();
		pagin.setTotalRows(count);
		pagin.setResults(new ArrayList<>());
		if (count == 0l) {
			return pagin;
		}

		Query query = em.createQuery(sqlBuilder.toString());
		query.setFirstResult(pagin.getOffset());
		query.setMaxResults(pagin.getLimit());

		List<ProjectTag> list = query.getResultList();
		
		list.forEach(li -> {
			ProjectTagDto dto = ProjectTagDto.build(li);
            pagin.getResults().add(dto);
        });
		
		return pagin;
		
	}
	
	@PostConstruct
	@Transactional
	public void init() {
		if (!projectTagRepository.findByName("ALL").isPresent()) {
			ProjectTag tag = new ProjectTag();
			tag.setName("ALL");
			tag.setDescription("ALL");
			projectTagRepository.save(tag);
		}
		if (!projectTagRepository.findByName("NA").isPresent()) {
			ProjectTag tag = new ProjectTag();
			tag.setName("NA");
			tag.setDescription("NA");
			projectTagRepository.save(tag);
		}
	}
	

}
