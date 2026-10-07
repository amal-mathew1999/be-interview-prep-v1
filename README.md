# be-interview-prep

Backend Interview Preparation — a Spring Boot project for practicing common backend engineering concepts.

## Tech Stack

- **Java 17**
- **Spring Boot 3.2**
- **Maven**
- **JUnit 5 + AssertJ** for testing

## Project Structure

```
src/
  main/
    java/com/interview/prep/
      InterviewPrepApplication.java    # Entry point
      controller/                      # REST controllers
      service/                         # Business logic
      repository/                      # Data access
      model/                           # Domain entities
      config/                          # Configuration classes
    resources/
      application.yml                  # App configuration
  test/
    java/com/interview/prep/
      InterviewPrepApplicationTest.java
```

## Getting Started

### Prerequisites
- Java 17+
- Maven 3.8+

### Run
```bash
mvn spring-boot:run
```

### Test
```bash
mvn test
```

The app starts on `http://localhost:8080`.

## Topics Covered

- REST API design
- Data structures & algorithms
- Database operations (JPA/Hibernate)
- Caching, pagination, error handling
- Authentication & authorization
- System design patterns
