package com.grad.camp.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 联合可用性计算留痕。对应表 avail_score。
 */
@Data
@TableName("avail_score")
public class AvailScore {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 预约ID */
    private Long bookingId;

    /** 营位ID */
    private Long siteId;

    /** 规则版本 */
    private Integer ruleVersion;

    /** 输入快照 JSON */
    private String inputSnapshot;

    /** 营位时段得分 */
    private BigDecimal sSiteTime;

    /** 容纳人数得分 */
    private BigDecimal sCapacity;

    /** 设施需求得分 */
    private BigDecimal sFacility;

    /** 物资库存得分 */
    private BigDecimal sStock;

    /** 物资周转得分 */
    private BigDecimal sTurnover;

    /** 维护状态得分 */
    private BigDecimal sMaintain;

    /** 总分 */
    private BigDecimal totalScore;

    /** 等级：HIGH/MID/LOW */
    private String level;

    /** 主要影响因素 */
    private String mainFactor;

    /** 未满足条件 */
    private String unmet;

    /** 可改进项 */
    private String improveTip;

    /** 替代组合建议 JSON */
    private String suggestJson;

    /** 人工调整后等级 */
    private String manualLevel;

    /** 人工调整原因 */
    private String manualReason;

    /** 调整人 */
    private Long manualBy;

    /** 调整人姓名 */
    private String manualName;

    /** 调整时间 */
    private LocalDateTime manualTime;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;

}
