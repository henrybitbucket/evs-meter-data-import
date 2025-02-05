package com.pa.evs.sv.impl;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.pa.evs.dto.PaginDto;
import com.pa.evs.dto.SettingDto;
import com.pa.evs.model.Setting;
import com.pa.evs.repository.SettingRepository;
import com.pa.evs.sv.SettingService;
import com.pa.evs.utils.AppProps;

@Service
public class SettingsServiceImpl implements SettingService {
	
	@Autowired
	SettingRepository settingRepository;
	
	@Autowired
	EntityManager em;

	@Override
	public List<SettingDto> findAll() {
		
		List<SettingDto> rp = new ArrayList<>();
		List<Setting> rms = settingRepository.findAll();

		rms.forEach(rm -> {
			SettingDto dto = SettingDto.from(rm);
			dto.setOrder(dto.getOrder() == null ? Integer.MAX_VALUE : dto.getOrder());
			rp.add(dto);
			AppProps.set(dto.getKey(), dto.getValue());
		});
		
		return rp;
	}

	@Override
	public SettingDto findByKey(String key) {
		Optional<Setting> stO = settingRepository.findByKey(key);
		return stO.isPresent() ? SettingDto.from(stO.get()) : null;
	}

	@Override
	public void save(SettingDto dto) {
		
		Optional<Setting> enO = settingRepository.findByKey(dto.getKey().toUpperCase());
		
		Setting en = null;
		if (enO.isPresent()) {
			en = enO.get();
		} else {
			en = new Setting();
			en.setCreateDate(new Date());
		}
		en.setKey(dto.getKey().toUpperCase());
		
		if (!(TIME_CHECK_PI_ONLINE.equalsIgnoreCase(dto.getKey())) || dto.getValue() != null && dto.getValue().matches("^[0-9]+")) {
			en.setValue(dto.getValue());
		}
		
		if (!(TIME_LOGIN_EXPIRED.equalsIgnoreCase(dto.getKey())) || dto.getValue() != null && dto.getValue().matches("^[0-9]+")) {
			en.setValue(dto.getValue());
		}
		
		en.setOrder(dto.getOrder() == null ? Integer.MAX_VALUE : dto.getOrder());
		en.setStatus("ACTIVE");
		en.setModifyDate(new Date());
		settingRepository.save(en);
		AppProps.set(en.getKey(), en.getValue());
	}

	@Override
	public void delete(Long id) {
		settingRepository.deleteById(id);		
	}

	@Override
	public Object search(PaginDto<SettingDto> pagin) {
		
		if (pagin.getLimit() == null) {
			pagin.setLimit(10);
		}
		if (pagin.getOffset() == null) {
			pagin.setOffset(0);
		}
		
		StringBuilder sqlBuilder = new StringBuilder("FROM Setting where 1=1");
		
		sqlBuilder.append(" ORDER BY id ASC ");
		
		Query q = em.createQuery(sqlBuilder.toString(), Setting.class);
		q.setFirstResult(pagin.getOffset());
		q.setMaxResults(pagin.getLimit());
		
		StringBuilder sqlCountBuilder = new StringBuilder("SELECT COUNT(*) FROM Setting where 1=1");
		Query qCount = em.createQuery(sqlCountBuilder.toString());
		
		@SuppressWarnings("unchecked")
		List<Setting> list = q.getResultList();
		List<SettingDto> dtos = new ArrayList<>();
		
		list.forEach(rm -> {
			SettingDto dto = SettingDto.from(rm);
			dto.setOrder(dto.getOrder() == null ? Integer.MAX_VALUE : dto.getOrder());
			dtos.add(dto);
		});
		
		pagin.setResults(dtos);
		pagin.setTotalRows(((Number)qCount.getSingleResult()).longValue());
		return pagin;
	}
}
