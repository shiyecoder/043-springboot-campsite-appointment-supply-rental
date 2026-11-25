package com.grad.camp.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 营位类型。对应表 site_type。
 */
@Data
@TableName("site_type")
public class SiteType {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 类型编码 */
    private String typeCode;

    /** 类型名称 */
    private String typeName;

    /** 基础价格（元/晚） */
    private BigDecimal basePrice;

    /** 标准容量 */
    private Integer stdCapacity;

    /** 最大容量 */
    private Integer maxCapacity;

    /** 说明 */
    private String description;

    /** 图标 */
    private String icon;

    /** 标识色 */
    private String color;

    /** 排序 */
    private Integer sortNo;

    /** 状态 */
    private String status;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;

}
