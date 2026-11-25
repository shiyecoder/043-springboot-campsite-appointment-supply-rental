package com.grad.camp.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;

/**
 * 营位主档。对应表 camp_site。
 */
@Data
@TableName("camp_site")
public class CampSite {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 营位编号（业务唯一） */
    private String siteNo;

    /** 营位名称 */
    private String siteName;

    /** 所属营区 */
    private Long areaId;

    /** 营位类型 */
    private Long typeId;

    /** 标准容量 */
    private Integer stdCapacity;

    /** 最大容量 */
    private Integer maxCapacity;

    /** 面积（平方米） */
    private Integer areaSize;

    /** 基础价格（元/晚） */
    private BigDecimal basePrice;

    /** 配套设施，逗号分隔 */
    private String facilities;

    /** 是否可用电：1是 0否 */
    private Integer powerSupply;

    /** 相邻环境说明 */
    private String environment;

    /** 列表图 */
    private String coverImage;

    /** 详情图 */
    private String detailImage;

    /** 营位状态：FREE/RESERVED/OCCUPIED/CLEANING/REPAIR/OFF */
    private String status;

    /** 开放日期 */
    private LocalDate openDate;

    /** 维护/停用说明 */
    private String deviceNote;

    /** 乐观锁版本 */
    @Version
    private Integer version;

    /** 创建人 */
    private Long createBy;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;

    /** 逻辑删除 */
    @TableLogic
    private Integer deleted;

}
