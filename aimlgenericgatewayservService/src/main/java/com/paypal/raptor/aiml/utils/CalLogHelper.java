package com.paypal.raptor.aiml.utils;

import org.apache.commons.lang3.StringUtils;
import com.ebay.kernel.cal.api.CalEvent;
import com.ebay.kernel.cal.api.CalStream;
import com.ebay.kernel.cal.api.CalStreamGroupFactory;
import com.ebay.kernel.cal.api.CalStreamUtils;
import com.paypal.raptor.aiml.common.exception.GatewayException;


/**
 * @author: qingwu
 **/
public final class CalLogHelper {

	private static final String LINE_SEPARATOR = System.getProperty("line.separator");

	private static final String CAL_STATUS_INFO = "0";

	private static final String CAL_STATUS_ERROR = "2";

	private static final String CAL_STATUS_EXCEPTION = "3";

	private static final String CAL_STATUS_WARNING = "4";

	private CalLogHelper(){

	}


	public static void logException(String name, Throwable e) {
		logCal(CalEvent.CAL_EXCEPTION, name,null, getCalStream(), e);
	}

	public static void logException(String name, GatewayException e) {
		logCal(CalEvent.CAL_EXCEPTION, name, e.getErrorMessage(), getCalStream());
	}

	public static void logException(String name, String errorMsg) {
		logCal(CalEvent.CAL_EXCEPTION, name, errorMsg, getCalStream());
	}

	public static void logError(String name, String data) {
		logCal(CalEvent.CAL_ERROR, name, data, getCalStream(), null);
	}

	private static void logCal(String type, String name, String data, CalStream calStream, Throwable e) {
		if (calStream != null) {
			CalEvent calEvent = calStream.event(type);
			calEvent.setName(name);
			if (!StringUtils.isEmpty(data)) {
				calEvent.addData(data);
			}
			if (e != null) {
				calEvent.addData(getStackTrace(e));
			}
			setStatus(calEvent, type);
			calEvent.completed();
		}
	}

	private static void logCal(String type, String name, String data, CalStream calStream) {
		if (calStream != null) {
			CalEvent calEvent = calStream.event(type);
			calEvent.setName(name);
			if (!StringUtils.isEmpty(data)) {
				calEvent.addData(data);
			}
			setStatus(calEvent, type);
			calEvent.completed();
		}
	}

	public static String getStackTrace(final Throwable exception) {
		StringBuilder result = new StringBuilder();
		if (!StringUtils.isEmpty(exception.getMessage())) {
			result.append(LINE_SEPARATOR + exception.getMessage());
		}
		result.append(LINE_SEPARATOR);

		for (StackTraceElement e : exception.getStackTrace()) {
			result.append(e.toString() + LINE_SEPARATOR);
		}

		return result.toString();
	}

	private static void setStatus(CalEvent calEvent, String event) {
		switch (event) {
			case CalEvent.CAL_ERROR:
				calEvent.setStatus(CAL_STATUS_ERROR);
				break;
			case CalEvent.CAL_EXCEPTION:
				calEvent.setStatus(CAL_STATUS_EXCEPTION);
				break;
			case CalEvent.CAL_WARNING:
				calEvent.setStatus(CAL_STATUS_WARNING);
				break;
			default:
				calEvent.setStatus(CAL_STATUS_INFO);
				break;
		}
	}

	public static CalStream getCalStream() {
		CalStream calStream = CalStreamUtils.getInstance()
						.currentCalStream();
		return calStream == null ? CalStreamGroupFactory.create().acquireCalStream(): calStream;
	}
}
