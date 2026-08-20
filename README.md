<div align="center">

# DAYBIT 🌈

### 하루를 기록하고, 색·질문·경험·이미지로 다시 돌아보는 AI 일기 서비스

**멋쟁이사자처럼 14기 중앙 해커톤 · Team 오늘날씨맑음**

<br />

![Java](https://img.shields.io/badge/Java-17-007396?logo=openjdk\&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.1.0-6DB33F?logo=springboot\&logoColor=white)
![MySQL](https://img.shields.io/badge/MySQL-Production-4479A1?logo=mysql\&logoColor=white)
![AWS](https://img.shields.io/badge/AWS-EC2%20%7C%20S3-232F3E?logo=amazonwebservices\&logoColor=white)
![GitHub Actions](https://img.shields.io/badge/GitHub%20Actions-CI%2FCD-2088FF?logo=githubactions\&logoColor=white)
![OpenAI](https://img.shields.io/badge/OpenAI-AI%20Pipeline-412991?logo=openai\&logoColor=white)

<br />

**[서비스 바로가기](https://www.daybit.cloud)** ·
**[Backend Repository](https://github.com/ww123ok/mutsa-14th-aac-backend)**

</div>

---

## 🌤️ DAYBIT

**DAYBIT = DAY + BIT, 하루의 조각**

사용자가 부담 없이 하루를 기록하고, 기록을 바탕으로 **오늘의 색과 성찰 질문**을 받으며, 비슷한 경험을 가진 다른 사용자와 **경험조각**으로 연결되고, 한 주의 기록을 **AI 이미지 보상**으로 다시 받아볼 수 있는 일기 서비스입니다.

> **기록 → 성찰 → 연결 → 시각화**

---

## ✨ Key Features

| 영역                      | 주요 기능                                                  |
| ----------------------- | ------------------------------------------------------ |
| **Diary**               | 하루 1개 일기, 임시저장, 추가 기록, 숨김, 휴지통, 복원                     |
| **DAYBIT Time**         | 사용자별 `dayStartTime`을 기준으로 논리적 하루 계산 및 미완성 Draft 자동완료   |
| **AI Writing Help**     | 작성 전·작성 중 Context를 구분한 AI 작성 도움 질문                     |
| **Daily Reward**        | 일기 기반 색상, 키워드, 색 코멘트 생성                                |
| **Reflection**          | 일기 맥락을 활용한 성찰 질문 생성 및 답변 저장                            |
| **User Memory**         | 사용자 동의 기반 개인화 기억 후보 추출·검토·중복 방지                        |
| **Experience Fragment** | 익명화, Embedding, 유사 경험 매칭, Inbox 전달, Credit 및 피드백       |
| **Weekly Reward**       | 주간 기록 집계 → Visual Plan → AI 이미지 생성·검수 → S3 저장          |
| **Notification**        | 일기 리마인더, 경험조각 도착·피드백, 주간 보상 완료 알림                      |
| **Auth & Security**     | Kakao OAuth2, 이메일 로그인, JWT HttpOnly Cookie, CSRF, CORS |

---

## 🤖 AI Pipeline

### Daily

```text
Diary Complete
    ↓
Daily Color / Keywords / Comment
    ↓
Reflection Question
    ↓
Memory Candidate
```

AI 작업은 일기 작성 요청과 분리하여 비동기로 처리합니다.

### Experience Fragment

```text
Diary
  ↓
Experience Structure
  ↓
Anonymization
  ↓
User Review
  ↓
Embedding & Similarity Matching
  ↓
Inbox
  ↓
Receive with Credit
```

원문을 다른 사용자에게 직접 노출하지 않고, 사용자가 승인한 익명화 경험만 공유 대상으로 사용합니다.

### Weekly Reward

```text
Weekly Diaries + Daily Colors
          ↓
      Visual Plan
          ↓
     Image Prompt
          ↓
 OpenAI Image Generation
          ↓
   Quality Validation
      ↙        ↘
   Retry       PASS
                ↓
              S3
                ↓
     Title / Summary / Keywords
```

이미지 품질 검수와 Image API의 일시적 `429 / 5xx / network error` 재시도를 분리해 관리하고, 최종 이미지는 S3에 저장한 뒤 Presigned URL로 제공합니다.

---

## 🏗️ Architecture

```text
src/main/java/mutsa/hackathon
├── config/          # Security, CORS, CSRF, Async, Swagger
├── domain/          # JPA Entity / Enum
├── dto/             # Request / Response DTO
├── presentation/    # REST Controller
├── repository/      # Spring Data JPA
├── security/        # JWT / OAuth2
├── service/         # Business Logic / AI / Scheduler
├── global/          # Common Response / Exception
└── util/            # Date / Cookie / Hash Utility
```

주요 설계 특징:

* 사용자별 `dayStartTime` 기반 논리 날짜
* AI 외부 호출 비동기 처리
* 일기 Soft Delete 및 관련 FK 생명주기 관리
* 사용자 동의 기반 AI 개인화
* Embedding 기반 경험 유사도 매칭
* AI fallback / retry / quality validation
* S3 Private Object + Presigned URL
* Scheduler 기반 자동완료 및 주간 보상 복구

---

## 🛠 Tech Stack

| Category  | Technology                          |
| --------- | ----------------------------------- |
| Language  | Java 17                             |
| Framework | Spring Boot 4.1.0                   |
| Web       | Spring MVC                          |
| ORM       | Spring Data JPA                     |
| Security  | Spring Security, OAuth2, JWT        |
| Database  | H2(Local), MySQL(Production)        |
| AI        | OpenAI Text / Embedding / Image API |
| Storage   | AWS S3                              |
| Server    | AWS EC2                             |
| API Docs  | Springdoc OpenAPI / Swagger         |
| Build     | Gradle                              |
| CI/CD     | GitHub Actions                      |

---

## 📡 API Overview

| Domain              | Path                                             |
| ------------------- | ------------------------------------------------ |
| Auth                | `/api/auth/**`, `/api/logout`                    |
| User                | `/api/me`                                        |
| Diary               | `/api/v1/diaries/**`                             |
| Draft               | `/api/v1/diaries/draft/**`                       |
| Writing Help        | `/api/v1/ai/writing-help/**`                     |
| User Memory         | `/api/v1/diaries/{diaryId}/memory-candidates/**` |
| Experience Fragment | `/api/v1/experience-fragments/**`                |
| Notification        | `/api/v1/notifications/**`                       |
| Weekly Reward       | `/api/v1/weekly-rewards/**`                      |

로컬 Swagger:

```text
http://localhost:8080/swagger-ui/index.html
```

---

## 🚀 Local Run

### 1. Clone

```bash
git clone https://github.com/ww123ok/mutsa-14th-aac-backend.git
cd mutsa-14th-aac-backend
```

### 2. Secret 설정

로컬 실행에 필요한 Kakao OAuth2 / JWT 등의 Secret은 **환경변수 또는 Git에 포함되지 않는 로컬 설정**으로 주입합니다.

예시:

```bash
export APP_JWT_SECRET="your-jwt-secret"

export SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_KAKAO_CLIENT_ID="your-client-id"
export SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_KAKAO_CLIENT_SECRET="your-client-secret"
```

OpenAI 기능을 사용할 경우:

```bash
export OPENAI_API_KEY="your-openai-api-key"
```

> 실제 API Key, JWT Secret, OAuth Client Secret, DB Password는 저장소에 Commit하지 않습니다.

### 3. Run

```bash
./gradlew bootRun
```

Windows PowerShell / CMD:

```bash
gradlew.bat bootRun
```

기본 Local DB는 H2를 사용합니다.

---

## 🧪 Test

전체 테스트:

```bash
./gradlew clean test
```

PR 전 기본 검증:

```bash
git diff --check
./gradlew compileJava
./gradlew clean test
```

실제 OpenAI / AWS API를 사용하는 Live Test는 일반 CI와 분리합니다.

---

## 🔄 CI/CD

### Pull Request → CI

`main` 대상 PR 생성 시:

```text
PR
 ↓
GitHub Actions
 ↓
JDK 17
 ↓
./gradlew clean build
```

테스트용 Dummy Secret을 사용하며 실제 운영 Secret은 CI에 노출하지 않습니다.

### Merge to `main` → Deploy

```text
main Merge
    ↓
GitHub Actions
    ↓
Gradle Build
    ↓
JAR → AWS EC2
    ↓
deploy.sh
    ↓
Spring Boot Server
```

---

# 🌿 Git & GitHub Convention

자세한 규칙은 [`docs/GITHUB_CONVENTION.md`](docs/GITHUB_CONVENTION.md)를 따릅니다.

## Branch Convention

기능 단위로 브랜치를 분리합니다.

```text
feat/{기능}
fix/{문제}
refactor/{대상}
test/{대상}
docs/{대상}
chore/{작업}
```

예시:

```text
feat/weekly-reward
fix/weekly-image-generation-stability
refactor/diary-service
docs/readme
```

브랜치 생성 전:

```bash
git checkout main
git pull origin main
```

으로 `main`을 최신화합니다.

---

## 💬 Commit Convention

커밋 메시지는 반드시 다음 형식을 사용합니다.

```text
[Type] 변경 내용
```

| Type         | 설명             |
| ------------ | -------------- |
| `[Feat]`     | 새로운 기능         |
| `[Fix]`      | 버그 수정          |
| `[Refactor]` | 동작 변경 없는 구조 개선 |
| `[Test]`     | 테스트 추가 / 수정    |
| `[Docs]`     | 문서 변경          |
| `[Chore]`    | 개발 환경 / 협업 설정  |
| `[Config]`   | 애플리케이션 / 배포 설정 |

예시:

```text
[Feat] 색 보상 키워드 생성 기능 추가
[Fix] 주간 이미지 생성 실패 안정성 보완
[Test] 주간 이미지 생성 실패 회귀 테스트 추가
[Docs] DAYBIT 백엔드 README 작성
```

### Commit Rule

* 하나의 Commit에는 가능한 한 **하나의 의미 있는 변경**을 담습니다.
* 변경 목적을 알 수 없는 Commit 메시지는 사용하지 않습니다.

```text
❌ 수정
❌ 최종
❌ 진짜 최종
❌ 오류 수정

✅ [Fix] AI 외부 호출 트랜잭션 분리
✅ [Test] 색 보상 금지 색상 검증 추가
```

---

## Pull Request

PR 제목도 Commit과 같은 Type 형식을 사용합니다.

```text
[Feat] 색 보상 키워드 구조 개편
[Fix] 주간 이미지 생성 실패 안정성 보완
```

PR에는 필요한 경우 다음 내용을 기록합니다.

* Issue Number
* 변경 사항
* API 변경
* As-Is / To-Be
* 설계 결정
* 프론트엔드 / DB / 환경변수 영향
* 실제 실행한 검증
* 제외 범위
* 리뷰 반영 내용

### Merge Rule

다음을 확인한 뒤 `main`에 병합합니다.

1. CI 성공
2. 관련 테스트 성공
3. `git diff` 확인
4. Secret 포함 여부 확인
5. API / DB 영향 확인
6. Review Comment 해결

> 가능하면 `main`에 직접 Push하지 않고 **Pull Request를 통해 병합**합니다.

---

## 🔒 Secret Policy

다음 정보는 Git에 Commit하지 않습니다.

```text
API Key
JWT Secret
OAuth Client Secret
DB Password
AWS Access Key / Secret Key
Private Key
```

Local에서는 Git에 포함되지 않는 Secret 설정을 사용하고, 배포 환경에서는 환경변수 또는 GitHub Secrets를 사용합니다.

---

<div align="center">

### 오늘의 기록이, 다시 돌아보고 싶은 하나의 조각이 되도록.

**DAYBIT**

🌐 https://www.daybit.cloud
🗂️ https://github.com/ww123ok/mutsa-14th-aac-backend

</div>
