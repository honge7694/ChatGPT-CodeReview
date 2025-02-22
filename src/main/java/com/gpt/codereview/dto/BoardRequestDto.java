package com.gpt.codereview.dto;

import lombok.Data;

@Data
public class BoardRequestDto {
    private long id;
    private String title;
    private String content;
    private String author;
}
