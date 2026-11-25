package com.grad.camp.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.grad.camp.entity.Payment;
import org.apache.ibatis.annotations.Mapper;

/** 线下收款记录 数据访问。 */
@Mapper
public interface PaymentMapper extends BaseMapper<Payment> {
}
