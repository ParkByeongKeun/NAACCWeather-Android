package net.ijoon.ijoonweatherstation.util;

import android.util.Log;

public class SunSet {

    private static final double PI = 3.141592;
    private Double hour;
    private Double minute;

    private boolean IsLeapYear(int year) {
        return ((year % 4 == 0 && year % 100 != 0) || year % 400 == 0);
    }

    private int GetLastDay(int uiYear, int ucMonth) {
        switch (ucMonth) {
            case 2: // 2월
                if ((uiYear % 4) == 0) {        // 4로 나누어 떨어지는 해는 윤년임.
                    if (uiYear % 100 == 0) {    // 그중에서 100으로 나누어 떨어지는 해는 평년임
                        if (uiYear % 400 == 0) return 29; // 그중에서 400으로 나누어 떨어지는 해는 윤년임.
                        return 28; // 평년
                    }
                    return 29;    // 윤년
                }
                return 28;    // else 평년
            case 4:
            case 6:
            case 9:
            case 11: // 4, 6, 9, 11월
                return 30; // 30일
        }

        return 31; // 그외 31일
    }


    private int CalcJulianDay(int uiYear, int ucMonth, int ucDay) {
        int i;
        int iJulDay;
        iJulDay = 0;
        for (i = 1; i < ucMonth; i++) {
            iJulDay += GetLastDay(uiYear, i);
        }
        iJulDay += ucDay;
        return iJulDay;
    }

    private double CalcGamma(int iJulDay) {
        return (2.0 * PI / 365.0) * (iJulDay - 1);
    }

    private double CalcGamma2(int iJulDay, int hour) {
        return (2.0 * PI / 365.0) * (iJulDay - 1 + (hour / 24.0));
    }

    // Return the equation of time value for the given date.
    private double CalcEqofTime(double gamma) {
        return (229.18 * (0.000075 + 0.001868 * Math.cos(gamma) - 0.032077 * Math.sin(gamma) - 0.014615 * Math.cos(2 * gamma) - 0.040849 * Math.sin(2 * gamma)));

    }

    // Return the solar declination angle (in radians) for the given date.
    private double CalcSolarDec(double gamma) {
        return (0.006918 - 0.399912 * Math.cos(gamma) + 0.070257 * Math.sin(gamma) - 0.006758 * Math.cos(2 * gamma) + 0.000907 * Math.sin(2 * gamma));
    }

    private double DegreeToRadian(double angleDeg) {
        return (PI * angleDeg / 180.0);
    }

    private double RadianToDegree(double angleRad) {
        return (180 * angleRad / PI);
    }

    private double CalcHourAngle(double latitude, double solarDec, int time) {
        double latRad = DegreeToRadian(latitude);
        double hour_angle = Math.acos(Math.cos(DegreeToRadian(90.833)) / (Math.cos(latRad) * Math.cos(solarDec)) - Math.tan(latRad) * Math.tan(solarDec));
        if (time == 1) {
            return hour_angle;
        } else if (time == 0) {
            return -hour_angle;
        }
        return 0;
    }

    private double CalcSunriseGMT(int iJulDay, double latitude, double longitude) {
        double gamma = CalcGamma(iJulDay);
        double eqTime = CalcEqofTime(gamma);
        double solarDec = CalcSolarDec(gamma);
        double hourAngle = CalcHourAngle(latitude, solarDec, 1);
        double delta = longitude - RadianToDegree(hourAngle);
        double timeDiff = 4.0 * delta;
        double timeGMT = 720.0 + timeDiff - eqTime;
        double gamma_sunrise = CalcGamma2(iJulDay, (int) (timeGMT / 60.0));
        eqTime = CalcEqofTime(gamma_sunrise);
        solarDec = CalcSolarDec(gamma_sunrise);
        hourAngle = CalcHourAngle(latitude, solarDec, 1);
        delta = longitude - RadianToDegree(hourAngle);
        timeDiff = 4.0 * delta;
        timeGMT = 720.0 + timeDiff - eqTime;

        return timeGMT;
    }

    private double CalcSunsetGMT(int iJulDay, double latitude, double longitude) {
        // First calculates sunrise and approx length of day
        double gamma = CalcGamma(iJulDay + 1);
        double eqTime = CalcEqofTime(gamma);
        double solarDec = CalcSolarDec(gamma);

        double hourAngle = CalcHourAngle(latitude, solarDec, 0);
        double delta = longitude - RadianToDegree(hourAngle);
        double timeDiff = 4.0 * delta;
        double setTimeGMT = 720.0 + timeDiff - eqTime;
        // first pass used to include fractional day in gamma calc
        double gamma_sunset = CalcGamma2(iJulDay, (int) (setTimeGMT / 60.0));
        eqTime = CalcEqofTime(gamma_sunset);
        solarDec = CalcSolarDec(gamma_sunset);
        hourAngle = CalcHourAngle(latitude, solarDec, 0);
        delta = longitude - RadianToDegree(hourAngle);
        timeDiff = 4.0 * delta;
        setTimeGMT = 720.0 + timeDiff - eqTime; // in minutes
        return setTimeGMT;
    }


    private void GetTimeString(double minutes)
    // timeString returns a zero-padded string given time in minutes
    {
        double floatHour = minutes / 60.0;
        double hourD = Math.floor(floatHour);
        double floatMinute = 60.0 * (floatHour - Math.floor(floatHour));
        double minuteD = Math.floor(floatMinute);
        double floatSec = 60.0 * (floatMinute - Math.floor(floatMinute));
        double secondD = Math.floor(floatSec);

        hour = hourD;
        minute = minuteD;
    }

    public Double getSunSetHour() {

        return this.hour;
    }

    public Double getSunSetMinute() {

        return this.minute;
    }

    public double GetSunriseTime(int year, int month, int day, double latitude, double longitude, int zone, int daySavings) {
        int julday = CalcJulianDay(year, month, day);
        double timeLST = CalcSunriseGMT(julday, latitude, longitude) - (60 * zone) + daySavings; // minutes
        return timeLST;
    }

    public double GetSunsetTime(int year, int month, int day, double latitude, double longitude, int zone, int daySavings) {
        int julday = CalcJulianDay(year, month, day);
        double timeLST = CalcSunsetGMT(julday, latitude, longitude) - (60 * zone) + daySavings;
        return timeLST;
    }

    public void getSunset(int year, int month, int day, double latitude, double longitude) {
        double lst;

        lst = GetSunsetTime(year, month, day, latitude, -longitude, -9, 0);
        GetTimeString(lst);
    }

    public void getSunrise(int year, int month, int day, double latitude, double longitude) {
        double lst;

        lst = GetSunriseTime(year, month, day, latitude, -longitude, -9, 0);
        GetTimeString(lst);
    }
}