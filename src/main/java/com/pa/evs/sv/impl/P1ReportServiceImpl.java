package com.pa.evs.sv.impl;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.Session;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pa.evs.dto.P1ReportDto;
import com.pa.evs.dto.P1ReportItemDto;
import com.pa.evs.dto.PaginDto;
import com.pa.evs.model.CARequestLog;
import com.pa.evs.model.P1Report;
import com.pa.evs.model.P1ReportItem;
import com.pa.evs.repository.CARequestLogRepository;
import com.pa.evs.repository.P1ReportItemRepository;
import com.pa.evs.repository.P1ReportRepository;
import com.pa.evs.repository.UserRepository;
import com.pa.evs.sv.P1ReportService;
import com.pa.evs.utils.SecurityUtils;

@Service
public class P1ReportServiceImpl implements P1ReportService {
	
	@Autowired
	UserRepository userRepository;

	@Autowired
	CARequestLogRepository caRequestLogRepository;
	
	@Autowired
	private EntityManager em;
	
	@Autowired
	private P1ReportRepository p1ReportRepository;
	
	@Autowired
	private P1ReportItemRepository p1ReportItemRepository;
	
	
	ObjectMapper mapper = new ObjectMapper();

	@SuppressWarnings("unchecked")
	@Transactional
	@Override
	public void save(MultipartFile file) throws Exception {
		
		P1Report mcr = new P1Report();
		
		mcr.setRawContent(
				em.unwrap(Session.class).getLobHelper().createBlob(file.getInputStream(), file.getSize()));
		mcr.setFileName(file.getOriginalFilename());
		mcr.setUserSubmit(SecurityUtils.getEmail());
		mcr.setTimeSubmit(System.currentTimeMillis());
		mcr.setIsLatest(false);
		p1ReportRepository.save(mcr);
		
		Map<String, Object> mapCOntent = mapper.readValue(file.getInputStream(), Map.class);
		
		List<Map<String, Object>> categories = (List<Map<String, Object>>) mapCOntent.computeIfAbsent("categories", k -> new ArrayList<>());
		Map<String, Object> testItemsSummary = categories.stream().filter(it -> "Test Items Summary".equals(it.get("title"))).findFirst().orElse(new LinkedHashMap<>());
		List<Map<String, Object>> mcuItemsTestedInfo = (List<Map<String, Object>>) testItemsSummary.computeIfAbsent("items", k -> new ArrayList<>());
		Set<String> sns = new HashSet<>();
		for (Map<String, Object> mcuItem : mcuItemsTestedInfo) {
			Map<String, Map<String, Object>> mcuTestedInfo = new LinkedHashMap<>(); 
			((List<Map<String, Object>>) mcuItem.computeIfAbsent("mcuTestedInfo", k -> new ArrayList<>()))
					.forEach(tt -> mcuTestedInfo.put((String) tt.get("label"), tt));
			
			String sn = (String) mcuTestedInfo.computeIfAbsent("Tested MCU SN", k -> new LinkedHashMap<>()).get("value");
			if (sns.contains(sn)) {
				continue;
			}
			sns.add(sn);
			
			P1ReportItem p1ReportItem = new P1ReportItem();
			p1ReportItem.setP1Report(mcr);
			p1ReportItem.setSn(sn);
			p1ReportItem.setRawContent(mapper.writeValueAsString(mcuItem));
			p1ReportItem.setUserSubmit(mcr.getUserSubmit());
			p1ReportItem.setTimeSubmit(mcr.getTimeSubmit());
			p1ReportItem.setIsLatest(false);
			p1ReportItemRepository.save(p1ReportItem);
		}
	}
	
	
	
	@Transactional
	@Override
	public void save(List<MultipartFile> files) throws Exception {
		if (files == null || files.isEmpty()) {
			return;
		}
		for (MultipartFile file : files) {
			save(file);
		}
	}

	@Transactional(readOnly = false)
	@SuppressWarnings("unchecked")
	public void searchReportItem(PaginDto<Object> pagin) {
		
		Map<String, Object> options = pagin.getOptions();
		Number p1ReportId = (Number) options.get("p1ReportId");
		String sn = (String) options.get("querySn");
		String exportCSV = options.get("exportCSV") + "";
		
		pagin.setResults(new ArrayList<>());
		
		StringBuilder sqlBuilder = new StringBuilder("FROM P1ReportItem p1RPIt ");
		StringBuilder sqlCountBuilder = new StringBuilder("select count(*) FROM P1ReportItem p1RPIt ");
		StringBuilder sqlCommonBuilder = new StringBuilder(" WHERE 1=1 and p1RPIt.p1Report.id = " + p1ReportId);
		
		if (StringUtils.isNotBlank(sn)) {
			sqlCommonBuilder.append(" AND lower(p1RPIt.sn) like lower('%" + sn + "%') ");
		}
		
		sqlBuilder.append(sqlCommonBuilder);
		sqlCountBuilder.append(sqlCommonBuilder);
		
		sqlBuilder.append(" ORDER by createDate DESC NULLS LAST ");

		Query qrCount = em.createQuery(sqlCountBuilder.toString());
		Query qr = em.createQuery(sqlBuilder.toString());
		
		Long count = ((Number) qrCount.getSingleResult()).longValue();
		pagin.setTotalRows(count);
		if (count == 0l) {
			return;
		}
		
		Integer offset = pagin.getOffset();
		Integer limit = pagin.getLimit();
		if (offset == null || offset < 0) {
			offset = 0;
			pagin.setOffset(offset);
		}
		if (limit == null || limit <= 0) {
			limit = 10;
			pagin.setLimit(limit);
		}
		qr.setFirstResult(offset);
		if (!"true".equalsIgnoreCase(exportCSV)) {
			qr.setMaxResults(limit);
		} else {
			qr.setMaxResults(5000);
		}
		
		////
		
		
		List<P1ReportItem> mcrs = qr.getResultList();
		mcrs.forEach(rp -> pagin.getResults().add(P1ReportItemDto.from(rp)));
	}
	
	@Transactional(readOnly = false)
	@SuppressWarnings("unchecked")
	@Override
	public void search(PaginDto<Object> pagin) {

		Map<String, Object> options = pagin.getOptions();
		Number p1ReportId = (Number) options.get("p1ReportId");
		if (p1ReportId != null) {
			searchReportItem(pagin);
			return;
		}

		pagin.setResults(new ArrayList<>());
		
		Long fromDate = (Long) options.get("fromDate");
		Long toDate = (Long) options.get("toDate");
		String sn = (String) options.get("querySn");
		String userSubmit = (String) options.get("userSent");
		String msn = (String) options.get("queryMsn");
		String exportCSV = options.get("exportCSV") + "";
		
		StringBuilder sqlBuilder = new StringBuilder("FROM P1Report p1RP ");
		StringBuilder sqlCountBuilder = new StringBuilder("select count(*) FROM P1Report p1RP ");
		StringBuilder sqlCommonBuilder = new StringBuilder(" WHERE 1=1 ");
		
		if (StringUtils.isNotBlank(sn)) {
			sqlCommonBuilder.append(" and exists (select 1 from P1ReportItem p1RPIt where p1RPIt.p1Report.id = p1RP.id AND lower(p1RPIt.sn) like lower('%" + sn + "%')) ");
		}
		
		if (StringUtils.isNotBlank(msn)) {
			sqlCommonBuilder.append(" and exists (select 1 from P1ReportItem p1RPIt where p1RPIt.p1Report.id = p1RP.id AND lower(p1RPIt.msn) like lower('%" + msn + "%')) ");
		}
		
		if (StringUtils.isNotBlank(userSubmit)) {
			sqlCommonBuilder.append(" AND p1RP.userSubmit = '" + userSubmit + "' ");	
		}
		
		if (fromDate != null) {
			sqlCommonBuilder.append(" AND p1RP.createDate >= :fromDate  ");	
		}
		if (toDate != null) {
			sqlCommonBuilder.append(" AND p1RP.createDate <= :toDate  ");	
		}
		
		sqlBuilder.append(sqlCommonBuilder);
		sqlCountBuilder.append(sqlCommonBuilder);
		
		sqlBuilder.append(" ORDER by createDate DESC NULLS LAST ");
		
		Query qrCount = em.createQuery(sqlCountBuilder.toString());
		Query qr = em.createQuery(sqlBuilder.toString());
		if (sqlCommonBuilder.indexOf(":fromDate") > -1) {
			qrCount.setParameter("fromDate", new Date(fromDate));
			qr.setParameter("fromDate", new Date(fromDate));
		}
		if (sqlCommonBuilder.indexOf(":toDate") > -1) {
			qrCount.setParameter("toDate", new Date(toDate));
			qr.setParameter("toDate", new Date(toDate));
		}
		
		Long count = ((Number) qrCount.getSingleResult()).longValue();
		pagin.setTotalRows(count);
		if (count == 0l) {
			return;
		}
		
		Integer offset = pagin.getOffset();
		Integer limit = pagin.getLimit();
		if (offset == null || offset < 0) {
			offset = 0;
			pagin.setOffset(offset);
		}
		if (limit == null || limit <= 0) {
			limit = 10;
			pagin.setLimit(limit);
		}
		qr.setFirstResult(offset);
		if (!"true".equalsIgnoreCase(exportCSV)) {
			qr.setMaxResults(limit);
		} else {
			qr.setMaxResults(5000);
		}
		
		////
		
		
		List<P1Report> mcrs = qr.getResultList();
		mcrs.forEach(rp -> pagin.getResults().add(P1ReportDto.from(rp)));
	}

}
