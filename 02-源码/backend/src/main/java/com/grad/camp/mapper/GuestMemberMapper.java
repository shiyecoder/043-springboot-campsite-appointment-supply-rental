package com.grad.camp.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.grad.camp.entity.GuestMember;
import org.apache.ibatis.annotations.Mapper;

/** 同行人 数据访问。 */
@Mapper
public interface GuestMemberMapper extends BaseMapper<GuestMember> {
}
