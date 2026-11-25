package com.grad.camp.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.grad.camp.entity.PatrolTask;
import org.apache.ibatis.annotations.Mapper;

/** 巡检任务 数据访问。 */
@Mapper
public interface PatrolTaskMapper extends BaseMapper<PatrolTask> {
}
