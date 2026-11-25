package com.grad.camp.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.grad.camp.entity.Review;
import org.apache.ibatis.annotations.Mapper;

/** 服务评价 数据访问。 */
@Mapper
public interface ReviewMapper extends BaseMapper<Review> {
}
