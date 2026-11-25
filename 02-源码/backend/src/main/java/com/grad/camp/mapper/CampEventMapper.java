package com.grad.camp.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.grad.camp.entity.CampEvent;
import org.apache.ibatis.annotations.Mapper;

/** 运营事件 数据访问。 */
@Mapper
public interface CampEventMapper extends BaseMapper<CampEvent> {
}
