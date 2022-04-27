package com.demo.app.movement.utils;

import java.util.Calendar;
import java.util.Date;

public class DateProcess {

    public static Date addDay(Date date) {
        return new Date(date.getTime() + (1000 * 60 * 60 * 24));
    }

    public static Date addMonth(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.add(Calendar.MONTH, 1);
        return calendar.getTime();
    }
}
