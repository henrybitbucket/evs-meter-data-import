package com.pa.evs.sv;

public interface EVSPAService {

	void publish(String topic, Object message) throws Exception;
	
	Long nextvalMID();
}
