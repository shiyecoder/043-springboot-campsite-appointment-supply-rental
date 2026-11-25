package com.grad.camp.entity;

import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 入营车辆。对应表 booking_vehicle。
 */
@Data
@TableName("booking_vehicle")
public class BookingVehicle {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 预约ID */
    private Long bookingId;

    /** 车牌号 */
    private String plateNo;

    /** 车型 */
    private String vehicleType;

    /** 核载人数 */
    private Integer seatCount;

    /** 预计到达时间 */
    private LocalDateTime arriveTime;

    /** 备注 */
    private String remark;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;

}
