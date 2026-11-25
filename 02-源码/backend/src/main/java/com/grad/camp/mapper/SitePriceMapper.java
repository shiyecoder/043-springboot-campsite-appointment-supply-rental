package com.grad.camp.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.grad.camp.entity.SitePrice;
import org.apache.ibatis.annotations.Mapper;

/** 特殊日期价格 数据访问。 */
@Mapper
public interface SitePriceMapper extends BaseMapper<SitePrice> {
}
