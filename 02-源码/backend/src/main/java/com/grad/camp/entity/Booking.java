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
 * 营位预约与物资租赁主单。对应表 booking。
 */
@Data
@TableName("booking")
public class Booking {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 预约编号 */
    private String bookingNo;

    /** 预订人 */
    private Long userId;

    /** 预订人姓名 */
    private String customerName;

    /** 联系电话 */
    private String customerPhone;

    /** 营位ID */
    private Long siteId;

    /** 营位编号冗余 */
    private String siteNo;

    /** 入住日期 */
    private LocalDate checkInDate;

    /** 退营日期 */
    private LocalDate checkOutDate;

    /** 晚数 */
    private Integer nights;

    /** 入住人数 */
    private Integer guestCount;

    /** 儿童/老人/特殊人群说明 */
    private String specialNeed;

    /** 来源：WEB/PHONE/WALKIN */
    private String source;

    /** 营位费 */
    private BigDecimal siteFee;

    /** 物资租金 */
    private BigDecimal goodsFee;

    /** 押金 */
    private BigDecimal deposit;

    /** 合计金额 */
    private BigDecimal totalFee;

    /** 状态：DRAFT/SUBMITTED/CONFIRMED/RESERVED/OCCUPIED/CHECKED/COMPLETED/CANCELLED/REJECTED/NOSHOW */
    private String status;

    /** 当前责任人 */
    private Long responsibleId;

    /** 取消原因 */
    private String cancelReason;

    /** 备注 */
    private String remark;

    /** 乐观锁版本 */
    @Version
    private Integer version;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;

    /** 逻辑删除 */
    @TableLogic
    private Integer deleted;

}
