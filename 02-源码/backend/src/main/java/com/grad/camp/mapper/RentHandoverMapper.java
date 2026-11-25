package com.grad.camp.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.grad.camp.entity.RentHandover;
import org.apache.ibatis.annotations.Mapper;

/** 物资发放归还 数据访问。 */
@Mapper
public interface RentHandoverMapper extends BaseMapper<RentHandover> {
}
