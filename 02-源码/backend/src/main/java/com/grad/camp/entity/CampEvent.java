package com.grad.camp.entity;

import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 运营事件。对应表 camp_event。
 */
@Data
@TableName("camp_event")
public class CampEvent {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 事件编号 */
    private String eventNo;

    /** 类型：WEATHER/NOISE/DEVICE/OTHER */
    private String eventType;

    /** 级别 */
    private String level;

    /** 事件内容 */
    private String content;

    /** 关联营位 */
    private Long siteId;

    /** 上报人 */
    private Long reporterId;

    /** 上报人姓名 */
    private String reporterName;

    /** 处理状态：OPEN/HANDLING/CLOSED */
    private String handleStatus;

    /** 处理结果 */
    private String handleResult;

    /** 处理时间 */
    private LocalDateTime handleTime;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;

}
