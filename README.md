# Intern Register Management System

A comprehensive, full-stack web application designed to replace a manual, paper-based intern registration, attendance tracking, and leave management process.

## 🚀 Features

- **User Authentication**: JWT-based authentication with role-based access control (Admin, Supervisor, and Intern).
- **Attendance Management**: Sign-in/out tracking with geolocation verification.
- **Leave Request System**: Submit, track, approve, and reject leave requests.
- **Reporting**: Generate professional reports in PDF and Excel format.
- **Security & Quality**: Built-in rate limiting, password strength validation, and secure email verification.

## ⚙️ System Overview
The system is divided into two primary components:
1. **Backend API**: A RESTful Spring Boot service managing the database (MySQL), business logic, and security.
2. **Frontend client**: An interactive Angular-based user interface consumed by admins, supervisors, and interns.

## 🛠️ Tech Stack

### Backend
- Spring Boot 3.5.7
- Java 17
- MySQL Database (support for PostgreSQL)
- Spring Security with JWT
- Hibernate/JPA

### Frontend
- Angular 20
- Bootstrap 5
- RxJS
- Leaflet.js (for geolocation mapping)
- StompJS (WebSockets for real-time updates)

## 📋 Prerequisites

- Java 17 or higher
- MySQL 8.0 or higher
- Maven 3.6+
- Node.js (v18+) and npm

## ⚙️ Setup & Installation

### 1. Database Setup

1. Create a MySQL database named `internregister`:
```sql
CREATE DATABASE internregister;
```

2. Copy `application.properties.template` to `application.properties`:
```bash
cp src/main/resources/application.properties.template src/main/resources/application.properties
```

3. Update `application.properties` with your database credentials:
```properties
spring.datasource.username=root
spring.datasource.password=YOUR_PASSWORD
```

### 2. Backend Setup

1. Build the project using the Maven Wrapper:
```bash
./mvnw clean install
```

2. Start the Spring Boot backend:
```bash
./mvnw spring-boot:run
```
The API server will run at `http://localhost:8082`.

### 3. Frontend Setup

1. Navigate to the frontend directory:
```bash
cd Univen-intern-Register-frontend
```

2. Install the package dependencies:
```bash
npm install
```

3. Start the Angular development server:
```bash
npm start
```
Access the application at `http://localhost:4200`.

## 🔒 Default Users

After first startup, the system seeds default accounts for convenience:

- **Admin**: `admin@univen.ac.za` / `admin123`
- **Supervisor**: `supervisor@univen.ac.za` / `supervisor123`
- **Intern**: `intern@univen.ac.za` / `intern123`

> [!WARNING]
> Please change these credentials upon deployment to production.

## 🧪 Testing

Run backend tests using Maven:
```bash
./mvnw test
```

## 📚 Documentation
Detailed documentation is located in the frontend's `docs/` directory:
- [System Requirements Specification (SRS.md)](file:///c:/Users/kulani.baloyi/Downloads/Intern%20Register%20final%20%281%29/Intern%20Register%20final/Univen-intern-Register-frontend/docs/SRS.md)
- [Technical Architecture (ARCHITECTURE.md)](file:///c:/Users/kulani.baloyi/Downloads/Intern%20Register%20final%20%281%29/Intern%20Register%20final/Univen-intern-Register-frontend/docs/ARCHITECTURE.md)
- [API Reference Guide (API_REFERENCE.md)](file:///c:/Users/kulani.baloyi/Downloads/Intern%20Register%20final%20%281%29/Intern%20Register%20final/Univen-intern-Register-frontend/docs/API_REFERENCE.md)

## 👥 Contributors

- Kulani Baloyi
