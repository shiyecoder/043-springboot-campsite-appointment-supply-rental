package com.grad.camp.common;

/**
 * 当前登录用户上下文。由 JWT 过滤器写入，Controller 只读。
 * 使用 ThreadLocal 保证同一请求线程内可取到角色与数据范围。
 */
public class UserContext {

    /**
     * dataScope 取值：ALL=全部数据、ORG=本院系、SELF=本人负责的数据。
     * 需求文档：学生只看本人，校内导师看本人指导学生，企业导师看本人负责学生，管理员看全量。
     */
    public record CurrentUser(Long userId, String username, String realName, String roleCode,
                              Long orgId, String dataScope) {
    }

    private static final ThreadLocal<CurrentUser> HOLDER = new ThreadLocal<>();

    public static void set(CurrentUser user) {
        HOLDER.set(user);
    }

    public static CurrentUser get() {
        return HOLDER.get();
    }

    public static Long userId() {
        CurrentUser u = HOLDER.get();
        return u == null ? null : u.userId();
    }

    public static String realName() {
        CurrentUser u = HOLDER.get();
        return u == null ? "" : u.realName();
    }

    public static String roleCode() {
        CurrentUser u = HOLDER.get();
        return u == null ? "" : u.roleCode();
    }

    public static Long orgId() {
        CurrentUser u = HOLDER.get();
        return u == null ? null : u.orgId();
    }

    public static String dataScope() {
        CurrentUser u = HOLDER.get();
        return u == null ? "SELF" : u.dataScope();
    }

    /** 学生：只能看到本人档案与本人周报 */
    public static boolean isStudent() {
        return "student".equals(roleCode());
    }

    /** 校内指导教师：看本人指导学生，负责申请审核与周报批阅 */
    public static boolean isTeacher() {
        return "teacher".equals(roleCode());
    }

    /** 企业导师：确认到岗、工作内容、考勤与企业评价 */
    public static boolean isMentor() {
        return "mentor".equals(roleCode());
    }

    /** 实习管理员拥有全量数据范围 */
    public static boolean isAdmin() {
        return "admin".equals(roleCode());
    }

    public static void clear() {
        HOLDER.remove();
    }
}
