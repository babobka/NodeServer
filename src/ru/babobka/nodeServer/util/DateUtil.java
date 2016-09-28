package ru.babobka.nodeServer.util;

import java.util.Calendar;

public final class DateUtil {

	private DateUtil() {

	}

	public static int getCurrentHour() {
		return (int) ((System.currentTimeMillis() / 1000 / 60 / 60) % 24);
	}

	public static String getMonthYear() {
		Calendar calendar = Calendar.getInstance();
		return calendar.get(Calendar.YEAR) + "" + calendar.get(Calendar.MONTH);
	}

}