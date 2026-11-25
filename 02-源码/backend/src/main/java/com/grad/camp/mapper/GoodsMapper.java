package com.grad.camp.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.grad.camp.entity.Goods;
import org.apache.ibatis.annotations.Mapper;

/** 租赁物资 数据访问。 */
@Mapper
public interface GoodsMapper extends BaseMapper<Goods> {
}
