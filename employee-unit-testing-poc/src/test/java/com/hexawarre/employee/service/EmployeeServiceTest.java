package com.hexawarre.employee.service;

import com.hexawarre.employee.exception.DuplicateEmailException;
import com.hexawarre.employee.exception.ResourceNotFoundException;
import com.hexawarre.employee.model.Employee;
import com.hexawarre.employee.repository.EmployeeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class EmployeeServiceTest {

    @Mock
    private EmployeeRepository employeeRepository;

    @InjectMocks
    private EmployeeService employeeService;

    private Employee employee;

    @BeforeEach
    void setUp() {
        employee = new Employee(1L, "Nagendra", "Sharma", "nagendra@hexawarre.com",
                "Engineering", new BigDecimal("50000.00"));
    }

    // ---------------------------------------------------------------- create

    @Test
    @DisplayName("createEmployee: saves and returns employee when email is new")
    void createEmployee_whenEmailIsNew_savesEmployee() {
        given(employeeRepository.existsByEmail(employee.getEmail())).willReturn(false);
        given(employeeRepository.save(employee)).willReturn(employee);

        Employee saved = employeeService.createEmployee(employee);

        assertThat(saved).isNotNull();
        assertThat(saved.getEmail()).isEqualTo("nagendra@hexawarre.com");
        verify(employeeRepository, times(1)).save(employee);
    }

    @Test
    @DisplayName("createEmployee: throws DuplicateEmailException and never saves when email exists")
    void createEmployee_whenEmailExists_throwsException() {
        given(employeeRepository.existsByEmail(employee.getEmail())).willReturn(true);

        assertThatThrownBy(() -> employeeService.createEmployee(employee))
                .isInstanceOf(DuplicateEmailException.class)
                .hasMessageContaining("nagendra@hexawarre.com");

        verify(employeeRepository, never()).save(any(Employee.class));
    }

    // ------------------------------------------------------------------ read

    @Test
    @DisplayName("getEmployeeById: returns employee when found")
    void getEmployeeById_whenFound_returnsEmployee() {
        given(employeeRepository.findById(1L)).willReturn(Optional.of(employee));

        Employee result = employeeService.getEmployeeById(1L);

        assertThat(result.getFirstName()).isEqualTo("Nagendra");
    }

    @Test
    @DisplayName("getEmployeeById: throws ResourceNotFoundException when missing")
    void getEmployeeById_whenMissing_throwsException() {
        given(employeeRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> employeeService.getEmployeeById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Employee not found with id: 99");
    }

    @Test
    @DisplayName("getAllEmployees: returns every employee from repository")
    void getAllEmployees_returnsList() {
        Employee second = new Employee(2L, "Asha", "Verma", "asha@hexawarre.com",
                "HR", new BigDecimal("40000.00"));
        given(employeeRepository.findAll()).willReturn(List.of(employee, second));

        List<Employee> result = employeeService.getAllEmployees();

        assertThat(result).hasSize(2).containsExactly(employee, second);
    }

    @Test
    @DisplayName("getEmployeesByDepartment: delegates to repository")
    void getEmployeesByDepartment_returnsMatchingEmployees() {
        given(employeeRepository.findByDepartmentIgnoreCase("engineering")).willReturn(List.of(employee));

        List<Employee> result = employeeService.getEmployeesByDepartment("engineering");

        assertThat(result).containsExactly(employee);
    }

    // ---------------------------------------------------------------- update

    @Test
    @DisplayName("updateEmployee: copies new values onto the existing employee (ArgumentCaptor)")
    void updateEmployee_whenValid_updatesFields() {
        Employee details = new Employee(null, "Nagendra", "Kumar", "nagendra@hexawarre.com",
                "Platform", new BigDecimal("60000.00"));
        given(employeeRepository.findById(1L)).willReturn(Optional.of(employee));
        given(employeeRepository.save(any(Employee.class))).willAnswer(inv -> inv.getArgument(0));

        employeeService.updateEmployee(1L, details);

        ArgumentCaptor<Employee> captor = ArgumentCaptor.forClass(Employee.class);
        verify(employeeRepository).save(captor.capture());
        Employee savedArg = captor.getValue();
        assertThat(savedArg.getId()).isEqualTo(1L);
        assertThat(savedArg.getLastName()).isEqualTo("Kumar");
        assertThat(savedArg.getDepartment()).isEqualTo("Platform");
        assertThat(savedArg.getSalary()).isEqualByComparingTo("60000.00");
    }

    @Test
    @DisplayName("updateEmployee: throws when employee does not exist")
    void updateEmployee_whenMissing_throwsException() {
        given(employeeRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> employeeService.updateEmployee(99L, employee))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(employeeRepository, never()).save(any(Employee.class));
    }

    @Test
    @DisplayName("updateEmployee: throws when new email already belongs to someone else")
    void updateEmployee_whenNewEmailTaken_throwsException() {
        Employee details = new Employee(null, "Nagendra", "Sharma", "taken@hexawarre.com",
                "Engineering", new BigDecimal("50000.00"));
        given(employeeRepository.findById(1L)).willReturn(Optional.of(employee));
        given(employeeRepository.existsByEmail("taken@hexawarre.com")).willReturn(true);

        assertThatThrownBy(() -> employeeService.updateEmployee(1L, details))
                .isInstanceOf(DuplicateEmailException.class);

        verify(employeeRepository, never()).save(any(Employee.class));
    }

    @Test
    @DisplayName("updateEmployee: does not check email uniqueness when email is unchanged")
    void updateEmployee_whenEmailUnchanged_skipsEmailCheck() {
        Employee details = new Employee(null, "Nagendra", "Sharma", "NAGENDRA@hexawarre.com",
                "Engineering", new BigDecimal("52000.00"));
        given(employeeRepository.findById(1L)).willReturn(Optional.of(employee));
        given(employeeRepository.save(any(Employee.class))).willAnswer(inv -> inv.getArgument(0));

        employeeService.updateEmployee(1L, details);

        verify(employeeRepository, never()).existsByEmail(any());
    }

    // ---------------------------------------------------------------- delete

    @Test
    @DisplayName("deleteEmployee: deletes when employee exists")
    void deleteEmployee_whenExists_deletes() {
        given(employeeRepository.existsById(1L)).willReturn(true);

        employeeService.deleteEmployee(1L);

        verify(employeeRepository).deleteById(1L);
    }

    @Test
    @DisplayName("deleteEmployee: throws and never deletes when employee is missing")
    void deleteEmployee_whenMissing_throwsException() {
        given(employeeRepository.existsById(99L)).willReturn(false);

        assertThatThrownBy(() -> employeeService.deleteEmployee(99L))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(employeeRepository, never()).deleteById(any());
    }

    // ------------------------------------------------------- salary hike rule

    @ParameterizedTest(name = "salary {0} with {1}% hike becomes {2}")
    @CsvSource({
            "50000.00, 10,   55000.00",
            "60000.00, 5,    63000.00",
            "40000.50, 12.5, 45000.56"
    })
    @DisplayName("applySalaryHike: calculates new salary correctly")
    void applySalaryHike_calculatesNewSalary(BigDecimal salary, double percent, BigDecimal expected) {
        employee.setSalary(salary);
        given(employeeRepository.findById(1L)).willReturn(Optional.of(employee));
        given(employeeRepository.save(any(Employee.class))).willAnswer(inv -> inv.getArgument(0));

        Employee result = employeeService.applySalaryHike(1L, percent);

        assertThat(result.getSalary()).isEqualByComparingTo(expected);
    }

    @ParameterizedTest(name = "hike of {0}% is rejected")
    @ValueSource(doubles = {0, -5, 50.01, 100})
    @DisplayName("applySalaryHike: rejects percent outside 0 (exclusive) to 50 (inclusive)")
    void applySalaryHike_withInvalidPercent_throwsException(double percent) {
        assertThatThrownBy(() -> employeeService.applySalaryHike(1L, percent))
                .isInstanceOf(IllegalArgumentException.class);

        // validation fails first, so the repository must not be touched at all
        verifyNoInteractions(employeeRepository);
    }

    @Test
    @DisplayName("applySalaryHike: throws when employee does not exist")
    void applySalaryHike_whenEmployeeMissing_throwsException() {
        given(employeeRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> employeeService.applySalaryHike(99L, 10))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
