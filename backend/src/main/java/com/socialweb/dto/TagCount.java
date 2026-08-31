package com.socialweb.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TagCount {

    private String name;
    private long count;
}
