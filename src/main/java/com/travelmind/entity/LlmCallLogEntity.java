package com.travelmind.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * LLM 调用日志实体
 */
@Data
@TableName("llm_call_log")
public class LlmCallLogEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long sessionId;

    private String provider;

    private String model;

    private String callType;

    private Integer promptTokens;

    private Integer completionTokens;

    private Integer latencyMs;

    private String status;

    private String errorMessage;

    private String requestJson;

    private String responseJson;

    private LocalDateTime createdAt;
}
