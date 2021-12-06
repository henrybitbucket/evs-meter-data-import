package com.pa.evs.utils;

import java.text.SimpleDateFormat;

public class SimpleDateFormatter extends ThreadSafeFormatter<SimpleDateFormat> {
	private static final long serialVersionUID = 1L;

	public SimpleDateFormatter(String template) {
		super(template);
	}

	@Override
	protected SimpleDateFormat getInstance(String template) {
		return new SimpleDateFormat(template);
	}
}
