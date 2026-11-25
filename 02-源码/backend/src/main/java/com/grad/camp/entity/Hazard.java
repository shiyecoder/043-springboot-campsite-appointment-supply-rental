package com.grad.camp.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 隐患台账。对应表 hazard。
 */
@Data
@TableName("hazard")
public class Hazard {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 隐患编号 */
    private String hazardNo;

    /** 来源巡检任务 */
    private Long taskId;

    /** 营区ID */
    private Long areaId;

    /** 隐患类型 */
    private String hazardType;

    /** 级别：LOW/MID/HIGH */
    private String level;

    /** 描述 */
    private String description;

    /** 现场照片 */
    private String image;

    /** 整改责任人 */
    private String rectifyOwner;

    /** 整改期限 */
    private LocalDate deadline;

    /** 状态：OPEN/RECTIFY/CLOSED */
    private String status;

    /** 关闭时间 */
    private LocalDateTime closeTime;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;

}
