package com.grad.camp.entity;

import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 预约变更记录。对应表 booking_change。
 */
@Data
@TableName("booking_change")
public class BookingChange {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 预约ID */
    private Long bookingId;

    /** 类型：CONFIRM/REJECT/RESCHEDULE/CANCEL/ARRIVE/NOSHOW */
    private String changeType;

    /** 变更前 */
    private String oldValue;

    /** 变更后 */
    private String newValue;

    /** 原因（不少于5字） */
    private String reason;

    /** 操作人 */
    private Long operatorId;

    /** 操作人姓名 */
    private String operatorName;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;

}
