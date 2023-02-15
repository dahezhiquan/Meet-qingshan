package com.qingshan.dto;

import lombok.Data;

import java.util.List;

/**
 * 滚动分页返回结果实体
 */
@Data
public class ScrollResult {
    private List<?> list;
    private Long minTime;
    private Integer offset;
}
