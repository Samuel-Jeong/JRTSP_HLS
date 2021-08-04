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

  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
