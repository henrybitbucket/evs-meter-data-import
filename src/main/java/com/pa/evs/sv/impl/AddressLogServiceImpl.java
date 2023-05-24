package com.pa.evs.sv.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import com.pa.evs.dto.AddressLogDto;
import com.pa.evs.dto.PaginDto;
import com.pa.evs.model.AddressLog;
import com.pa.evs.sv.AddressLogService;

@Service
public class AddressLogServiceImpl implements AddressLogService {

	@Autowired
	private EntityManager em;

	@Override
	@SuppressWarnings("unchecked")
	public PaginDto<AddressLogDto> getAddressLogs(PaginDto<AddressLogDto> pagin) {
		StringBuilder sqlBuilder = new StringBuilder("FROM AddressLog addr ");
		StringBuilder sqlCountBuilder = new StringBuilder("SELECT count(*) FROM AddressLog addr");

		StringBuilder sqlCommonBuilder = new StringBuilder();
		if (CollectionUtils.isEmpty(pagin.getOptions())) {
			sqlCommonBuilder.append(" WHERE 1=1 ");
		} else {
			Map<String, Object> options = pagin.getOptions();
			String query = (String) options.get("query");

			if (StringUtils.isNotBlank(query)) {
				sqlCommonBuilder.append(" WHERE     ");
				sqlCommonBuilder.append(" ( ");
				sqlCommonBuilder.append(" upper(sn) like '%" + query.toUpperCase() + "%' OR ");
				sqlCommonBuilder.append(" upper(msn) like '%" + query.toUpperCase() + "%' OR ");
				sqlCommonBuilder.append(" upper(country) like '%" + query.toUpperCase() + "%' OR ");
				sqlCommonBuilder.append(" upper(town) like '%" + query.toUpperCase() + "%' OR ");
				sqlCommonBuilder.append(" upper(street) like '%" + query.toUpperCase() + "%' OR ");
				sqlCommonBuilder.append(" upper(streetNumber) like '%" + query.toUpperCase() + "%' OR ");
				sqlCommonBuilder.append(" upper(postalCode) like '%" + query.toUpperCase() + "%' OR ");
				sqlCommonBuilder.append(" upper(building) like '%" + query.toUpperCase() + "%' OR ");
				sqlCommonBuilder.append(" upper(block) like '%" + query.toUpperCase() + "%' OR ");
				sqlCommonBuilder.append(" upper(level) like '%" + query.toUpperCase() + "%' OR ");
				sqlCommonBuilder.append(" upper(unitNumber) like '%" + query.toUpperCase() + "%' OR ");
				sqlCommonBuilder.append(" upper(remark) like '%" + query.toUpperCase() + "%' OR ");
				sqlCommonBuilder.append(" upper(type) like '%" + query.toUpperCase() + "%' ");
				sqlCommonBuilder.append(" ) ");
			} else {
				sqlCommonBuilder.append(" WHERE 1=1 ");
			}
		}
		sqlBuilder.append(sqlCommonBuilder).append(" ORDER BY id DESC");
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

		List<AddressLog> list = query.getResultList();
		
		list.forEach(li -> {
            AddressLogDto dto = AddressLogDto.builder()
                    .id(li.getId())
                    .block(li.getBlock())
                    .building(li.getBuilding())
                    .city(li.getCity())
                    .country(li.getCountry())
                    .level(li.getLevel())
                    .msn(li.getMsn())
                    .postalCode(li.getPostalCode())
                    .remark(li.getRemark())
                    .unitNumber(li.getUnitNumber())
                    .type(li.getType())
                    .town(li.getTown())
                    .streetNumber(li.getStreetNumber())
                    .street(li.getStreet())
                    .sn(li.getSn())
                    .createdDate(li.getCreateDate())
                    .build();
            pagin.getResults().add(dto);
        });
		
		return pagin;
	}
}
