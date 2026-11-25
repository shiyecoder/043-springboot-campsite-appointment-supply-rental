package com.grad.camp.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 特殊日期价格。对应表 site_price。
 */
@Data
@TableName("site_price")
public class SitePrice {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 营位ID */
    private Long siteId;

    /** 类型：HOLIDAY/WEEKEND/PEAK */
    private String ruleType;

    /** 开始日期 */
    private LocalDate startDate;

    /** 结束日期 */
    private LocalDate endDate;

    /** 价格 */
    private BigDecimal price;

    /** 说明 */
    private String remark;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;

}
