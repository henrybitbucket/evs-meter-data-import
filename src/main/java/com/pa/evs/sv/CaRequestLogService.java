package com.pa.evs.sv;

import java.util.Optional;

import com.pa.evs.model.CARequestLog;

public interface CaRequestLogService {
	Optional<CARequestLog> findByUid(String uid);
}
