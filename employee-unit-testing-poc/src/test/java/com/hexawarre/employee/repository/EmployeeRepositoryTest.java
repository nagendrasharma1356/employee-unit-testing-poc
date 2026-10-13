package com.hexawarre.employee.repository;

import com.hexawarre.employee.model.Employee;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class EmployeeRepositoryTest {

    @Autowired
    private EmployeeRepository employeeRepository;

    @BeforeEach
    void setUp() {
        employeeRepository.save(new Employee(null, "Nagendra", "Sharma", "nagendra@hexawarre.com",
                "Engineering", new BigDecimal("50000.00")));
        employeeRepository.save(new Employee(null, "Asha", "Verma", "asha@hexawarre.com",
                "HR", new BigDecimal("40000.00")));
        employeeRepository.save(new Employee(null, "Ravi", "Singh", "ravi@hexawarre.com",
                "Engineering", new BigDecimal("45000.00")));
    }

    @Test
    @DisplayName("save: generates an id")
    void save_generatesId() {
        Employee saved = employeeRepository.save(new Employee(null, "Neha", "Gupta", "neha@hexawarre.com",
                "Finance", new BigDecimal("42000.00")));

        assertThat(saved.getId()).isNotNull();
    }

    @Test
    @DisplayName("findByEmail: returns employee for existing email")
    void findByEmail_whenExists_returnsEmployee() {
        Optional<Employee> result = employeeRepository.findByEmail("asha@hexawarre.com");

        assertThat(result).isPresent();
        assertThat(result.get().getFirstName()).isEqualTo("Asha");
    }

    @Test
    @DisplayName("findByEmail: returns empty for unknown email")
    void findByEmail_whenMissing_returnsEmpty() {
        assertThat(employeeRepository.findByEmail("nobody@hexawarre.com")).isEmpty();
    }

    @Test
    @DisplayName("existsByEmail: true for existing, false for unknown")
    void existsByEmail_works() {
        assertThat(employeeRepository.existsByEmail("ravi@hexawarre.com")).isTrue();
        assertThat(employeeRepository.existsByEmail("nobody@hexawarre.com")).isFalse();
    }

    @Test
    @DisplayName("findByDepartmentIgnoreCase: matches regardless of case")
    void findByDepartmentIgnoreCase_returnsMatches() {
        List<Employee> result = employeeRepository.findByDepartmentIgnoreCase("engineering");

        assertThat(result).hasSize(2)
                .extracting(Employee::getFirstName)
                .containsExactlyInAnyOrder("Nagendra", "Ravi");
    }

    @Test
    @DisplayName("deleteById: removes the employee")
    void deleteById_removesEmployee() {
        Long id = employeeRepository.findByEmail("asha@hexawarre.com").orElseThrow().getId();

        employeeRepository.deleteById(id);

        assertThat(employeeRepository.findById(id)).isEmpty();
        assertThat(employeeRepository.count()).isEqualTo(2);
    }
}
