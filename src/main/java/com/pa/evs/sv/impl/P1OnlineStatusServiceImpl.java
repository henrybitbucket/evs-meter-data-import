package com.pa.evs.sv.impl;

import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pa.evs.dto.P1OnlineStatusDto;
import com.pa.evs.dto.PaginDto;
import com.pa.evs.model.P1OnlineStatus;
import com.pa.evs.sv.P1OnlineStatusService;

@Service
public class P1OnlineStatusServiceImpl implements P1OnlineStatusService {

	@Autowired
	private EntityManager em;

	@Override
	@Transactional(readOnly = false)
	@SuppressWarnings("unchecked")
	public void search(PaginDto<P1OnlineStatusDto> pagin) {

		StringBuilder sqlBuilder = new StringBuilder("FROM P1OnlineStatus p1 ");
		StringBuilder sqlCountBuilder = new StringBuilder("select count(*) FROM P1OnlineStatus p1 ");
		StringBuilder sqlCommonBuilder = new StringBuilder(" WHERE 1=1 ");
		Map<String, Object> options = pagin.getOptions();

		String uid = options.get("queryUid") != null ? (String) options.get("queryUid") : "";
		String sn = options.get("querySn") != null ? (String) options.get("querySn") : "";
		String msn = options.get("queryMsn") != null ? (String) options.get("queryMsn") : "";

		Long fromDate = options.get("fromDate") != null ? (Long) options.get("fromDate") : null;
		Long toDate = options.get("toDate") != null ? (Long) options.get("toDate") : null;
		String dateType = options.get("dateType") != null ? (String) options.get("dateType") : "";// reveiced/sent
		String userSent = options.get("userSent") != null ? (String) options.get("userSent") : "";
		String status = options.get("status") != null ? (String) options.get("status") : "";

		Boolean exportCSV = options.get("exportCSV") != null && (Boolean) options.get("exportCSV");

		if (fromDate != null) {
			sqlCommonBuilder.append(
					" AND " + ("reveiced".equalsIgnoreCase(dateType) ? "p1OnlineLastReceived" : "p1OnlineLastSent")
							+ " >= " + fromDate);
		}

		if (toDate != null) {
			sqlCommonBuilder.append(
					" AND " + ("reveiced".equalsIgnoreCase(dateType) ? "p1OnlineLastReceived" : "p1OnlineLastSent")
							+ " <= " + toDate);
		}

		if (StringUtils.isNotBlank(userSent)) {
			sqlCommonBuilder.append(" AND lower(p1OnlineLastUserSent) = '" + userSent.toLowerCase() + "' ");
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

		if (StringUtils.isNotBlank(status)) {
            sqlCommonBuilder.append(" lower(p1Online) = '" + status.toLowerCase() + "' AND ");
        }
		
		sqlBuilder.append(sqlCommonBuilder);
		sqlCountBuilder.append(sqlCommonBuilder);

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
			pagin.setOffset(offset);
		}
		if (limit == null || limit <= 0) {
			limit = 10;
			pagin.setLimit(limit);
		}
		qr.setFirstResult(offset);
		if (BooleanUtils.isFalse(exportCSV)) {
			qr.setMaxResults(limit);
		}
		pagin.setTotalRows(count);
		List<P1OnlineStatus> resultList = qr.getResultList();

		resultList.forEach(li -> {
			P1OnlineStatusDto dto = new P1OnlineStatusDto();

			dto.setId(li.getId());
			dto.setCid(li.getCid());
			dto.setUid(li.getUid());
			dto.setType(li.getType());
			dto.setSn(li.getSn());
			dto.setMsn(li.getMsn());
			dto.setP1Online(li.getP1Online());
			dto.setP1OnlineLastReceived(li.getP1OnlineLastReceived());
			dto.setP1OnlineLastSent(li.getP1OnlineLastSent());
			dto.setP1OnlineLastUserSent(li.getP1OnlineLastUserSent());
			dto.setCreateDate(li.getCreateDate());
			dto.setModifyDate(li.getModifyDate());
			dto.setVendor(li.getVendor());
			dto.setVersion(li.getVersion());
			
			pagin.getResults().add(dto);
		});
	}
}
