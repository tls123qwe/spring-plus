package org.example.expert.domain.todo.repository;

import com.querydsl.core.types.Expression;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.example.expert.domain.comment.entity.QComment;
import org.example.expert.domain.manager.entity.QManager;
import org.example.expert.domain.todo.dto.response.TodoSearchResponse;
import org.example.expert.domain.todo.entity.QTodo;
import org.example.expert.domain.todo.entity.Todo;
import org.example.expert.domain.user.entity.QUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor

public class TodoRepositoryCustomImpl implements TodoRepositoryCustom {

    private final JPAQueryFactory jpaQueryFactory;

    @Override
    public Optional<Todo> findByIdWithUser(Long todoId) {

         QTodo todo = QTodo.todo;
        QUser user = QUser.user;

        Todo result = jpaQueryFactory
                .selectFrom(todo)
                .leftJoin(todo.user, user).fetchJoin()
                .where(todo.id.eq(todoId))
                .fetchOne();

        return Optional.ofNullable(result);
    }

    @Override
    public Page<TodoSearchResponse> searchTodos(String title, String managerNickname, Pageable pageable) {

        QTodo todo = QTodo.todo;
        QManager manager = QManager.manager;
        QComment comment = QComment.comment;

        // 담당자 수 계산용 쿼리
        Expression<Long> mangerCount = JPAExpressions
                .select(manager.count())
                .from(manager)
                .where(manager.todo.id.eq(todo.id));

        // 댓글 수 계산용 쿼리
        Expression<Long> commentCount = JPAExpressions
                .select(comment.count())
                .from(comment)
                .where(comment.todo.id.eq(todo.id));

        // 실제 목록 조회
        // 제목/담당자수/댓글수만 뽑아서 DTO로 맵핑
        List<TodoSearchResponse> content = jpaQueryFactory
                .select(Projections.constructor(TodoSearchResponse.class,
                        todo.id, todo.title, mangerCount, commentCount))
                .from(todo)
                .where(titleContains(title),                            // title이 없으면 Null(자동 무시)
                        managerNicknameEq(managerNickname, manager))    // nickname이 없으면 Null(자동 무시)
                .orderBy(todo.createdAt.desc())                         // 생성일 최신순
                .offset(pageable.getOffset())                           // 페이징: 시작 위치
                .limit(pageable.getPageSize())                          // 페이징: 페이지당 개수
                .fetch();

        // 전체 페이징 총 개수 계산용 쿼리
        JPAQuery<Long> countQuery = jpaQueryFactory
                .select(todo.count())
                .from(todo)
                .where(titleContains(title),
                        managerNicknameEq(managerNickname, manager));

        // content + count를 합쳐서 Page 객체로 반환
        return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchOne);
    }

    // 제목 부분 검색 조건
    private BooleanExpression titleContains(String title) {

        return StringUtils.hasText(title) ? QTodo.todo.title.contains(title) : null;
    }

    // 담당자 닉네임으로 검색
    private BooleanExpression managerNicknameEq(String nickname, QManager manager) {

        if (!StringUtils.hasText(nickname)) return null; // 값이 없다면 조건을 안걸리게

        return QTodo.todo.id.in(JPAExpressions
                .select(manager.todo.id)
                .from(manager)
                .where(manager.user.nickname.eq(nickname)));
    }

}
