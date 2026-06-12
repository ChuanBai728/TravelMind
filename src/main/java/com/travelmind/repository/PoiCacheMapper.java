package com.travelmind.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.travelmind.entity.PoiCacheEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * POI 缓存 Mapper
 */
@Mapper
public interface PoiCacheMapper extends BaseMapper<PoiCacheEntity> {
}
