package com.grad.camp.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.grad.camp.entity.CheckinRecord;
import org.apache.ibatis.annotations.Mapper;

/** 入住交接 数据访问。 */
@Mapper
public interface CheckinRecordMapper extends BaseMapper<CheckinRecord> {
}
