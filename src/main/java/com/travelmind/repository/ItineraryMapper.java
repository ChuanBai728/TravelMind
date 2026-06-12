package com.travelmind.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.travelmind.entity.ItineraryEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 行程 Mapper
 */
@Mapper
public interface ItineraryMapper extends BaseMapper<ItineraryEntity> {
}
