# TRAVEL HUB

## 프로젝트 소개

여행 정보 탐색과 일정 관리를 지원하는 웹 서비스의 백엔드 팀 프로젝트입니다.

Java/Spring Boot 기반으로 개발했으며, 저는 지도 기능과 커뮤니티 기능을 담당했습니다.

## 프로젝트 기술 스택

- Java
- Spring Boot
- Spring Data JPA
- MySQL
- Gradle
- Kakao API
- Tour API

## 담당 기능

### 지도 기능

- Kakao API를 활용한 위치 및 좌표 관련 기능 구현
- 외부 API 응답을 백엔드 서비스에서 처리하도록 연동

### 커뮤니티 기능

- 게시글 및 댓글 관련 기능 개발
- 댓글 수정, 삭제, 좋아요 기능 구현

## 프로젝트 종료 후 개인 개선

팀 프로젝트 종료 후 기존 코드를 다시 검토하며 코드 품질과 설정 관리 측면에서 개선 작업을 진행했습니다.

- API Key, DB 비밀번호 등 민감 설정 파일을 Git 추적에서 제외
- 실제 설정값 대신 환경변수를 사용하는 `application-example.yml` 추가
- Kakao 좌표 API의 빈 응답으로 발생할 수 있는 예외 처리
- 댓글 API에서 URL의 게시글 ID와 실제 댓글이 속한 게시글의 일치 여부 검증
- Gradle 빌드 결과물 및 IntelliJ 설정 파일을 Git 추적 대상에서 제외

## 실행 설정

보안을 위해 실제 API Key와 데이터베이스 정보가 포함된 `application.yml`은 저장소에 포함하지 않습니다.

`src/main/resources/application-example.yml`을 참고하여 필요한 환경변수를 설정해야 합니다.