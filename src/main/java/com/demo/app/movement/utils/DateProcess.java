package com.demo.app.movement.utils;

import java.util.Calendar;
import java.util.Date;

public class DateProcess {

    public static Date addMonth(Date date,int quanttyMonths) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.add(Calendar.MONTH,quanttyMonths);
        return calendar.getTime();
    }

    public static Date reduceOneMonth(Date date,int quanttyMonths) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.add(Calendar.MONTH, quanttyMonths);
        return calendar.getTime();
    }

    public static Date updateDate(Date date, int param) {
        Calendar updateDate = Calendar.getInstance();
        Calendar lastDate = Calendar.getInstance();
        lastDate.setTime(date);

        int oldDay = lastDate.DAY_OF_MONTH;
        int month = updateDate.MONTH;
        int year = updateDate.YEAR;

        updateDate.set(year, month, oldDay);
        if (param == 0) {
            updateDate.add(Calendar.DAY_OF_MONTH, 1);
        } else {
            return updateDate.getTime();
        }
        return updateDate.getTime();
    }

    public static Boolean dateCompare(Date firstDate, Date lastDate) {
        Boolean result = false;
        return firstDate.before(lastDate);
    }
}
