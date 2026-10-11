package com.terranangel.common.date;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Date;

/**
 * 日期时间工具类。
 * <p>
 * 定位：纯 JDK 工具，零 Spring 依赖，任何项目通用、永不变更。
 * <p>
 * 背景：Spring 本身对"前端传什么格式、接口返回什么格式"没有统一的
 * 日期处理约定，每个项目都要自己定一套。这里把最常见的格式和转换
 * 收敛起来，形成统一的约定。
 * <p>
 * 统一格式建议：{@link #PATTERN} = yyyy-MM-dd HH:mm:ss
 */
public final class DateUtils {

    /** 统一的日期时间格式：2026-08-20 10:30:00 */
    public static final String PATTERN = "yyyy-MM-dd HH:mm:ss";

    /** 统一日期格式：2026-08-20 */
    public static final String DATE_PATTERN = "yyyy-MM-dd";

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern(PATTERN);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern(DATE_PATTERN);

    private DateUtils() {
    }

    // ==================== 格式化 ====================

    /** LocalDateTime -> "yyyy-MM-dd HH:mm:ss" */
    public static String format(LocalDateTime dateTime) {
        return dateTime == null ? null : dateTime.format(FORMATTER);
    }

    /** LocalDate -> "yyyy-MM-dd" */
    public static String format(LocalDate date) {
        return date == null ? null : date.format(DATE_FORMATTER);
    }

    /** Date -> "yyyy-MM-dd HH:mm:ss" */
    public static String format(Date date) {
        return date == null ? null : format(toLocalDateTime(date));
    }

    /** 自定义格式 */
    public static String format(LocalDateTime dateTime, String pattern) {
        return dateTime == null ? null : dateTime.format(DateTimeFormatter.ofPattern(pattern));
    }

    // ==================== 解析 ====================

    /** "yyyy-MM-dd HH:mm:ss" -> LocalDateTime，解析失败返回 null */
    public static LocalDateTime parse(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        try {
            return LocalDateTime.parse(text.trim(), FORMATTER);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    /** "yyyy-MM-dd" -> LocalDate，解析失败返回 null */
    public static LocalDate parseDate(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(text.trim(), DATE_FORMATTER);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    // ==================== 转换 ====================

    /** Date -> LocalDateTime */
    public static LocalDateTime toLocalDateTime(Date date) {
        if (date == null) {
            return null;
        }
        return LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault());
    }

    /** LocalDateTime -> Date */
    public static Date toDate(LocalDateTime dateTime) {
        if (dateTime == null) {
            return null;
        }
        return Date.from(dateTime.atZone(ZoneId.systemDefault()).toInstant());
    }

    /** 时间戳（毫秒）-> LocalDateTime */
    public static LocalDateTime fromMillis(long millis) {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(millis), ZoneId.systemDefault());
    }

    // ==================== 便捷方法 ====================

    /** 当前时间，格式化为字符串 */
    public static String now() {
        return format(LocalDateTime.now());
    }
}
