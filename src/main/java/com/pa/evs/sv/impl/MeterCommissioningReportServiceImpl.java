package com.pa.evs.sv.impl;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.pa.evs.dto.MeterCommissioningReportDto;
import com.pa.evs.exception.ApiException;
import com.pa.evs.model.MeterCommissioningReport;
import com.pa.evs.model.Users;
import com.pa.evs.repository.MeterCommissioningReportRepository;
import com.pa.evs.repository.UserRepository;
import com.pa.evs.sv.MeterCommissioningReportService;

@Service
public class MeterCommissioningReportServiceImpl implements MeterCommissioningReportService {
	
	@Autowired
	private UserRepository userRepository;
	
	@Autowired
	private MeterCommissioningReportRepository meterCommissioningReportRepository;

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
		
		if (dto.getInstaller() != null) {
			Optional<Users> installer = userRepository.findById(dto.getInstaller());
			mcr.setInstaller(installer.orElse(null));
		}
		
		meterCommissioningReportRepository.save(mcr);
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
		
		if (mcr.getInstaller() != null) {
			dto.setInstaller(mcr.getInstaller().getUserId());
			dto.setInstallerName(mcr.getInstaller().getUsername());
			dto.setInstallerEmail(mcr.getInstaller().getEmail());
		}
		return dto;
	}

}
