package com.grad.camp.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.grad.camp.entity.SysDict;
import org.apache.ibatis.annotations.Mapper;

/** 数据字典 数据访问。 */
@Mapper
public interface SysDictMapper extends BaseMapper<SysDict> {
}
