package com.grad.camp.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 费用明细。对应表 fee_item。
 */
@Data
@TableName("fee_item")
public class FeeItem {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 预约ID */
    private Long bookingId;

    /** 结算单ID */
    private Long checkoutId;

    /** 类型：SITE/GOODS/OVERTIME/DAMAGE/DISCOUNT/DEPOSIT */
    private String feeType;

    /** 费用名称 */
    private String feeName;

    /** 金额 */
    private BigDecimal amount;

    /** 备注 */
    private String remark;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;

}
