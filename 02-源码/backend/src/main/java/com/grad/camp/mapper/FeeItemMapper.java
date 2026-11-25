package com.grad.camp.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.grad.camp.entity.FeeItem;
import org.apache.ibatis.annotations.Mapper;

/** 费用明细 数据访问。 */
@Mapper
public interface FeeItemMapper extends BaseMapper<FeeItem> {
}
