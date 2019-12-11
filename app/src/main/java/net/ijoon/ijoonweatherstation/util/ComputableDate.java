package net.ijoon.ijoonweatherstation.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * Created by pbj on 15. 10. 1..
 */
/*          Example Code...
 *
 *
 *
//        ComputableDate computableDate = new ComputableDate();
        ComputableDate computableDate = new ComputableDate("2016-01-19 11:22:33");
        Log.d("computableDate", "Date: " + computableDate.getCurrentTimeText());
        computableDate.addYear(10);
        Log.d("computableDate", "Date: " + computableDate.getCurrentTimeText());
        computableDate.addMonth(1);
        Log.d("computableDate", "Date: " + computableDate.getCurrentTimeText());
        computableDate.addDayOfMonth(15);
        Log.d("computableDate", "Date: " + computableDate.getCurrentTimeText());
        computableDate.addHour(2);
        Log.d("computableDate", "Date: " + computableDate.getCurrentTimeText());
        computableDate.addMinute(10);
        Log.d("computableDate", "Date: " + computableDate.getCurrentTimeText());
        computableDate.addSecond(5);
        Log.d("computableDate", "Date: " + computableDate.getCurrentTimeText());

        computableDate.setDate("1970-01-01 09:00:10");
        Log.d("computableDate", "Date: " + computableDate.getCurrentTimeText());

        Log.d("computableDate", "CurrentTime Milis is " + computableDate.getCurrentTimeMilis() + " milisecond");

        Log.d("computableDate", "Sparse Date is below ");
        computableDate.setYear(2016);
        computableDate.setMonth(1);
        computableDate.setDay(1);
        computableDate.setHour(1);
        computableDate.setMinute(2);
        computableDate.setSecond(3);
        Log.d("computableDate", computableDate.getYear()+"-"+computableDate.getMonth()+"-"+computableDate.getDay()+
                " " + computableDate.getHour()+":"+computableDate.getMinute()+":"+computableDate.getSecond());


* */

public class ComputableDate {

    private String mDateFormat = "yyyy-MM-dd HH:mm:ss";
    private Calendar mCalendar;
    private Date mDate;
    private SimpleDateFormat mSimpleDateFormat;
    ////////////////////////////////////
    // Constructor
    ////////////////////////////////////

    public ComputableDate() {
        initialize(null);
    }

    public ComputableDate(ComputableDate computableDate) {
        initialize(computableDate.getCurrentTimeText());
    }

    public ComputableDate(String dateStr) {
        initialize(dateStr);
    }

    public ComputableDate(int year, int month, int dayOfMonth, int hour, int minute, int second) {
        String dateStr = String.format("%04d-%02d-%02d %02d:%02d:%02d", year, month, dayOfMonth, hour, minute, second);
        initialize(dateStr);
    }

    // mCalendar에 사용자가 원하는 시간 또는 현재 시간을 저장하여 초기화
    private void initialize(String dateStr) {
        mSimpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        if(dateStr != null) {
            try {
                mDate = mSimpleDateFormat.parse(dateStr);
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }
        else {
            mDate = new Date();
        }
        mCalendar = Calendar.getInstance();
        mCalendar.setTime(mDate);
    }

    ////////////////////////////////////
    // Complex Setter
    ////////////////////////////////////
    public void setDate(String dateStr) {
        try {
            mDate = mSimpleDateFormat.parse(dateStr);
            mCalendar.setTime(mDate);
        } catch (ParseException | NullPointerException e) {
            e.printStackTrace();
        }
    }

    public void setDate(Date date) {
        mDate = date;
        mCalendar.setTime(mDate);
    }

    public ComputableDate setDate(int uts) {
        mDate.setTime((long)uts*1000);
        mCalendar.setTime(mDate);
        return this;
    }

    ////////////////////////////////////
    // Complex Getter
    ////////////////////////////////////
    public long getCurrentTimeMilis() {
        return mCalendar.getTimeInMillis();
    }

    public long getCurrentTimeSecond() {
        return mCalendar.getTimeInMillis()/1000;
    }

    public String getCurrentTimeText() {
        return mSimpleDateFormat.format(mCalendar.getTime());
    }

    public String getCurrentTimeText(String format) {
        SimpleDateFormat sdf = new SimpleDateFormat(format);
        return sdf.format(mCalendar.getTime());
    }

    public int getAMPM() {
        return mCalendar.get(Calendar.AM_PM);
    }

    ////////////////////////////////////
    // Compute Function
    ////////////////////////////////////
    public void addYear(int year) {
        mCalendar.add(Calendar.YEAR, year);
    }

    public void addMonth(int month) {
        mCalendar.add(Calendar.MONTH, month);
    }

    public void addDayOfMonth(int dayOfMonth) {
        mCalendar.add(Calendar.DATE, dayOfMonth);
    }

    public void addHour(int hour) {
        mCalendar.add(Calendar.HOUR, hour);
    }

    public void addMinute(int minute) {
        mCalendar.add(Calendar.MINUTE, minute);
    }

    public void addSecond(int second) {
        mCalendar.add(Calendar.SECOND, second);
    }

    ///////////////////////////////////
    // Compare Function
    ////////////////////////////////////
    public long getTimeDiffWith(ComputableDate computableDate) {
        return ( (this.getCurrentTimeMilis() - computableDate.getCurrentTimeMilis()) / 1000);
    }

    ////////////////////////////////////
    // Member Variable Getter & Setter
    ////////////////////////////////////
    public int getYear() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy");
        return Integer.parseInt(sdf.format(mCalendar.getTime()));
    }

    public String getYearStr() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy");
        return sdf.format(mCalendar.getTime());
    }

    public void setYear(int year) {
        String currentDate = mSimpleDateFormat.format(mCalendar.getTime());
        String[]dateAndTime = currentDate.split(" ");
        String[]YearMonthDay = dateAndTime[0].split("-");
        YearMonthDay[0] = String.format("%04d", year);
        try {
            mDate = mSimpleDateFormat.parse(
                    YearMonthDay[0] + "-" + YearMonthDay[1] + "-" + YearMonthDay[2]
                            + " " + dateAndTime[1]);
            mCalendar.setTime(mDate);
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    public int getMonth() {
        SimpleDateFormat sdf = new SimpleDateFormat("MM");
        return Integer.parseInt(sdf.format(mCalendar.getTime()));
    }

    public String getMonthStr() {
        SimpleDateFormat sdf = new SimpleDateFormat("MM");
        return sdf.format(mCalendar.getTime());
    }

    public void setMonth(int month) {
        String currentDate = mSimpleDateFormat.format(mCalendar.getTime());
        String[]dateAndTime = currentDate.split(" ");
        String[]YearMonthDay = dateAndTime[0].split("-");
        YearMonthDay[1] = String.format("%02d", month);
        try {
            mDate = mSimpleDateFormat.parse(
                    YearMonthDay[0] + "-" + YearMonthDay[1] + "-" + YearMonthDay[2]
                            + " " + dateAndTime[1]);
            mCalendar.setTime(mDate);
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    public int getDay() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd");
        return Integer.parseInt(sdf.format(mCalendar.getTime()));
    }

    public String getDayStr() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd");
        return sdf.format(mCalendar.getTime());
    }

    public void setDay(int day) {
        String currentDate = mSimpleDateFormat.format(mCalendar.getTime());
        String[]dateAndTime = currentDate.split(" ");
        String[]YearMonthDay = dateAndTime[0].split("-");
        YearMonthDay[2] = String.format("%02d", day);
        try {
            mDate = mSimpleDateFormat.parse(
                    YearMonthDay[0] + "-" + YearMonthDay[1] + "-" + YearMonthDay[2]
                            + " " + dateAndTime[1]);
            mCalendar.setTime(mDate);
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    public int getHour() {
        SimpleDateFormat sdf = new SimpleDateFormat("HH");
        return Integer.parseInt(sdf.format(mCalendar.getTime()));
    }

    public String getHourStr() {
        SimpleDateFormat sdf = new SimpleDateFormat("HH");
        return sdf.format(mCalendar.getTime());
    }

    public void setHour(int hour) {
        String currentDate = mSimpleDateFormat.format(mCalendar.getTime());
        String[]dateAndTime = currentDate.split(" ");
        String[]HourMinuteSecond = dateAndTime[1].split(":");
        HourMinuteSecond[0] = String.format("%02d", hour);
        try {
            mDate = mSimpleDateFormat.parse(
                    dateAndTime[0] + " "
                            + HourMinuteSecond[0] + ":" + HourMinuteSecond[1] + ":" + HourMinuteSecond[2]
            );
            mCalendar.setTime(mDate);
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    public int getMinute() {
        SimpleDateFormat sdf = new SimpleDateFormat("mm");
        return Integer.parseInt(sdf.format(mCalendar.getTime()));
    }

    public String getMinuteStr() {
        SimpleDateFormat sdf = new SimpleDateFormat("mm");
        return sdf.format(mCalendar.getTime());
    }

    public void setMinute(int minute) {
        String currentDate = mSimpleDateFormat.format(mCalendar.getTime());
        String[]dateAndTime = currentDate.split(" ");
        String[]HourMinuteSecond = dateAndTime[1].split(":");
        HourMinuteSecond[1] = String.format("%02d", minute);
        try {
            mDate = mSimpleDateFormat.parse(
                    dateAndTime[0] + " "
                            + HourMinuteSecond[0] + ":" + HourMinuteSecond[1] + ":" + HourMinuteSecond[2]
            );
            mCalendar.setTime(mDate);
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    public int getSecond() {
        SimpleDateFormat sdf = new SimpleDateFormat("ss");
        return Integer.parseInt(sdf.format(mCalendar.getTime()));
    }

    public String getSecondStr() {
        SimpleDateFormat sdf = new SimpleDateFormat("ss");
        return sdf.format(mCalendar.getTime());
    }

    public void setSecond(int second) {
        String currentDate = mSimpleDateFormat.format(mCalendar.getTime());
        String[]dateAndTime = currentDate.split(" ");
        String[]HourMinuteSecond = dateAndTime[1].split(":");
        HourMinuteSecond[2] = String.format("%02d", second);
        try {
            mDate = mSimpleDateFormat.parse(
                    dateAndTime[0] + " "
                            + HourMinuteSecond[0] + ":" + HourMinuteSecond[1] + ":" + HourMinuteSecond[2]
            );
            mCalendar.setTime(mDate);
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    public String getDateFormat() {
        return mDateFormat;
    }

    public void setDateFormat(String dateFormat) {
        mSimpleDateFormat = new SimpleDateFormat(dateFormat);
        this.mDateFormat = dateFormat;
    }

    public Date getDate() {
        return mDate;
    }

    public Calendar getCalendar() {
        return mCalendar;
    }

    public int getDayOfWeek() {
        return mCalendar.get(Calendar.DAY_OF_WEEK);
    }
}