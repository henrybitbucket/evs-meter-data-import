package com.pa.evs.utils.log4j2;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.impl.ThrowableFormatOptions;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.apache.logging.log4j.core.pattern.ConverterKeys;
import org.apache.logging.log4j.core.pattern.DatePatternConverter;
import org.apache.logging.log4j.core.pattern.PatternConverter;
import org.apache.logging.log4j.core.pattern.PatternFormatter;
import org.apache.logging.log4j.core.pattern.PatternParser;
import org.apache.logging.log4j.core.pattern.ThrowablePatternConverter;
import org.apache.logging.log4j.core.util.StringBuilderWriter;
import org.apache.logging.log4j.util.Strings;

@Plugin(name = "CtThrowablePatternConverter", category = PatternConverter.CATEGORY)
@ConverterKeys({ "cthrowable" })
public class CtThrowablePatternConverter extends ThrowablePatternConverter {

	/**
	 * Lists {@link PatternFormatter}s for the suffix attribute.
	 */
	protected final List<PatternFormatter> formatters;
	private String rawOption;
	private final boolean subShortOption;
	final boolean nonStandardLineSeparator;
	
	private DatePatternConverter dateConverter = null;

	/**
	 * Options.
	 */
	protected final ThrowableFormatOptions options;

	/**
	 * Constructor.
	 * 
	 * @param name
	 *            Name of converter.
	 * @param style
	 *            CSS style for output.
	 * @param options
	 *            options, may be null.
	 * @param config
	 */
	protected CtThrowablePatternConverter(final String name, final String style, final String[] options,
			final Configuration config) {
		super(name, style, options, config);
		this.options = ThrowableFormatOptions.newInstance(options);
		if (options != null && options.length > 0) {
			rawOption = options[0];
		}
		if (this.options.getSuffix() != null) {
			final PatternParser parser = PatternLayout.createPatternParser(config);
			final List<PatternFormatter> parsedSuffixFormatters = parser.parse(this.options.getSuffix());
			// filter out nested formatters that will handle throwable
			boolean hasThrowableSuffixFormatter = false;
			for (final PatternFormatter suffixFormatter : parsedSuffixFormatters) {
				if (suffixFormatter.handlesThrowable()) {
					hasThrowableSuffixFormatter = true;
				}
			}
			if (!hasThrowableSuffixFormatter) {
				this.formatters = parsedSuffixFormatters;
			} else {
				final List<PatternFormatter> suffixFormatters = new ArrayList<>();
				for (final PatternFormatter suffixFormatter : parsedSuffixFormatters) {
					if (!suffixFormatter.handlesThrowable()) {
						suffixFormatters.add(suffixFormatter);
					}
				}
				this.formatters = suffixFormatters;
			}
		} else {
			this.formatters = Collections.emptyList();
		}
		subShortOption = ThrowableFormatOptions.MESSAGE.equalsIgnoreCase(rawOption)
				|| ThrowableFormatOptions.LOCALIZED_MESSAGE.equalsIgnoreCase(rawOption)
				|| ThrowableFormatOptions.FILE_NAME.equalsIgnoreCase(rawOption)
				|| ThrowableFormatOptions.LINE_NUMBER.equalsIgnoreCase(rawOption)
				|| ThrowableFormatOptions.METHOD_NAME.equalsIgnoreCase(rawOption)
				|| ThrowableFormatOptions.CLASS_NAME.equalsIgnoreCase(rawOption);
		nonStandardLineSeparator = !Strings.LINE_SEPARATOR.equals(this.options.getSeparator());
		
		dateConverter = DatePatternConverter.newInstance(new String[] {"yyyy-MM-dd HH:mm:ss.SSS", "GMT+7"});
	}

	/**
	 * Gets an instance of the class.
	 *
	 * @param config
	 * @param options
	 *            pattern options, may be null. If first element is "short", only
	 *            the first line of the throwable will be formatted.
	 * @return instance of class.
	 */
	public static CtThrowablePatternConverter newInstance(final Configuration config, final String[] options) {
		return new CtThrowablePatternConverter("CThrowable", "cthrowable", options, config);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void format(final LogEvent event, final StringBuilder buffer) {
		final Throwable t = event.getThrown();

		if (subShortOption) {
			formatSubShortOption(t, getSuffix(event), buffer);
		} else if (t != null && options.anyLines()) {
			formatOption(t, getSuffix(event), buffer);
		}
	}

	private void formatSubShortOption(final Throwable t, final String suffix, final StringBuilder buffer) {
		StackTraceElement[] trace;
		StackTraceElement throwingMethod = null;
		int len;

		if (t != null) {
			trace = t.getStackTrace();
			if (trace != null && trace.length > 0) {
				throwingMethod = trace[0];
			}
		}

		if (t != null && throwingMethod != null) {
			String toAppend = Strings.EMPTY;

			if (ThrowableFormatOptions.CLASS_NAME.equalsIgnoreCase(rawOption)) {
				toAppend = throwingMethod.getClassName();
			} else if (ThrowableFormatOptions.METHOD_NAME.equalsIgnoreCase(rawOption)) {
				toAppend = throwingMethod.getMethodName();
			} else if (ThrowableFormatOptions.LINE_NUMBER.equalsIgnoreCase(rawOption)) {
				toAppend = String.valueOf(throwingMethod.getLineNumber());
			} else if (ThrowableFormatOptions.MESSAGE.equalsIgnoreCase(rawOption)) {
				toAppend = t.getMessage();
			} else if (ThrowableFormatOptions.LOCALIZED_MESSAGE.equalsIgnoreCase(rawOption)) {
				toAppend = t.getLocalizedMessage();
			} else if (ThrowableFormatOptions.FILE_NAME.equalsIgnoreCase(rawOption)) {
				toAppend = throwingMethod.getFileName();
			}

			len = buffer.length();
			if (len > 0 && !Character.isWhitespace(buffer.charAt(len - 1))) {
				buffer.append(' ');
			}
			buffer.append(toAppend);

			if (Strings.isNotBlank(suffix)) {
				buffer.append(' ');
				buffer.append(suffix);
			}
		}
	}

	private void formatOption(final Throwable throwable, final String suffix, final StringBuilder buffer) {
		final int len = buffer.length();
		
		int index = buffer.indexOf("ERROR");
		String prefix = index != -1 ? buffer.subSequence(0, index + 5).toString() : "";
		if (len > 0 && !Character.isWhitespace(buffer.charAt(len - 1))) {
			buffer.append(' ');
		}
		if (dateConverter != null) {
			final StringWriter w = new StringWriter();
			throwable.printStackTrace(new PrintWriter(w));

			final String[] array = w.toString().split(Strings.LINE_SEPARATOR);
			final int limit = options.minLines(array.length) - 1;
			final boolean suffixNotBlank = Strings.isNotBlank(suffix);
			//dateConverter.format(new Date(), buffer);
			buffer.append(options.getSeparator());
			for (int i = 0; i <= limit; ++i) {
				//dateConverter.format(new Date(), buffer);
				buffer.append(prefix + ' ' + array[i]);
				if (suffixNotBlank) {
					buffer.append(' ');
					buffer.append(suffix);
				}
				//if (i < limit) {
					buffer.append(options.getSeparator());
				//}
			}
		} else {
			throwable.printStackTrace(new PrintWriter(new StringBuilderWriter(buffer)));
		}
	}

	/**
	 * This converter obviously handles throwables.
	 *
	 * @return true.
	 */
	@Override
	public boolean handlesThrowable() {
		return true;
	}

	protected String getSuffix(final LogEvent event) {
		if (formatters.isEmpty()) {
			return Strings.EMPTY;
		}
		// noinspection ForLoopReplaceableByForEach
		final StringBuilder toAppendTo = new StringBuilder();
		for (int i = 0, size = formatters.size(); i < size; i++) {
			formatters.get(i).format(event, toAppendTo);
		}
		return toAppendTo.toString();
	}

	public ThrowableFormatOptions getOptions() {
		return options;
	}
}
