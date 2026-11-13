package com.hexawarre.employee.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hexawarre.employee.exception.DuplicateEmailException;
import com.hexawarre.employee.exception.ResourceNotFoundException;
import com.hexawarre.employee.model.Employee;
import com.hexawarre.employee.service.EmployeeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(EmployeeController.class)
class EmployeeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private EmployeeService employeeService;

    private Employee employee;

    @BeforeEach
    void setUp() {
        employee = new Employee(1L, "Nagendra", "Sharma", "nagendra@hexawarre.com",
                "Engineering", new BigDecimal("50000.00"));
    }

    @Test
    @DisplayName("POST /api/employees -> 201 Created with saved employee")
    void createEmployee_returns201() throws Exception {
        given(employeeService.createEmployee(any(Employee.class))).willReturn(employee);

        mockMvc.perform(post("/api/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(employee)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.email").value("nagendra@hexawarre.com"));
    }

    @Test
    @DisplayName("POST /api/employees with invalid body -> 400 with field errors and service not called")
    void createEmployee_withInvalidBody_returns400() throws Exception {
        Employee invalid = new Employee(null, "", "Sharma", "not-an-email", "Engineering", new BigDecimal("-1"));

        mockMvc.perform(post("/api/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.errors.firstName").value("First name is required"))
                .andExpect(jsonPath("$.errors.email").value("Email must be valid"))
                .andExpect(jsonPath("$.errors.salary").value("Salary must be greater than zero"));

        verify(employeeService, never()).createEmployee(any(Employee.class));
    }

    @Test
    @DisplayName("POST /api/employees with existing email -> 409 Conflict")
    void createEmployee_withDuplicateEmail_returns409() throws Exception {
        given(employeeService.createEmployee(any(Employee.class)))
                .willThrow(new DuplicateEmailException("Employee already exists with email: nagendra@hexawarre.com"));

        mockMvc.perform(post("/api/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(employee)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    @DisplayName("GET /api/employees/{id} -> 200 with employee")
    void getEmployee_returns200() throws Exception {
        given(employeeService.getEmployeeById(1L)).willReturn(employee);

        mockMvc.perform(get("/api/employees/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Nagendra"))
                .andExpect(jsonPath("$.salary").value(50000.00));
    }

    @Test
    @DisplayName("GET /api/employees/{id} -> 404 when not found")
    void getEmployee_whenMissing_returns404() throws Exception {
        given(employeeService.getEmployeeById(99L))
                .willThrow(new ResourceNotFoundException("Employee not found with id: 99"));

        mockMvc.perform(get("/api/employees/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Employee not found with id: 99"));
    }

    @Test
    @DisplayName("GET /api/employees -> 200 with all employees")
    void getAllEmployees_returns200() throws Exception {
        Employee second = new Employee(2L, "Asha", "Verma", "asha@hexawarre.com", "HR", new BigDecimal("40000.00"));
        given(employeeService.getAllEmployees()).willReturn(List.of(employee, second));

        mockMvc.perform(get("/api/employees"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[1].firstName").value("Asha"));
    }

    @Test
    @DisplayName("GET /api/employees?department=HR -> filters by department")
    void getEmployeesByDepartment_returns200() throws Exception {
        given(employeeService.getEmployeesByDepartment("Engineering")).willReturn(List.of(employee));

        mockMvc.perform(get("/api/employees").param("department", "Engineering"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].department").value("Engineering"));

        verify(employeeService, never()).getAllEmployees();
    }

    @Test
    @DisplayName("PUT /api/employees/{id} -> 200 with updated employee")
    void updateEmployee_returns200() throws Exception {
        Employee updated = new Employee(1L, "Nagendra", "Kumar", "nagendra@hexawarre.com",
                "Platform", new BigDecimal("60000.00"));
        given(employeeService.updateEmployee(eq(1L), any(Employee.class))).willReturn(updated);

        mockMvc.perform(put("/api/employees/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updated)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lastName").value("Kumar"))
                .andExpect(jsonPath("$.department").value("Platform"));
    }

    @Test
    @DisplayName("DELETE /api/employees/{id} -> 204 No Content")
    void deleteEmployee_returns204() throws Exception {
        mockMvc.perform(delete("/api/employees/1"))
                .andExpect(status().isNoContent());

        verify(employeeService).deleteEmployee(1L);
    }

    @Test
    @DisplayName("DELETE /api/employees/{id} -> 404 when not found")
    void deleteEmployee_whenMissing_returns404() throws Exception {
        doThrow(new ResourceNotFoundException("Employee not found with id: 99"))
                .when(employeeService).deleteEmployee(99L);

        mockMvc.perform(delete("/api/employees/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("PATCH /api/employees/{id}/hike?percent=10 -> 200 with new salary")
    void applyHike_returns200() throws Exception {
        Employee hiked = new Employee(1L, "Nagendra", "Sharma", "nagendra@hexawarre.com",
                "Engineering", new BigDecimal("55000.00"));
        given(employeeService.applySalaryHike(1L, 10.0)).willReturn(hiked);

        mockMvc.perform(patch("/api/employees/1/hike").param("percent", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.salary").value(55000.00));
    }

    @Test
    @DisplayName("PATCH /api/employees/{id}/hike with invalid percent -> 400")
    void applyHike_withInvalidPercent_returns400() throws Exception {
        given(employeeService.applySalaryHike(1L, 80.0))
                .willThrow(new IllegalArgumentException("Hike percent must be greater than 0 and at most 50"));

        mockMvc.perform(patch("/api/employees/1/hike").param("percent", "80"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Hike percent must be greater than 0 and at most 50"));
    }
}
