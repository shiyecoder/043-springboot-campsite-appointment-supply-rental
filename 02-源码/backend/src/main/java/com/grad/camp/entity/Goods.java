package com.grad.camp.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 租赁物资。对应表 goods。
 */
@Data
@TableName("goods")
public class Goods {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 物资编号 */
    private String goodsNo;

    /** 物资名称 */
    private String goodsName;

    /** 分类 */
    private Long categoryId;

    /** 规格 */
    private String spec;

    /** 总库存 */
    private Integer totalStock;

    /** 押金 */
    private BigDecimal deposit;

    /** 租金 */
    private BigDecimal rentPrice;

    /** 计价单位：晚/天/套 */
    private String rentUnit;

    /** 周转缓冲小时（清洁检查） */
    private Integer turnoverHours;

    /** 图片 */
    private String image;

    /** 状态 */
    private String status;

    /** 备注 */
    private String remark;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;

}
