package com.pa.evs.sv;

public interface MeterService {

	void publish(String topic, Object message) throws Exception;

	void publish(Object message) throws Exception;
}
