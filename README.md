# Best Offer (경매 거래 시스템)

기존 고정가 기반 중고 거래 플랫폼의 한계를 개선하여, **Best Offer**는 경매 시스템을 통해 셀러와 구매자 모두 납득할 최적의 시장 가격을 찾아 희귀·한정판 물품의 진정한 가치를 발견할 수 있도록 돕습니다.

<br>

## 프로젝트 목표 (Project Goals)

### 1. 유연한 확장성 확보 (Scalability)
- 단순 기능 구현을 넘어, 추후 서비스 성장 시 Scale-out 및 유지보수가 용이한 시스템 구조를 설계합니다.

### 2. 철저한 동시성 제어 및 가용성 높은 트래픽 처리
- 유저들이 동시에 입찰하는 경합 상황에서도 데이터 정합성(Consistency)을 유지하며, 데드락 방지를 통해 안정성을 보장합니다.
- 경매 마감 직전 수많은 유저가 입찰 경쟁을 하는 트래픽 집중 상황을 최대한 안정적으로 해결해야 합니다.

### 3. 자동화 과정 강화
- CI/CD 파이프라인을 구축하여 테스트부터 AWS 배포까지 전 과정을 자동화함으로써, 휴먼 에러를 최소화하고 운영의 안정성을 높입니다.
- 시스템의 성능(TPS, 지표)을 실시간으로 모니터링하는 환경을 구축하여, 장애를 조기에 감지하고 대응할 수 있는 환경을 구축합니다.

<br>

## 사용 기술 (Tech Stack)

### **Backend**
- **Language:** Java 21
- **Framework:** Spring Boot 3.2.5
- **Data Access:** Spring Data JPA

### **Database & Cache**
- **Main DB:** MySQL
- **Cache/Global Lock:** Redis (Redisson)

### **Infrastructure & DevOps**
- **Cloud:** AWS (EC2, RDS)
- **Container:** Docker, Docker Compose
- **CI/CD:** GitHub Actions
- **Monitoring:** Prometheus, Grafana

### **Testing**
- **Unit/Integration Test:** JUnit5, Mockito

<br>

## 시스템 아키텍처
<img width="865" height="548" alt="Architecture" src="https://github.com/user-attachments/assets/d43a16ef-07f5-4c5d-83fd-6d029a8ea503" />

<br>

## 화면 구성 (초안)
<img width="857" height="922" alt="화면구성" src="https://github.com/user-attachments/assets/9c46dda7-1ad3-4c0b-8ab5-387077afba6d" />

<br>

## Documentation
추가적인 사항은 [프로젝트 위키(Wiki)](https://github.com/anjsk11/best-offer/wiki)에서 확인하실 수 있습니다.

