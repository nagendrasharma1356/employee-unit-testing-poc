package com.hexawarre.employee.dto;

import lombok.Data;

@Data
public class EmployeeResponse {

    private Long id;
    private String firstName;
    private String lastName;
    private String email;
    private String department;

}