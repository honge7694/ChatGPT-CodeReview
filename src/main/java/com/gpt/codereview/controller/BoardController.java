package com.gpt.codereview.controller;

import com.gpt.codereview.dto.BoardRequestDto;
import com.gpt.codereview.dto.BoardResponseDto;
import com.gpt.codereview.service.BoardService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/board")
@RequiredArgsConstructor
public class BoardController {

    private final BoardService boardService;

    @GetMapping("/list")
    public List<BoardResponseDto> getBoardList() {
        return boardService.getBoardList();
    }

    @PostMapping("/")
    public BoardResponseDto saveBoard(@RequestBody BoardRequestDto boardRequestDto) {
        return boardService.saveBoard(boardRequestDto);
    }
}
