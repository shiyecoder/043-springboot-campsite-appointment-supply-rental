package com.grad.camp.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.grad.camp.common.BizException;
import com.grad.camp.common.PageResult;
import com.grad.camp.entity.*;
import com.grad.camp.mapper.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 系统管理：用户、角色、菜单与分配、字典、消息、日志。
 * 菜单按角色动态下发；角色分配菜单是独立表 sys_role_menu。
 */
@Service
@RequiredArgsConstructor
public class SystemService {

    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;
    private final SysMenuMapper menuMapper;
    private final SysDictMapper dictMapper;
    private final SysLogMapper logMapper;
    private final SysMessageMapper messageMapper;
    private final SysOrgMapper orgMapper;
    private final PasswordEncoder encoder;
    private final org.springframework.jdbc.core.JdbcTemplate jdbc;

    // ---------------- 用户 ----------------
    public PageResult<Map<String, Object>> users(int page, int size, String keyword, String roleCode) {
        LambdaQueryWrapper<SysUser> q = new LambdaQueryWrapper<>();
        q.eq(StringUtils.hasText(roleCode), SysUser::getRoleCode, roleCode)
         .and(StringUtils.hasText(keyword), w -> w.like(SysUser::getUsername, keyword)
                 .or().like(SysUser::getRealName, keyword));
        q.orderByDesc(SysUser::getId);
        Page<SysUser> p = userMapper.selectPage(new Page<>(page, size), q);
        List<Map<String, Object>> rows = new ArrayList<>();
        for (SysUser u : p.getRecords()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", u.getId());
            m.put("username", u.getUsername());
            m.put("realName", u.getRealName());
            m.put("phone", u.getPhone());
            m.put("email", u.getEmail());
            m.put("avatar", u.getAvatar());
            m.put("roleCode", u.getRoleCode());
            m.put("roleName", roleName(u.getRoleCode()));
            m.put("orgId", u.getOrgId());
            m.put("orgName", u.getOrgName());
            m.put("dataScope", u.getDataScope());
            m.put("status", u.getStatus());
            m.put("lastLoginTime", u.getLastLoginTime());
            m.put("createTime", u.getCreateTime());
            rows.add(m);
        }
        return PageResult.of(p.getTotal(), page, size, rows);
    }

    private String roleName(String code) {
        return switch (code == null ? "" : code) {
            case "admin" -> "系统管理员";
            case "manager" -> "运营经理";
            case "front" -> "前台人员";
            case "warehouse" -> "仓库人员";
            case "tourist" -> "游客/预订人";
            default -> code;
        };
    }

    @Transactional
    public Long saveUser(SysUser req, Long id) {
        if (id == null) {
            Long dup = userMapper.selectCount(new LambdaQueryWrapper<SysUser>()
                    .eq(SysUser::getUsername, req.getUsername()));
            if (dup != null && dup > 0) {
                throw new BizException("账号已存在");
            }
            SysUser u = new SysUser();
            u.setUsername(req.getUsername());
            u.setPassword(encoder.encode(StringUtils.hasText(req.getPassword()) ? req.getPassword() : "123456"));
            u.setRealName(req.getRealName());
            u.setPhone(req.getPhone());
            u.setEmail(req.getEmail());
            u.setAvatar(req.getAvatar());
            u.setRoleCode(req.getRoleCode());
            u.setOrgId(req.getOrgId() == null ? 1L : req.getOrgId());
            u.setOrgName(req.getOrgName());
            u.setDataScope(req.getDataScope() == null ? "SELF" : req.getDataScope());
            u.setStatus(req.getStatus() == null ? 1 : req.getStatus());
            u.setFailCount(0);
            u.setCreateTime(LocalDateTime.now());
            userMapper.insert(u);
            return u.getId();
        }
        SysUser u = new SysUser();
        u.setId(id);
        u.setRealName(req.getRealName());
        u.setPhone(req.getPhone());
        u.setEmail(req.getEmail());
        u.setRoleCode(req.getRoleCode());
        u.setDataScope(req.getDataScope());
        u.setStatus(req.getStatus());
        u.setOrgId(req.getOrgId());
        u.setOrgName(req.getOrgName());
        userMapper.updateById(u);
        return id;
    }

    @Transactional
    public void resetPassword(Long userId, String password) {
        SysUser u = new SysUser();
        u.setId(userId);
        u.setPassword(encoder.encode(StringUtils.hasText(password) ? password : "123456"));
        u.setFailCount(0);
        u.setLockUntil(null);
        userMapper.updateById(u);
    }

    // ---------------- 角色与菜单 ----------------
    public List<SysRole> roles() {
        return roleMapper.selectList(new LambdaQueryWrapper<SysRole>().orderByAsc(SysRole::getId));
    }

    public List<SysMenu> menus() {
        return menuMapper.selectList(new LambdaQueryWrapper<SysMenu>().orderByAsc(SysMenu::getSortNo));
    }

    /** 菜单树，供「分配菜单」使用 */
    public List<Map<String, Object>> menuTree() {
        List<SysMenu> all = menus();
        Map<Long, Map<String, Object>> nodes = new LinkedHashMap<>();
        for (SysMenu m : all) {
            Map<String, Object> n = new LinkedHashMap<>();
            n.put("id", m.getId());
            n.put("label", m.getMenuName());
            n.put("path", m.getPath());
            n.put("parentId", m.getParentId());
            n.put("menuType", m.getMenuType());
            n.put("children", new ArrayList<Map<String, Object>>());
            nodes.put(m.getId(), n);
        }
        List<Map<String, Object>> roots = new ArrayList<>();
        for (Map<String, Object> n : nodes.values()) {
            Long pid = (Long) n.get("parentId");
            Map<String, Object> p = pid == null ? null : nodes.get(pid);
            if (p == null) {
                roots.add(n);
            } else {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> kids = (List<Map<String, Object>>) p.get("children");
                kids.add(n);
            }
        }
        return roots;
    }

    public List<Long> roleMenuIds(String roleCode) {
        List<Long> ids = menuMapper.selectMenuIdsByRole(roleCode);
        return ids == null ? List.of() : ids;
    }

    @Transactional
    public void assignMenus(String roleCode, List<Long> menuIds) {
        // 关联表按 role_id 存储，查询侧用 role_code 联表；这里先换算出 role_id
        List<Long> roleIds = jdbc.queryForList("SELECT id FROM sys_role WHERE role_code = ?", Long.class, roleCode);
        if (roleIds.isEmpty()) {
            throw new BizException("角色不存在：" + roleCode);
        }
        Long roleId = roleIds.get(0);
        jdbc.update("DELETE FROM sys_role_menu WHERE role_id = ?", roleId);
        if (menuIds != null) {
            for (Long id : menuIds) {
                jdbc.update("INSERT INTO sys_role_menu(role_id, menu_id, create_time, update_time) VALUES(?,?,NOW(),NOW())", roleId, id);
            }
        }
    }

    // ---------------- 字典 ----------------
    public List<SysDict> dicts(String type) {
        return dictMapper.selectList(new LambdaQueryWrapper<SysDict>()
                .eq(StringUtils.hasText(type), SysDict::getDictType, type)
                .orderByAsc(SysDict::getDictType).orderByAsc(SysDict::getSortNo));
    }

    @Transactional
    public void saveDict(SysDict d) {
        if (d.getId() == null) {
            dictMapper.insert(d);
        } else {
            dictMapper.updateById(d);
        }
    }

    public void deleteDict(Long id) {
        dictMapper.deleteById(id);
    }

    public List<SysOrg> orgs() {
        return orgMapper.selectList(null);
    }

    // ---------------- 消息与日志 ----------------
    public List<SysMessage> messages() {
        return messageMapper.selectList(new LambdaQueryWrapper<SysMessage>()
                .orderByDesc(SysMessage::getId).last("limit 50"));
    }

    public long unreadCount() {
        Long c = messageMapper.selectCount(new LambdaQueryWrapper<SysMessage>().eq(SysMessage::getIsRead, 0));
        return c == null ? 0 : c;
    }

    @Transactional
    public void readMessage(Long id) {
        SysMessage m = new SysMessage();
        m.setId(id);
        m.setIsRead(1);
        messageMapper.updateById(m);
    }

    @Transactional
    public void readAll() {
        jdbc.update("UPDATE sys_message SET is_read = 1 WHERE is_read = 0");
    }

    public PageResult<SysLog> logs(int page, int size, String keyword) {
        LambdaQueryWrapper<SysLog> q = new LambdaQueryWrapper<>();
        q.and(StringUtils.hasText(keyword), w -> w.like(SysLog::getUserName, keyword)
                .or().like(SysLog::getAction, keyword));
        q.orderByDesc(SysLog::getId);
        Page<SysLog> p = logMapper.selectPage(new Page<>(page, size), q);
        return PageResult.of(p.getTotal(), page, size, p.getRecords());
    }
}
