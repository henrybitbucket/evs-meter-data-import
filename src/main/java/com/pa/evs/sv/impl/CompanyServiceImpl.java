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

import com.pa.evs.dto.CompanyDto;
import com.pa.evs.dto.PaginDto;
import com.pa.evs.exception.ApiException;
import com.pa.evs.model.Company;
import com.pa.evs.repository.AppCodeRepository;
import com.pa.evs.repository.CompanyRepository;
import com.pa.evs.repository.UserCompanyRepository;
import com.pa.evs.sv.CompanyService;
import com.pa.evs.utils.AppCodeSelectedHolder;

@Service
public class CompanyServiceImpl implements CompanyService {
	
	private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger(CompanyServiceImpl.class);
	
	@Autowired
	private CompanyRepository companyRepository;
	
	@Autowired
	AppCodeRepository appCodeRepository;
	
	@Autowired
	UserCompanyRepository userCompanyRepository;
	
	@Autowired
	private EntityManager em;

	@Override
	public void save(CompanyDto dto) {
		if (dto.getId() != null) {
			LOGGER.info("Edit Company");
			
			if (dto.getName().equalsIgnoreCase("all") || dto.getName().equalsIgnoreCase("na")) {
				throw new ApiException("This cpn can't be edited!");
			}
			Optional<Company> cpnOpt = companyRepository.findById(dto.getId());
			if (!cpnOpt.isPresent()) {
				throw new ApiException("Company not found!");
			}
			
			Company cpn = cpnOpt.get();
			List<Company> otherTags = companyRepository.findByNameAndAppCodeAndDifferenceId(dto.getName(), AppCodeSelectedHolder.get(), cpn.getId());
			if (!otherTags.isEmpty()) {
				throw new ApiException("Company name already exists!");
			}
			
			cpn.setName(dto.getName());
			cpn.setDescription(dto.getDescription());
			cpn.setModifyDate(new Date());		
			companyRepository.save(cpn);
		} else {
			LOGGER.info("Create new Company");
			
			Optional<Company> cpnOpt = companyRepository.findByNameAndAppCodeName(dto.getName(), AppCodeSelectedHolder.get());
			if (cpnOpt.isPresent() || dto.getName().equalsIgnoreCase("all") || dto.getName().equalsIgnoreCase("na")) {
				throw new ApiException("Company name already exists!");
			}
			
			Company cpn = new Company();
			cpn.setName(dto.getName());
			cpn.setDescription(dto.getDescription());
			cpn.setAppCode(appCodeRepository.findByName(AppCodeSelectedHolder.get()));
			companyRepository.save(cpn);
		}
		
	}

	@Override
	@Transactional
	public void delete(Long cpnId) {
		LOGGER.info("Delete Company");
		
		Optional<Company> cpnOpt = companyRepository.findById(cpnId);
		if (!cpnOpt.isPresent()) {
			throw new ApiException("Company not found!");
		}
		if (cpnOpt.get().getName().equalsIgnoreCase("all") || cpnOpt.get().getName().equalsIgnoreCase("na")) {
			throw new ApiException("This Company can't be deleted!");
		}
		
		userCompanyRepository.findByCompanyId(cpnId)
		.forEach(userCompanyRepository::delete);
		
		userCompanyRepository.flush();
		
		companyRepository.deleteById(cpnId);
	}

	@SuppressWarnings("unchecked")
	@Override
	public PaginDto<CompanyDto> search(PaginDto<CompanyDto> pagin) {
		LOGGER.info("Get Company List");
		
		StringBuilder sqlBuilder = new StringBuilder("FROM Company cpn ");
		StringBuilder sqlCountBuilder = new StringBuilder("SELECT count(*) FROM Company cpn");

		StringBuilder sqlCommonBuilder = new StringBuilder();
		sqlCommonBuilder.append(" WHERE 1=1 ");
		
		sqlCommonBuilder.append(" AND cpn.appCode.name = '" + AppCodeSelectedHolder.get() + "' ");
		
		sqlBuilder.append(sqlCommonBuilder).append(" ORDER BY cpn.id ASC");
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

		List<Company> list = query.getResultList();
		
		list.forEach(li -> {
			CompanyDto dto = CompanyDto.build(li);
            pagin.getResults().add(dto);
        });
		
		return pagin;
		
	}
	
	@PostConstruct
	@Transactional
	public void init() {
		if (!companyRepository.findByNameAndAppCodeName("ALL", "DMS").isPresent()) {
			Company cpn = new Company();
			cpn.setName("ALL");
			cpn.setDescription("ALL");
			cpn.setAppCode(appCodeRepository.findByName("DMS"));
			companyRepository.save(cpn);
		}
		if (!companyRepository.findByNameAndAppCodeName("NA", "DMS").isPresent()) {
			Company cpn = new Company();
			cpn.setName("NA");
			cpn.setDescription("NA");
			cpn.setAppCode(appCodeRepository.findByName("DMS"));
			companyRepository.save(cpn);
		}
		if (!companyRepository.findByNameAndAppCodeName("ALL", "MMS").isPresent()) {
			Company cpn = new Company();
			cpn.setName("ALL");
			cpn.setDescription("ALL");
			cpn.setAppCode(appCodeRepository.findByName("MMS"));
			companyRepository.save(cpn);
		}
		if (!companyRepository.findByNameAndAppCodeName("NA", "MMS").isPresent()) {
			Company cpn = new Company();
			cpn.setName("NA");
			cpn.setDescription("NA");
			cpn.setAppCode(appCodeRepository.findByName("MMS"));
			companyRepository.save(cpn);
		}
	}
	

}
