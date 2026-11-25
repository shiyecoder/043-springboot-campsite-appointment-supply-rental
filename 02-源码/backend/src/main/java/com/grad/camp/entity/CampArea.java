package com.grad.camp.entity;

import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 营区。对应表 camp_area。
 */
@Data
@TableName("camp_area")
public class CampArea {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 营区编码 */
    private String areaCode;

    /** 营区名称 */
    private String areaName;

    /** 地形：草坪/林地/湖畔/山脊 */
    private String terrain;

    /** 营位数量 */
    private Integer siteCount;

    /** 营区说明 */
    private String description;

    /** 营区图片 */
    private String coverImage;

    /** 区域地图 */
    private String mapImage;

    /** 状态 */
    private String status;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;

}
