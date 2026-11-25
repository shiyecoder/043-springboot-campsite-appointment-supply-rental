package com.grad.camp.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 入住交接。对应表 checkin_record。
 */
@Data
@TableName("checkin_record")
public class CheckinRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 预约ID */
    private Long bookingId;

    /** 交接单号 */
    private String handoverNo;

    /** 营位ID */
    private Long siteId;

    /** 实际人数 */
    private Integer actualGuestCount;

    /** 收取押金 */
    private BigDecimal deposit;

    /** 安全须知确认 */
    private Integer safetyConfirmed;

    /** 营位交接照片 */
    private String sitePhoto;

    /** 物资交接照片 */
    private String goodsPhoto;

    /** 经办人 */
    private Long operatorId;

    /** 经办人姓名 */
    private String operatorName;

    /** 入住时间 */
    private LocalDateTime checkinTime;

    /** 状态：DONE/VOID */
    private String status;

    /** 备注 */
    private String remark;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;

}
