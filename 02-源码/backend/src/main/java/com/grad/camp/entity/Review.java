package com.grad.camp.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 服务评价。对应表 review。
 */
@Data
@TableName("review")
public class Review {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 预约ID */
    private Long bookingId;

    /** 评价人 */
    private Long userId;

    /** 评价人姓名 */
    private String customerName;

    /** 营位环境分 */
    private Integer siteScore;

    /** 自然环境分 */
    private Integer envScore;

    /** 设施分 */
    private Integer facilityScore;

    /** 服务分 */
    private Integer serviceScore;

    /** 平均分 */
    private BigDecimal avgScore;

    /** 评价内容 */
    private String content;

    /** 图片，逗号分隔 */
    private String images;

    /** 商家回复 */
    private String reply;

    /** 回复时间 */
    private LocalDateTime replyTime;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;

}
