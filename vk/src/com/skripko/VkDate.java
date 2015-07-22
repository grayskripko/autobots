package com.skripko;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class VkDate {

    private String poorString;
    private Date date;

    public VkDate(String poorString) {
        this.poorString = poorString;
        date = parseString();
    }

    private Date parseString() {
        //SimpleDateFormat parser = new SimpleDateFormat("dd MMM 'at' HH:mm a");
        Calendar now = Calendar.getInstance();
        if (poorString.endsWith("ago")) {
            String[] arr = poorString.split(" ");
            arr[0].replace("one", "1").replace("two", "2").replace("three", "3");
            int period = Calendar.SECOND; //"54 seconds ago"
            if (arr[1].contains("minute")) { //"20 minutes ago"
                period = Calendar.MINUTE;
            } else if(arr[1].contains("hour")) { //"one hour ago"
                period = Calendar.HOUR;
            }
            now.add(period, -1 * Integer.parseInt(arr[0]));
            return now.getTime();
        }

        String dateStr = poorString;
        String currentDay;
        String currentMonth;
        if (dateStr.contains("(Yesterday)|(Вчера)|(Today)|(Сегодня)|(Tomorrow)|(Завтра)")) {
            if (dateStr.contains("(Yesterday)|(Вчера)")) { //"Yesterday at 4:00 am"
                now.add(Calendar.DATE, -1);
            } else if (dateStr.contains("(Tomorrow)|(Завтра)")) { //"Tomorrow at 4:00 am"
                now.add(Calendar.DATE, 1);
            }
            currentDay = Integer.toString(now.get(Calendar.DATE));
            currentMonth = Integer.toString(now.get(Calendar.MONTH));
            dateStr = dateStr.replace("(Yesterday)|(Вчера)|(Today)|(Сегодня)|(Tomorrow)|(Завтра)", currentDay + " " + currentMonth); //"Today at 9:00 pm"
        }
        try {
            return new SimpleDateFormat("dd MM 'at' HH:mm a").parse(dateStr);
        } catch (ParseException ex) {
        }

        if (poorString.equals("Just now")) {
            return new Date();
        }

        try {
            return new SimpleDateFormat("dd MMM 'at' HH:mm a").parse(poorString); //"1 Feb at 8:00 pm"
        } catch (ParseException ex) {
        }

        poorString = "couldnt recognize >> " + poorString;
        return null;
    }

    public String getPoorString() {
        return poorString;
    }

    @Override
    public String toString() {
        if (date == null) {
            return poorString;
        }
        return new SimpleDateFormat("dd MMM 'at' HH:mm a").format(date);
    }
}