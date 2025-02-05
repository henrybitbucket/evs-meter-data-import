package com.pa.evs.dto;

import java.io.Serializable;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Map;

import com.pa.evs.utils.SimpleDateFormatter;

public class ReportJasperParameterDto implements Serializable {

	private static final long serialVersionUID = 1L;

	public static final String PARAM_NAME_LOV_ID = "lov.id";

	public static enum ParameterType {
		/*
		 * Descended order [9-0] by type size. It should be ordered like that to get right type. In
		 * other cases if we have java.sql.Timestamp which is assigned to java.util.Date then we can
		 * missed the type.
		 */
		TIMESTAMP("timestamp", Timestamp.class, new SimpleDateFormatter("yyyyMMddHHmmss")),
		DATE("date", Date.class, new SimpleDateFormatter("yyyyMMdd")),
		BIGDECIMAL("bigdecimal", BigDecimal.class),
		DOUBLE("double", Double.class),
		LONG("long", Long.class),
		INTEGER("integer", Integer.class),
		SHORT("short", Short.class),
		BYTE("byte", Byte.class),
		BOOLEAN("boolean", Boolean.class),
		STRING("string", String.class),
		COLLECTION("collection", Collection.class);

		private ParameterType(String name, Class<?> cls) {
			this.name = name;
			this.cls = cls;
			this.format = null;
		}

		private ParameterType(String name, Class<?> cls, Format format) {
			this.name = name;
			this.cls = cls;
			this.format = format;
		}

		public static ParameterType valueOfByName(String name) {
			for (ParameterType type : values()) {
				if (type.name.equals(name)) {
					return type;
				}
			}
			return STRING;
		}

		public static ParameterType valueOfByClass(Class<?> cls) {
			for (ParameterType type : ParameterType.values()) {
				if (type.isAssignable(cls)) {
					return type;
				}
			}
			return STRING;
		}

		@Override
		public String toString() {
			return name;
		}

		public String format(Object obj) {
			return ((null == format) ? "" + obj : format.format(obj));
		}

		public boolean isAssignable(Class<?> cls) {
			return this.cls.isAssignableFrom(cls);
		}

		private final String name;
		private final Format format;
		private final Class<?> cls;
	};

	private String name;
	private String description;
	private ParameterType type;
	private Object value;
	private Map<String, String> properties;
	private boolean show;
	private String valueLabel;

	public String getValueLabel() {
		return valueLabel;
	}

	public void setValueLabel(String valueLabel) {
		this.valueLabel = valueLabel;
	}

	public boolean isShow() {
		return show;
	}

	public void setShow(boolean show) {
		this.show = show;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public ParameterType getType() {
		return type;
	}

	public void setType(ParameterType type) {
		this.type = type;
	}

	public String getTypeString() {
		return "" + type;
	}

	public Object getValue() {
		return value;
	}

	public void setValue(Object val) {
		ParameterType dt = type;
		switch (dt) {
		case DATE:
		case TIMESTAMP:
			value = toDate(val);
			break;
		case BOOLEAN:
			value = toBool(val);
			break;
		case BYTE:
			value = toByte(val);
			break;
		case SHORT:
			value = toShort(val);
			break;
		case INTEGER:
			value = toInt(val);
			break;
		case LONG:
			value = toLong(val);
			break;
		case DOUBLE:
			value = toDouble(val);
		case BIGDECIMAL:
			value = toDecimal(val);
			break;
		default:
			value = val;
			break;
		}
	}

	public String getStrValue() {
		if (value == null) {
			return null;
		}
		return value.toString();
	}

	public Map<String, String> getProperties() {
		if (properties == null) {
			return Collections.emptyMap();
		} else {
			return properties;
		}
	}

	public void setProperties(Map<String, String> properties) {
		this.properties = properties;
	}

	public boolean isNumeric() {
		ParameterType dt = type;
		switch (dt) {
		case BYTE:
		case SHORT:
		case INTEGER:
		case LONG:
		case DOUBLE:
		case BIGDECIMAL:
			return true;
		default:
			return false;
		}
	}

	public boolean isInteger() {
		ParameterType dt = type;
		switch (dt) {
		case BYTE:
		case SHORT:
		case INTEGER:
		case LONG:
			return true;
		default:
			return false;
		}
	}

	private Integer toInt(Object val) {
		if (isEmpty(val)) {
			return null;
		}
		return Integer.valueOf(toLong(val).intValue());
	}

	private Byte toByte(Object val) {
		if (isEmpty(val)) {
			return null;
		}
		return Byte.valueOf(toInt(val).byteValue());
	}

	private Short toShort(Object val) {
		if (isEmpty(val)) {
			return null;
		}
		return Short.valueOf((short) toInt(val).intValue());
	}

	private Long toLong(Object val) {
		if (isEmpty(val)) {
			return null;
		}
		if (val instanceof Long) {
			return (Long) val;
		}
		if (val instanceof Number) {
			return Long.valueOf(((Number) val).intValue());
		}
		return Long.valueOf(val.toString());
	}

	private BigDecimal toDecimal(Object val) {
		if (isEmpty(val)) {
			return null;
		}
		return new BigDecimal(toDouble(val).doubleValue());
	}

	private Double toDouble(Object val) {
		if (isEmpty(val)) {
			return null;
		}
		if (val instanceof Double) {
			return (Double) val;
		}
		if (val instanceof Number) {
			return Double.valueOf(((Number) val).doubleValue());
		}
		return Double.valueOf(val.toString());
	}

	private Date toDate(Object obj) {

		if (obj == null) {
			return null;
		}

		if (obj instanceof String) {
			SimpleDateFormat sf = new SimpleDateFormat();
			if ((obj + "").matches("^[0-9]{4}-[0-9]{2}-[0-9]{2}$")) {
				sf.applyPattern("yyyy-MM-dd");
			} else if ((obj + "").matches("^[0-9]{4}-[0-9]{2}-[0-9]{2} [0-9]{2}\\:[0-9]{2}$")) {
				sf.applyPattern("yyyy-MM-dd HH:mm");
			} else if ((obj + "").matches("^[0-9]{4}-[0-9]2}-[0-9]{2}.*")) {
				obj = (obj + "").substring(0, 8);
				sf.applyPattern("yyyy-MM-dd");
			} else {
				return null;
			}
			try {
				return sf.parse((obj + ""));
			} catch (Exception e) {
				return null;
			}
		}

		return (Date) obj;
	}

	private Boolean toBool(Object obj) {
		if (isEmpty(obj)) {
			return null;
		}
		if (obj instanceof Number) {
			return ((Number) obj).intValue() != 0;
		}
		return Boolean.valueOf(obj.toString());
	}

	private boolean isEmpty(Object obj) {
		if (obj == null || !(obj instanceof String)) {
			return true;
		}
		String str = (String) obj;
		return str.trim().length() == 0;
	}
}
