package com.grad.camp.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.grad.camp.entity.Hazard;
import org.apache.ibatis.annotations.Mapper;

/** 隐患台账 数据访问。 */
@Mapper
public interface HazardMapper extends BaseMapper<Hazard> {
}
