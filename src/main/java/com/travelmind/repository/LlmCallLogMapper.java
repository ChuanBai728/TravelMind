package com.travelmind.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.travelmind.entity.LlmCallLogEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * LLM 调用日志 Mapper
 */
@Mapper
public interface LlmCallLogMapper extends BaseMapper<LlmCallLogEntity> {
}
