package com.grad.camp.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.grad.camp.entity.Booking;
import org.apache.ibatis.annotations.Mapper;

/** 营位预约与物资租赁主单 数据访问。 */
@Mapper
public interface BookingMapper extends BaseMapper<Booking> {
}
