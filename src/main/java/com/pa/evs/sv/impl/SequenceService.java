package com.pa.evs.sv.impl;

import jakarta.persistence.EntityManager;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(propagation = Propagation.REQUIRES_NEW)
public class SequenceService {

	@Autowired
	EntityManager em;
	
	public void createMIDSeq(Number vendorId) {
		em.createNativeQuery("CREATE SEQUENCE IF NOT EXISTS {h-schema}mid_sequence_vendor_" + vendorId).executeUpdate();
	}

	public Number nextvalMID(Number vendorId) {
		return (Number) em.createNativeQuery("select nextval('{h-schema}mid_sequence_vendor_" + vendorId + "')").getSingleResult();
	};

	public void nextvalMID(Long lastValue, Number vendorId) {
		em.createNativeQuery("ALTER SEQUENCE {h-schema}mid_sequence_vendor_" + vendorId + " RESTART WITH " + lastValue).executeUpdate();
	}
}
