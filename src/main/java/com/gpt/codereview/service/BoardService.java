package com.gpt.codereview.service;

import com.gpt.codereview.dto.BoardRequestDto;
import com.gpt.codereview.dto.BoardResponseDto;
import com.gpt.codereview.entity.Board;
import com.gpt.codereview.repository.BoardRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BoardService {

    private final BoardRepository boardRepository;

    public List<BoardResponseDto> getBoardList() {
        return boardRepository.findAll().stream().map(BoardResponseDto::new).collect(Collectors.toList());
    }

    public BoardResponseDto saveBoard(BoardRequestDto boardRequestDto) {
        return new BoardResponseDto(boardRepository.save(new Board(boardRequestDto)));
    }
}
