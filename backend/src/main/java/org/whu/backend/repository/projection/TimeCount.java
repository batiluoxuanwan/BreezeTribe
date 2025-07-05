package org.whu.backend.repository.projection;

public interface TimeCount {
    String getPeriod();  // 对应查询中时间分组字段，如"period"
    Long getCount();  // 对应查询中统计计数字段，如"count"
}
