package com.gpt.codereview.dto;

import lombok.Data;

@Data
public class BoardRequestDto {
    private int id;
    private String title;
    private String content;
    private String author;
}
