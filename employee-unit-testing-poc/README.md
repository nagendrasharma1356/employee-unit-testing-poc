# Employee Management: Unit Testing POC (JUnit 5 + Mockito)

A small Spring Boot 3 Employee Management project whose purpose is to show, step by step, how unit tests are written with **JUnit 5** and **Mockito** at every layer: service, controller and repository.

- Java 21 (change `java.version` in `pom.xml` to 17 if your IntelliJ uses JDK 17)
- Spring Boot 3.3.5, Spring Web, Spring Data JPA, Bean Validation
- H2 in-memory database
- JUnit 5, Mockito, AssertJ, MockMvc (all come with `spring-boot-starter-test`)
- JaCoCo for the coverage report

## Project structure

```
src/main/java/com/hexawarre/employee
  EmployeeApplication.java
  model/Employee.java                      entity + validation rules
  repository/EmployeeRepository.java       Spring Data JPA repository
  service/EmployeeService.java             business logic (what we unit test with Mockito)
  controller/EmployeeController.java       REST APIs
  exception/                               custom exceptions + GlobalExceptionHandler

src/test/java/com/hexawarre/employee
  service/EmployeeServiceTest.java         JUnit 5 + Mockito (no Spring, no DB)
  controller/EmployeeControllerTest.java   @WebMvcTest + MockMvc + @MockBean
  repository/EmployeeRepositoryTest.java   @DataJpaTest with H2
  EmployeeApplicationTests.java            context loads smoke test
```

## Step 1: Open the project in IntelliJ

1. Unzip `employee-unit-testing-poc.zip`.
2. IntelliJ: File > Open > select the `employee-unit-testing-poc` folder (the one with `pom.xml`).
3. Wait for Maven to download dependencies (internet needed the first time).
4. Set the Project SDK to JDK 21 (File > Project Structure > Project).

## Step 2: Run the tests

**From IntelliJ**
- Right-click `src/test/java` > Run 'All Tests', or right-click any single test class or method > Run.

**From terminal**
```
mvn test
```

Run only one class:
```
mvn -Dtest=EmployeeServiceTest test
```

**Coverage report** (created after `mvn test`): open `target/site/jacoco/index.html` in a browser.

## Step 3: What each test class teaches

### 1. EmployeeServiceTest: a pure Mockito unit test
No Spring context and no database. The repository is faked with a mock, so only the service logic is tested.

| Concept | Where to see it |
|---|---|
| `@ExtendWith(MockitoExtension.class)` | top of the class: enables Mockito annotations |
| `@Mock` | `EmployeeRepository` is replaced by a fake |
| `@InjectMocks` | creates the real `EmployeeService` and injects the mock into it |
| `given(...).willReturn(...)` | tell the mock what to return (BDD style: Given, When, Then) |
| `willAnswer(inv -> inv.getArgument(0))` | return whatever object was passed in |
| `verify(mock).save(...)` | check a method was called |
| `verify(mock, never())` | check a method was NOT called |
| `verifyNoInteractions(mock)` | check the mock was not touched at all |
| `ArgumentCaptor` | capture the object passed to `save()` and assert on its fields |
| `assertThatThrownBy(...)` | AssertJ way to test exceptions |
| `@ParameterizedTest` + `@CsvSource` / `@ValueSource` | one test method, many inputs (salary hike rule) |
| `@DisplayName` | readable test names in the IntelliJ run window |

### 2. EmployeeControllerTest: web layer test
`@WebMvcTest` loads only the controller, JSON conversion, validation and the exception handler. `EmployeeService` is a `@MockBean`.

- `MockMvc.perform(...)` calls the API without starting a server.
- Checks the HTTP status (201, 200, 204, 400, 404, 409) and the JSON body with `jsonPath`.
- Shows that invalid input returns 400 with field errors and that the service is never called.

### 3. EmployeeRepositoryTest: JPA test
`@DataJpaTest` starts only the JPA layer with embedded H2. It tests the derived queries (`findByEmail`, `existsByEmail`, `findByDepartmentIgnoreCase`). Each test is rolled back automatically.

### 4. EmployeeApplicationTests
One `@SpringBootTest` that only checks the full application context starts.

## Test naming convention used

```
methodName_whenCondition_expectedResult
createEmployee_whenEmailExists_throwsException
```

## Business rules covered by the tests

- Email must be unique (create and update).
- Updating with the same email is allowed (no uniqueness check).
- Salary hike must be greater than 0 and at most 50 percent. The result is rounded to 2 decimals.
- Missing employee returns 404, duplicate email returns 409, invalid input returns 400.

## Optional: run the application and try the APIs

```
mvn spring-boot:run
```

H2 console: http://localhost:8080/h2-console (JDBC URL `jdbc:h2:mem:employeedb`, user `sa`, empty password)

| Method | URL | Description |
|---|---|---|
| POST | /api/employees | create employee |
| GET | /api/employees | list all employees |
| GET | /api/employees?department=Engineering | filter by department |
| GET | /api/employees/{id} | get one employee |
| PUT | /api/employees/{id} | update employee |
| DELETE | /api/employees/{id} | delete employee |
| PATCH | /api/employees/{id}/hike?percent=10 | apply salary hike |

Sample create request body:
```json
{
  "firstName": "Nagendra",
  "lastName": "Sharma",
  "email": "nagendra@hexawarre.com",
  "department": "Engineering",
  "salary": 50000.00
}
```

## Quick Mockito cheat sheet

```java
// stub
given(repo.findById(1L)).willReturn(Optional.of(emp));
given(repo.findById(99L)).willReturn(Optional.empty());
given(repo.save(any(Employee.class))).willAnswer(inv -> inv.getArgument(0));
doThrow(new RuntimeException()).when(repo).deleteById(1L);   // for void methods

// verify
verify(repo).save(emp);               // called once
verify(repo, times(2)).save(emp);     // called twice
verify(repo, never()).save(any());    // never called
verifyNoInteractions(repo);           // mock not used at all

// capture
ArgumentCaptor<Employee> captor = ArgumentCaptor.forClass(Employee.class);
verify(repo).save(captor.capture());
captor.getValue();
```
