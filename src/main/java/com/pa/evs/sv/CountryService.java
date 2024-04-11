package com.pa.evs.sv;

import java.util.List;

import com.pa.evs.dto.PaginDto;
import com.pa.evs.model.CountryCode;

public interface CountryService {

	void search(@SuppressWarnings("rawtypes") PaginDto cs);

	void save(List<CountryCode> cs);
}
