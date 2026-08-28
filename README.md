# SPRING PLUS

## Level 1
### 1.코드 개선 퀴즈 _ @Transactional의 이해
 - 오류 코드 내용을 확인하면 read-only라는 용어를 확인
 - TodoService를 확인하니 공통 어노테이션에 @Transactional이 read-only로 묶여 있음을 확인
 - 해당 서비스에 조회 로직만 있으면 상관없으나 생성 로직도 있으니 수정
 - 생성 로직에는 @Transactional만 조회 로직은 read-only까지