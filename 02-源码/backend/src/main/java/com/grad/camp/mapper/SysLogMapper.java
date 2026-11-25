package com.grad.camp.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.grad.camp.entity.SysLog;
import org.apache.ibatis.annotations.Mapper;

/** 操作日志 数据访问。 */
@Mapper
public interface SysLogMapper extends BaseMapper<SysLog> {
}
