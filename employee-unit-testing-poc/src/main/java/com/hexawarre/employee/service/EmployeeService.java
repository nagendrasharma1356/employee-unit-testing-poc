package com.hexawarre.employee.service;

import com.hexawarre.employee.exception.DuplicateEmailException;
import com.hexawarre.employee.exception.ResourceNotFoundException;
import com.hexawarre.employee.model.Employee;
import com.hexawarre.employee.repository.EmployeeRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
public class EmployeeService {

    private static final double MAX_HIKE_PERCENT = 50.0;

    private final EmployeeRepository employeeRepository;

    public EmployeeService(EmployeeRepository employeeRepository) {
        this.employeeRepository = employeeRepository;
    }

    public Employee createEmployee(Employee employee) {
        if (employeeRepository.existsByEmail(employee.getEmail())) {
            throw new DuplicateEmailException("Employee already exists with email: " + employee.getEmail());
        }
        return employeeRepository.save(employee);
    }

    public Employee getEmployeeById(Long id) {
        return employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with id: " + id));
    }

    public List<Employee> getAllEmployees() {
        return employeeRepository.findAll();
    }

    public List<Employee> getEmployeesByDepartment(String department) {
        return employeeRepository.findByDepartmentIgnoreCase(department);
    }

    public Employee updateEmployee(Long id, Employee details) {
        Employee existing = getEmployeeById(id);

        boolean emailChanged = !existing.getEmail().equalsIgnoreCase(details.getEmail());
        if (emailChanged && employeeRepository.existsByEmail(details.getEmail())) {
            throw new DuplicateEmailException("Employee already exists with email: " + details.getEmail());
        }

        existing.setFirstName(details.getFirstName());
        existing.setLastName(details.getLastName());
        existing.setEmail(details.getEmail());
        existing.setDepartment(details.getDepartment());
        existing.setSalary(details.getSalary());
        return employeeRepository.save(existing);
    }

    public void deleteEmployee(Long id) {
        if (!employeeRepository.existsById(id)) {
            throw new ResourceNotFoundException("Employee not found with id: " + id);
        }
        employeeRepository.deleteById(id);
    }


    public Employee applySalaryHike(Long id, double percent) {
        if (percent <= 0 || percent > MAX_HIKE_PERCENT) {
            throw new IllegalArgumentException("Hike percent must be greater than 0 and at most " + (int) MAX_HIKE_PERCENT);
        }

        Employee employee = getEmployeeById(id);
        BigDecimal factor = BigDecimal.ONE.add(BigDecimal.valueOf(percent).divide(BigDecimal.valueOf(100)));
        employee.setSalary(employee.getSalary().multiply(factor).setScale(2, RoundingMode.HALF_UP));
        return employeeRepository.save(employee);
    }
}
