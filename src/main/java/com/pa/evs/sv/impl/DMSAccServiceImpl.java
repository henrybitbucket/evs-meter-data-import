package com.pa.evs.sv.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pa.evs.dto.DMSAccDto;
import com.pa.evs.dto.DMSLockVendorDto;
import com.pa.evs.dto.PaginDto;
import com.pa.evs.dto.VendorDMSAccDto;
import com.pa.evs.model.DMSLockVendor;
import com.pa.evs.model.DMSMcAcc;
import com.pa.evs.model.DMSVendorMCAcc;
import com.pa.evs.repository.DMSAccRepository;
import com.pa.evs.repository.DMSLockVendorRepository;
import com.pa.evs.repository.DMSVendorMCAccRepository;
import com.pa.evs.sv.DMSAccService;
import com.pa.evs.utils.AppCodeSelectedHolder;
import com.pa.evs.utils.SecurityUtils;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;

@Service
public class DMSAccServiceImpl implements DMSAccService {

	@Autowired
	private EntityManager em;

	@Autowired
	private DMSAccRepository dmsAccRepository;

	@Autowired
	private DMSVendorMCAccRepository dmsVendorMCAccRepository;

	@Autowired
	private DMSLockVendorRepository dmsLockVendorRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Override
	public void getDMSMCUsers(PaginDto<DMSAccDto> pagin) {
		StringBuilder sqlBuilder = new StringBuilder("FROM DMSMcAcc us");
		StringBuilder sqlCountBuilder = new StringBuilder("SELECT count(*) FROM DMSMcAcc us");

		Map<String, Object> options = pagin.getOptions();
		String queryUserName = options.get("queryUserName") != null ? (String) options.get("queryUserName") : null;
		String queryEmail = options.get("queryEmail") != null ? (String) options.get("queryEmail") : null;
		String queryPhoneNumber = options.get("queryPhoneNumber") != null ? (String) options.get("queryPhoneNumber")
				: null;

		StringBuilder sqlCommonBuilder = new StringBuilder();
		sqlCommonBuilder.append(" WHERE 1=1 ");

		if (StringUtils.isNotBlank(queryUserName)) {
			sqlCommonBuilder.append(" AND lower(username) like '%" + queryUserName.toLowerCase() + "%' ");
		}
		if (StringUtils.isNotBlank(queryEmail)) {
			sqlCommonBuilder.append(" AND lower(email) like '%" + queryEmail.toLowerCase() + "%' ");
		}
		if (StringUtils.isNotBlank(queryPhoneNumber)) {
			sqlCommonBuilder.append(" AND phoneNumber like '%" + queryPhoneNumber + "%' ");
		}

		sqlBuilder.append(sqlCommonBuilder).append(" ORDER BY us.id asc");
		sqlCountBuilder.append(sqlCommonBuilder);

		if (pagin.getOffset() == null || pagin.getOffset() < 0) {
			pagin.setOffset(0);
		}

		if (pagin.getLimit() == null || pagin.getLimit() <= 0) {
			pagin.setLimit(100);
		}

		Query queryCount = em.createQuery(sqlCountBuilder.toString());

		Long count = ((Number) queryCount.getSingleResult()).longValue();
		pagin.setTotalRows(count);
		pagin.setResults(new ArrayList<>());
		if (count == 0l) {
			return;
		}

		Query query = em.createQuery(sqlBuilder.toString());
		query.setFirstResult(pagin.getOffset());
		query.setMaxResults(pagin.getLimit());

		List<DMSMcAcc> users = query.getResultList();
		users.forEach(user -> {
			DMSAccDto dto = DMSAccDto.builder().id(user.getId()).email(user.getEmail()).build();
			pagin.getResults().add(dto);
		});
	}

	@Override
	@Transactional
	public void saveDMSMCUser(DMSAccDto user) {
		DMSMcAcc en = null;

		if (user.getEmail() != null) {
			user.setEmail(user.getEmail().toLowerCase());
		}

		if (user.getId() != null && user.getId().longValue() > 0) {
			Optional<DMSMcAcc> opt = dmsAccRepository.findById(user.getId());
			if (opt.isPresent()) {
				if (!SecurityUtils.hasAnyRole("SUPER_ADMIN", AppCodeSelectedHolder.get() + "_SUPER_ADMIN")
						&& !opt.get().getEmail().equals(SecurityUtils.getEmail())) {
					throw new RuntimeException("Access denied!");
				}
				en = opt.get();
			} else {
				throw new RuntimeException("User not found!");
			}
		} else {
			en = new DMSMcAcc();
			en.setEmail(user.getEmail());
		}

		if (en.getId() == null && dmsAccRepository.findByEmail(user.getEmail()) != null) {
			throw new RuntimeException("Email already exists!");
		}

		if (StringUtils.isBlank(user.getPassword())) {
			throw new RuntimeException("Password is required!");
		}
		en.setPassword(passwordEncoder.encode(user.getPassword()));

		dmsAccRepository.save(en);
	}

	@Override
	@Transactional
	public void saveOrUpdateVendorAndUser(VendorDMSAccDto dto) {
		DMSLockVendorDto vendorDto = dto.getVendor();
		List<DMSAccDto> dmsAccDtos = dto.getDmsAccDtos();
		if (vendorDto.getId() == null) {
			if (dmsLockVendorRepository.findByLabel(vendorDto.getLabel()).isPresent()) {
				throw new RuntimeException("Label already exists!");
			}
			
			DMSLockVendor vendor = new DMSLockVendor();
			vendor.setName(vendorDto.getName());
			vendor.setType(vendorDto.getType());
			vendor.setCompanyName(vendorDto.getCompanyName());
			vendor.setLabel(vendorDto.getLabel());
			vendor = dmsLockVendorRepository.save(vendor);

			List<DMSVendorMCAcc> list = new ArrayList<>();
			for (DMSAccDto dmsAccDto : dmsAccDtos) {
				DMSVendorMCAcc dmsVendorMCAcc = new DMSVendorMCAcc();

				DMSMcAcc mcAcc = new DMSMcAcc();
				mcAcc.setEmail(dmsAccDto.getEmail());
				mcAcc.setPassword(passwordEncoder.encode(dmsAccDto.getPassword()));
				mcAcc = dmsAccRepository.save(mcAcc);

				dmsVendorMCAcc.setVendor(vendor);
				dmsVendorMCAcc.setMcAcc(mcAcc);
				list.add(dmsVendorMCAcc);
			}
			dmsVendorMCAccRepository.saveAll(list);
		} else {
			Optional<DMSLockVendor> vendorOpt = dmsLockVendorRepository.findById(vendorDto.getId());
			if (!vendorOpt.isPresent()) {
				throw new RuntimeException("Vendor not found!");
			}
			Optional<DMSLockVendor> vendorCheckLabelOpt = dmsLockVendorRepository.findById(vendorDto.getId());
			if (vendorCheckLabelOpt.isPresent() && !vendorCheckLabelOpt.get().getId().equals(vendorDto.getId())) {
				throw new RuntimeException("Label already exists!");
			}
			DMSLockVendor vendor = vendorOpt.get();
			vendor.setName(vendorDto.getName());
			vendor.setType(vendorDto.getType());
			vendor.setCompanyName(vendorDto.getCompanyName());
			vendor.setLabel(vendorDto.getLabel());
			vendor = dmsLockVendorRepository.save(vendor);

			List<DMSVendorMCAcc> list = dmsVendorMCAccRepository.findByVendor(vendor);
			List<Long> existingList = list.stream().map(li -> li.getMcAcc().getId()).collect(Collectors.toList());
			List<Long> updatedList = new ArrayList<>();

			dmsAccDtos.forEach(dmsAccDto -> {
				if (dmsAccDto.getId() != null) {
					Optional<DMSMcAcc> mcAccOpt = dmsAccRepository.findById(dmsAccDto.getId());
					if (!mcAccOpt.isPresent()) {
						throw new RuntimeException("MC user with email: " + dmsAccDto.getEmail() + " not found!");
					}
					updatedList.add(mcAccOpt.get().getId());
				} else {
					DMSMcAcc mcAcc = new DMSMcAcc();
					mcAcc.setEmail(dmsAccDto.getEmail());
					mcAcc.setPassword(passwordEncoder.encode(dmsAccDto.getPassword()));
					mcAcc = dmsAccRepository.save(mcAcc);
					updatedList.add(mcAcc.getId());
				}
			});

			List<Long> addedList = updatedList.stream().filter(element -> !existingList.contains(element))
					.collect(Collectors.toList());
			List<Long> deletedList = existingList.stream().filter(element -> !updatedList.contains(element))
					.collect(Collectors.toList());
			List<DMSVendorMCAcc> listUpdatedVendorMCAcc = new ArrayList<>();

			for (Long add : addedList) {
				Optional<DMSMcAcc> mcAccOpt = dmsAccRepository.findById(add);
				if (!mcAccOpt.isPresent()) {
					throw new RuntimeException("MC user not found!");
				}
				DMSVendorMCAcc dmsVendorMCAcc = new DMSVendorMCAcc();
				dmsVendorMCAcc.setMcAcc(mcAccOpt.get());
				dmsVendorMCAcc.setVendor(vendor);
				listUpdatedVendorMCAcc.add(dmsVendorMCAcc);
			}
			;

			if (!deletedList.isEmpty()) {
				dmsVendorMCAccRepository.deleteByVendorAndMcAccIn(vendor.getId(), deletedList);
				dmsVendorMCAccRepository.flush();
			}

			if (!listUpdatedVendorMCAcc.isEmpty()) {
				dmsVendorMCAccRepository.saveAll(listUpdatedVendorMCAcc);
			}
		}
	}

	@Override
	public DMSLockVendorDto getVendorAndMcAccs(Long vendorId) {
		Optional<DMSLockVendor> vendorOpt = dmsLockVendorRepository.findById(vendorId);
		if (!vendorOpt.isPresent()) {
			throw new RuntimeException("Vendor not found!");
		}
		DMSLockVendor vendor = vendorOpt.get();
		DMSLockVendorDto vendorDto = new DMSLockVendorDto();
		vendorDto.setId(vendor.getId());
		vendorDto.setName(vendor.getName());
		vendorDto.setType(vendor.getType());
		vendorDto.setCompanyName(vendor.getCompanyName());
		vendorDto.setLabel(vendor.getLabel());

		List<DMSVendorMCAcc> list = dmsVendorMCAccRepository.findByVendor(vendor);

		List<DMSAccDto> mcAccs = new ArrayList<>();
		list.forEach(li -> {
			DMSMcAcc mcAcc = li.getMcAcc();
			DMSAccDto dto = new DMSAccDto().builder().id(mcAcc.getId()).email(mcAcc.getEmail()).build();
			mcAccs.add(dto);
		});
		vendorDto.setMcAccs(mcAccs);
		return vendorDto;
	}

	@Override
	@Transactional
	public void deleteVendor(Long vendorId) {
		Optional<DMSLockVendor> vendorOpt = dmsLockVendorRepository.findById(vendorId);
		if (!vendorOpt.isPresent()) {
			throw new RuntimeException("Vendor not found!");
		}
		DMSLockVendor vendor = vendorOpt.get();

		dmsVendorMCAccRepository.deleteByVendor(vendor);
		dmsVendorMCAccRepository.flush();

		dmsLockVendorRepository.deleteById(vendorId);
	}

	@Override
	public void getVendorsUsers(PaginDto<DMSLockVendorDto> pagin) {
		StringBuilder sqlBuilder = new StringBuilder("FROM DMSLockVendor vendor");
		StringBuilder sqlCountBuilder = new StringBuilder("SELECT count(*) FROM DMSLockVendor vendor");

		Map<String, Object> options = pagin.getOptions();
		String queryName = options.get("queryName") != null ? (String) options.get("queryName") : null;

		StringBuilder sqlCommonBuilder = new StringBuilder();
		sqlCommonBuilder.append(" WHERE 1=1 ");

		if (StringUtils.isNotBlank(queryName)) {
			sqlCommonBuilder.append(" AND lower(name) like '%" + queryName.toLowerCase() + "%' ");
		}

		sqlBuilder.append(sqlCommonBuilder).append(" ORDER BY vendor.id asc");
		sqlCountBuilder.append(sqlCommonBuilder);

		if (pagin.getOffset() == null || pagin.getOffset() < 0) {
			pagin.setOffset(0);
		}

		if (pagin.getLimit() == null || pagin.getLimit() <= 0) {
			pagin.setLimit(100);
		}

		Query queryCount = em.createQuery(sqlCountBuilder.toString());

		Long count = ((Number) queryCount.getSingleResult()).longValue();
		pagin.setTotalRows(count);
		pagin.setResults(new ArrayList<>());
		if (count == 0l) {
			return;
		}

		Query query = em.createQuery(sqlBuilder.toString());
		query.setFirstResult(pagin.getOffset());
		query.setMaxResults(pagin.getLimit());

		List<DMSLockVendor> vendors = query.getResultList();
		vendors.forEach(vendor -> {
			DMSLockVendorDto dto = new DMSLockVendorDto().build(vendor);

			List<DMSVendorMCAcc> list = dmsVendorMCAccRepository.findByVendor(vendor);
			List<DMSAccDto> mcAccsDto = new ArrayList<>();
			for (DMSVendorMCAcc li : list) {
				DMSMcAcc mcAcc = li.getMcAcc();
				DMSAccDto dmsAccDto = new DMSAccDto().builder().id(mcAcc.getId()).email(mcAcc.getEmail()).build();
				mcAccsDto.add(dmsAccDto);
			}
			dto.setMcAccs(mcAccsDto);

			pagin.getResults().add(dto);
		});
	}
}
