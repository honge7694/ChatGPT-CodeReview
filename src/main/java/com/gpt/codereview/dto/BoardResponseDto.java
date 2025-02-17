package com.gpt.codereview.dto;

import com.gpt.codereview.controller.BoardController;
import com.gpt.codereview.entity.Board;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class BoardResponseDto {
    private long id;
    private String title;
    private String content;
    private String author;

    public BoardResponseDto(Board board) {
        this.id = board.getId();
        this.title = board.getTitle();
        this.content = board.getContent();
        this.author = board.getAuthor();
    }
}
