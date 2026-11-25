package com.grad.camp.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.grad.camp.entity.SysMenu;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/** 菜单数据访问，额外提供「按角色取菜单ID」的联表查询。 */
@Mapper
public interface SysMenuMapper extends BaseMapper<SysMenu> {

    /** 角色菜单关联查询，菜单按角色动态下发。 */
    @Select("SELECT m.id FROM sys_menu m "
            + "JOIN sys_role_menu rm ON rm.menu_id = m.id "
            + "JOIN sys_role r ON r.id = rm.role_id "
            + "WHERE r.role_code = #{roleCode}")
    List<Long> selectMenuIdsByRole(@Param("roleCode") String roleCode);
}
