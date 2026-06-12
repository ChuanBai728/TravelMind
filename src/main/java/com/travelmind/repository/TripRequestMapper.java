package com.travelmind.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.travelmind.entity.TripRequestEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 旅行需求 Mapper
 */
@Mapper
public interface TripRequestMapper extends BaseMapper<TripRequestEntity> {
}
