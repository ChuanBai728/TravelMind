package com.travelmind.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 行程实体
 */
@Data
@TableName("itinerary")
public class ItineraryEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long sessionId;

    private Long requestId;

    private Integer version;

    private String title;

    private String itineraryJson;

    private String markdownContent;

    private String validationStatus;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
