# RTSP (Real Time Streaming Protocol) Study
- Based RFC-2326
- https://datatracker.ietf.org/doc/html/rfc2326
  
## 1. Definition
- 직접 RTP 를 전송하기 위한 프로토콜이 아니다.
- SIP 처럼 미디어 세션(연속적인 미디어 스트림)을 관리하는 프로토콜이다.
- 미디어 스트림은 주로 SDP 로 정의된다.
- RTSP Connection 은 TCP, UDP 둘 다 가능하다.
- RTSP Connection 식별은 서버에서 지정한 식별자(identifier)로 가능하다.
- 서버와 클라이언트 모두 요청 보낼 수 있다.
- HTTP 는 Stateless 이지만, RTSP 는 Statefull 이다.
- Request-URI 는 Absoulte URI 이다. > HTTP/1.1 버전과의 하위 호환성(Backward compatibility) 때문이다.
  
## 2. Terminology
### 1) Aggregate control (다중 제어)
- 서버에서 단일 타임라인을 사용하는 다중 스트림을 제어
- 클라이언트에서 동시 재생되는 오디오와 비디오에 대한 제어를 동시에 수행
  
### 2) Conference
- 하나 이상의 미디어 집합
  
### 3) Client
- 미디어 서버로부터 연속적인 미디어 데이터를 수신하는 주체
  
### 4) Connection
- 두 개의 프로그램들이 통신을 목적으로 생성된 가상 순환 전송 계층
  
### 5) Container file
- 여러 미디어 스트림들이 포함되어 있는 파일
- RTSP 서버는 이 파일에 다중 제어를 제공할 수 있다.
- Container file 은 RTSP 에 종속되지 않는다.
  
### 6) Continuous media
- 소스와 싱크 사이의 타이밍 관계를 가지는 데이터
- 싱크는 소스에 존재하는 타이밍 관계를 계속해서 생성한다.
- 가장 좋은 예는 오디오와 모션 비디오이다.
- Real-time(강하게 적용), streaming(덜 강하게 적용) 둘 다 될 수 있다.
  
### 7) Entity
- 요청과 응답 메시지의 페이로드로 전송되는 정보
- Entity 는 Header 와 Body 로 구성된다.
- Header 는 Meta-Info, Body 는 Content 가 저장된다.
  
### 8) Media initialization
- 구체적인 데이터 유형 또는 코덱 초기 설정
- Clock-rate, color tables 등과 같은 정보들도 설정한다.
- 미디어 스트림 재생을 위해 클라이언트가 필요로 하는 독립적인 전송 정보는 미디어 스트림 설정 단계의 미디어 초기 설정에서 발생한다.
  
### 9) Media parameter
- 미디어 스트림 재생 중 또는 재생 전에 변경되는 구체적인 미디어 유형을 결졍하는 매개변수이다.
  
### 10) Media server
- 하나 또는 여러 미디어 스트림에 대한 미디어 재생과 녹취 서비스를 제공하는 서버이다.
- 하나의 presentation 내에 존재하는 서로 다른 미디어 스트림들은 서로 다른 미디어 서버들로부터 생성된다.
- 하나의 미디어 서버는 웹 서버처럼 같거나 다른 호스트에 존재할 수 있따.
  
### 11) Media server indirection
- 다른 미디어 서버로 미디어 클라이언트를 재설정(redirection)한다.
  
### 12) (Media) stream
- 단일 미디어 인스턴스이다.
- 하나의 화이트보트 또는 공유 어플리케이션 그룹으로서 하나의 오디오 또는 비디어 스트림이다.
- RTP 를 사용하는 경우, 미디어 스트림은 지정한 RTP 세션 내에는 RTP 와 RTCP 패킷만 존재한다.
- DSM-CC 스트림의 정의와 동일하다.
  
### 13) Message
- RTSP 통신에 가장 기본적인 단위이다.
- Section 15 에 정의된 syntax 와 매칭되는 연속적인 옥텟 데이터로 구성되어 있다.
- TCP(connection protocol) 또는 UDP(connectionless protocol) 를 통해 전송된다.
  
### 14) Participant
- Conference 의 구성원
- 미디어 녹취 또는 재생 서버가 하나의 구성원이 될 수 있다. (machine 이라고 불린다.)
  
### 15) Presentation
- 아래 정의된 presentation description 을 사용한 완성된 미디어 정보이다.
- 클라이언트에게 보여주는 하나 또는 그 이상의 미디어 스트림의 집합이다.
- RTSP context 에서의 대부분의 경우에 미디어 스트림들의 다중 제어 정보를 제공하지만, 필수 정보는 아니다.
  
### 16) Presentation description
- 하나의 presentation 내에 존재하는 하나 또는 그 이상의 미디어 스트림에 대한 정보이다.
- Content 에 대한 정보 네트워크 주소, 인코딩에 대한 정보 집합으로 구성된다.
- SDP 와 같은 다른 IETF 프로토콜에서는 실시간 presentation 을 "session" 이라는 단어로 사용된다.
- 서로 다른 유형을 가질 수 있는데, SDP 를 포함할 수는 있지만, SDP 에 국한되지 않는다.
  
### 17) Response
- RTSP 응답 메시지이다.
- HTTP 요청이 선행되면, 반드시 전송되어야 한다.
  
### 18) Request
- RTSP 요청 메시지이다.
- HTTP 요청이 선행되면, 반드시 전송되어야 한다.
  
### 19) RTSP session
- 완전한 RTSP 트랜잭션이다.
- 연속적인 미디어 스트림을 위한 전송 메커니즘 설정하는 클라이언트로 구성되어 있다.
- PLAY 또는 RECORD 명령으로 시작으로 미디어 스트림을 시작시키고, TEARDOWN 명령으로 미디어 스트림을 종료시킨다.
  
### 20) Transport initialization
- 클라이언트와 서버 사이의 전송 정보를 협상하는 것이다.
- 포트 번호 또는 전송 프로토콜(TCP, UDP)을 협상한다.
  
## 3. RTSP 특징
### 1) 확장 가능 : 새로운 method 와 매개변수를 쉽게 추가할 수 있다.
### 2) 쉽게 파싱 가능 : 표준 HTTP 또는 MIME 파서로 쉽게 파싱할 수 있다.
### 3) 높은 보안성 : 웹 보안 메커니즘을 재사용한다. 모든 HTTP 인증 메커니즘(RFC 2068), 암호 메커니즘(RFC 2069)을 직접 적용할 수 있다. L3, L4 계층 보안 메키니즘도 재사용한다.
### 4) 독립적인 전송 방법 : L3 신뢰성(TCP)을 잘 사용하지 않는다. L7 에서 전송 신뢰성을 보장하도록 한다. 그래서 UDP 또는 RDP(RFC 1151) 를 사용한다.
### 5) 다중 서버 허용 : 하나의 presentation (session) 안에 있는 각각의 미디어 스트림은 각기 다른 미디어 서버에서 관리될 수 있다. 미디어 동기화는 L3 에서 수행된다.
### 6) 녹취 제어 가능 : 재생과 녹취 둘 다 제어 가능하다. (VCR mode)
### 7) 








  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
