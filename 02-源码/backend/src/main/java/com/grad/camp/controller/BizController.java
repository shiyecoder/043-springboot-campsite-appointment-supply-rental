package com.grad.camp.controller;

import com.grad.camp.common.PageResult;
import com.grad.camp.common.R;
import com.grad.camp.service.BizService;
import com.grad.camp.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 业务接口：营位、预约、物资、入住退营、巡检隐患、评价黑名单、规则与创新评分、经营看板。
 * 统一返回 R<T>；所有状态变更都在服务层做权限与状态机校验，前端按钮只是交互提示。
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class BizController {

    private final BizService biz;
    private final DashboardService dashboard;

    // ---------- 看板 ----------
    @GetMapping("/dashboard")
    public R<Map<String, Object>> dashboard(@RequestParam(required = false) String period) {
        return R.ok(dashboard.board(period));
    }

    // ---------- 营位 ----------
    @GetMapping("/site/page")
    public R<PageResult<Map<String, Object>>> sitePage(@RequestParam(defaultValue = "1") int page,
                                                       @RequestParam(defaultValue = "10") int size,
                                                       @RequestParam(required = false) String keyword,
                                                       @RequestParam(required = false) Long areaId,
                                                       @RequestParam(required = false) Long typeId,
                                                       @RequestParam(required = false) String status,
                                                       @RequestParam(required = false) Integer guests,
                                                       @RequestParam(required = false) Boolean power) {
        return R.ok(biz.sitePage(page, size, keyword, areaId, typeId, status, guests, power));
    }

    @GetMapping("/site/options")
    public R<Map<String, Object>> siteOptions() {
        return R.ok(biz.siteOptions());
    }

    @GetMapping("/site/{id}")
    public R<Map<String, Object>> siteDetail(@PathVariable Long id) {
        return R.ok(biz.siteDetail(id));
    }

    @PostMapping("/site")
    public R<Long> createSite(@RequestBody Map<String, Object> body) {
        return R.ok(biz.saveSite(body, null));
    }

    @PutMapping("/site/{id}")
    public R<Long> updateSite(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        return R.ok(biz.saveSite(body, id));
    }

    @DeleteMapping("/site/{id}")
    public R<Void> deleteSite(@PathVariable Long id) {
        biz.deleteSite(id);
        return R.ok();
    }

    // ---------- 预约 ----------
    @GetMapping("/booking/page")
    public R<PageResult<Map<String, Object>>> bookingPage(@RequestParam(defaultValue = "1") int page,
                                                          @RequestParam(defaultValue = "10") int size,
                                                          @RequestParam(required = false) String keyword,
                                                          @RequestParam(required = false) String status,
                                                          @RequestParam(required = false) Long siteId,
                                                          @RequestParam(required = false) String quick,
                                                          @RequestParam(required = false) String from,
                                                          @RequestParam(required = false) String to) {
        return R.ok(biz.bookingPage(page, size, keyword, status, siteId, quick, from, to));
    }

    @GetMapping("/booking/calendar")
    public R<List<Map<String, Object>>> calendar(@RequestParam(required = false) String from,
                                                  @RequestParam(required = false) String to) {
        return R.ok(biz.calendar(from, to));
    }

    @GetMapping("/booking/{id}")
    public R<Map<String, Object>> bookingDetail(@PathVariable Long id) {
        return R.ok(biz.bookingDetail(id));
    }

    @PostMapping("/booking")
    public R<Long> createBooking(@RequestBody Map<String, Object> body) {
        return R.ok(biz.createBooking(body));
    }

    /** 状态流转：target 为目标状态，reason 在取消/拒绝/爽约时必填 */
    @PostMapping("/booking/{id}/transition")
    public R<Map<String, Object>> transition(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        return R.ok(biz.transition(id, (String) body.get("target"), (String) body.get("reason"),
                (String) body.get("checkInDate"), (String) body.get("checkOutDate")));
    }

    // ---------- 物资 ----------
    @GetMapping("/goods/page")
    public R<PageResult<Map<String, Object>>> goodsPage(@RequestParam(defaultValue = "1") int page,
                                                        @RequestParam(defaultValue = "10") int size,
                                                        @RequestParam(required = false) String keyword,
                                                        @RequestParam(required = false) Long categoryId,
                                                        @RequestParam(required = false) String status) {
        return R.ok(biz.goodsPage(page, size, keyword, categoryId, status));
    }

    @PostMapping("/goods")
    public R<Long> createGoods(@RequestBody Map<String, Object> body) {
        return R.ok(biz.saveGoods(body, null));
    }

    @PutMapping("/goods/{id}")
    public R<Long> updateGoods(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        return R.ok(biz.saveGoods(body, id));
    }

    @GetMapping("/goods/picklist")
    public R<Map<String, Object>> pickList(@RequestParam(required = false) String date) {
        return R.ok(biz.pickList(date));
    }

    @PostMapping("/goods/handover")
    public R<Void> handover(@RequestBody Map<String, Object> body) {
        biz.handover(body);
        return R.ok();
    }

    // ---------- 入住与退营 ----------
    @GetMapping("/stay/board")
    public R<Map<String, Object>> stayBoard(@RequestParam(required = false) String date) {
        return R.ok(biz.stayBoard(date));
    }

    @PostMapping("/stay/checkin")
    public R<Map<String, Object>> checkin(@RequestBody Map<String, Object> body) {
        return R.ok(biz.checkin(body));
    }

    @GetMapping("/checkout/{bookingId}/preview")
    public R<Map<String, Object>> checkoutPreview(@PathVariable Long bookingId) {
        return R.ok(biz.checkoutPreview(bookingId));
    }

    @PostMapping("/checkout/settle")
    public R<Map<String, Object>> settle(@RequestBody Map<String, Object> body) {
        return R.ok(biz.settle(body));
    }

    // ---------- 巡检 / 隐患 / 事件 ----------
    @GetMapping("/patrol/page")
    public R<PageResult<Map<String, Object>>> patrolPage(@RequestParam(defaultValue = "1") int page,
                                                         @RequestParam(defaultValue = "10") int size,
                                                         @RequestParam(required = false) String status,
                                                         @RequestParam(required = false) Long areaId,
                                                         @RequestParam(required = false) String type) {
        return R.ok(biz.patrolPage(page, size, status, areaId, type));
    }

    @PostMapping("/patrol/finish")
    public R<Void> finishPatrol(@RequestBody Map<String, Object> body) {
        biz.finishPatrol(body);
        return R.ok();
    }

    @GetMapping("/hazard/page")
    public R<PageResult<Map<String, Object>>> hazardPage(@RequestParam(defaultValue = "1") int page,
                                                         @RequestParam(defaultValue = "10") int size,
                                                         @RequestParam(required = false) String status) {
        return R.ok(biz.hazardPage(page, size, status));
    }

    @PostMapping("/hazard/{id}/close")
    public R<Void> closeHazard(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        biz.closeHazard(id, (String) body.get("note"));
        return R.ok();
    }

    @GetMapping("/event/page")
    public R<PageResult<Map<String, Object>>> eventPage(@RequestParam(defaultValue = "1") int page,
                                                        @RequestParam(defaultValue = "10") int size,
                                                        @RequestParam(required = false) String status,
                                                        @RequestParam(required = false) String type) {
        return R.ok(biz.eventPage(page, size, status, type));
    }

    @PostMapping("/event")
    public R<Void> saveEvent(@RequestBody Map<String, Object> body) {
        biz.saveEvent(body);
        return R.ok();
    }

    // ---------- 评价 / 黑名单 ----------
    @GetMapping("/review/page")
    public R<PageResult<Map<String, Object>>> reviewPage(@RequestParam(defaultValue = "1") int page,
                                                         @RequestParam(defaultValue = "10") int size,
                                                         @RequestParam(required = false) Integer minScore) {
        return R.ok(biz.reviewPage(page, size, minScore));
    }

    @PostMapping("/review/{id}/reply")
    public R<Void> replyReview(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        biz.replyReview(id, (String) body.get("reply"));
        return R.ok();
    }

    @GetMapping("/blacklist/page")
    public R<PageResult<Map<String, Object>>> blacklistPage(@RequestParam(defaultValue = "1") int page,
                                                            @RequestParam(defaultValue = "10") int size,
                                                            @RequestParam(required = false) String status) {
        return R.ok(biz.blacklistPage(page, size, status));
    }

    @PostMapping("/blacklist")
    public R<Void> applyBlacklist(@RequestBody Map<String, Object> body) {
        biz.applyBlacklist(body);
        return R.ok();
    }

    @PostMapping("/blacklist/{id}/audit")
    public R<Void> auditBlacklist(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        biz.auditBlacklist(id, (String) body.get("result"), (String) body.get("note"));
        return R.ok();
    }

    @PostMapping("/blacklist/{id}/release")
    public R<Void> releaseBlacklist(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        biz.releaseBlacklist(id, (String) body.get("note"));
        return R.ok();
    }

    // ---------- 创新点：规则与评分 ----------
    @GetMapping("/rule/list")
    public R<List<Map<String, Object>>> rules() {
        return R.ok(biz.rules());
    }

    @GetMapping("/rule/active")
    public R<Map<String, Object>> activeRule() {
        return R.ok(biz.activeRule());
    }

    @PostMapping("/rule/config")
    public R<Long> saveRule(@RequestBody Map<String, Object> body) {
        return R.ok(biz.saveRule(body));
    }

    @PostMapping("/avail/evaluate")
    public R<Map<String, Object>> evaluate(@RequestBody Map<String, Object> body) {
        return R.ok(biz.evaluate(body));
    }

    @GetMapping("/avail/page")
    public R<PageResult<Map<String, Object>>> scorePage(@RequestParam(defaultValue = "1") int page,
                                                        @RequestParam(defaultValue = "10") int size,
                                                        @RequestParam(required = false) String level) {
        return R.ok(biz.scorePage(page, size, level));
    }

    @PostMapping("/avail/{id}/manual")
    public R<Void> manualAdjust(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        biz.manualAdjust(id, (String) body.get("level"), (String) body.get("reason"));
        return R.ok();
    }
}
