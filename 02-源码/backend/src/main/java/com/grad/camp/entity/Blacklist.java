package com.grad.camp.entity;

import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 黑名单。对应表 blacklist。
 */
@Data
@TableName("blacklist")
public class Blacklist {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 客户ID */
    private Long userId;

    /** 客户姓名 */
    private String customerName;

    /** 联系电话 */
    private String phone;

    /** 类型：DAMAGE/VIOLATION/NOSHOW */
    private String reasonType;

    /** 原因 */
    private String reason;

    /** 状态：PENDING/ACTIVE/RELEASED/REJECTED */
    private String status;

    /** 申请人 */
    private Long applyBy;

    /** 申请人姓名 */
    private String applyName;

    /** 审批人 */
    private Long auditBy;

    /** 审批人姓名 */
    private String auditName;

    /** 审批时间 */
    private LocalDateTime auditTime;

    /** 审批意见 */
    private String auditNote;

    /** 解除时间 */
    private LocalDateTime releaseTime;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;

}
