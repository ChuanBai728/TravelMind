package com.travelmind.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 旅行会话实体
 */
@Data
@TableName("travel_session")
public class TravelSessionEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String sessionName;

    private String status;

    private Long currentItineraryId;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
