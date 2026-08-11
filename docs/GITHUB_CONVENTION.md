# DAYBIT GitHub Convention

DAYBIT 백엔드에서 사용하는 Git / GitHub 협업 규칙입니다.

> 목표는 단순히 형식을 맞추는 것이 아니라, **변경 목적과 검증 결과를 팀원이 쉽게 이해하고 안전하게 리뷰 및 병합할 수 있도록 하는 것**입니다.

---

## 1. Branch Convention
기능 단위로 브랜치를 분리합니다.

```text
feat/{기능}
fix/{문제}
refactor/{대상}
test/{대상}
docs/{대상}
chore/{작업}
```

**[예시]**
- `feat/reward-keywords`
- `feat/form-auth`
- `feat/weekly-reward`
- `fix/ai-transaction-boundary`
- `refactor/diary-service`
- `chore/github-collaboration`

---

## 2. Commit Convention
커밋 메시지는 다음 형식을 사용합니다.

```text
[Type] 변경 내용
```

### Type
- **`[Feat]`** : 새로운 기능
- **`[Fix]`** : 버그 수정
- **`[Refactor]`** : 동작 변경 없는 구조 개선
- **`[Test]`** : 테스트 추가/수정
- **`[Docs]`** : 문서 변경
- **`[Chore]`** : 개발 환경/협업 설정 등의 작업
- **`[Config]`** : 애플리케이션/배포 설정 변경

**[예시]**
- `[Feat] 색 보상 키워드 생성 기능 추가`
- `[Fix] AI 외부 호출 트랜잭션 분리`
- `[Test] 색 보상 금지 색상 검증 추가`
- `[Docs] 백엔드 API 명세 업데이트`
- `[Chore] GitHub PR 템플릿 추가`

> 💡 **주의:** 가능하면 하나의 커밋에는 하나의 의미 있는 변경을 담습니다. 단순히 다음과 같은 메시지는 사용하지 않습니다.
> ❌ `수정`, `최종`, `진짜 최종`, `오류 수정`, `테스트`

---

## 3. Pull Request Title
PR 제목도 Commit Convention과 같은 Type을 사용합니다.

**[예시]**
- `[Feat] 색 보상 키워드 구조 개편`
- `[Fix] AI 외부 호출 트랜잭션 분리`

---

## 4. Pull Request Description
PR만 보고도 다음 내용을 이해할 수 있어야 합니다.

- [ ] 무엇이 바뀌었는가
- [ ] 왜 바꾸었는가
- [ ] API가 어떻게 바뀌었는가
- [ ] 프론트엔드 / DB / 환경변수 영향이 있는가
- [ ] 어떤 테스트를 실행했는가
- [ ] 이번 PR에서 의도적으로 하지 않은 것은 무엇인가

**작성 가이드**
- 구조 변경이 큰 경우 **As-Is / To-Be**를 작성합니다.
- API 변경이 있다면 **Method와 Path**를 명시합니다.

**[API 변경 명시 예시]**
```http
GET /api/v1/ai/writing-help/status
POST /api/v1/ai/writing-help/questions
```

---

## 5. Test Evidence
PR의 검증 항목에는 **실제로 실행한 테스트만** 작성합니다.

**기본 확인**
```bash
git diff --check
./gradlew compileJava
./gradlew clean test
```

기능별로 필요한 단위 테스트와 통합 테스트를 추가로 실행합니다.

> ⚠️ OpenAI Live Test처럼 **실제 외부 API를 사용하는 테스트**는 일반 CI에서 자동으로 실행하지 않습니다. Live Test를 실행했다면 PR 설명에 별도로 기록합니다.

---

## 6. Code Review
리뷰는 코드 스타일뿐 아니라 다음 관점에서 확인합니다.

- **`[버그]`** : 정확성, 예외 상황, 동시성, 데이터 정합성
- **`[설계]`** : 책임 분리, 트랜잭션 경계, 의존 관계
- **`[성능]`** : 불필요한 쿼리, 반복 연산, 외부 API 호출
- **`[보안]`** : 인증/인가, 개인정보, Secret 노출
- **`[사소]`** : 이름, 중복 코드, 가독성

> 💡 **Tip:** 리뷰어는 가능하면 **문제가 발생하는 이유**와 **개선 방향**을 함께 설명합니다.

---

## 7. Review Response
리뷰를 수정했다면 단순히 *"수정했습니다."* 로 끝내지 않습니다.

**[좋은 예시]**
> 리뷰 감사합니다. 말씀해주신 대로 OpenAI 호출을 DB transaction 밖으로 분리했습니다.
> - `DiaryService`의 긴 transaction 제거
> - DB 저장을 `DiaryCreatePersistenceService`로 분리
> - transaction boundary 통합 테스트 추가
>
> 반영 commit: `abc1234`

리뷰가 여러 개라면 각각 어떤 방식으로 반영했는지 남깁니다.

---

## 8. Merge Rule
다음 조건을 확인한 뒤 `main`에 병합합니다.

1. CI 성공
2. 관련 테스트 성공
3. `git diff` 자체 검토
4. Secret 포함 여부 확인
5. API / DB 영향 확인
6. 리뷰 Comment 해결

> 💡 **원칙:** 가능하면 기능 브랜치에서 직접 `main`에 push하지 않고, **Pull Request를 통해 병합**합니다.

---

## 9. Secret Rule
다음 정보는 **Git에 Commit하지 않습니다.**

- 🚫 API Key
- 🚫 JWT Secret
- 🚫 OAuth Client Secret
- 🚫 DB Password
- 🚫 AWS Access Key / Secret Key
- 🚫 Private Key

로컬에서는 Git에 포함되지 않는 Secret 설정을 사용하고, 배포 환경에서는 환경변수 또는 GitHub Secrets를 사용합니다.

---

## 10. DAYBIT Development Flow

1. `main` 최신화
2. 기능 Branch 생성
3. 구현
4. 단위 테스트
5. 통합 / 회귀 테스트
6. `git diff` 검토
7. Commit
8. Push
9. Pull Request
10.  CI
11. Code Review
12. 수정 및 재검증
13. **Merge**