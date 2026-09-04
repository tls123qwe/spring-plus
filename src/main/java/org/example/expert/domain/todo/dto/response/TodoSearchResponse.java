package org.example.expert.domain.todo.dto.response;

public record TodoSearchResponse(
        Long todoId,
        String title,
        Long managerCount,
        Long commentCount
) { }
