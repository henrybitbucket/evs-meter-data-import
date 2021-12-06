package com.pa.evs.enums;

public enum JasperFormat {
	PDF("pdf", 0), XLSX("xlsx", 1), CSV("csv", 3);

	private JasperFormat(String ext,
			int index) {
		this.ext = ext;
		this.index = index;
	}

	public static JasperFormat valueOf(int index) {
		for (JasperFormat item : JasperFormat.values()) {
			if (item.getIndex() == index) {
				return item;
			}
		}
		throw new IllegalArgumentException("Undefined JasperFormat with index " + index);
	}

	public String getExt() {
		return ext;
	}

	public int getIndex() {
		return index;
	}

	private String ext;
	private int index;
}
