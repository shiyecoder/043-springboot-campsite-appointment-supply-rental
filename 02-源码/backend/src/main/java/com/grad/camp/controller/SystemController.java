package com.grad.camp.controller;

import com.grad.camp.common.PageResult;
import com.grad.camp.common.R;
import com.grad.camp.entity.*;
import com.grad.camp.service.SystemService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** 系统管理接口：用户、角色、菜单、字典、消息、日志。 */
@RestController
@RequestMapping("/api/system")
@RequiredArgsConstructor
public class SystemController {

    private final SystemService service;

    @GetMapping("/user/page")
    public R<PageResult<Map<String, Object>>> users(@RequestParam(defaultValue = "1") int page,
                                                    @RequestParam(defaultValue = "10") int size,
                                                    @RequestParam(required = false) String keyword,
                                                    @RequestParam(required = false) String roleCode) {
        return R.ok(service.users(page, size, keyword, roleCode));
    }

    @PostMapping("/user")
    public R<Long> createUser(@RequestBody SysUser req) {
        return R.ok(service.saveUser(req, null));
    }

    @PutMapping("/user/{id}")
    public R<Long> updateUser(@PathVariable Long id, @RequestBody SysUser req) {
        return R.ok(service.saveUser(req, id));
    }

    @PostMapping("/user/{id}/reset-password")
    public R<Void> resetPwd(@PathVariable Long id, @RequestParam(required = false) String password) {
        service.resetPassword(id, password);
        return R.ok();
    }

    @GetMapping("/role/list")
    public R<List<SysRole>> roles() {
        return R.ok(service.roles());
    }

    @GetMapping("/menu/list")
    public R<List<SysMenu>> menus() {
        return R.ok(service.menus());
    }

    @GetMapping("/menu/tree")
    public R<List<Map<String, Object>>> menuTree() {
        return R.ok(service.menuTree());
    }

    @GetMapping("/role/{roleCode}/menus")
    public R<List<Long>> roleMenus(@PathVariable String roleCode) {
        return R.ok(service.roleMenuIds(roleCode));
    }

    /** 为角色分配菜单 */
    @PostMapping("/role/{roleCode}/menus")
    public R<Void> assignMenus(@PathVariable String roleCode, @RequestBody List<Long> menuIds) {
        service.assignMenus(roleCode, menuIds);
        return R.ok();
    }

    @GetMapping("/dict/list")
    public R<List<SysDict>> dicts(@RequestParam(required = false) String type) {
        return R.ok(service.dicts(type));
    }

    @PostMapping("/dict")
    public R<Void> saveDict(@RequestBody SysDict d) {
        service.saveDict(d);
        return R.ok();
    }

    @DeleteMapping("/dict/{id}")
    public R<Void> deleteDict(@PathVariable Long id) {
        service.deleteDict(id);
        return R.ok();
    }

    @GetMapping("/org/list")
    public R<List<SysOrg>> orgs() {
        return R.ok(service.orgs());
    }

    @GetMapping("/message/list")
    public R<List<SysMessage>> messages() {
        return R.ok(service.messages());
    }

    @GetMapping("/message/unread")
    public R<Map<String, Object>> unread() {
        return R.ok(Map.of("count", service.unreadCount()));
    }

    @PostMapping("/message/{id}/read")
    public R<Void> read(@PathVariable Long id) {
        service.readMessage(id);
        return R.ok();
    }

    @PostMapping("/message/read-all")
    public R<Void> readAll() {
        service.readAll();
        return R.ok();
    }

    @GetMapping("/log/page")
    public R<PageResult<SysLog>> logs(@RequestParam(defaultValue = "1") int page,
                                      @RequestParam(defaultValue = "10") int size,
                                      @RequestParam(required = false) String keyword) {
        return R.ok(service.logs(page, size, keyword));
    }
}
