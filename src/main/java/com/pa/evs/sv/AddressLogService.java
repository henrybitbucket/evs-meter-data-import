package com.pa.evs.sv;

import com.pa.evs.dto.AddressLogDto;
import com.pa.evs.dto.PaginDto;

public interface AddressLogService {

	PaginDto<AddressLogDto> getAddressLogs(PaginDto<AddressLogDto> pagin);

}
