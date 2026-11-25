package com.grad.camp.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 退营验收与结算。对应表 checkout_record。
 */
@Data
@TableName("checkout_record")
public class CheckoutRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 预约ID */
    private Long bookingId;

    /** 卫生评分 1-5 */
    private Integer cleanliness;

    /** 物资数量是否齐全 */
    private Integer goodsQtyOk;

    /** 损坏说明 */
    private String damageDesc;

    /** 超时小时数 */
    private BigDecimal overdueHours;

    /** 营位费 */
    private BigDecimal siteFee;

    /** 物资租金 */
    private BigDecimal goodsFee;

    /** 超时费 */
    private BigDecimal overtimeFee;

    /** 损坏赔偿 */
    private BigDecimal damageFee;

    /** 押金退还 */
    private BigDecimal depositRefund;

    /** 优惠减免 */
    private BigDecimal discount;

    /** 应收合计 */
    private BigDecimal totalAmount;

    /** 状态：DRAFT/SETTLED */
    private String status;

    /** 经办人 */
    private Long operatorId;

    /** 经办人姓名 */
    private String operatorName;

    /** 验收时间 */
    private LocalDateTime checkTime;

    /** 验收照片 */
    private String photo;

    /** 备注 */
    private String remark;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;

}
