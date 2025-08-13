# Data Collector using Smartwatch

---

## ⏱️ 스마트워치를 활용한 장시간 데이터 수집
<img src="https://github.com/user-attachments/assets/b76b1a97-c84d-4a30-912b-b5be63843aec">

---

## 📑 프로젝트 소개
### 👤 스마트워치 기반 장시간 센서 데이터 수집 애플리케이션
- 스마트워치와 스마트폰을 연동하여 **IMU 센서**와 **오디오 센서** 데이터를 동시 수집
- **수집 모드, 센서 샘플링 속도, 수집 시간**을 사용자 지정 가능
- **포그라운드 서비스**를 이용하여 화면이 꺼진 상태에서도 지속적인 데이터 수집 가능
- IMU와 오디오 데이터를 **타임스탬프 동기화** 후 `.csv` 파일 형태로 저장하여 후처리 용이

> **특징**  
> - 장시간 연속 데이터 수집에 최적화  
> - IMU(Accelerometer, Gyroscope, Rotation Vector)와 오디오(16kHz) 데이터의 동기화 처리  
> - 사용자 맞춤형 수집 환경 설정 지원  
> - 오프라인 환경에서도 안정적인 데이터 로깅 가능  

---

## 🛠 사용 기술 스택
- **Android** : Kotlin  
- **Data** : IMU 센서(Accelerometer, Gyroscope, Rotation Vector), Audio(16kHz)  
- **Processing** : 타임스탬프 기반 멀티모달 데이터 동기화, CSV 저장
- **Architecture** : MVVM, Foreground Service
