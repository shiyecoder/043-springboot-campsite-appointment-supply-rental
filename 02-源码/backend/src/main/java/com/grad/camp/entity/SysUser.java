package com.grad.camp.entity;

import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 系统用户。对应表 sys_user。
 */
@Data
@TableName("sys_user")
public class SysUser {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 登录账号 */
    private String username;

    /** BCrypt 密码，不外泄 */
    private String password;

    /** 姓名 */
    private String realName;

    /** 联系电话 */
    private String phone;

    /** 邮箱 */
    private String email;

    /** 头像相对路径 */
    private String avatar;

    /** 角色编码：tourist/front/warehouse/manager/admin */
    private String roleCode;

    /** 所属组织ID */
    private Long orgId;

    /** 组织名称冗余 */
    private String orgName;

    /** 岗位，如前台主管、仓库管理员 */
    private String post;

    /** 数据范围：ALL/ORG/SELF */
    private String dataScope;

    /** 状态：1启用 0停用 */
    private Integer status;

    /** 连续登录失败次数 */
    private Integer failCount;

    /** 锁定截止时间 */
    private LocalDateTime lockUntil;

    /** 最近登录时间 */
    private LocalDateTime lastLoginTime;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;

}
