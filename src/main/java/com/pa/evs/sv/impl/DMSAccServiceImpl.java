package com.pa.evs.sv.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.transaction.Transactional;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.pa.evs.dto.DMSAccDto;
import com.pa.evs.dto.PaginDto;
import com.pa.evs.dto.VendorDto;
import com.pa.evs.model.DMSMcAcc;
import com.pa.evs.model.DMSVendorMCAcc;
import com.pa.evs.model.Vendor;
import com.pa.evs.repository.CountryCodeRepository;
import com.pa.evs.repository.DMSAccRepository;
import com.pa.evs.repository.DMSVendorMCAccRepository;
import com.pa.evs.repository.VendorRepository;
import com.pa.evs.sv.DMSAccService;
import com.pa.evs.sv.VendorService;
import com.pa.evs.utils.AppCodeSelectedHolder;
import com.pa.evs.utils.SecurityUtils;

@Service
public class DMSAccServiceImpl implements DMSAccService {

	@Autowired
	private EntityManager em;

	@Autowired
	private DMSAccRepository dmsAccRepository;

	@Autowired
	private CountryCodeRepository countryCodeRepository;
	
	@Autowired
	private DMSVendorMCAccRepository dmsVendorMCAccRepository;
	
	@Autowired
	private VendorService vendorService;
	
	@Autowired
	private VendorRepository vendorRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	private List<String> cCodes = new ArrayList<>();

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
		if ("true".equalsIgnoreCase(options.get("hasPhone") + "")) {
			sqlCommonBuilder.append(" AND us.phoneNumber is not null ");
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
			DMSAccDto dto = DMSAccDto.builder().id(user.getId()).username(user.getUsername()).email(user.getEmail())
					.phoneNumber(user.getPhoneNumber()).build();
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
		if (StringUtils.isBlank(user.getUsername())) {
			user.setUsername(user.getEmail());
		}
		if (user.getUsername() != null) {
			user.setUsername(user.getUsername());
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
			en.setUsername(user.getUsername());
			en.setEmail(user.getEmail());
		}

		if (en.getId() == null && dmsAccRepository.findByEmail(user.getEmail()) != null) {
			throw new RuntimeException("Email already exists!");
		}

		if (user.getPhoneNumber() != null && user.getPhoneNumber().trim().startsWith("+")) {
			String phone = "+" + user.getPhoneNumber().trim().replaceAll("[^0-9]", "");
			String callingCode = null;
			for (String c : cCodes) {
				if (phone.trim().startsWith("+" + c)) {
					callingCode = c;
					break;
				}
			}

			if (StringUtils.isBlank(callingCode)) {
				throw new RuntimeException("Unknown phone code!");
			}
			String lcPhone = phone.substring(callingCode.length() + 1);
			if (!"86".equals(callingCode) && !"91".equals(callingCode) && !lcPhone.matches("^(0[1-9][0-9]{1,8})|([1-9][0-9]{1,9})$")) {
				throw new RuntimeException("Phone invalid (Maximum 10 numeric characters)!");
			}
			if ("86".equals(callingCode) && !lcPhone.matches("^(0[1-9][0-9]{1,9})|([1-9][0-9]{1,10})$")) {
				throw new RuntimeException("Phone invalid (Maximum 11 numeric characters)!");
			}
			if ("91".equals(callingCode) && !lcPhone.matches("^(0[1-9][0-9]{1,10})|([1-9][0-9]{1,11})$")) {
				throw new RuntimeException("Phone invalid (Maximum 12 numeric characters)!");
			}

			phone = "+" + callingCode + (lcPhone.startsWith("0") ? lcPhone.substring(1) : lcPhone);

			DMSMcAcc existsUser = dmsAccRepository.findByPhoneNumber(phone);
			if ((existsUser != null && en.getId() != null && existsUser.getId().longValue() != en.getId().longValue())
					|| (en.getId() == null && existsUser != null)) {
				throw new RuntimeException("Phone already exists!");
			}

			en.setPhoneNumber(phone);
		} else if (StringUtils.isBlank(user.getPhoneNumber())) {
			en.setPhoneNumber(null);
		}

		if (StringUtils.isBlank(user.getPassword())) {
			throw new RuntimeException("Password is required!");
		}
		en.setPassword(passwordEncoder.encode(user.getPassword()));

		dmsAccRepository.save(en);
	}
	
	@Override
	public void saveVendorAndUser(VendorDto vendorDto, List<DMSAccDto> dmsAccDtos) {
		Vendor vendor = vendorService.saveVendor(vendorDto);
		List<DMSVendorMCAcc> list = dmsVendorMCAccRepository.findByVendor(vendor);
		
		List<Long> currentAccs = list.stream().map(li -> li.getMcAcc().getId()).collect(Collectors.toList());
		List<Long> updatedAccs = dmsAccDtos.stream().map(li -> li.getId()).collect(Collectors.toList());
		
//		List<Long> deletedAccs = updatedAccs
//		
//		dmsAccDtos.forEach(dto -> {
//			Optional<DMSMcAcc> mcAcc = dmsAccRepository.findById(dto.getId());
//			if (mcAcc.isPresent()) {
//				DMSVendorMCAcc vendorAcc = new DMSVendorMCAcc(vendor, mcAcc.get());
//				dmsVendorMCAccRepository.save(vendorAcc);
//			}
//		});
	}
	
	@Override
	public VendorDto getVendorAndMcAccs(Long vendorId) {
		Optional<Vendor> vendorOpt = vendorRepository.findById(vendorId);
		if (!vendorOpt.isPresent()) {
			throw new RuntimeException("Vendor not found!");
		}
		Vendor vendor = vendorOpt.get();
		VendorDto vendorDto = new VendorDto();
		vendorDto.setDescrption(vendor.getDescription());
		vendorDto.setId(vendor.getId());
		vendorDto.setName(vendor.getName());
		vendorDto.setType(vendor.getType());
		
		List<DMSVendorMCAcc> list = dmsVendorMCAccRepository.findByVendor(vendor);
		
		List<DMSAccDto> mcAccs = new ArrayList<>();
		list.forEach(li -> {
			DMSMcAcc mcAcc = li.getMcAcc();
			DMSAccDto dto = new DMSAccDto()
					.builder()
					.id(mcAcc.getId())
					.email(mcAcc.getEmail())
					.phoneNumber(mcAcc.getPhoneNumber())
					.username(mcAcc.getUsername())
					.build();
			mcAccs.add(dto);
		});
		vendorDto.setMcAccs(mcAccs);
		return vendorDto;
	}

	@PostConstruct
	public void init() {
		new Thread(() -> {
			countryCodeRepository.findAll().forEach(c -> {
				cCodes.add(c.getCallingCode().replaceAll("[^0-9]", ""));
			});
			cCodes.sort((o1, o2) -> {
				return Integer.parseInt(o1) < Integer.parseInt(o2) ? 1 : -1;
			});
		}).start();
	}
}
