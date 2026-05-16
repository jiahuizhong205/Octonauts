package com.campushub.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("biz_order")
public class BizOrder {

    @TableId(value = "order_id", type = IdType.ASSIGN_ID)
    private Long orderId;

    @TableField("req_id")
    private Long reqId;

    @TableField("receiver_id")
    private Long receiverId;

    private BigDecimal amount;

    private String status;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField("finished_at")
    private LocalDateTime finishedAt;
}
