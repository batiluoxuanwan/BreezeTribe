package org.whu.backend.util;

import org.apache.commons.lang3.tuple.Pair;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.time.temporal.WeekFields;
import java.util.*;

/**
 * 工具类：用于构造趋势统计图所需的数据格式
 *
 * 特别适用于：
 *  - 用户增长趋势图
 *  - 旅行团增长趋势图
 *  - 订单增长趋势图
 *  - 任意基于时间维度的折线图/柱状图统计需求
 */
public class ChartDataUtil {

    /**
     * 构造图表数据
     *
     * @param period     时间粒度：支持 "day" | "week" | "month"
     * @param startDate  起始日期（含）
     * @param endDate    结束日期（含）
     * @param rawData    原始查询结果，每一项是 Object[]{时间分组值, 数量}
     * @param label      附加信息的字段名（如 "role"、"type"）
     * @param typeLabel  附加信息字段的值
     * @return 标准格式的折线图/柱状图数据：包含 xAxis、yAxis、总计等字段
     */
    public static Map<String, Object> buildChartData(
            String period,
            LocalDate startDate,
            LocalDate endDate,
            List<Object[]> rawData,
            String label,
            String typeLabel
    ) {
        // 将原始数据库结果转为 Map<时间分组, 数量>
        Map<String, Long> countMap = new HashMap<>();
        for (Object[] entry : rawData) {
            String key = entry[0].toString(); // 防止 java.sql.Date -> String 的类型转换异常
            Long count = (Long) entry[1];
            countMap.put(key, count);
        }

        List<String> xAxis = new ArrayList<>(); // 横坐标：日期、周、月
        List<Long> yAxis = new ArrayList<>();   // 纵坐标：数量值

        switch (period.toLowerCase()) {
            case "day" -> {
                for (LocalDate d = startDate; !d.isAfter(endDate); d = d.plusDays(1)) {
                    String key = d.toString(); // yyyy-MM-dd
                    xAxis.add(key);
                    yAxis.add(countMap.getOrDefault(key, 0L));
                }
            }
            case "week" -> {
                LocalDate startOfWeek = startDate.with(java.time.DayOfWeek.MONDAY);
                LocalDate endOfWeek = endDate.with(java.time.DayOfWeek.MONDAY);
                for (LocalDate d = startOfWeek; !d.isAfter(endOfWeek); d = d.plusWeeks(1)) {
                    String key = d.getYear() + "-W" + String.format("%02d", d.get(java.time.temporal.WeekFields.ISO.weekOfWeekBasedYear()));
                    xAxis.add(key);
                    yAxis.add(countMap.getOrDefault(key, 0L));
                }
            }
            case "month" -> {
                YearMonth startMonth = YearMonth.from(startDate);
                YearMonth endMonth = YearMonth.from(endDate);
                for (YearMonth ym = startMonth; !ym.isAfter(endMonth); ym = ym.plusMonths(1)) {
                    String key = ym.toString(); // yyyy-MM
                    xAxis.add(key);
                    yAxis.add(countMap.getOrDefault(key, 0L));
                }
            }
            default -> throw new IllegalArgumentException("不支持的 period 类型: " + period);
        }

        // 构造结果
        Map<String, Object> res = new HashMap<>();
        res.put("xAxis", xAxis);
        res.put("yAxis", yAxis);
        res.put("total", yAxis.stream().mapToLong(Long::longValue).sum()); // 求和
        res.put("period", period); // 用于前端标记
        res.put(label, typeLabel); // 附加信息（如 role, type 等）
        res.put("startDate", startDate.toString());
        res.put("endDate", endDate.toString());

        return res;
    }

    public static Map<String, Object> buildDualChartDataY2(
            String period,
            LocalDate startDate,
            LocalDate endDate,
            List<Object[]> rawData
    ) {
        // 建立两个 Map<时间点, 值>
        Map<String, Long> travelerMap = new HashMap<>();
        Map<String, BigDecimal> revenueMap = new HashMap<>();

        for (Object[] entry : rawData) {
            String key = entry[0].toString();
            Long travelerCount = (entry[1] != null) ? (Long) entry[1] : 0L;
            BigDecimal revenue = (entry[2] != null) ? (BigDecimal) entry[2] : BigDecimal.ZERO;

            travelerMap.put(key, travelerCount);
            revenueMap.put(key, revenue);
        }

        List<String> xAxis = new ArrayList<>();
        List<Long> travelerYAxis = new ArrayList<>();
        List<BigDecimal> revenueYAxis = new ArrayList<>();

        switch (period.toLowerCase()) {
            case "day" -> {
                for (LocalDate d = startDate; !d.isAfter(endDate); d = d.plusDays(1)) {
                    String key = d.toString();
                    xAxis.add(key);
                    travelerYAxis.add(travelerMap.getOrDefault(key, 0L));
                    revenueYAxis.add(revenueMap.getOrDefault(key, BigDecimal.ZERO));
                }
            }
            case "week" -> {
                LocalDate d = startDate.with(DayOfWeek.MONDAY);
                LocalDate end = endDate.with(DayOfWeek.MONDAY);
                while (!d.isAfter(end)) {
                    String key = d.getYear() + "-W" + String.format("%02d", d.get(WeekFields.ISO.weekOfWeekBasedYear()));
                    xAxis.add(key);
                    travelerYAxis.add(travelerMap.getOrDefault(key, 0L));
                    revenueYAxis.add(revenueMap.getOrDefault(key, BigDecimal.ZERO));
                    d = d.plusWeeks(1);
                }
            }
            case "month" -> {
                YearMonth ym = YearMonth.from(startDate);
                YearMonth endYm = YearMonth.from(endDate);
                while (!ym.isAfter(endYm)) {
                    String key = ym.toString();
                    xAxis.add(key);
                    travelerYAxis.add(travelerMap.getOrDefault(key, 0L));
                    revenueYAxis.add(revenueMap.getOrDefault(key, BigDecimal.ZERO));
                    ym = ym.plusMonths(1);
                }
            }
            default -> throw new IllegalArgumentException("不支持的 period 类型: " + period);
        }

        // 构造结果
        Map<String, Object> res = new HashMap<>();
        res.put("xAxis", xAxis);
        res.put("travelerCount", travelerYAxis);
        res.put("totalTravelerCount", travelerYAxis.stream().mapToLong(Long::longValue).sum());
        res.put("revenue", revenueYAxis);
        res.put("totalRevenue", revenueYAxis.stream().reduce(BigDecimal.ZERO, BigDecimal::add));
        res.put("total", Map.of(
                "travelerCount", res.get("totalTravelerCount"),
                "revenue", res.get("totalRevenue")
        ));
        res.put("period", period);
        res.put("startDate", startDate.toString());
        res.put("endDate", endDate.toString());
        return res;
    }
    public static Map<String, Object> buildMultiSeriesChartData(
            String period,
            LocalDate startDate,
            LocalDate endDate,
            List<Object[]> rawData,
            List<String> metricNames // 如 ["travelerCount", "revenue", "refundAmount"]
    ) {
        // 1. 初始化多个维度的数据映射
        Map<String, Map<String, Number>> dataMap = new HashMap<>();
        for (int i = 0; i < metricNames.size(); i++) {
            dataMap.put(metricNames.get(i), new HashMap<>());
        }

        // 2. 读取原始数据并填入各维度 Map<时间点, value>
        for (Object[] row : rawData) {
            String key = row[0].toString();
            for (int i = 0; i < metricNames.size(); i++) {
                String metric = metricNames.get(i);
                Object val = row[i + 1]; // 注意：第0列是时间字段
                Number value = (val instanceof Number) ? (Number) val : 0;
                dataMap.get(metric).put(key, value);
            }
        }

        // 3. 构造 xAxis
        List<String> xAxis = new ArrayList<>();
        List<YearMonth> allMonths = new ArrayList<>();
        switch (period.toLowerCase()) {
            case "day" -> {
                for (LocalDate d = startDate; !d.isAfter(endDate); d = d.plusDays(1)) {
                    xAxis.add(d.toString());
                }
            }
            case "week" -> {
                for (LocalDate d = startDate.with(DayOfWeek.MONDAY); !d.isAfter(endDate); d = d.plusWeeks(1)) {
                    xAxis.add(d.getYear() + "-W" + String.format("%02d", d.get(WeekFields.ISO.weekOfWeekBasedYear())));
                }
            }
            case "month" -> {
                for (YearMonth ym = YearMonth.from(startDate); !ym.isAfter(YearMonth.from(endDate)); ym = ym.plusMonths(1)) {
                    xAxis.add(ym.toString());
                }
            }
            default -> throw new IllegalArgumentException("不支持的 period 类型: " + period);
        }

        // 4. 构造 Y 轴数据和统计总和
        Map<String, List<Number>> series = new HashMap<>();
        Map<String, Number> totals = new HashMap<>();

        for (String metric : metricNames) {
            List<Number> values = new ArrayList<>();
            Number total = (dataMap.get(metric) instanceof Map) ? 0 : BigDecimal.ZERO;

            for (String x : xAxis) {
                Number value = dataMap.get(metric).getOrDefault(x, 0);
                values.add(value);

                if (value instanceof BigDecimal b) {
                    total = ((BigDecimal) total).add(b);
                } else {
                    total = ((Number) total).longValue() + value.longValue();
                }
            }

            series.put(metric, values);
            totals.put(metric, total);
        }

        // 5. 封装结果
        Map<String, Object> result = new HashMap<>();
        result.put("xAxis", xAxis);
        result.put("series", series);
        result.put("total", totals);
        result.put("period", period);
        result.put("startDate", startDate.toString());
        result.put("endDate", endDate.toString());

        return result;
    }
    /**
     * 根据 period 类型生成默认的时间范围。左闭右开区间）
     */
    public static Pair<LocalDate, LocalDate> resolveRange(String period, LocalDate startDate, LocalDate endDate) {

        LocalDate today = LocalDate.now();

        if (startDate != null && endDate != null) {
            ChartDataUtil.validateRange(period, startDate, endDate);
            return Pair.of(startDate, endDate);
        }

        switch (period.toLowerCase()) {
            case "day" -> {
                endDate = today;
                startDate = endDate.minusDays(30);
            }
            case "week" -> {
                endDate = today;
                startDate = endDate.minusWeeks(12);
            }
            case "month" -> {
                YearMonth ym = YearMonth.from(today);
                endDate = ym.atEndOfMonth();
                startDate = ym.minusMonths(11).atDay(1);
            }
            default -> throw new IllegalArgumentException("不支持的 period 类型: " + period);
        }

        return Pair.of(startDate, endDate);
    }

    /**
     * 将 LocalDate 转为 LocalDateTime 范围（闭区间）
     */
    public static Pair<LocalDateTime, LocalDateTime> toDateTimeRange(LocalDate startDate, LocalDate endDate) {
        return Pair.of(
                startDate.atStartOfDay(),
                endDate.plusDays(1).atStartOfDay() // exclusive ending
        );
    }


    //检验
    public static void validateRange(String period, LocalDate start, LocalDate end) {
        long days = ChronoUnit.DAYS.between(start, end);
        switch (period.toLowerCase()) {
            case "day" -> {
                if (days > 31) throw new IllegalArgumentException("按天最多31天");
            }
            case "week" -> {
                if (days > 90) throw new IllegalArgumentException("按周最多3个月");
            }
            case "month" -> {
                if (ChronoUnit.MONTHS.between(YearMonth.from(start), YearMonth.from(end)) > 11)
                    throw new IllegalArgumentException("按月最多12个月");
            }
            default -> throw new IllegalArgumentException("不支持的 period 类型");
        }
    }

}

