package com.campushub.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("biz_evaluation")
public class BizEvaluation {

    @TableId(value = "eval_id", type = IdType.ASSIGN_ID)
    private Long evalId;

    @TableField("order_id")
    private Long orderId;

    @TableField("reviewer_id")
    private Long reviewerId;

    @TableField("target_id")
    private Long targetId;

    private Integer star;

    private String content;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
