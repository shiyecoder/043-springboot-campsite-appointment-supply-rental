package com.grad.camp.service;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.*;

/**
 * 经营看板：6 个核心指标 + 7 个图表，全部在数据库侧聚合，前端不再二次汇总。
 * 时间口径由 period 决定（month/quarter/year），默认近 30 天。
 */
@Service
@RequiredArgsConstructor
public class DashboardService {

    private final JdbcTemplate jdbc;

    public Map<String, Object> board(String period) {
        String p = StringUtils.hasText(period) ? period : "month";
        int days = switch (p) {
            case "quarter" -> 90;
            case "year" -> 365;
            case "week" -> 7;
            default -> 30;
        };
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("period", p);
        m.put("days", days);

        // ---- KPI ----
        int totalSites = n(jdbc.queryForObject("SELECT COUNT(*) FROM camp_site WHERE deleted = 0 AND status <> 'OFF'", Long.class));
        int occupied = n(jdbc.queryForObject("SELECT COUNT(*) FROM camp_site WHERE deleted = 0 AND status = 'OCCUPIED'", Long.class));
        m.put("siteTotal", totalSites);
        m.put("siteOccupied", occupied);
        m.put("occupancyRate", totalSites == 0 ? 0 : Math.round(occupied * 1000.0 / totalSites) / 10.0);
        m.put("todayIn", n(jdbc.queryForObject(
                "SELECT COUNT(*) FROM booking WHERE deleted = 0 AND check_in_date = CURDATE() "
                        + "AND status IN ('RESERVED','CONFIRMED','SUBMITTED')", Long.class)));
        m.put("todayOut", n(jdbc.queryForObject(
                "SELECT COUNT(*) FROM booking WHERE deleted = 0 AND check_out_date = CURDATE() "
                        + "AND status IN ('OCCUPIED','CHECKED')", Long.class)));
        m.put("bookingCount", n(jdbc.queryForObject(
                "SELECT COUNT(*) FROM booking WHERE deleted = 0 AND create_time >= DATE_SUB(NOW(), INTERVAL ? DAY)", Long.class, days)));
        m.put("pendingPick", n(jdbc.queryForObject(
                "SELECT COUNT(*) FROM rent_item ri JOIN booking b ON b.id = ri.booking_id "
                        + "WHERE ri.status = 'PENDING' AND b.status IN ('RESERVED','CONFIRMED','OCCUPIED')", Long.class)));
        m.put("pendingRectify", n(jdbc.queryForObject(
                "SELECT COUNT(*) FROM hazard WHERE status IN ('OPEN','RECTIFY')", Long.class)));
        m.put("monthIncome", jdbc.queryForObject(
                "SELECT COALESCE(SUM(amount),0) FROM payment WHERE status = 'PAID' "
                        + "AND pay_time >= DATE_FORMAT(CURDATE(),'%Y-%m-01')", Double.class));
        Double avg = jdbc.queryForObject(
                "SELECT COALESCE(AVG(total_amount),0) FROM checkout_record WHERE status = 'SETTLED' "
                        + "AND check_time >= DATE_SUB(NOW(), INTERVAL ? DAY)", Double.class, days);
        m.put("avgOrder", avg == null ? 0 : Math.round(avg * 100) / 100.0);
        // 物资出租率直接取当日库存快照，避免在聚合里嵌套关联子查询
        Double goodsRate = jdbc.queryForObject(
                "SELECT CASE WHEN SUM(total_qty) = 0 THEN 0 ELSE SUM(occupied_qty) * 100.0 / SUM(total_qty) END "
                        + "FROM goods_stock WHERE stock_date = CURDATE()", Double.class);
        m.put("goodsRentRate", goodsRate == null ? 0 : Math.round(goodsRate * 10) / 10.0);
        m.put("overdueReturn", n(jdbc.queryForObject(
                "SELECT COUNT(*) FROM rent_item ri JOIN booking b ON b.id = ri.booking_id "
                        + "WHERE ri.status IN ('PICKED','DAMAGED') AND b.check_out_date < CURDATE()", Long.class)));

        // ---- 图表 1：营位预订趋势（按天）----
        m.put("trend", jdbc.queryForList(
                "SELECT DATE_FORMAT(create_time,'%m-%d') AS label, COUNT(*) AS value "
                        + "FROM booking WHERE deleted = 0 AND create_time >= DATE_SUB(CURDATE(), INTERVAL ? DAY) "
                        + "GROUP BY DATE_FORMAT(create_time,'%m-%d') ORDER BY MIN(create_time)", days));

        // ---- 图表 2：营位类型利用率 ----
        m.put("typeUsage", jdbc.queryForList(
                "SELECT t.type_name AS label, COUNT(s.id) AS total, "
                        + "SUM(CASE WHEN s.status = 'OCCUPIED' THEN 1 ELSE 0 END) AS occupied, "
                        + "ROUND(SUM(CASE WHEN s.status = 'OCCUPIED' THEN 1 ELSE 0 END) * 100.0 / COUNT(s.id), 1) AS value "
                        + "FROM site_type t LEFT JOIN camp_site s ON s.type_id = t.id AND s.deleted = 0 "
                        + "GROUP BY t.id, t.type_name ORDER BY t.sort_no"));

        // ---- 图表 3：物资租赁排行 TOP8 ----
        m.put("goodsRank", jdbc.queryForList(
                "SELECT g.goods_name AS label, COALESCE(SUM(ri.qty),0) AS value "
                        + "FROM goods g LEFT JOIN rent_item ri ON ri.goods_id = g.id "
                        + "GROUP BY g.id, g.goods_name ORDER BY value DESC LIMIT 8"));

        // ---- 图表 4：收入构成 ----
        m.put("incomeMix", jdbc.queryForList(
                // 押金退还与优惠减免是支出项，不计入收入构成
                "SELECT CASE fee_type WHEN 'SITE' THEN '营位费' WHEN 'GOODS' THEN '物资租金' "
                        + "WHEN 'OVERTIME' THEN '超时费' WHEN 'DAMAGE' THEN '损坏赔偿' ELSE '其他' END AS label, "
                        + "ROUND(SUM(amount),2) AS value FROM fee_item "
                        + "WHERE fee_type IN ('SITE','GOODS','OVERTIME','DAMAGE') "
                        + "GROUP BY label HAVING SUM(amount) > 0 ORDER BY value DESC"));

        // ---- 图表 5：取消原因分布 ----
        m.put("cancelReason", jdbc.queryForList(
                "SELECT CASE WHEN cancel_reason IS NULL OR cancel_reason = '' THEN '未填写' "
                        + "WHEN cancel_reason LIKE '%天气%' THEN '天气原因' "
                        + "WHEN cancel_reason LIKE '%行程%' OR cancel_reason LIKE '%身体%' THEN '行程/健康' "
                        + "WHEN cancel_reason LIKE '%人数%' THEN '人数变化' ELSE '其他原因' END AS label, "
                        + "COUNT(*) AS value FROM booking WHERE status IN ('CANCELLED','REJECTED','NOSHOW') GROUP BY label"));

        // ---- 图表 6：评分分布 ----
        m.put("scoreDist", jdbc.queryForList(
                // only_full_group_by 下 SELECT 里的表达式必须与 GROUP BY 一致，先聚合再套标签
                "SELECT CONCAT(ROUND(score_key), ' 分') AS label, cnt AS value FROM ("
                        + "SELECT ROUND(avg_score) AS score_key, COUNT(*) AS cnt FROM review GROUP BY ROUND(avg_score)"
                        + ") t ORDER BY score_key DESC"));

        // ---- 图表 7：创新结果分布 ----
        m.put("availDist", jdbc.queryForList(
                "SELECT CASE level WHEN 'HIGH' THEN '推荐' WHEN 'MID' THEN '可接受' ELSE '不推荐' END AS label, "
                        + "COUNT(*) AS value FROM avail_score GROUP BY level"));

        // ---- 区域热度 ----
        m.put("areaHeat", jdbc.queryForList(
                "SELECT a.area_name AS label, COUNT(b.id) AS value FROM camp_area a "
                        + "LEFT JOIN camp_site s ON s.area_id = a.id LEFT JOIN booking b ON b.site_id = s.id "
                        + "AND b.deleted = 0 AND b.status NOT IN ('CANCELLED','REJECTED','DRAFT') "
                        + "GROUP BY a.id, a.area_name ORDER BY a.id"));

        // ---- 待办清单 ----
        m.put("todos", jdbc.queryForList(
                "SELECT b.id, b.booking_no, b.customer_name, b.site_no, b.status, b.check_in_date, b.check_out_date, "
                        + "b.guest_count, s.site_name FROM booking b LEFT JOIN camp_site s ON s.id = b.site_id "
                        + "WHERE b.deleted = 0 AND b.status IN ('SUBMITTED','CHECKED') ORDER BY b.update_time DESC LIMIT 6"));

        // ---- 创新模块效果：采纳率、人工调整率 ----
        Long totalScore = jdbc.queryForObject("SELECT COUNT(*) FROM avail_score", Long.class);
        Long manual = jdbc.queryForObject("SELECT COUNT(*) FROM avail_score WHERE manual_level IS NOT NULL", Long.class);
        Long high = jdbc.queryForObject("SELECT COUNT(*) FROM avail_score WHERE level = 'HIGH'", Long.class);
        m.put("scoreTotal", n(totalScore));
        m.put("manualCount", n(manual));
        m.put("adoptRate", n(totalScore) == 0 ? 0 : Math.round(n(high) * 1000.0 / n(totalScore)) / 10.0);
        m.put("manualRate", n(totalScore) == 0 ? 0 : Math.round(n(manual) * 1000.0 / n(totalScore)) / 10.0);
        m.put("today", LocalDate.now().toString());
        return m;
    }

    private static int n(Long v) {
        return v == null ? 0 : v.intValue();
    }
}
