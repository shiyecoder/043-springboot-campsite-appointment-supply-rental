package com.grad.camp.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 线下收款记录。对应表 payment。
 */
@Data
@TableName("payment")
public class Payment {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 收款单号 */
    private String payNo;

    /** 预约ID */
    private Long bookingId;

    /** 结算单ID */
    private Long checkoutId;

    /** 方式：CASH/WECHAT/ALIPAY/CARD */
    private String payType;

    /** 金额 */
    private BigDecimal amount;

    /** 收款时间 */
    private LocalDateTime payTime;

    /** 经办人 */
    private Long operatorId;

    /** 经办人姓名 */
    private String operatorName;

    /** 状态：PAID/REFUND */
    private String status;

    /** 备注 */
    private String remark;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;

}
