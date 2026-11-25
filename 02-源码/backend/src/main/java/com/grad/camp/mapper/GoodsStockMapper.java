package com.grad.camp.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.grad.camp.entity.GoodsStock;
import org.apache.ibatis.annotations.Mapper;

/** 物资日库存 数据访问。 */
@Mapper
public interface GoodsStockMapper extends BaseMapper<GoodsStock> {
}
