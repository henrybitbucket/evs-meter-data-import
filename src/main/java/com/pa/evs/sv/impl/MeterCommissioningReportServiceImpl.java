package com.pa.evs.sv.impl;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pa.evs.dto.MeterCommissioningReportDto;
import com.pa.evs.dto.PaginDto;
import com.pa.evs.exception.ApiException;
import com.pa.evs.model.CARequestLog;
import com.pa.evs.model.MeterCommissioningReport;
import com.pa.evs.model.Users;
import com.pa.evs.repository.CARequestLogRepository;
import com.pa.evs.repository.MeterCommissioningReportRepository;
import com.pa.evs.repository.UserRepository;
import com.pa.evs.sv.MeterCommissioningReportService;

@Service
public class MeterCommissioningReportServiceImpl implements MeterCommissioningReportService {
	
	@Autowired
	private UserRepository userRepository;

	@Autowired
	private CARequestLogRepository caRequestLogRepository;
	
	@Autowired
	private EntityManager em;
	
	@Autowired
	private MeterCommissioningReportRepository meterCommissioningReportRepository;

//	set is_latest = true where m1.id in (
//		select tmp.id from (
//			select m2.msn, max(m2.id) id from pa_evs_db.meter_commissioning_report m2 group by m2.msn
//		) tmp
//	);
//
//	update pa_evs_db.ca_request_log ca
//	set last_meter_commissioning_report = tmp.create_date
//	from (
//		select m3.msn, m3.id, m3.create_date from pa_evs_db.meter_commissioning_report m3 join (
//			select m2.msn, max(m2.id) id from pa_evs_db.meter_commissioning_report m2 group by m2.msn
//		) tmp1 on m3.id = tmp1.id
//	) tmp
//	where ca.msn = tmp.msn
	@Transactional
	@Override
	public void save(MeterCommissioningReportDto dto) {
		
		MeterCommissioningReport mcr = new MeterCommissioningReport();
		mcr.setCid(dto.getCid());
		mcr.setUid(dto.getUid());
		mcr.setType(dto.getType());
		mcr.setStatus(dto.getStatus());
		mcr.setSn(dto.getSn());
		mcr.setMsn(dto.getMsn());
		mcr.setLastOBRDate(dto.getLastOBRDate());
		mcr.setMeterPhotos(dto.getMeterPhotos());
		mcr.setIsPassed(dto.getIsPassed());
		mcr.setKwh(dto.getKwh());
		mcr.setKw(dto.getKw());
		mcr.setI(dto.getI());
		mcr.setPf(dto.getPf());
		mcr.setV(dto.getV());
		mcr.setDt(dto.getDt());
		mcr.setIsLatest(true);
		mcr.setUserSubmit(dto.getUserSubmit());
		mcr.setTimeSubmit(dto.getTimeSubmit());
		mcr.setCoupledUser(dto.getCoupledUser());
		em.createQuery("UPDATE MeterCommissioningReport set isLatest = false where uid = '" + mcr.getUid() + "'").executeUpdate();
		em.flush();
		
		if (dto.getInstaller() != null) {
			Optional<Users> installer = userRepository.findById(dto.getInstaller());
			mcr.setInstaller(installer.orElse(null));
		}
		
		meterCommissioningReportRepository.save(mcr);
		
		Optional<CARequestLog> caOpt = caRequestLogRepository.findByUid(dto.getUid());
		
		if (caOpt.isPresent()) {
			caOpt.get().setLastMeterCommissioningReport(mcr.getCreateDate());
			caRequestLogRepository.save(caOpt.get());
		}
	}
	
	@Transactional(readOnly = false)
	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public void searchP1OnlineTest(PaginDto<? extends Object> pagin) {
		pagin.setResults(new ArrayList<>());
		
		StringBuilder sqlBuilder = new StringBuilder("FROM CARequestLog mcr ");
		StringBuilder sqlCountBuilder = new StringBuilder("select count(*) FROM CARequestLog mcr ");
		StringBuilder sqlCommonBuilder = new StringBuilder(" WHERE 1=1 ");
		Map<String, Object> options = pagin.getOptions();

		String uid = (String) options.get("uid");
		String sn = (String) options.get("sn");
		String msn = (String) options.get("msn");

		Long fromDate = (Long) options.get("fromDate");
		Long toDate = (Long) options.get("toDate");
		String dateType = (String) options.get("dateType");// reveiced/sent
		String userSent = (String) options.get("userSent");
		
		String exportCSV = (String) options.get("exportCSV");
		
		if (fromDate != null) {
			sqlCommonBuilder.append(" AND " + ("reveiced".equalsIgnoreCase(dateType) ? "p1OnlineLastReceived" : "p1OnlineLastSent") + " >= " + fromDate);
		}
		
		if (toDate != null) {
			sqlCommonBuilder.append(" AND " + ("reveiced".equalsIgnoreCase(dateType) ? "p1OnlineLastReceived" : "p1OnlineLastSent") + " <= " + toDate);
		}

		if (StringUtils.isNotBlank(userSent)) {
			sqlCommonBuilder.append(" AND lower(userSent) = '" + userSent.toLowerCase() + "' ");
		}
		
		if (StringUtils.isNotBlank(uid)) {
			sqlCommonBuilder.append(" AND uid = '" + uid + "' ");
		}
		
		if (StringUtils.isNotBlank(sn)) {
			sqlCommonBuilder.append(" AND sn = '" + sn + "' ");
		}
		
		if (StringUtils.isNotBlank(msn)) {
			sqlCommonBuilder.append(" AND msn = '" + msn + "' ");
		}
		
		sqlBuilder.append(sqlCommonBuilder);
		sqlCountBuilder.append(sqlCommonBuilder);
		sqlCountBuilder.append(" ORDER by p1OnlineLastSent DESC ");
		
		Query qrCount = em.createQuery(sqlCountBuilder.toString());
		Query qr = em.createQuery(sqlBuilder.toString());
		
		Long count = ((Number) qrCount.getSingleResult()).longValue();
		if (count == 0l) {
			return;
		}
		
		Integer offset = pagin.getOffset();
		Integer limit = pagin.getLimit();
		if (offset == null || offset < 0) {
			offset = 0;
		}
		if (limit == null || limit <= 0) {
			offset = 10;
		}
		qr.setFirstResult(offset);
		if (!"true".equalsIgnoreCase(exportCSV)) {
			qr.setMaxResults(limit);
		}
		
		List rs = new ArrayList<>();
		qr.getResultList().forEach(obj -> {
			CARequestLog ca = (CARequestLog) obj;
			MeterCommissioningReportDto dto = new MeterCommissioningReportDto();
			
			dto.setId(ca.getId());
			dto.setCid(ca.getCid());
			dto.setUid(ca.getUid());
			dto.setType(ca.getType());
			dto.setStatus(ca.getStatus());
			dto.setSn(ca.getSn());
			dto.setMsn(ca.getMsn());
			dto.setLastOBRDate(ca.getLastOBRDate());
			dto.setP1Online(ca.getP1Online());
			dto.setP1OnlineLastReceived(ca.getP1OnlineLastReceived());
			dto.setP1OnlineLastSent(ca.getP1OnlineLastSent());
			dto.setP1OnlineLastUserSent(ca.getP1OnlineLastUserSent());
			
			if (ca.getInstaller() != null) {
				dto.setInstaller(ca.getInstaller().getUserId());
				dto.setInstallerName(ca.getInstaller().getUsername());
				dto.setInstallerEmail(ca.getInstaller().getEmail());
			}
			rs.add(dto);
		});
		pagin.setResults(rs);
	}

	@Transactional(readOnly = false)
	@SuppressWarnings("unchecked")
	@Override
	public void search(PaginDto<MeterCommissioningReportDto> pagin) {
		pagin.setResults(new ArrayList<>());
		
		
		Map<String, Object> options = pagin.getOptions();
		Long fromDate = (Long) options.get("fromDate");
		Long toDate = (Long) options.get("toDate");
		String uid = (String) options.get("uid");
		String sn = (String) options.get("querySn");
		String userSubmit = (String) options.get("userSubmit");
		String msn = (String) options.get("queryMsn");
		String exportCSV = options.get("exportCSV") + "";
		String hasSubmission = options.get("hasSubmission") + "";
		
		StringBuilder sqlBuilder = new StringBuilder("FROM CARequestLog ca ");
		StringBuilder sqlCountBuilder = new StringBuilder("select count(*) FROM CARequestLog ca ");
		StringBuilder sqlCommonBuilder = new StringBuilder(" WHERE 1=1 ");
		
		if (StringUtils.isNotBlank(uid)) {
			sqlCommonBuilder.append(" AND uid = '" + uid + "' ");
		}
		
		if (StringUtils.isNotBlank(sn)) {
			sqlCommonBuilder.append(" AND sn = '" + sn + "' ");
		}
		
		if (StringUtils.isNotBlank(msn)) {
			sqlCommonBuilder.append(" AND msn = '" + msn + "' ");
		}
		
		if ("true".equalsIgnoreCase(hasSubmission)) {
			sqlCommonBuilder.append(" AND lastMeterCommissioningReport is not null ");
			if (fromDate != null) {
				sqlCommonBuilder.append(" AND lastMeterCommissioningReport >= " + fromDate + " ");
			}
			if (toDate != null) {
				sqlCommonBuilder.append(" AND lastMeterCommissioningReport <= " + toDate + " ");
			}
			if (fromDate != null && toDate != null) {
				sqlCommonBuilder.append(" AND exists (select 1 FROM MeterCommissioningReport mcr where mcr.uid = ca.uid and mcr.timeSubmit >= " + fromDate + " and mcr.timeSubmit <= " + toDate + ") ");	
			} else if (fromDate != null) {
				sqlCommonBuilder.append(" AND exists (select 1 FROM MeterCommissioningReport mcr where mcr.uid = ca.uid and mcr.timeSubmit >= " + fromDate + ") ");
			} else if (toDate != null) {
				sqlCommonBuilder.append(" AND exists (select 1 FROM MeterCommissioningReport mcr where mcr.uid = ca.uid and mcr.timeSubmit <= " + toDate + ") ");
			} else {
				sqlCommonBuilder.append(" AND exists (select 1 FROM MeterCommissioningReport mcr where mcr.uid = ca.uid) ");
			}
			
		}
		
		if (StringUtils.isNotBlank(userSubmit)) {
			if (fromDate != null && toDate != null) {
				sqlCommonBuilder.append(" AND exists (select 1 FROM MeterCommissioningReport mcr where mcr.uid = ca.uid and mcr.userSubmit = '" + userSubmit + "' and mcr.timeSubmit >= " + fromDate + " and mcr.timeSubmit <= " + toDate + ") ");	
			} else if (fromDate != null) {
				sqlCommonBuilder.append(" AND exists (select 1 FROM MeterCommissioningReport mcr where mcr.uid = ca.uid and mcr.userSubmit = '" + userSubmit + "' and mcr.timeSubmit >= " + fromDate + ") ");
			} else if (toDate != null) {
				sqlCommonBuilder.append(" AND exists (select 1 FROM MeterCommissioningReport mcr where mcr.uid = ca.uid and mcr.userSubmit = '" + userSubmit + "' and mcr.timeSubmit <= " + toDate + ") ");
			} else {
				sqlCommonBuilder.append(" AND exists (select 1 FROM MeterCommissioningReport mcr where mcr.uid = ca.uid AND mcr.userSubmit = '" + userSubmit + "') ");
			}
		}
		
		sqlCommonBuilder.append(" AND sn is not null and sn <> '' and msn is not null and msn <> '' ");
		
		sqlBuilder.append(sqlCommonBuilder);
		sqlCountBuilder.append(sqlCommonBuilder);
		
		sqlBuilder.append(" ORDER by lastMeterCommissioningReport DESC NULLS LAST ");
		
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
		
		Map<String, CARequestLog> cas = new LinkedHashMap<>();
		List<CARequestLog> ens = qr.getResultList();
		ens.forEach(en -> cas.put(en.getUid(), en));
		////
		
		
		sqlBuilder = new StringBuilder("FROM MeterCommissioningReport mcr ");
		sqlCommonBuilder = new StringBuilder(" WHERE isLatest = true ");

		
		if (fromDate != null) {
			sqlCommonBuilder.append(" AND timeSubmit >= :fromDate ");
		}
		
		if (toDate != null) {
			sqlCommonBuilder.append(" AND timeSubmit <= :toDate ");
		}
		
		if (StringUtils.isNotBlank(userSubmit)) {
			sqlCommonBuilder.append(" AND userSubmit = '" + userSubmit + "' ");
		}
		
		sqlCommonBuilder.append(" AND uid in (:uids) ");
		
		sqlBuilder.append(sqlCommonBuilder);
		
		qr = em.createQuery(sqlBuilder.toString()).setParameter("uids", cas.keySet());
		
		if (fromDate != null) {
			qr.setParameter("fromDate", fromDate);
		}
		
		if (toDate != null) {
			qr.setParameter("toDate", toDate);
		}
		
		List<MeterCommissioningReport> mcrs = qr.getResultList();
		
		Map<String, MeterCommissioningReport> mcrUid = new LinkedHashMap<>();
		mcrs.forEach(m -> mcrUid.put(m.getUid(), m));
		
		ens.forEach(ca -> {
			MeterCommissioningReport mcr = mcrUid.get(ca.getUid());
			MeterCommissioningReportDto dto = new MeterCommissioningReportDto();
			dto.setCid(ca.getCid());
			dto.setUid(ca.getUid());
			dto.setSn(ca.getSn());
			dto.setMsn(ca.getMsn());
			
			if (mcr != null) {
				dto.setType(mcr.getType());
				dto.setId(mcr.getId());
				dto.setStatus(mcr.getStatus());
				dto.setLastOBRDate(mcr.getLastOBRDate());
				dto.setMeterPhotos(mcr.getMeterPhotos());
				dto.setIsPassed(mcr.getIsPassed());
				dto.setKwh(mcr.getKwh());
				dto.setKw(mcr.getKw());
				dto.setI(mcr.getI());
				dto.setPf(mcr.getPf());
				dto.setV(mcr.getV());
				dto.setDt(mcr.getDt());
				dto.setCoupledUser(mcr.getCoupledUser());
				dto.setUserSubmit(mcr.getUserSubmit());
				dto.setTimeSubmit(mcr.getTimeSubmit());
				
				if (mcr.getInstaller() != null) {
					dto.setInstaller(mcr.getInstaller().getUserId());
					dto.setInstallerName(mcr.getInstaller().getUsername());
					dto.setInstallerEmail(mcr.getInstaller().getEmail());
				}
			}
			pagin.getResults().add(dto);
		});
	}
	
	@Override
	public MeterCommissioningReportDto getLastSubmit(String uid, String msn) {
		Optional<MeterCommissioningReport> mcrOpt = meterCommissioningReportRepository.findLastSubmit(uid, msn);
		if (!mcrOpt.isPresent()) {
			throw new ApiException("No Meter commissioning report found!");
		}
		
		MeterCommissioningReport mcr = mcrOpt.get();
		MeterCommissioningReportDto dto = new MeterCommissioningReportDto();
		
		dto.setId(mcr.getId());
		dto.setCid(mcr.getCid());
		dto.setUid(mcr.getUid());
		dto.setType(mcr.getType());
		dto.setStatus(mcr.getStatus());
		dto.setSn(mcr.getSn());
		dto.setMsn(mcr.getMsn());
		dto.setLastOBRDate(mcr.getLastOBRDate());
		dto.setMeterPhotos(mcr.getMeterPhotos());
		dto.setIsPassed(mcr.getIsPassed());
		dto.setKwh(mcr.getKwh());
		dto.setKw(mcr.getKw());
		dto.setI(mcr.getI());
		dto.setPf(mcr.getPf());
		dto.setV(mcr.getV());
		dto.setDt(mcr.getDt());
		dto.setCoupledUser(mcr.getCoupledUser());
		dto.setUserSubmit(mcr.getUserSubmit());
		dto.setTimeSubmit(mcr.getTimeSubmit());
		
		if (mcr.getInstaller() != null) {
			dto.setInstaller(mcr.getInstaller().getUserId());
			dto.setInstallerName(mcr.getInstaller().getUsername());
			dto.setInstallerEmail(mcr.getInstaller().getEmail());
		}
		return dto;
	}

}
