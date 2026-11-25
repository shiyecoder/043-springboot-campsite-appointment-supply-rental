package com.grad.camp.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.grad.camp.entity.CheckoutRecord;
import org.apache.ibatis.annotations.Mapper;

/** 退营验收与结算 数据访问。 */
@Mapper
public interface CheckoutRecordMapper extends BaseMapper<CheckoutRecord> {
}
