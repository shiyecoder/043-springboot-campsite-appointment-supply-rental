package com.grad.camp.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 巡检任务。对应表 patrol_task。
 */
@Data
@TableName("patrol_task")
public class PatrolTask {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 任务编号 */
    private String taskNo;

    /** 营区ID */
    private Long areaId;

    /** 类型：FIRE/POWER/SANITARY/ROAD/FACILITY */
    private String taskType;

    /** 计划日期 */
    private LocalDate planDate;

    /** 巡检人 */
    private Long inspectorId;

    /** 巡检人姓名 */
    private String inspectorName;

    /** 状态：PENDING/RUNNING/DONE/OVERDUE */
    private String status;

    /** 结果：NORMAL/PROBLEM */
    private String result;

    /** 问题级别：LOW/MID/HIGH */
    private String problemLevel;

    /** 问题描述 */
    private String problemDesc;

    /** 现场照片 */
    private String image;

    /** 整改责任人 */
    private String rectifyOwner;

    /** 整改期限 */
    private LocalDate deadline;

    /** 完成时间 */
    private LocalDateTime finishTime;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;

}
