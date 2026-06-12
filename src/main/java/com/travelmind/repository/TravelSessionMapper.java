package com.travelmind.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.travelmind.entity.TravelSessionEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 旅行会话 Mapper
 */
@Mapper
public interface TravelSessionMapper extends BaseMapper<TravelSessionEntity> {
}
