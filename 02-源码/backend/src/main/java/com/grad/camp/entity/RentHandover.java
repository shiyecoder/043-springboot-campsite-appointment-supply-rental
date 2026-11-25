package com.grad.camp.entity;

import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 物资发放归还。对应表 rent_handover。
 */
@Data
@TableName("rent_handover")
public class RentHandover {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 租赁明细ID */
    private Long rentItemId;

    /** 预约ID */
    private Long bookingId;

    /** 物资ID */
    private Long goodsId;

    /** 类型：OUT/IN */
    private String handoverType;

    /** 数量 */
    private Integer qty;

    /** 序列号 */
    private String serialNo;

    /** 物品状态：GOOD/DIRTY/DAMAGED/LOST */
    private String itemStatus;

    /** 交接照片 */
    private String photo;

    /** 经办人 */
    private Long operatorId;

    /** 经办人姓名 */
    private String operatorName;

    /** 交接时间 */
    private LocalDateTime handoverTime;

    /** 备注 */
    private String remark;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;

}
