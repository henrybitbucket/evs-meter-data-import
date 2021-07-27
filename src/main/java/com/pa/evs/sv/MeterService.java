package com.pa.evs.sv;

public interface MeterService {

	void publish(String topic, Object message, String type) throws Exception;

	void publish(Object message, String type) throws Exception;
}
