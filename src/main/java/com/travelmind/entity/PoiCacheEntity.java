package com.travelmind.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * POI 缓存实体
 */
@Data
@TableName("poi_cache")
public class PoiCacheEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String source;

    private String sourcePoiId;

    private String name;

    private String city;

    private String address;

    private String location;

    private String category;

    private String rawJson;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
