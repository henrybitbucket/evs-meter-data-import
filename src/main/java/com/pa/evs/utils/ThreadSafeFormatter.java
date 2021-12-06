package com.pa.evs.utils;

import java.text.FieldPosition;
import java.text.Format;
import java.text.ParsePosition;

public abstract class ThreadSafeFormatter<T extends Format> extends Format {
    private static final long serialVersionUID = 1L;

	protected abstract T getInstance(String template);

	public ThreadSafeFormatter(String template) {
		this.template = template;
	}

	@Override
    public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
		return get().format(obj, toAppendTo, pos);
	}

	@Override
    public Object parseObject(String source, ParsePosition pos) {
		return get().parseObject(source, pos);
    }

	private T get() {
		T format = formatter.get();
		if (null == format) {
			format = getInstance(template);
			formatter.set(format);
		}
		return format;
	}

	private final ThreadLocal<T> formatter = new ThreadLocal<T>();
	private final String template;
}
