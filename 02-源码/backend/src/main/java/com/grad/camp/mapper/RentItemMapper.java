package com.grad.camp.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.grad.camp.entity.RentItem;
import org.apache.ibatis.annotations.Mapper;

/** 租赁明细 数据访问。 */
@Mapper
public interface RentItemMapper extends BaseMapper<RentItem> {
}
