package com.grad.camp.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.grad.camp.entity.AvailScore;
import org.apache.ibatis.annotations.Mapper;

/** 联合可用性计算留痕 数据访问。 */
@Mapper
public interface AvailScoreMapper extends BaseMapper<AvailScore> {
}
