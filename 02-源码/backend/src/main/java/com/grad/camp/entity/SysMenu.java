package com.grad.camp.entity;

import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 菜单。对应表 sys_menu。
 */
@Data
@TableName("sys_menu")
public class SysMenu {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 菜单名称 */
    private String menuName;

    /** 前端路由 */
    private String path;

    /** 图标名 */
    private String icon;

    /** 上级菜单 */
    private Long parentId;

    /** 类型：DIR/MENU/BUTTON */
    private String menuType;

    /** 是否可见 */
    private Integer visible;

    /** 排序 */
    private Integer sortNo;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;

}
