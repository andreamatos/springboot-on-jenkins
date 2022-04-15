package br.ce.wcaquino.taskbackend.utils;

import java.time.LocalDate;

import org.junit.Assert;
import org.junit.Test;

public class DateUtilsTest {

    @Test
    public void shouldReturnTrueWithFutureDates() {
        LocalDate date = LocalDate.of(2030, 01, 01);
        Assert.assertTrue(DateUtils.isEqualOrFutureDate(date));
    }

    @Test
    public void shouldReturnTrueWithPresentDate() {
        LocalDate date = LocalDate.now();
        Assert.assertTrue(DateUtils.isEqualOrFutureDate(date));
    }

    @Test
    public void shouldReturnFalseWithPastDates() {
        LocalDate date = LocalDate.of(2012, 01, 01);
        Assert.assertFalse(DateUtils.isEqualOrFutureDate(date));
    }
}