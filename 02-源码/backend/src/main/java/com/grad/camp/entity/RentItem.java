package com.grad.camp.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 租赁明细。对应表 rent_item。
 */
@Data
@TableName("rent_item")
public class RentItem {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 预约ID */
    private Long bookingId;

    /** 物资ID */
    private Long goodsId;

    /** 物资名称冗余 */
    private String goodsName;

    /** 数量 */
    private Integer qty;

    /** 租用时长 */
    private Integer rentDays;

    /** 单价 */
    private BigDecimal unitPrice;

    /** 押金小计 */
    private BigDecimal deposit;

    /** 租金小计 */
    private BigDecimal amount;

    /** 状态：PENDING/PICKED/RETURNED/DAMAGED/LOST */
    private String status;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;

}
