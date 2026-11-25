package com.grad.camp.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.grad.camp.entity.BookingChange;
import org.apache.ibatis.annotations.Mapper;

/** 预约变更记录 数据访问。 */
@Mapper
public interface BookingChangeMapper extends BaseMapper<BookingChange> {
}
