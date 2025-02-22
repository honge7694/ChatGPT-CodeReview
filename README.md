# GIT WEBHOOKS + OPEN AI를 이용한 코드 리뷰 프로젝트
GitHub Webhook과 OpenAI(ChatGPT)를 활용하여 자동 코드 리뷰를 수행하는 프로젝트입니다.

## 프로젝트 개요

### 동작 확인
1. pull Requests를 요청
2. GIT WEBHOOKS가 변경 사항 감지
3. CHAT GPT가 변경 사항을 분석하고 리뷰 생성
4. GitHub Pr 페이지에서 리뷰 확인

### 주요 기술 스택

- Java 17 + Spring Boot
- GitHub Webhooks API
- OpenAI API
- Ngrok (로컬 개발 환경에서 Webhook 사용)

## 실행 방법

### ✅ 1. ngrok을 사용하여 포트 포워딩 설정

1. ngrok 설치
ngrok을 사용하여 로컬 서버를 외부(GitHub Webhook)에서도 접속할 수 있도록 터널링해줍니다.    
[ngrok 공식 사이트](https://ngrok.com/)에서 다운로드 및 설치합니다.    

2. ngrok 토큰 생성
[ngrok 토큰 생성](https://dashboard.ngrok.com/get-started/your-authtoken)에서 토큰을 생성하여 저장합니다.

3. ngrok 토큰 발급
설치한 파일이 있는 곳으로 이동 후 다음의 명령어를 실행해줍니다.
   - windows
       - `.\ngrok.exe config add-authtoken {토큰번호}`
   - Mac & Linux
       - `ngrok config add-authtoken {토큰번호}`

4. ngrok 실행
   - windows
       - `.\ngrok.exe http 8080`
   - Mac & Linux
       - `./ngrok http 8080`

```
Forwarding https://e147-1-220-56-227.ngrok-free.app -> http://localhost:8080 
```
실행 후, Forwarding(https://e147-1-220-56-227.ngrok-free.app)이 나타내는 주소를 사용하면 된다.

### ✅ 2. GitHub Webhook 설정

1. GitHub Repository → Settings → Webhooks → Add webhook
2. Payload URL : https://e147-1-220-56-227.ngrok-free.app/api/webhook/ 작성
3. Content type : application/json
4. Webhook 이벤트 Let me select individual events. → pull request reviews, pull request 선택

### ✅ 3. Github Token 발급
1. [Github 토큰 발급](https://github.com/settings/personal-access-tokens)에 접속하여 Generate new token 클릭
2. Repository access에서 Only select repositories로 repository 선택
3. Repository permissions에서 Read access to metadata, Read and Write access to pull requests and security events 선택

### ✅ 4. OpenAI API 발급
1. [OpenAI API 발급](https://platform.openai.com/settings/organization/api-keys)에 접속하여 API 발급
2. [Token 결제](https://platform.openai.com/settings/organization/billing/overview)에서 Token을 결제해야 사용 가능

### ✅ 5. Github Token, OpenAI API 등록 후 프로젝트 실행
1. 각각의 값을 등록한 후 프로젝트 실행
2. pull Request 생성 후 확인

TEST4