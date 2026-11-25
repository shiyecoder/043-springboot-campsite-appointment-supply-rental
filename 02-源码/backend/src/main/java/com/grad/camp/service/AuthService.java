package com.grad.camp.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.grad.camp.common.BizException;
import com.grad.camp.common.JwtUtil;
import com.grad.camp.common.UserContext;
import com.grad.camp.dto.AuthDtos;
import com.grad.camp.entity.SysLog;
import com.grad.camp.entity.SysMenu;
import com.grad.camp.entity.SysUser;
import com.grad.camp.mapper.SysLogMapper;
import com.grad.camp.mapper.SysMenuMapper;
import com.grad.camp.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 认证与个人中心。
 * 安全要求（需求文档 106 行）：连续失败触发短时锁定，密码 BCrypt 保存，任何返回与日志都不得带密码字段。
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final SysUserMapper userMapper;
    private final SysMenuMapper menuMapper;
    private final SysLogMapper logMapper;
    private final PasswordEncoder encoder;
    private final JwtUtil jwtUtil;

    @Value("${app.login-fail-limit:5}")
    private int failLimit;

    @Value("${app.login-lock-minutes:10}")
    private int lockMinutes;

    @Transactional
    public AuthDtos.LoginResp login(AuthDtos.LoginReq req) {
        SysUser user = userMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, req.username().trim()).last("limit 1"));
        if (user == null) {
            throw new BizException("账号或密码错误");
        }
        if (user.getStatus() != null && user.getStatus() == 0) {
            throw new BizException("账号已停用，请联系管理员");
        }
        // 锁定窗口内直接拒绝，避免暴力破解
        if (user.getLockUntil() != null && user.getLockUntil().isAfter(LocalDateTime.now())) {
            throw new BizException("登录失败次数过多，请于 " + user.getLockUntil() + " 后重试");
        }
        if (!encoder.matches(req.password(), user.getPassword())) {
            int fail = (user.getFailCount() == null ? 0 : user.getFailCount()) + 1;
            SysUser upd = new SysUser();
            upd.setId(user.getId());
            upd.setFailCount(fail);
            if (fail >= failLimit) {
                upd.setLockUntil(LocalDateTime.now().plusMinutes(lockMinutes));
                upd.setFailCount(0);
            }
            userMapper.updateById(upd);
            throw new BizException("账号或密码错误，还可尝试 " + Math.max(0, failLimit - fail) + " 次");
        }
        SysUser upd = new SysUser();
        upd.setId(user.getId());
        upd.setFailCount(0);
        upd.setLockUntil(null);
        upd.setLastLoginTime(LocalDateTime.now());
        userMapper.updateById(upd);

        UserContext.CurrentUser cu = new UserContext.CurrentUser(user.getId(), user.getUsername(),
                user.getRealName(), user.getRoleCode(), user.getOrgId(),
                user.getDataScope() == null ? "SELF" : user.getDataScope());
        String token = jwtUtil.create(cu);

        SysLog log = new SysLog();
        log.setUserId(user.getId());
        log.setUserName(user.getRealName());
        log.setAction("登录系统");
        log.setMethod("POST /api/auth/login");
        log.setParams("账号 " + user.getUsername());
        log.setCostMs(0L);
        log.setCreateTime(LocalDateTime.now());
        logMapper.insert(log);

        return new AuthDtos.LoginResp(token, toBrief(user), menus(user.getRoleCode()));
    }

    @Transactional
    public void register(AuthDtos.RegisterReq req) {
        Long exists = userMapper.selectCount(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, req.username().trim()));
        if (exists != null && exists > 0) {
            throw new BizException("账号已存在");
        }
        if (req.password().length() < 6) {
            throw new BizException("密码至少 6 位");
        }
        SysUser u = new SysUser();
        u.setUsername(req.username().trim());
        u.setPassword(encoder.encode(req.password()));
        u.setRealName(req.realName());
        u.setPhone(req.phone());
        // 自助注册只允许游客，前台/仓库/经理/管理员均由后台创建，避免提权
        String role = "tourist";
        u.setRoleCode(role);
        u.setDataScope("SELF");
        u.setStatus(1);
        u.setFailCount(0);
        u.setOrgId(1L);
        u.setOrgName("桃源谷露营基地");
        u.setCreateTime(LocalDateTime.now());
        userMapper.insert(u);
    }

    public Map<String, Object> me() {
        SysUser u = userMapper.selectById(UserContext.userId());
        if (u == null) {
            throw new BizException(401, "登录状态已失效，请重新登录");
        }
        Map<String, Object> m = toBrief(u);
        m.put("menus", menus(u.getRoleCode()));
        return m;
    }

    @Transactional
    public Map<String, Object> updateProfile(AuthDtos.ProfileReq req) {
        SysUser u = new SysUser();
        u.setId(UserContext.userId());
        u.setRealName(req.realName());
        u.setPhone(req.phone());
        u.setEmail(req.email());
        if (req.avatar() != null) {
            u.setAvatar(req.avatar());
        }
        userMapper.updateById(u);
        return me();
    }

    @Transactional
    public void changePassword(AuthDtos.PasswordReq req) {
        SysUser u = userMapper.selectById(UserContext.userId());
        if (u == null) {
            throw new BizException(401, "登录状态已失效，请重新登录");
        }
        if (!encoder.matches(req.oldPassword(), u.getPassword())) {
            throw new BizException("原密码不正确");
        }
        if (req.newPassword().length() < 6) {
            throw new BizException("新密码至少 6 位");
        }
        SysUser upd = new SysUser();
        upd.setId(u.getId());
        upd.setPassword(encoder.encode(req.newPassword()));
        userMapper.updateById(upd);
    }

    /** 菜单按角色动态获取，隐藏「菜单管理」入口（需求硬性要求第 12 条） */
    public List<Map<String, Object>> menus(String roleCode) {
        List<Long> ids = menuMapper.selectMenuIdsByRole(roleCode == null ? "tourist" : roleCode);
        List<SysMenu> all = menuMapper.selectList(new LambdaQueryWrapper<SysMenu>()
                .eq(SysMenu::getVisible, 1).orderByAsc(SysMenu::getSortNo));
        Map<Long, Map<String, Object>> nodes = new LinkedHashMap<>();
        for (SysMenu m : all) {
            if (ids != null && !ids.isEmpty() && !ids.contains(m.getId())) {
                continue;
            }
            if ("/system/menu".equals(m.getPath())) {
                continue; // 菜单管理对业务角色隐藏
            }
            Map<String, Object> n = new LinkedHashMap<>();
            n.put("id", m.getId());
            n.put("name", m.getMenuName());
            n.put("path", m.getPath());
            n.put("icon", m.getIcon());
            n.put("parentId", m.getParentId());
            n.put("children", new ArrayList<Map<String, Object>>());
            nodes.put(m.getId(), n);
        }
        List<Map<String, Object>> roots = new ArrayList<>();
        for (Map<String, Object> n : nodes.values()) {
            Long pid = (Long) n.get("parentId");
            Map<String, Object> parent = pid == null ? null : nodes.get(pid);
            if (parent == null) {
                roots.add(n);
            } else {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> kids = (List<Map<String, Object>>) parent.get("children");
                kids.add(n);
            }
        }
        return roots;
    }

    /** 用户摘要：绝不返回 password 字段 */
    private Map<String, Object> toBrief(SysUser u) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", u.getId());
        m.put("username", u.getUsername());
        m.put("realName", u.getRealName());
        m.put("phone", u.getPhone());
        m.put("email", u.getEmail());
        m.put("avatar", u.getAvatar());
        m.put("roleCode", u.getRoleCode());
        m.put("orgId", u.getOrgId());
        m.put("orgName", u.getOrgName());
        m.put("dataScope", u.getDataScope());
        m.put("lastLoginTime", u.getLastLoginTime());
        return m;
    }
}
