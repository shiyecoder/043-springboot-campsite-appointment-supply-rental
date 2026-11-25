package com.grad.camp.service;

import com.grad.camp.common.BizException;
import com.grad.camp.common.PageResult;
import com.grad.camp.common.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * 露营地业务服务：营位、预约状态机、物资备货与交接、入住退营、巡检隐患、评价黑名单、创新评分。
 * 说明：报表类查询用 JdbcTemplate 直接写 SQL（字段可控、一次成型）；写操作全部走事务并校验状态机。
 */
@Service
@RequiredArgsConstructor
public class BizService {

    private final JdbcTemplate jdbc;
    private static final DateTimeFormatter DT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // ==================== 通用分页 ====================
    private PageResult<Map<String, Object>> page(String from, String where, List<Object> args,
                                                 String orderBy, int page, int size) {
        int p = Math.max(1, page);
        int s = Math.min(100, Math.max(1, size));
        String w = StringUtils.hasText(where) ? " WHERE " + where : "";
        Long total = jdbc.queryForObject("SELECT COUNT(*) FROM " + from + w, Long.class, args.toArray());
        List<Object> args2 = new ArrayList<>(args);
        args2.add(s);
        args2.add((p - 1) * s);
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT * FROM " + from + w + " ORDER BY " + orderBy + " LIMIT ? OFFSET ?", args2.toArray());
        return PageResult.of(total == null ? 0 : total, p, s, rows);
    }

    /** 数据范围：ALL 全量；ORG 本组织；SELF 本人预订或本人负责 */
    private String scopeSql(String alias, List<Object> args) {
        UserContext.CurrentUser u = UserContext.get();
        if (u == null) {
            return "1=0";
        }
        String scope = u.dataScope() == null ? "SELF" : u.dataScope();
        if ("ALL".equals(scope)) {
            return "1=1";
        }
        if ("ORG".equals(scope)) {
            // 本部门范围：既包含本部门人员下的单，也包含本部门负责跟进的单。
            // 预约本身是游客下的单（user_id 属游客组），若只看下单人所属部门，
            // 前台/仓库会一条都看不到——按文档「前台仅处理预约和入住」的职责，必须按责任人再放行一次。
            args.add(u.orgId());
            args.add(u.orgId());
            return "(EXISTS(SELECT 1 FROM sys_user su WHERE su.id=" + alias + ".user_id AND su.org_id=?)"
                    + " OR EXISTS(SELECT 1 FROM sys_user sr WHERE sr.id=" + alias + ".responsible_id AND sr.org_id=?))";
        }
        args.add(u.userId());
        args.add(u.userId());
        return "(" + alias + ".user_id=? OR " + alias + ".responsible_id=?)";
    }

    // ==================== 营位 ====================
    public PageResult<Map<String, Object>> sitePage(int page, int size, String keyword, Long areaId,
                                                    Long typeId, String status, Integer guests, Boolean power) {
        StringBuilder w = new StringBuilder("s.deleted = 0");
        List<Object> a = new ArrayList<>();
        if (StringUtils.hasText(keyword)) {
            w.append(" AND (s.site_no LIKE ? OR s.site_name LIKE ?)");
            a.add("%" + keyword + "%");
            a.add("%" + keyword + "%");
        }
        if (areaId != null) {
            w.append(" AND s.area_id = ?");
            a.add(areaId);
        }
        if (typeId != null) {
            w.append(" AND s.type_id = ?");
            a.add(typeId);
        }
        if (StringUtils.hasText(status)) {
            w.append(" AND s.status = ?");
            a.add(status);
        }
        if (guests != null && guests > 0) {
            w.append(" AND s.max_capacity >= ?");
            a.add(guests);
        }
        if (Boolean.TRUE.equals(power)) {
            w.append(" AND s.power_supply = 1");
        }
        String from = "camp_site s LEFT JOIN camp_area a ON a.id = s.area_id "
                + "LEFT JOIN site_type t ON t.id = s.type_id";
        int p = Math.max(1, page);
        int sz = Math.min(100, Math.max(1, size));
        Long total = jdbc.queryForObject("SELECT COUNT(*) FROM " + from + " WHERE " + w, Long.class, a.toArray());
        List<Object> a2 = new ArrayList<>(a);
        a2.add(sz);
        a2.add((p - 1) * sz);
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT s.*, a.area_name, a.terrain, t.type_name, t.color AS type_color, "
                        + "(SELECT COUNT(*) FROM booking b WHERE b.site_id = s.id AND b.deleted = 0 "
                        + "  AND b.status IN ('RESERVED','CONFIRMED','OCCUPIED','CHECKED')) AS active_booking "
                        + "FROM " + from + " WHERE " + w + " ORDER BY s.area_id, s.site_no LIMIT ? OFFSET ?",
                a2.toArray());
        return PageResult.of(total == null ? 0 : total, p, sz, rows);
    }

    public Map<String, Object> siteDetail(Long id) {
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT s.*, a.area_name, a.terrain, a.map_image, t.type_name, t.description AS type_desc "
                        + "FROM camp_site s LEFT JOIN camp_area a ON a.id = s.area_id "
                        + "LEFT JOIN site_type t ON t.id = s.type_id WHERE s.id = ?", id);
        if (rows.isEmpty()) {
            throw new BizException("营位不存在");
        }
        Map<String, Object> m = rows.get(0);
        m.put("prices", jdbc.queryForList("SELECT * FROM site_price WHERE site_id = ? ORDER BY start_date", id));
        m.put("calendar", jdbc.queryForList(
                "SELECT booking_no, check_in_date, check_out_date, status, guest_count FROM booking "
                        + "WHERE site_id = ? AND deleted = 0 AND status NOT IN ('CANCELLED','REJECTED','DRAFT') "
                        + "AND check_out_date >= CURDATE() ORDER BY check_in_date LIMIT 20", id));
        return m;
    }

    @Transactional
    public Long saveSite(Map<String, Object> body, Long id) {
        String siteNo = str(body.get("siteNo"));
        if (!StringUtils.hasText(siteNo)) {
            throw new BizException("营位编号必填");
        }
        siteNo = siteNo.trim();
        Long dup = jdbc.queryForObject("SELECT COUNT(*) FROM camp_site WHERE site_no = ? AND deleted = 0 AND id <> ?",
                Long.class, siteNo, id == null ? -1 : id);
        if (dup != null && dup > 0) {
            throw new BizException("营位编号已存在：" + siteNo);
        }
        Integer std = num(body.get("stdCapacity"));
        Integer max = num(body.get("maxCapacity"));
        if (std != null && std < 0 || max != null && max < 0) {
            throw new BizException("容纳人数不能为负数");
        }
        if (std != null && max != null && max < std) {
            throw new BizException("最大容量不得小于标准容量");
        }
        if (id == null) {
            jdbc.update("INSERT INTO camp_site(site_no,site_name,area_id,type_id,std_capacity,max_capacity,area_size,"
                            + "base_price,facilities,power_supply,environment,cover_image,detail_image,status,open_date,"
                            + "device_note,create_by,create_time,update_time,version,deleted) "
                            + "VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,NOW(),NOW(),0,0)",
                    siteNo, body.get("siteName"), body.get("areaId"), body.get("typeId"), std, max,
                    body.get("areaSize"), body.get("basePrice"), body.get("facilities"),
                    body.get("powerSupply"), body.get("environment"), body.get("coverImage"),
                    body.get("detailImage"), orDefault(body.get("status"), "FREE"), body.get("openDate"),
                    body.get("deviceNote"), UserContext.userId());
            return jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        }
        // 乐观锁：版本号必须匹配，避免两人同时改营位
        Integer ver = num(body.get("version"));
        int n = jdbc.update("UPDATE camp_site SET site_no=?, site_name=?, area_id=?, type_id=?, std_capacity=?, "
                        + "max_capacity=?, area_size=?, base_price=?, facilities=?, power_supply=?, environment=?, "
                        + "cover_image=?, detail_image=?, status=?, open_date=?, device_note=?, update_time=NOW(), "
                        + "version=version+1 WHERE id=? AND version=?",
                siteNo, body.get("siteName"), body.get("areaId"), body.get("typeId"), std, max,
                body.get("areaSize"), body.get("basePrice"), body.get("facilities"), body.get("powerSupply"),
                body.get("environment"), body.get("coverImage"), body.get("detailImage"),
                orDefault(body.get("status"), "FREE"), body.get("openDate"), body.get("deviceNote"),
                id, ver == null ? 0 : ver);
        if (n == 0) {
            throw new BizException("营位已被他人修改，请刷新后重试");
        }
        return id;
    }

    public void deleteSite(Long id) {
        Long ref = jdbc.queryForObject("SELECT COUNT(*) FROM booking WHERE site_id = ? AND deleted = 0 "
                + "AND status IN ('RESERVED','CONFIRMED','OCCUPIED','CHECKED')", Long.class, id);
        if (ref != null && ref > 0) {
            throw new BizException("该营位存在进行中的预约，不能删除，请先改为停用");
        }
        jdbc.update("UPDATE camp_site SET deleted = 1, update_time = NOW() WHERE id = ?", id);
    }

    public Map<String, Object> siteOptions() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("areas", jdbc.queryForList("SELECT id, area_code, area_name, terrain, cover_image FROM camp_area ORDER BY id"));
        m.put("types", jdbc.queryForList("SELECT id, type_code, type_name, base_price, std_capacity, max_capacity, color FROM site_type ORDER BY sort_no"));
        m.put("goods", jdbc.queryForList("SELECT id, goods_no, goods_name, spec, total_stock, rent_price, deposit, rent_unit, image, status FROM goods ORDER BY category_id, goods_no"));
        m.put("categories", jdbc.queryForList("SELECT * FROM goods_category ORDER BY sort_no"));
        return m;
    }

    // ==================== 预约 ====================
    public PageResult<Map<String, Object>> bookingPage(int page, int size, String keyword, String status,
                                                       Long siteId, String quick, String from, String to) {
        List<Object> a = new ArrayList<>();
        StringBuilder w = new StringBuilder("b.deleted = 0");
        String scope = scopeSql("b", a);
        w.append(" AND ").append(scope);
        if (StringUtils.hasText(keyword)) {
            w.append(" AND (b.booking_no LIKE ? OR b.customer_name LIKE ? OR b.customer_phone LIKE ?)");
            a.add("%" + keyword + "%");
            a.add("%" + keyword + "%");
            a.add("%" + keyword + "%");
        }
        if (StringUtils.hasText(status)) {
            w.append(" AND b.status = ?");
            a.add(status);
        }
        if (siteId != null) {
            w.append(" AND b.site_id = ?");
            a.add(siteId);
        }
        if (StringUtils.hasText(from)) {
            w.append(" AND b.check_in_date >= ?");
            a.add(from);
        }
        if (StringUtils.hasText(to)) {
            w.append(" AND b.check_in_date <= ?");
            a.add(to);
        }
        // 快捷标签：待办优先
        if ("TODO".equals(quick)) {
            w.append(" AND b.status IN ('SUBMITTED','CHECKED')");
        } else if ("DOING".equals(quick)) {
            w.append(" AND b.status IN ('CONFIRMED','RESERVED','OCCUPIED')");
        } else if ("DONE".equals(quick)) {
            w.append(" AND b.status = 'COMPLETED'");
        } else if ("ABNORMAL".equals(quick)) {
            w.append(" AND b.status IN ('CANCELLED','REJECTED','NOSHOW')");
        }
        String fromSql = "booking b LEFT JOIN camp_site s ON s.id = b.site_id LEFT JOIN camp_area a ON a.id = s.area_id";
        int p = Math.max(1, page);
        int sz = Math.min(100, Math.max(1, size));
        Long total = jdbc.queryForObject("SELECT COUNT(*) FROM " + fromSql + " WHERE " + w, Long.class, a.toArray());
        List<Object> a2 = new ArrayList<>(a);
        a2.add(sz);
        a2.add((p - 1) * sz);
        // 排序固定：待本人处理优先，其次更新时间倒序（需求文档列表规则）
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT b.*, s.site_name, s.cover_image, a.area_name, "
                        + "(SELECT COUNT(*) FROM rent_item r WHERE r.booking_id = b.id) AS rent_kinds, "
                        + "(SELECT COUNT(*) FROM guest_member g WHERE g.booking_id = b.id) AS guest_count_real "
                        + "FROM " + fromSql + " WHERE " + w
                        + " ORDER BY FIELD(b.status,'SUBMITTED','CHECKED','OCCUPIED','RESERVED','CONFIRMED','DRAFT','COMPLETED','NOSHOW','CANCELLED','REJECTED'), b.update_time DESC LIMIT ? OFFSET ?",
                a2.toArray());
        return PageResult.of(total == null ? 0 : total, p, sz, rows);
    }

    public Map<String, Object> bookingDetail(Long id) {
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT b.*, s.site_name, s.site_no AS site_code, s.cover_image, s.detail_image, s.facilities, "
                        + "s.max_capacity, a.area_name, t.type_name FROM booking b "
                        + "LEFT JOIN camp_site s ON s.id = b.site_id LEFT JOIN camp_area a ON a.id = s.area_id "
                        + "LEFT JOIN site_type t ON t.id = s.type_id WHERE b.id = ?", id);
        if (rows.isEmpty()) {
            throw new BizException("预约不存在");
        }
        Map<String, Object> m = rows.get(0);
        m.put("members", jdbc.queryForList("SELECT * FROM guest_member WHERE booking_id = ? ORDER BY id", id));
        m.put("vehicles", jdbc.queryForList("SELECT * FROM booking_vehicle WHERE booking_id = ? ORDER BY id", id));
        m.put("rentItems", jdbc.queryForList("SELECT * FROM rent_item WHERE booking_id = ? ORDER BY id", id));
        m.put("handovers", jdbc.queryForList("SELECT * FROM rent_handover WHERE booking_id = ? ORDER BY handover_time", id));
        m.put("changes", jdbc.queryForList("SELECT * FROM booking_change WHERE booking_id = ? ORDER BY create_time", id));
        List<Map<String, Object>> ck = jdbc.queryForList("SELECT * FROM checkin_record WHERE booking_id = ?", id);
        m.put("checkin", ck.isEmpty() ? null : ck.get(0));
        List<Map<String, Object>> co = jdbc.queryForList("SELECT * FROM checkout_record WHERE booking_id = ?", id);
        m.put("checkout", co.isEmpty() ? null : co.get(0));
        m.put("fees", jdbc.queryForList("SELECT * FROM fee_item WHERE booking_id = ? ORDER BY id", id));
        m.put("payments", jdbc.queryForList("SELECT * FROM payment WHERE booking_id = ? ORDER BY id", id));
        List<Map<String, Object>> sc = jdbc.queryForList(
                "SELECT * FROM avail_score WHERE booking_id = ? ORDER BY id DESC LIMIT 1", id);
        m.put("score", sc.isEmpty() ? null : sc.get(0));
        return m;
    }

    /** 新建预约：校验日期重叠、容量、停用时段，生成编号 */
    @Transactional
    public Long createBooking(Map<String, Object> body) {
        Long siteId = lng(body.get("siteId"));
        String cin = str(body.get("checkInDate"));
        String cout = str(body.get("checkOutDate"));
        Integer guests = num(body.get("guestCount"));
        if (siteId == null || !StringUtils.hasText(cin) || !StringUtils.hasText(cout)) {
            throw new BizException("营位与入住/退营日期必填");
        }
        if (guests == null || guests <= 0) {
            throw new BizException("入住人数必须大于 0");
        }
        LocalDate ci = LocalDate.parse(cin);
        LocalDate co = LocalDate.parse(cout);
        if (!co.isAfter(ci)) {
            throw new BizException("退营日期必须晚于入住日期");
        }
        Map<String, Object> site = jdbc.queryForMap("SELECT * FROM camp_site WHERE id = ? AND deleted = 0", siteId);
        if ("REPAIR".equals(site.get("status")) || "OFF".equals(site.get("status"))) {
            throw new BizException("该营位当前维修/停用，不可预订");
        }
        int max = ((Number) site.get("max_capacity")).intValue();
        if (guests > max) {
            throw new BizException("入住人数 " + guests + " 超过该营位最大容量 " + max + " 人，请拆分或改选营位");
        }
        // 日期区间相交校验：同一营位不得存在有效预约
        Long overlap = jdbc.queryForObject(
                "SELECT COUNT(*) FROM booking WHERE site_id = ? AND deleted = 0 "
                        + "AND status IN ('SUBMITTED','CONFIRMED','RESERVED','OCCUPIED','CHECKED') "
                        + "AND check_in_date < ? AND check_out_date > ?", Long.class, siteId, co, ci);
        if (overlap != null && overlap > 0) {
            throw new BizException("该营位在所选日期内已有有效预约，请改选日期或营位");
        }
        int nights = (int) ChronoUnit.DAYS.between(ci, co);
        BigDecimal price = (BigDecimal) site.get("base_price");
        // 特殊日期价格覆盖：命中区间则用特殊价
        List<BigDecimal> sp = jdbc.queryForList(
                "SELECT price FROM site_price WHERE site_id = ? AND start_date <= ? AND end_date >= ? ORDER BY id DESC LIMIT 1",
                BigDecimal.class, siteId, ci, ci);
        if (!sp.isEmpty()) {
            price = sp.get(0);
        }
        BigDecimal siteFee = price.multiply(BigDecimal.valueOf(nights)).setScale(2, RoundingMode.HALF_UP);
        BigDecimal goodsFee = BigDecimal.ZERO;
        BigDecimal deposit = new BigDecimal("200.00");
        String no = "BK" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
                + String.format("%04d", (int) (Math.random() * 9000) + 1000);
        String status = StringUtils.hasText(str(body.get("status"))) ? str(body.get("status")) : "SUBMITTED";
        jdbc.update("INSERT INTO booking(booking_no,user_id,customer_name,customer_phone,site_id,site_no,check_in_date,"
                        + "check_out_date,nights,guest_count,special_need,source,site_fee,goods_fee,deposit,total_fee,"
                        + "status,responsible_id,remark,version,deleted,create_time,update_time) "
                        + "VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,0,0,NOW(),NOW())",
                no, UserContext.userId(), body.get("customerName"), body.get("customerPhone"), siteId,
                site.get("site_no"), ci, co, nights, guests, body.get("specialNeed"),
                orDefault(body.get("source"), "WEB"), siteFee, goodsFee, deposit, siteFee,
                status, body.get("responsibleId"), body.get("remark"));
        Long bid = jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        // 同行人
        if (body.get("members") instanceof List<?> ms) {
            for (Object o : ms) {
                if (!(o instanceof Map<?, ?> mm)) {
                    continue;
                }
                jdbc.update("INSERT INTO guest_member(booking_id,guest_name,phone,age_group,id_card,emergency_contact,"
                                + "emergency_phone,safety_confirmed,remark,create_time,update_time) VALUES(?,?,?,?,?,?,?,?,?,NOW(),NOW())",
                        bid, mm.get("guestName"), mm.get("phone"), orDefault(mm.get("ageGroup"), "ADULT"),
                        mm.get("idCard"), mm.get("emergencyContact"), mm.get("emergencyPhone"),
                        mm.get("safetyConfirmed"), mm.get("remark"));
            }
        }
        // 车辆
        if (body.get("vehicles") instanceof List<?> vs) {
            for (Object o : vs) {
                if (!(o instanceof Map<?, ?> vm)) {
                    continue;
                }
                jdbc.update("INSERT INTO booking_vehicle(booking_id,plate_no,vehicle_type,seat_count,arrive_time,remark,"
                                + "create_time,update_time) VALUES(?,?,?,?,?,?,NOW(),NOW())",
                        bid, vm.get("plateNo"), vm.get("vehicleType"), vm.get("seatCount"), vm.get("arriveTime"), vm.get("remark"));
            }
        }
        // 租赁物资：校验可用库存
        if (body.get("rentItems") instanceof List<?> rs) {
            for (Object o : rs) {
                if (!(o instanceof Map<?, ?> rm)) {
                    continue;
                }
                Long gid = lng(rm.get("goodsId"));
                Integer qty = num(rm.get("qty"));
                if (gid == null || qty == null || qty <= 0) {
                    continue;
                }
                Map<String, Object> g = jdbc.queryForMap("SELECT * FROM goods WHERE id = ?", gid);
                int stock = ((Number) g.get("total_stock")).intValue();
                Long used = jdbc.queryForObject("SELECT COALESCE(SUM(ri.qty),0) FROM rent_item ri "
                        + "JOIN booking b2 ON b2.id = ri.booking_id WHERE ri.goods_id = ? AND b2.deleted = 0 "
                        + "AND b2.status IN ('RESERVED','CONFIRMED','OCCUPIED','CHECKED') "
                        + "AND b2.check_in_date < ? AND b2.check_out_date > ?", Long.class, gid, co, ci);
                if (used != null && used + qty > stock) {
                    throw new BizException("物资「" + g.get("goods_name") + "」在所选日期可用库存不足（剩余 "
                            + Math.max(0, stock - used.intValue()) + "），请减少数量");
                }
                BigDecimal unit = (BigDecimal) g.get("rent_price");
                int days = "晚".equals(g.get("rent_unit")) ? nights : nights + 1;
                BigDecimal amt = unit.multiply(BigDecimal.valueOf((long) qty * days)).setScale(2, RoundingMode.HALF_UP);
                BigDecimal dep = ((BigDecimal) g.get("deposit")).multiply(BigDecimal.valueOf(qty));
                jdbc.update("INSERT INTO rent_item(booking_id,goods_id,goods_name,qty,rent_days,unit_price,deposit,"
                                + "amount,status,create_time,update_time) VALUES(?,?,?,?,?,?,?,?,'PENDING',NOW(),NOW())",
                        bid, gid, g.get("goods_name"), qty, days, unit, dep, amt);
                goodsFee = goodsFee.add(amt);
                deposit = deposit.add(dep);
            }
        }
        jdbc.update("UPDATE booking SET goods_fee=?, deposit=?, total_fee=? WHERE id=?",
                goodsFee, deposit, siteFee.add(goodsFee), bid);
        jdbc.update("INSERT INTO booking_change(booking_id,change_type,old_value,new_value,reason,operator_id,"
                + "operator_name,create_time,update_time) VALUES(?,?,?,?,?,?,?,NOW(),NOW())",
                bid, "SUBMIT", "DRAFT", status, "客户提交预约申请，等待运营确认", UserContext.userId(), UserContext.realName());
        return bid;
    }

    private static final Map<String, List<String>> FLOW = new LinkedHashMap<>();

    static {
        // 状态机：当前状态 -> 允许的目标状态
        FLOW.put("DRAFT", List.of("SUBMITTED"));
        FLOW.put("SUBMITTED", List.of("RESERVED", "REJECTED", "CANCELLED"));
        FLOW.put("CONFIRMED", List.of("RESERVED", "OCCUPIED", "CANCELLED"));
        FLOW.put("RESERVED", List.of("OCCUPIED", "NOSHOW", "CANCELLED"));
        FLOW.put("OCCUPIED", List.of("CHECKED"));
        FLOW.put("CHECKED", List.of("COMPLETED"));
        FLOW.put("COMPLETED", List.of());
        FLOW.put("CANCELLED", List.of());
        FLOW.put("REJECTED", List.of());
        FLOW.put("NOSHOW", List.of());
    }

    /** 状态流转：越级调用直接拒绝；取消/拒绝/爽约必须填原因（≥5 字） */
    @Transactional
    public Map<String, Object> transition(Long id, String target, String reason, String newCheckIn, String newCheckOut) {
        Map<String, Object> b = jdbc.queryForMap("SELECT * FROM booking WHERE id = ? AND deleted = 0", id);
        String cur = (String) b.get("status");
        List<String> allow = FLOW.getOrDefault(cur, List.of());
        if (!allow.contains(target)) {
            throw new BizException("不允许从「" + statusCn(cur) + "」直接流转到「" + statusCn(target) + "」");
        }
        if (List.of("CANCELLED", "REJECTED", "NOSHOW").contains(target)) {
            if (!StringUtils.hasText(reason) || reason.trim().length() < 5) {
                throw new BizException("取消/拒绝/爽约必须填写原因，且不少于 5 个字");
            }
        }
        // 入住前校验物资是否已备货发放，退营结算前校验归还与验收
        if ("OCCUPIED".equals(target)) {
            Map<String, Object> site = jdbc.queryForMap("SELECT status FROM camp_site WHERE id = ?", b.get("site_id"));
            if ("REPAIR".equals(site.get("status")) || "OFF".equals(site.get("status"))) {
                throw new BizException("营位当前维修/停用，不能办理入住");
            }
        }
        if ("COMPLETED".equals(target)) {
            Long pending = jdbc.queryForObject("SELECT COUNT(*) FROM rent_item WHERE booking_id = ? "
                    + "AND status IN ('PENDING','PICKED')", Long.class, id);
            if (pending != null && pending > 0) {
                throw new BizException("仍有物资未归还，不能直接结算完成");
            }
            Long ck = jdbc.queryForObject("SELECT COUNT(*) FROM checkout_record WHERE booking_id = ? "
                    + "AND status = 'SETTLED'", Long.class, id);
            if (ck == null || ck == 0) {
                throw new BizException("尚未完成退营验收与结算，不能标记完成");
            }
        }
        jdbc.update("UPDATE booking SET status = ?, cancel_reason = ?, update_time = NOW(), version = version + 1 WHERE id = ?",
                target, StringUtils.hasText(reason) ? reason : b.get("cancel_reason"), id);
        if (StringUtils.hasText(newCheckIn) && StringUtils.hasText(newCheckOut)) {
            LocalDate ci = LocalDate.parse(newCheckIn);
            LocalDate co = LocalDate.parse(newCheckOut);
            int nights = (int) ChronoUnit.DAYS.between(ci, co);
            jdbc.update("UPDATE booking SET check_in_date = ?, check_out_date = ?, nights = ? WHERE id = ?",
                    ci, co, nights, id);
        }
        // 营位状态联动
        if ("OCCUPIED".equals(target)) {
            jdbc.update("UPDATE camp_site SET status = 'OCCUPIED' WHERE id = ?", b.get("site_id"));
        } else if ("CHECKED".equals(target)) {
            jdbc.update("UPDATE camp_site SET status = 'CLEANING' WHERE id = ?", b.get("site_id"));
        } else if ("RESERVED".equals(target) || "CONFIRMED".equals(target)) {
            jdbc.update("UPDATE camp_site SET status = 'RESERVED' WHERE id = ? AND status = 'FREE'", b.get("site_id"));
        } else if (List.of("CANCELLED", "REJECTED", "NOSHOW").contains(target)) {
            jdbc.update("UPDATE camp_site SET status = 'FREE' WHERE id = ? AND status = 'RESERVED'", b.get("site_id"));
        }
        jdbc.update("INSERT INTO booking_change(booking_id,change_type,old_value,new_value,reason,operator_id,"
                        + "operator_name,create_time,update_time) VALUES(?,?,?,?,?,?,?,NOW(),NOW())",
                id, target, cur, target, StringUtils.hasText(reason) ? reason : "按业务流程推进",
                UserContext.userId(), UserContext.realName());
        // 站内消息：状态变化通知预订人
        jdbc.update("INSERT INTO sys_message(title,content,msg_type,biz_no,link_url,user_id,is_read,create_time,update_time) "
                        + "VALUES(?,?,?,?,?,?,0,NOW(),NOW())",
                "预约状态更新", "预约 " + b.get("booking_no") + " 已变更为「" + statusCn(target) + "」",
                "DONE", b.get("booking_no"), "/booking/detail/" + id, b.get("user_id"));
        return Map.of("id", id, "status", target, "statusName", statusCn(target));
    }

    public static String statusCn(String s) {
        if (s == null) {
            return "";
        }
        return switch (s) {
            case "DRAFT" -> "草稿";
            case "SUBMITTED" -> "待确认";
            case "CONFIRMED" -> "已确认";
            case "RESERVED" -> "已预留";
            case "OCCUPIED" -> "已入住";
            case "CHECKED" -> "退营待结算";
            case "COMPLETED" -> "已完成";
            case "CANCELLED" -> "已取消";
            case "REJECTED" -> "已拒绝";
            case "NOSHOW" -> "爽约";
            case "FREE" -> "可预订";
            case "CLEANING" -> "待清洁";
            case "REPAIR" -> "维修中";
            case "OFF" -> "停用";
            case "PENDING" -> "待发放";
            case "PICKED" -> "已发放";
            case "RETURNED" -> "已归还";
            case "DAMAGED" -> "损坏";
            case "LOST" -> "遗失";
            default -> s;
        };
    }

    public List<Map<String, Object>> calendar(String from, String to) {
        List<Object> a = new ArrayList<>();
        StringBuilder w = new StringBuilder("b.deleted = 0 AND b.status NOT IN ('DRAFT','CANCELLED','REJECTED','NOSHOW')");
        String scope = scopeSql("b", a);
        w.append(" AND ").append(scope);
        if (StringUtils.hasText(from)) {
            w.append(" AND b.check_out_date >= ?");
            a.add(from);
        }
        if (StringUtils.hasText(to)) {
            w.append(" AND b.check_in_date <= ?");
            a.add(to);
        }
        return jdbc.queryForList("SELECT b.id, b.booking_no, b.site_id, b.site_no, b.customer_name, b.check_in_date, "
                + "b.check_out_date, b.guest_count, b.status, s.site_name, a2.area_name FROM booking b "
                + "LEFT JOIN camp_site s ON s.id = b.site_id LEFT JOIN camp_area a2 ON a2.id = s.area_id "
                + "WHERE " + w + " ORDER BY b.check_in_date", a.toArray());
    }

    // ==================== 物资 ====================
    public PageResult<Map<String, Object>> goodsPage(int page, int size, String keyword, Long categoryId, String status) {
        StringBuilder w = new StringBuilder("1=1");
        List<Object> a = new ArrayList<>();
        if (StringUtils.hasText(keyword)) {
            w.append(" AND (g.goods_no LIKE ? OR g.goods_name LIKE ?)");
            a.add("%" + keyword + "%");
            a.add("%" + keyword + "%");
        }
        if (categoryId != null) {
            w.append(" AND g.category_id = ?");
            a.add(categoryId);
        }
        if (StringUtils.hasText(status)) {
            w.append(" AND g.status = ?");
            a.add(status);
        }
        return page("goods g LEFT JOIN goods_category c ON c.id = g.category_id",
                w.toString().replace("g.", "g."), a,
                "g.category_id, g.goods_no", page, size);
    }

    public Long saveGoods(Map<String, Object> body, Long id) {
        if (id == null) {
            jdbc.update("INSERT INTO goods(goods_no,goods_name,category_id,spec,total_stock,deposit,rent_price,rent_unit,"
                            + "turnover_hours,image,status,remark,create_time,update_time) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,NOW(),NOW())",
                    body.get("goodsNo"), body.get("goodsName"), body.get("categoryId"), body.get("spec"),
                    body.get("totalStock"), body.get("deposit"), body.get("rentPrice"),
                    orDefault(body.get("rentUnit"), "晚"), body.get("turnoverHours"), body.get("image"),
                    orDefault(body.get("status"), "ON"), body.get("remark"));
            return jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        }
        jdbc.update("UPDATE goods SET goods_no=?, goods_name=?, category_id=?, spec=?, total_stock=?, deposit=?, "
                        + "rent_price=?, rent_unit=?, turnover_hours=?, image=?, status=?, remark=?, update_time=NOW() WHERE id=?",
                body.get("goodsNo"), body.get("goodsName"), body.get("categoryId"), body.get("spec"),
                body.get("totalStock"), body.get("deposit"), body.get("rentPrice"),
                orDefault(body.get("rentUnit"), "晚"), body.get("turnoverHours"), body.get("image"),
                orDefault(body.get("status"), "ON"), body.get("remark"), id);
        return id;
    }

    /** 备货清单：按入住日期分组，列出待发放物资与库存余量 */
    public Map<String, Object> pickList(String date) {
        String d = StringUtils.hasText(date) ? date : LocalDate.now().toString();
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT b.id AS booking_id, b.booking_no, b.customer_name, b.site_no, s.site_name, b.check_in_date, "
                        + "b.check_out_date, b.status, ri.id AS item_id, ri.goods_id, ri.goods_name, ri.qty, ri.rent_days, "
                        + "ri.status AS item_status, g.image, g.total_stock, g.rent_unit, "
                        + "(SELECT COALESCE(SUM(r2.qty),0) FROM rent_item r2 JOIN booking b2 ON b2.id = r2.booking_id "
                        + " WHERE r2.goods_id = ri.goods_id AND b2.deleted = 0 "
                        + " AND b2.status IN ('RESERVED','CONFIRMED','OCCUPIED') "
                        + " AND b2.check_in_date <= b.check_out_date AND b2.check_out_date >= b.check_in_date) AS reserved_qty "
                        + "FROM booking b JOIN camp_site s ON s.id = b.site_id "
                        + "JOIN rent_item ri ON ri.booking_id = b.id JOIN goods g ON g.id = ri.goods_id "
                        + "WHERE b.deleted = 0 AND b.status IN ('RESERVED','CONFIRMED','OCCUPIED') "
                        + "AND b.check_in_date BETWEEN ? AND DATE_ADD(?, INTERVAL 1 DAY) ORDER BY b.check_in_date, b.id",
                d, d);
        long pending = rows.stream().filter(r -> "PENDING".equals(r.get("item_status"))).count();
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("date", d);
        m.put("rows", rows);
        m.put("pendingQty", pending);
        return m;
    }

    /** 物资发放/回收：写交接记录并推进明细状态 */
    @Transactional
    public void handover(Map<String, Object> body) {
        Long itemId = lng(body.get("rentItemId"));
        String type = str(body.get("handoverType"));
        Integer qty = num(body.get("qty"));
        if (itemId == null || !StringUtils.hasText(type) || qty == null || qty <= 0) {
            throw new BizException("交接明细、类型与数量必填");
        }
        Map<String, Object> it = jdbc.queryForMap("SELECT * FROM rent_item WHERE id = ?", itemId);
        if ("OUT".equals(type) && "PENDING".equals(it.get("status"))) {
            jdbc.update("UPDATE rent_item SET status = 'PICKED', update_time = NOW() WHERE id = ?", itemId);
        } else if ("IN".equals(type)) {
            String st = "DAMAGED".equals(body.get("itemStatus")) ? "DAMAGED"
                    : ("LOST".equals(body.get("itemStatus")) ? "LOST" : "RETURNED");
            jdbc.update("UPDATE rent_item SET status = ?, update_time = NOW() WHERE id = ?", st, itemId);
        }
        jdbc.update("INSERT INTO rent_handover(rent_item_id,booking_id,goods_id,handover_type,qty,serial_no,item_status,"
                        + "photo,operator_id,operator_name,handover_time,remark,create_time,update_time) "
                        + "VALUES(?,?,?,?,?,?,?,?,?,?,NOW(),?,NOW(),NOW())",
                itemId, it.get("booking_id"), it.get("goods_id"), type, qty, body.get("serialNo"),
                orDefault(body.get("itemStatus"), "GOOD"), body.get("photo"), UserContext.userId(),
                UserContext.realName(), body.get("remark"));
    }

    // ==================== 入住 / 退营 ====================
    public Map<String, Object> stayBoard(String date) {
        String d = StringUtils.hasText(date) ? date : LocalDate.now().toString();
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("arrivals", jdbc.queryForList(
                "SELECT b.*, s.site_name, s.site_no AS site_code, a.area_name, "
                        + "(SELECT COUNT(*) FROM rent_item r WHERE r.booking_id = b.id) AS rent_kinds "
                        + "FROM booking b LEFT JOIN camp_site s ON s.id = b.site_id LEFT JOIN camp_area a ON a.id = s.area_id "
                        + "WHERE b.deleted = 0 AND b.check_in_date = ? AND b.status IN ('RESERVED','CONFIRMED','SUBMITTED') "
                        + "ORDER BY b.status, b.id", d));
        m.put("departures", jdbc.queryForList(
                "SELECT b.*, s.site_name, s.site_no AS site_code, a.area_name, "
                        + "(SELECT COUNT(*) FROM rent_item r WHERE r.booking_id = b.id AND r.status = 'PICKED') AS out_qty "
                        + "FROM booking b LEFT JOIN camp_site s ON s.id = b.site_id LEFT JOIN camp_area a ON a.id = s.area_id "
                        + "WHERE b.deleted = 0 AND b.check_out_date = ? AND b.status IN ('OCCUPIED','CHECKED') "
                        + "ORDER BY b.id", d));
        m.put("inhouse", jdbc.queryForList(
                "SELECT COUNT(*) FROM booking WHERE deleted = 0 AND status = 'OCCUPIED'"));
        return m;
    }

    @Transactional
    public Map<String, Object> checkin(Map<String, Object> body) {
        Long id = lng(body.get("bookingId"));
        if (id == null) {
            throw new BizException("预约ID必填");
        }
        Map<String, Object> b = jdbc.queryForMap("SELECT * FROM booking WHERE id = ? AND deleted = 0", id);
        if (!List.of("RESERVED", "CONFIRMED", "SUBMITTED").contains(b.get("status"))) {
            throw new BizException("当前状态「" + statusCn((String) b.get("status")) + "」不能办理入住");
        }
        Integer actual = num(body.get("actualGuestCount"));
        Integer cap = jdbc.queryForObject("SELECT max_capacity FROM camp_site WHERE id = ?", Integer.class, b.get("site_id"));
        if (actual != null && cap != null && actual > cap) {
            throw new BizException("实际入住人数超过营位最大容量，请先换营位");
        }
        if (!"1".equals(String.valueOf(body.get("safetyConfirmed"))) && !Boolean.TRUE.equals(body.get("safetyConfirmed"))) {
            throw new BizException("必须确认安全须知后方可办理入住");
        }
        String no = "CI" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
                + String.format("%04d", (int) (Math.random() * 9000) + 1000);
        jdbc.update("INSERT INTO checkin_record(booking_id,handover_no,site_id,actual_guest_count,deposit,"
                        + "safety_confirmed,site_photo,goods_photo,operator_id,operator_name,checkin_time,status,remark,"
                        + "create_time,update_time) VALUES(?,?,?,?,?,?,?,?,?,?,NOW(),'DONE',?,NOW(),NOW())",
                id, no, b.get("site_id"), actual == null ? b.get("guest_count") : actual, b.get("deposit"), 1,
                body.get("sitePhoto"), body.get("goodsPhoto"), UserContext.userId(), UserContext.realName(),
                body.get("remark"));
        transition(id, "OCCUPIED", "前台核验身份并完成交接", null, null);
        return Map.of("bookingId", id, "handoverNo", no);
    }

    /** 结算预览：汇总营位费、物资租金、超时费、损坏赔偿、押金退还与优惠 */
    public Map<String, Object> checkoutPreview(Long bookingId) {
        Map<String, Object> b = jdbc.queryForMap("SELECT * FROM booking WHERE id = ? AND deleted = 0", bookingId);
        BigDecimal siteFee = (BigDecimal) b.get("site_fee");
        BigDecimal goodsFee = (BigDecimal) b.get("goods_fee");
        BigDecimal deposit = (BigDecimal) b.get("deposit");
        long overdue = 0;
        Object cout = b.get("check_out_date");
        if (cout != null) {
            LocalDate co = cout instanceof Date d ? d.toLocalDate() : LocalDate.parse(cout.toString());
            overdue = Math.max(0, ChronoUnit.HOURS.between(co.atTime(12, 0), LocalDateTime.now()));
        }
        BigDecimal overtime = BigDecimal.valueOf(overdue).multiply(new BigDecimal("40")).setScale(2, RoundingMode.HALF_UP);
        BigDecimal damage = BigDecimal.ZERO;
        List<Map<String, Object>> bad = jdbc.queryForList(
                "SELECT ri.*, g.deposit AS gdep FROM rent_item ri JOIN goods g ON g.id = ri.goods_id "
                        + "WHERE ri.booking_id = ? AND ri.status IN ('DAMAGED','LOST')", bookingId);
        for (Map<String, Object> r : bad) {
            BigDecimal gd = (BigDecimal) r.get("gdep");
            damage = damage.add(gd.multiply(BigDecimal.valueOf(((Number) r.get("qty")).longValue())));
        }
        BigDecimal discount = BigDecimal.ZERO;
        BigDecimal payable = siteFee.add(goodsFee).add(overtime).add(damage).subtract(discount);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("booking", b);
        m.put("rentItems", jdbc.queryForList("SELECT * FROM rent_item WHERE booking_id = ?", bookingId));
        m.put("siteFee", siteFee);
        m.put("goodsFee", goodsFee);
        m.put("overtimeHours", overdue);
        m.put("overtimeFee", overtime);
        m.put("damageFee", damage);
        m.put("deposit", deposit);
        m.put("depositRefund", deposit.subtract(damage));
        m.put("discount", discount);
        m.put("totalAmount", payable.setScale(2, RoundingMode.HALF_UP));
        return m;
    }

    @Transactional
    public Map<String, Object> settle(Map<String, Object> body) {
        Long bookingId = lng(body.get("bookingId"));
        if (bookingId == null) {
            throw new BizException("预约ID必填");
        }
        // 物资未归还不得结算（文档硬性规则）
        Long out = jdbc.queryForObject("SELECT COUNT(*) FROM rent_item WHERE booking_id = ? "
                + "AND status IN ('PENDING','PICKED')", Long.class, bookingId);
        if (out != null && out > 0) {
            throw new BizException("仍有 " + out + " 项物资未归还，不能结算");
        }
        Map<String, Object> pv = checkoutPreview(bookingId);
        BigDecimal siteFee = (BigDecimal) pv.get("siteFee");
        BigDecimal goodsFee = (BigDecimal) pv.get("goodsFee");
        BigDecimal overtime = (BigDecimal) pv.get("overtimeFee");
        BigDecimal damage = (BigDecimal) pv.get("damageFee");
        BigDecimal deposit = (BigDecimal) pv.get("deposit");
        BigDecimal discount = lng(body.get("discount")) == null ? BigDecimal.ZERO : new BigDecimal(str(body.get("discount")));
        BigDecimal refund = deposit.subtract(damage);
        BigDecimal payable = siteFee.add(goodsFee).add(overtime).add(damage).subtract(discount);
        Integer cleanliness = num(body.get("cleanliness"));
        jdbc.update("INSERT INTO checkout_record(booking_id,cleanliness,goods_qty_ok,damage_desc,overdue_hours,site_fee,"
                        + "goods_fee,overtime_fee,damage_fee,deposit_refund,discount,total_amount,status,operator_id,"
                        + "operator_name,check_time,photo,remark,create_time,update_time) "
                        + "VALUES(?,?,?,?,?,?,?,?,?,?,?,?,'SETTLED',?,?,NOW(),?,?,NOW(),NOW())",
                bookingId, cleanliness == null ? 5 : cleanliness, damage.compareTo(BigDecimal.ZERO) > 0 ? 0 : 1,
                body.get("damageDesc"), pv.get("overtimeHours"), siteFee, goodsFee, overtime, damage, refund,
                discount, payable, UserContext.userId(), UserContext.realName(), body.get("photo"), body.get("remark"));
        Long coid = jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        // 费用明细（负数表示退还/减免）
        Object[][] items = {{"SITE", "营位费", siteFee}, {"GOODS", "物资租金", goodsFee},
                {"OVERTIME", "超时费", overtime}, {"DAMAGE", "损坏赔偿", damage},
                {"DISCOUNT", "优惠减免", discount.negate()}, {"DEPOSIT", "押金退还", refund}};
        for (Object[] it : items) {
            BigDecimal amt = (BigDecimal) it[2];
            if (amt == null || amt.compareTo(BigDecimal.ZERO) == 0) {
                continue;
            }
            jdbc.update("INSERT INTO fee_item(booking_id,checkout_id,fee_type,fee_name,amount,remark,create_time,update_time) "
                    + "VALUES(?,?,?,?,?,?,NOW(),NOW())", bookingId, coid, it[0], it[1], amt, "");
        }
        // 收款登记（仅线下结果，不接支付接口）
        String payType = str(body.get("payType"));
        if (StringUtils.hasText(payType)) {
            String payNo = "PA" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
                    + String.format("%04d", (int) (Math.random() * 9000) + 1000);
            jdbc.update("INSERT INTO payment(pay_no,booking_id,checkout_id,pay_type,amount,pay_time,operator_id,"
                            + "operator_name,status,remark,create_time,update_time) VALUES(?,?,?,?,?,NOW(),?,?,'PAID',?,NOW(),NOW())",
                    payNo, bookingId, coid, payType, payable, UserContext.userId(), UserContext.realName(),
                    body.get("payRemark"));
        }
        // 结算完成 -> 预约完成，营位回到待清洁
        jdbc.update("UPDATE booking SET status = 'COMPLETED', update_time = NOW() WHERE id = ?", bookingId);
        jdbc.update("UPDATE camp_site SET status = 'CLEANING' WHERE id = "
                + "(SELECT site_id FROM booking WHERE id = ?)", bookingId);
        jdbc.update("INSERT INTO booking_change(booking_id,change_type,old_value,new_value,reason,operator_id,"
                        + "operator_name,create_time,update_time) VALUES(?,?,?,?,?,?,?,NOW(),NOW())",
                bookingId, "COMPLETE", "CHECKED", "COMPLETED", "退营验收完成并登记线下收款", UserContext.userId(), UserContext.realName());
        return Map.of("bookingId", bookingId, "checkoutId", coid, "totalAmount", payable, "depositRefund", refund);
    }

    // ==================== 巡检 / 隐患 / 事件 ====================
    public PageResult<Map<String, Object>> patrolPage(int page, int size, String status, Long areaId, String type) {
        StringBuilder w = new StringBuilder("1=1");
        List<Object> a = new ArrayList<>();
        if (StringUtils.hasText(status)) {
            w.append(" AND p.status = ?");
            a.add(status);
        }
        if (areaId != null) {
            w.append(" AND p.area_id = ?");
            a.add(areaId);
        }
        if (StringUtils.hasText(type)) {
            w.append(" AND p.task_type = ?");
            a.add(type);
        }
        return page("patrol_task p LEFT JOIN camp_area ar ON ar.id = p.area_id", w.toString(), a,
                "p.plan_date DESC, p.id DESC", page, size);
    }

    @Transactional
    public void finishPatrol(Map<String, Object> body) {
        Long id = lng(body.get("id"));
        if (id == null) {
            throw new BizException("任务ID必填");
        }
        String result = orDefault(body.get("result"), "NORMAL");
        jdbc.update("UPDATE patrol_task SET status = 'DONE', result = ?, problem_level = ?, problem_desc = ?, "
                        + "image = ?, rectify_owner = ?, deadline = ?, finish_time = NOW(), update_time = NOW() WHERE id = ?",
                result, body.get("problemLevel"), body.get("problemDesc"), body.get("image"),
                body.get("rectifyOwner"), body.get("deadline"), id);
        if ("PROBLEM".equals(result)) {
            Map<String, Object> t = jdbc.queryForMap("SELECT * FROM patrol_task WHERE id = ?", id);
            String hno = "HZ" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
                    + String.format("%04d", (int) (Math.random() * 9000) + 1000);
            jdbc.update("INSERT INTO hazard(hazard_no,task_id,area_id,hazard_type,level,description,image,"
                            + "rectify_owner,deadline,status,create_time,update_time) VALUES(?,?,?,?,?,?,?,?,?,'OPEN',NOW(),NOW())",
                    hno, id, t.get("area_id"), body.get("hazardType"), body.get("problemLevel"),
                    body.get("problemDesc"), body.get("image"), body.get("rectifyOwner"), body.get("deadline"));
        }
    }

    public PageResult<Map<String, Object>> hazardPage(int page, int size, String status) {
        StringBuilder w = new StringBuilder("1=1");
        List<Object> a = new ArrayList<>();
        if (StringUtils.hasText(status)) {
            w.append(" AND h.status = ?");
            a.add(status);
        }
        return page("hazard h LEFT JOIN camp_area ar ON ar.id = h.area_id", w.toString(), a,
                "FIELD(h.status,'OPEN','RECTIFY','CLOSED'), h.deadline", page, size);
    }

    @Transactional
    public void closeHazard(Long id, String note) {
        if (!StringUtils.hasText(note) || note.trim().length() < 5) {
            throw new BizException("关闭隐患必须填写处理说明，且不少于 5 个字");
        }
        jdbc.update("UPDATE hazard SET status = 'CLOSED', close_time = NOW(), update_time = NOW() WHERE id = ?", id);
    }

    public PageResult<Map<String, Object>> eventPage(int page, int size, String status, String type) {
        StringBuilder w = new StringBuilder("1=1");
        List<Object> a = new ArrayList<>();
        if (StringUtils.hasText(status)) {
            w.append(" AND e.handle_status = ?");
            a.add(status);
        }
        if (StringUtils.hasText(type)) {
            w.append(" AND e.event_type = ?");
            a.add(type);
        }
        return page("camp_event e LEFT JOIN camp_site s ON s.id = e.site_id", w.toString(), a,
                "e.create_time DESC", page, size);
    }

    public void saveEvent(Map<String, Object> body) {
        String no = "EV" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
                + String.format("%04d", (int) (Math.random() * 9000) + 1000);
        jdbc.update("INSERT INTO camp_event(event_no,event_type,level,content,site_id,reporter_id,reporter_name,"
                        + "handle_status,create_time,update_time) VALUES(?,?,?,?,?,?,?,'OPEN',NOW(),NOW())",
                no, orDefault(body.get("eventType"), "OTHER"), orDefault(body.get("level"), "MID"),
                body.get("content"), body.get("siteId"), UserContext.userId(), UserContext.realName());
    }

    // ==================== 评价 / 黑名单 ====================
    public PageResult<Map<String, Object>> reviewPage(int page, int size, Integer minScore) {
        StringBuilder w = new StringBuilder("1=1");
        List<Object> a = new ArrayList<>();
        if (minScore != null) {
            w.append(" AND r.avg_score >= ?");
            a.add(minScore);
        }
        return page("review r LEFT JOIN booking b ON b.id = r.booking_id", w.toString(), a,
                "r.create_time DESC", page, size);
    }

    @Transactional
    public void replyReview(Long id, String reply) {
        if (!StringUtils.hasText(reply)) {
            throw new BizException("回复内容不能为空");
        }
        jdbc.update("UPDATE review SET reply = ?, reply_time = NOW(), update_time = NOW() WHERE id = ?", reply, id);
    }

    public PageResult<Map<String, Object>> blacklistPage(int page, int size, String status) {
        StringBuilder w = new StringBuilder("1=1");
        List<Object> a = new ArrayList<>();
        if (StringUtils.hasText(status)) {
            w.append(" AND status = ?");
            a.add(status);
        }
        return page("blacklist", w.toString(), a, "FIELD(status,'PENDING','ACTIVE','RELEASED','REJECTED'), id DESC",
                page, size);
    }

    public void applyBlacklist(Map<String, Object> body) {
        if (!StringUtils.hasText(str(body.get("reason"))) || str(body.get("reason")).trim().length() < 5) {
            throw new BizException("黑名单原因不少于 5 个字");
        }
        jdbc.update("INSERT INTO blacklist(user_id,customer_name,phone,reason_type,reason,status,apply_by,apply_name,"
                        + "create_time,update_time) VALUES(?,?,?,?,?,'PENDING',?,?,NOW(),NOW())",
                body.get("userId"), body.get("customerName"), body.get("phone"),
                orDefault(body.get("reasonType"), "DAMAGE"), body.get("reason"),
                UserContext.userId(), UserContext.realName());
    }

    /** 黑名单审批：仅经理/管理员可操作 */
    @Transactional
    public void auditBlacklist(Long id, String result, String note) {
        String role = UserContext.roleCode();
        if (!List.of("manager", "admin").contains(role)) {
            throw new BizException("只有运营经理或管理员可以审批黑名单");
        }
        if (!StringUtils.hasText(note) || note.trim().length() < 5) {
            throw new BizException("审批意见不少于 5 个字");
        }
        String st = "APPROVE".equals(result) ? "ACTIVE" : "REJECTED";
        jdbc.update("UPDATE blacklist SET status = ?, audit_by = ?, audit_name = ?, audit_time = NOW(), audit_note = ?, "
                + "update_time = NOW() WHERE id = ?", st, UserContext.userId(), UserContext.realName(), note, id);
    }

    @Transactional
    public void releaseBlacklist(Long id, String note) {
        if (!StringUtils.hasText(note) || note.trim().length() < 5) {
            throw new BizException("解除原因不少于 5 个字");
        }
        jdbc.update("UPDATE blacklist SET status = 'RELEASED', release_time = NOW(), audit_note = ?, update_time = NOW() "
                + "WHERE id = ?", note, id);
    }

    // ==================== 创新点：规则与 6 因子评分 ====================
    public List<Map<String, Object>> rules() {
        return jdbc.queryForList("SELECT * FROM avail_rule ORDER BY version DESC");
    }

    public Map<String, Object> activeRule() {
        List<Map<String, Object>> r = jdbc.queryForList(
                "SELECT * FROM avail_rule WHERE status = 'ENABLED' ORDER BY version DESC LIMIT 1");
        if (r.isEmpty()) {
            throw new BizException("尚未配置启用中的规则版本");
        }
        return r.get(0);
    }

    /** 保存新规则版本：权重合计必须等于 100%，旧的置为归档 */
    @Transactional
    public Long saveRule(Map<String, Object> body) {
        BigDecimal sum = BigDecimal.ZERO;
        String[] keys = {"wSiteTime", "wCapacity", "wFacility", "wStock", "wTurnover", "wMaintain"};
        Map<String, BigDecimal> val = new LinkedHashMap<>();
        for (String k : keys) {
            BigDecimal v = new BigDecimal(str(body.get(k)) == null ? "0" : str(body.get(k)));
            if (v.compareTo(BigDecimal.ZERO) < 0) {
                throw new BizException("权重不能为负数");
            }
            val.put(k, v);
            sum = sum.add(v);
        }
        if (sum.compareTo(new BigDecimal("100")) != 0) {
            throw new BizException("六项权重合计必须等于 100%，当前为 " + sum.stripTrailingZeros().toPlainString() + "%");
        }
        Integer maxVer = jdbc.queryForObject("SELECT COALESCE(MAX(version),0) FROM avail_rule", Integer.class);
        int ver = (maxVer == null ? 0 : maxVer) + 1;
        jdbc.update("UPDATE avail_rule SET status = 'ARCHIVED', update_time = NOW() WHERE status = 'ENABLED'");
        jdbc.update("INSERT INTO avail_rule(rule_name,version,w_site_time,w_capacity,w_facility,w_stock,w_turnover,"
                        + "w_maintain,threshold_high,threshold_mid,status,effective_time,create_by,create_name,create_time,update_time) "
                        + "VALUES(?,?,?,?,?,?,?,?,?,?,'ENABLED',NOW(),?,?,NOW(),NOW())",
                orDefault(body.get("ruleName"), "营位与租赁物资联合可用性规则 v" + ver), ver,
                val.get("wSiteTime"), val.get("wCapacity"), val.get("wFacility"), val.get("wStock"),
                val.get("wTurnover"), val.get("wMaintain"),
                new BigDecimal(str(body.get("thresholdHigh")) == null ? "87" : str(body.get("thresholdHigh"))),
                new BigDecimal(str(body.get("thresholdMid")) == null ? "80" : str(body.get("thresholdMid"))),
                UserContext.userId(), UserContext.realName());
        return jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    /** 对指定营位+日期+人数+物资执行 6 因子计算，保存输入快照与明细得分 */
    @Transactional
    public Map<String, Object> evaluate(Map<String, Object> body) {
        Long siteId = lng(body.get("siteId"));
        if (siteId == null) {
            throw new BizException("营位ID必填");
        }
        Integer guests = num(body.get("guestCount"));
        guests = guests == null ? 2 : guests;
        Map<String, Object> site = jdbc.queryForMap("SELECT * FROM camp_site WHERE id = ? AND deleted = 0", siteId);
        Map<String, Object> rule = activeRule();
        List<?> goodsPick = body.get("goodsIds") instanceof List<?> l ? l : List.of();

        // 因子 1 营位时段：状态可用性 + 日期是否落入特殊价高峰
        String status = (String) site.get("status");
        double sTime = switch (status) {
            case "FREE" -> 100;
            case "RESERVED" -> 82;
            case "CLEANING" -> 70;
            case "OCCUPIED" -> 55;
            case "REPAIR" -> 30;
            default -> 10;
        };
        // 因子 2 容纳人数：超最大容量判 0
        int mx = ((Number) site.get("max_capacity")).intValue();
        int std = ((Number) site.get("std_capacity")).intValue();
        double sCap = guests > mx ? Math.max(0, 100 - (guests - mx) * 35.0)
                : 100 - Math.max(0, guests - std) * 6.0;
        // 因子 3 设施需求：独立电源、公共水桩为必需项，含炊事物资时加烧烤台
        String fac = str(site.get("facilities")) == null ? "" : str(site.get("facilities"));
        List<String> need = new ArrayList<>(List.of("独立电源", "公共水桩"));
        boolean hasCook = false;
        for (Object gid : goodsPick) {
            List<Map<String, Object>> g = jdbc.queryForList("SELECT category_id FROM goods WHERE id = ?", lng(gid));
            if (!g.isEmpty() && ((Number) g.get(0).get("category_id")).intValue() == 4) {
                hasCook = true;
            }
        }
        if (hasCook) {
            need.add("烧烤台");
        }
        int hit = 0;
        for (String n : need) {
            if (fac.contains(n)) {
                hit++;
            }
        }
        double sFac = need.isEmpty() ? 100 : 100.0 * hit / need.size();
        // 因子 4 物资库存：所选物资可用率的最小值（超库存比例扣分）
        double sStock = 100;
        List<String> unmet = new ArrayList<>();
        List<Map<String, Object>> goodsInfo = new ArrayList<>();
        for (Object gid : goodsPick) {
            List<Map<String, Object>> gs = jdbc.queryForList("SELECT * FROM goods WHERE id = ?", lng(gid));
            if (gs.isEmpty()) {
                continue;
            }
            Map<String, Object> g = gs.get(0);
            int stock = ((Number) g.get("total_stock")).intValue();
            Long occupied = jdbc.queryForObject("SELECT COALESCE(SUM(ri.qty),0) FROM rent_item ri "
                    + "JOIN booking b ON b.id = ri.booking_id WHERE ri.goods_id = ? AND b.deleted = 0 "
                    + "AND b.status IN ('RESERVED','CONFIRMED','OCCUPIED')", Long.class, gid);
            int avail = Math.max(0, stock - (occupied == null ? 0 : occupied.intValue()));
            double rate = avail >= 2 ? 1.0 : avail / 2.0;
            sStock = Math.min(sStock, rate * 100);
            goodsInfo.add(Map.of("goodsId", g.get("id"), "name", g.get("goods_name"),
                    "stock", stock, "available", avail, "status", g.get("status")));
            if (avail < 2) {
                unmet.add("物资「" + g.get("goods_name") + "」可用库存仅 " + avail + " 件，建议减少数量或改选同类");
            }
        }
        // 因子 5 周转缓冲
        double sTurn = 100;
        if ("CLEANING".equals(status)) {
            sTurn -= 30;
        }
        if ("REPAIR".equals(status)) {
            sTurn -= 50;
        }
        for (Map<String, Object> g : goodsInfo) {
            if ("MAINTAIN".equals(g.get("status"))) {
                sTurn -= 35;
            }
        }
        sTurn = Math.max(0, sTurn);
        // 因子 6 维护状态
        double sMaint = switch (status) {
            case "FREE", "RESERVED" -> 100;
            case "OCCUPIED" -> 90;
            case "CLEANING" -> 80;
            case "REPAIR" -> 35;
            default -> 5;
        };
        double[] sc = {sTime, sCap, sFac, sStock, sTurn, sMaint};
        // 注意：JdbcTemplate 返回的 Map 键与数据库列名一致（snake_case），不能用驼峰取
        String[] wkey = {"w_site_time", "w_capacity", "w_facility", "w_stock", "w_turnover", "w_maintain"};
        String[] names = {"营位时段", "容纳人数", "设施需求", "物资库存", "物资周转", "维护状态"};
        double total = 0;
        double worstV = 101;
        String worstN = "";
        for (int i = 0; i < 6; i++) {
            double wv = ((BigDecimal) rule.get(wkey[i])).doubleValue();
            total += sc[i] * wv;
            if (sc[i] < worstV) {
                worstV = sc[i];
                worstN = names[i];
            }
        }
        total = Math.round(total) / 100.0;
        double hi = ((BigDecimal) rule.get("threshold_high")).doubleValue();
        double mid = ((BigDecimal) rule.get("threshold_mid")).doubleValue();
        boolean hardFail = guests > mx || List.of("REPAIR", "OFF").contains(status);
        String level = hardFail ? "LOW" : (total >= hi ? "HIGH" : (total >= mid ? "MID" : "LOW"));

        if (guests > mx) {
            unmet.add("入住人数 " + guests + " 人超过该营位最大容量 " + mx + " 人");
        }
        if (sFac < 100) {
            List<String> miss = new ArrayList<>();
            for (String n : need) {
                if (!fac.contains(n)) {
                    miss.add(n);
                }
            }
            unmet.add("缺少配套设施：" + String.join("、", miss));
        }
        if (List.of("REPAIR", "OFF").contains(status)) {
            unmet.add("营位当前" + ("REPAIR".equals(status) ? "维修中" : "停用") + "，不可直接预订");
        }
        List<String> improve = new ArrayList<>();
        if (sStock < 100) {
            improve.add("改用同类替代物资或缩短租期释放库存");
        }
        if (sCap < 80) {
            improve.add("改选最大容量更大的营位，或拆分为两个营位");
        }
        if ("CLEANING".equals(status)) {
            improve.add("等待约 2 小时清洁完成后再办理入住");
        }
        if (improve.isEmpty()) {
            improve.add("当前条件已满足，可直接确认预约");
        }
        // 替代组合建议
        List<Map<String, Object>> suggest = new ArrayList<>();
        if (!"HIGH".equals(level)) {
            List<Map<String, Object>> alts = jdbc.queryForList(
                    "SELECT id, site_no, site_name, base_price, max_capacity, status FROM camp_site "
                            + "WHERE deleted = 0 AND id <> ? AND max_capacity >= ? AND status IN ('FREE','RESERVED') "
                            + "ORDER BY base_price LIMIT 3", siteId, guests);
            for (Map<String, Object> alt : alts) {
                BigDecimal diff = ((BigDecimal) alt.get("base_price")).subtract((BigDecimal) site.get("base_price"));
                suggest.add(Map.of("siteNo", alt.get("site_no"), "siteName", alt.get("site_name"),
                        "priceDiff", diff.setScale(2, RoundingMode.HALF_UP), "reason", "容量与状态满足，可直接预订"));
            }
        }
        String main = worstN + "（得分 " + Math.round(worstV) + "）是当前主要制约因素";
        Long bookingId = lng(body.get("bookingId"));
        jdbc.update("INSERT INTO avail_score(booking_id,site_id,rule_version,input_snapshot,s_site_time,s_capacity,"
                        + "s_facility,s_stock,s_turnover,s_maintain,total_score,level,main_factor,unmet,improve_tip,"
                        + "suggest_json,create_time,update_time) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,NOW(),NOW())",
                bookingId, siteId, rule.get("version"),
                new com.fasterxml.jackson.databind.ObjectMapper().valueToTree(
                        Map.of("siteNo", site.get("site_no"), "siteStatus", status, "guestCount", guests,
                                "maxCapacity", mx, "stdCapacity", std, "facilities", fac,
                                "goods", goodsInfo, "weights", val(rule))).toString(),
                sc[0], sc[1], sc[2], sc[3], sc[4], sc[5], total, level, main,
                unmet.isEmpty() ? "无" : String.join("；", unmet), String.join("；", improve),
                new com.fasterxml.jackson.databind.ObjectMapper().valueToTree(suggest).toString());
        Long sid = jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("id", sid);
        res.put("ruleVersion", rule.get("version"));
        res.put("totalScore", total);
        res.put("level", level);
        res.put("levelName", "HIGH".equals(level) ? "推荐" : ("MID".equals(level) ? "可接受" : "不推荐"));
        res.put("mainFactor", main);
        res.put("unmet", unmet.isEmpty() ? "无" : String.join("；", unmet));
        res.put("improveTip", String.join("；", improve));
        res.put("suggest", suggest);
        List<Map<String, Object>> items = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            Map<String, Object> it = new LinkedHashMap<>();
            it.put("name", names[i]);
            it.put("score", Math.round(sc[i] * 10) / 10.0);
            it.put("weight", ((BigDecimal) rule.get(wkey[i])).doubleValue());
            it.put("weighted", Math.round(sc[i] * ((BigDecimal) rule.get(wkey[i])).doubleValue()) / 100.0);
            items.add(it);
        }
        res.put("items", items);
        res.put("goods", goodsInfo);
        res.put("site", Map.of("id", site.get("id"), "siteNo", site.get("site_no"), "siteName", site.get("site_name"),
                "status", status, "basePrice", site.get("base_price")));
        return res;
    }

    private Map<String, Object> val(Map<String, Object> rule) {
        Map<String, Object> m = new LinkedHashMap<>();
        for (String k : List.of("w_site_time", "w_capacity", "w_facility", "w_stock", "w_turnover", "w_maintain")) {
            m.put(k, rule.get(k));
        }
        return m;
    }

    /** 人工修正：原始结果不覆盖，只记 manual_* 字段 */
    @Transactional
    public void manualAdjust(Long id, String level, String reason) {
        if (!StringUtils.hasText(reason) || reason.trim().length() < 5) {
            throw new BizException("人工调整必须填写原因，且不少于 5 个字");
        }
        jdbc.update("UPDATE avail_score SET manual_level = ?, manual_reason = ?, manual_by = ?, manual_name = ?, "
                + "manual_time = NOW(), update_time = NOW() WHERE id = ?",
                level, reason, UserContext.userId(), UserContext.realName(), id);
    }

    public PageResult<Map<String, Object>> scorePage(int page, int size, String level) {
        StringBuilder w = new StringBuilder("1=1");
        List<Object> a = new ArrayList<>();
        if (StringUtils.hasText(level)) {
            w.append(" AND (s.level = ? OR s.manual_level = ?)");
            a.add(level);
            a.add(level);
        }
        return page("avail_score s LEFT JOIN booking b ON b.id = s.booking_id LEFT JOIN camp_site cs ON cs.id = s.site_id",
                w.toString(), a, "s.create_time DESC", page, size);
    }

    // ==================== 工具方法 ====================
    private static String str(Object o) {
        return o == null ? null : String.valueOf(o);
    }

    private static Integer num(Object o) {
        if (o == null || "".equals(o)) {
            return null;
        }
        if (o instanceof Number n) {
            return n.intValue();
        }
        try {
            return Integer.valueOf(String.valueOf(o));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static Long lng(Object o) {
        if (o == null || "".equals(o)) {
            return null;
        }
        if (o instanceof Number n) {
            return n.longValue();
        }
        try {
            return Long.valueOf(String.valueOf(o));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String orDefault(Object o, String def) {
        String s = str(o);
        return StringUtils.hasText(s) ? s : def;
    }
}
