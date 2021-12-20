package com.pa.evs.sv.impl;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pa.evs.dto.AddressDto;
import com.pa.evs.dto.BuildingDto;
import com.pa.evs.dto.PaginDto;
import com.pa.evs.enums.ResponseEnum;
import com.pa.evs.exception.ApiException;
import com.pa.evs.model.Address;
import com.pa.evs.model.Building;
import com.pa.evs.model.Building.BuildingType;
import com.pa.evs.repository.AddressRepository;
import com.pa.evs.repository.BuildingRepository;
import com.pa.evs.sv.BuildingService;
import com.pa.evs.utils.Utils;


@Transactional
@Service
public class BuildingServiceImpl implements BuildingService {

	@Autowired
	EntityManager em;

	@Autowired
	BuildingRepository buildingRepository;

	@Autowired
	AddressRepository addressRepository;

	@Override
	public void save(BuildingDto dto) throws ApiException {
		if (dto.getId() != null) {
			update(dto);
		} else {
			
			Building entity = new Building();
			Address address = new Address();
			AddressDto addressDto = dto.getAddress();
			address.setCity(addressDto.getCity());
			address.setCountry(addressDto.getCountry());
			address.setDisplayName(addressDto.getDisplayName());
			address.setBlock(addressDto.getBlock());
			address.setPostalCode(addressDto.getPostalCode());
			address.setStreet(addressDto.getStreet());
			address.setTown(addressDto.getTown());
			address.setUnitNumber(addressDto.getUnitNumber());
			address.setCreateDate(new Date());
			address.setModifyDate(new Date());
			addressRepository.save(address);

			entity.setAddress(address);
			entity.setDescription(dto.getDescription());
			entity.setHasTenant(dto.getHasTenant());
			entity.setId(dto.getId());
			entity.setName(dto.getName());
			entity.setType(BuildingType.from(dto.getType()));
			entity.setAddress(address);
			buildingRepository.save(entity);
		}
	}

	@Override
	public void search(PaginDto<BuildingDto> pagin) {

		StringBuilder sqlBuilder = new StringBuilder();
		sqlBuilder.append(" select b.id, b.name, b.type, b.description, b.has_tenant, ");
		sqlBuilder.append(" b.create_date, b.modify_date, ");
		sqlBuilder.append(" a.id as address_id, a.country, a.city, a.town, a.street, a.display_name, ");
		sqlBuilder.append(" a.postal_code, a.unit_number, a.block");
		sqlBuilder.append(" from {h-schema}building b " + "left join {h-schema}address a on b.address_id = a.id ");
		sqlBuilder.append(" where 1=1 ");
		if (StringUtils.isNotBlank(pagin.getKeyword())) {
			sqlBuilder.append(" and (b.name like '%" + pagin.getKeyword() + "%' or a.display_name like '%" + pagin.getKeyword() + "%')");
		}
		sqlBuilder.append(" offset " + pagin.getOffset() + " limit " + pagin.getLimit());
		Query q = em.createNativeQuery(sqlBuilder.toString());

		StringBuilder sqlCountBuilder = new StringBuilder();
		sqlCountBuilder.append(" select count(b.id)  ");
		sqlCountBuilder.append(" from {h-schema}building b ");
		sqlCountBuilder.append(" left join {h-schema}address a on b.address_id = a.id ");
		sqlCountBuilder.append(" where 1=1 ");
		if (StringUtils.isNotBlank(pagin.getKeyword())) {
			sqlCountBuilder.append(" and (b.name like '%" + pagin.getKeyword() + "%' or a.display_name like '%" + pagin.getKeyword() + "%')");
		}

		Query qr = em.createNativeQuery(sqlCountBuilder.toString());
		Number totalRows = (Number) qr.getSingleResult();
		pagin.setTotalRows(totalRows.longValue());
		List<BuildingDto> results = new ArrayList<>();
		for (int i = 0; i < q.getResultList().size(); i++) {
			BuildingDto a = new BuildingDto();
			AddressDto address = new AddressDto();

			Object[] o = (Object[]) q.getResultList().get(i);

			Number id = (Number) o[0];
			a.setId(id.longValue());
			a.setName((String) o[1]);
			a.setType((String) o[2]);
			a.setDescription((String) o[3]);
			a.setHasTenant((Boolean) o[4]);
			a.setCreatedDate((Date) o[5]);
			a.setModifiedDate((Date) o[6]);
			
			Number ida = (Number) o[7];
			address.setId(ida.longValue());
			address.setCountry((String) o[8]);
			address.setCity((String) o[9]);
			address.setTown((String) o[10]);
			address.setStreet((String) o[11]);
			address.setDisplayName((String) o[12]);
			address.setPostalCode((String) o[13]);
			address.setUnitNumber((String) o[14]);
			address.setBlock((String) o[15]);

			a.setAddress(address);
			a.setLabel(Utils.formatHomeAddress(a.getName(), address));
			results.add(a);
		}
		pagin.setResults(results);
	}

	@Override
	public void delete(Long id) throws ApiException {
		Building entity = buildingRepository.findById(id)
				.orElseThrow(() -> new ApiException(ResponseEnum.BUILDING_NOT_FOUND));
		buildingRepository.delete(entity);
		em.createNativeQuery("delete from {h-schema}address where id = " + entity.getAddress().getId()).executeUpdate();
	}

	@Override
	public void update(BuildingDto buildingDto) throws ApiException {

		Building building = buildingRepository.findById(buildingDto.getId())
				.orElseThrow(() -> new ApiException(ResponseEnum.BUILDING_NOT_FOUND));

		AddressDto addressDto = buildingDto.getAddress();
		Address address = addressRepository.findById(addressDto.getId()).orElse(new Address());
		address.setCity(addressDto.getCity());
		address.setCountry(addressDto.getCountry());
		address.setDisplayName(addressDto.getDisplayName());
		address.setBlock(addressDto.getBlock());
		address.setPostalCode(addressDto.getPostalCode());
		address.setStreet(addressDto.getStreet());
		address.setTown(addressDto.getTown());
		address.setUnitNumber(addressDto.getUnitNumber());
		address.setModifyDate(new Date());
		addressRepository.save(address);

		building.setModifyDate(new Date());
		building.setDescription(buildingDto.getDescription());
		building.setHasTenant(buildingDto.getHasTenant());
		building.setName(buildingDto.getName());
		building.setType(BuildingType.from(buildingDto.getType()));
		building.setAddress(address);
		buildingRepository.save(building);
	}
}
