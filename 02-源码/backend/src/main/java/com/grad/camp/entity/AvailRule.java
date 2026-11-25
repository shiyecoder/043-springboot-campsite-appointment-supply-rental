package com.grad.camp.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;

/**
 * 联合可用性规则版本。对应表 avail_rule。
 */
@Data
@TableName("avail_rule")
public class AvailRule {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 规则名称 */
    private String ruleName;

    /** 版本号 */
    @Version
    private Integer version;

    /** 营位时段权重 */
    private BigDecimal wSiteTime;

    /** 容纳人数权重 */
    private BigDecimal wCapacity;

    /** 设施需求权重 */
    private BigDecimal wFacility;

    /** 物资库存权重 */
    private BigDecimal wStock;

    /** 物资周转权重 */
    private BigDecimal wTurnover;

    /** 维护状态权重 */
    private BigDecimal wMaintain;

    /** 推荐阈值 */
    private BigDecimal thresholdHigh;

    /** 可接受阈值 */
    private BigDecimal thresholdMid;

    /** 状态：ENABLED/ARCHIVED */
    private String status;

    /** 生效时间 */
    private LocalDateTime effectiveTime;

    /** 创建人 */
    private Long createBy;

    /** 创建人姓名 */
    private String createName;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;

}
