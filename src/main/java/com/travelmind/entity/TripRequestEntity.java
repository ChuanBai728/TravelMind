package com.travelmind.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 旅行需求实体
 */
@Data
@TableName("trip_request")
public class TripRequestEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long sessionId;

    private String rawInput;

    private String destination;

    private Integer durationDays;

    private LocalDate startDate;

    private Integer peopleCount;

    private String budgetLevel;

    private String paceLevel;

    private String transportMode;

    private String preferencesJson;

    private String hotelArea;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
