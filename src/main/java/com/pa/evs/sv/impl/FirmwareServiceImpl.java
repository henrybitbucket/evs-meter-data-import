package com.pa.evs.sv.impl;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.annotation.PostConstruct;
import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.pa.evs.constant.Message;
import com.pa.evs.dto.FirmwareDto;
import com.pa.evs.dto.PaginDto;
import com.pa.evs.dto.VendorDto;
import com.pa.evs.model.Firmware;
import com.pa.evs.model.Vendor;
import com.pa.evs.repository.FirmwareRepository;
import com.pa.evs.repository.VendorRepository;
import com.pa.evs.sv.FirmwareService;

@Service
public class FirmwareServiceImpl implements FirmwareService {

    static final Logger LOG = LoggerFactory.getLogger(FirmwareServiceImpl.class);

    @Autowired
    private FirmwareRepository firmwareRepository;
    
    @Autowired
    private EntityManager em;
    
    @Autowired
    private VendorRepository vendorRepository;

    private Map<Long, Firmware> cache = null;

    @Value("${evs.pa.firmware.version}")
    private String firmwareVersion;

    @Value("${evs.pa.firmware.objectkey}")
    private String firmwareObjectKey;

    @Value("${evs.pa.firmware.hash}")
    private String firmwareHash;
    
    @PostConstruct
    public void init() {

    	new Thread(() -> {
        	List<Firmware> firmware = firmwareRepository.findTopByVendorOrderByIdDesc();
            if (cache == null) {
            	cache = new LinkedHashMap<>();
            	firmware.forEach(fw -> {
                	cache.put(fw.getVendor().getId(), fw);
                });
            }
    	}).start();
    }

    @Override
    public void upload(String version, String hashCode, Long vendor, MultipartFile file) throws Exception {
        Firmware entity = new Firmware();
        entity.setVersion(version);
        entity.setHashCode(hashCode);
        entity.setFileName(file.getOriginalFilename());
        
        Optional<Vendor> vendorOpt = vendorRepository.findById(vendor);
        
        if (!vendorOpt.isPresent()) {
        	throw new Exception(Message.VENDOR_NOT_FOUND);
        }
        
        entity.setVendor(vendorOpt.get());
        
        firmwareRepository.save(entity);
        List<Firmware> firmware = firmwareRepository.findTopByVendorOrderByIdDesc();
        firmware.forEach(fw -> {
        	cache.put(fw.getVendor().getId(), fw);
        });
    }

    @SuppressWarnings("unchecked")
    @Override
    public void getUploadedFirmwares(PaginDto<FirmwareDto> pagin) {
        StringBuilder sqlBuilder = new StringBuilder("FROM Firmware");
        StringBuilder sqlCountBuilder = new StringBuilder("SELECT count(*) FROM Firmware");
        
        StringBuilder sqlCommonBuilder = new StringBuilder();
        sqlCommonBuilder.append(" WHERE 1=1 ");
        sqlBuilder.append(sqlCommonBuilder).append(" ORDER BY id DESC");
        sqlCountBuilder.append(sqlCommonBuilder);
        
        if (pagin.getOffset() == null || pagin.getOffset() < 0) {
            pagin.setOffset(0);
        }
        
        if (pagin.getLimit() == null || pagin.getLimit() <= 0) {
            pagin.setLimit(100);
        }
        
        Query queryCount = em.createQuery(sqlCountBuilder.toString());
        
        Long count = ((Number)queryCount.getSingleResult()).longValue();
        pagin.setTotalRows(count);
        pagin.setResults(new ArrayList<>());
        if (count == 0l) {
            return;
        }
        
        Query query = em.createQuery(sqlBuilder.toString());
        query.setFirstResult(pagin.getOffset());
        query.setMaxResults(pagin.getLimit());
        
        List<Firmware> list = query.getResultList();
        
        list.forEach(li -> {
            FirmwareDto dto = FirmwareDto.builder()
                    .id(li.getId())
                    .version(li.getVersion())
                    .hashCode(li.getHashCode())
                    .fileName(li.getFileName())
                    .vendor(new VendorDto(li.getVendor()))
                    .build();
            pagin.getResults().add(dto);
        });
    }

    @Override
    public void deleteFirmware(Long id) {
        firmwareRepository.deleteById(id);
        
    }

    @Override
    public void editFirmware(Long id, String version, String hashCode, MultipartFile file) throws Exception {
        Optional<Firmware> opt = firmwareRepository.findById(id);
        
        if (!opt.isPresent()) {
            throw new Exception(Message.FIRMWARE_NOT_FOUND);
        }
        
        Firmware entity = opt.get();
        entity.setVersion(version);
        entity.setHashCode(hashCode);
        entity.setFileName(file.getOriginalFilename());
        
        firmwareRepository.save(entity);
    }

    @Override
    public Map<Long, Firmware> getLatestFirmware() {
        return cache;
    }

}
