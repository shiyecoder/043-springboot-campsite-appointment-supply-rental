package com.grad.camp.entity;

import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 同行人。对应表 guest_member。
 */
@Data
@TableName("guest_member")
public class GuestMember {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 预约ID */
    private Long bookingId;

    /** 姓名 */
    private String guestName;

    /** 联系方式 */
    private String phone;

    /** 年龄段：ADULT/CHILD/ELDER */
    private String ageGroup;

    /** 证件号后四位 */
    private String idCard;

    /** 紧急联系人 */
    private String emergencyContact;

    /** 紧急联系电话 */
    private String emergencyPhone;

    /** 安全提示是否确认 */
    private Integer safetyConfirmed;

    /** 备注 */
    private String remark;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;

}
