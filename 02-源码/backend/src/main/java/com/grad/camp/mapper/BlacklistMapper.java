package com.grad.camp.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.grad.camp.entity.Blacklist;
import org.apache.ibatis.annotations.Mapper;

/** 黑名单 数据访问。 */
@Mapper
public interface BlacklistMapper extends BaseMapper<Blacklist> {
}
