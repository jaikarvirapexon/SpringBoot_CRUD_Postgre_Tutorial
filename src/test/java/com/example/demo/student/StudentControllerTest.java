package com.example.demo.student;

import com.example.demo.school.SchoolClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.Month;
import java.util.Optional;

import static org.hamcrest.Matchers.not;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(StudentController.class)
class StudentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private StudentService studentService;

    @MockBean
    private SchoolClient schoolClient;

    @Test
    void getStudentById_idExists_returns200WithStudentBody() throws Exception {
        // Arrange
        Student student = new Student(1L, "Mariam", "mariam.jamal@gmail.com",
                LocalDate.of(2000, Month.JANUARY, 5));
        given(studentService.getStudentById(1L)).willReturn(Optional.of(student));

        // Act & Assert
        mockMvc.perform(get("/api/v1/student/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Mariam"))
                .andExpect(jsonPath("$.email").value("mariam.jamal@gmail.com"))
                .andExpect(jsonPath("$.dob").value("2000-01-05"));
    }

    @Test
    void getStudentById_idAbsent_returns404NotAnUnhandled500() throws Exception {
        // Arrange
        given(studentService.getStudentById(999L)).willReturn(Optional.empty());

        // Act & Assert
        mockMvc.perform(get("/api/v1/student/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getStudentById_nonNumericPathVariable_returns400() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/v1/student/abc"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getStudentById_negativeOrZeroId_returns404NotBadRequest() throws Exception {
        // Arrange
        given(studentService.getStudentById(-1L)).willReturn(Optional.empty());
        given(studentService.getStudentById(0L)).willReturn(Optional.empty());

        // Act & Assert
        mockMvc.perform(get("/api/v1/student/-1"))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/student/0"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getStudentById_idAbsent_responseBodyContainsNoStackTraceOrInternalIdentifier() throws Exception {
        // Arrange
        given(studentService.getStudentById(999L)).willReturn(Optional.empty());

        // Act & Assert
        mockMvc.perform(get("/api/v1/student/999"))
                .andExpect(status().isNotFound())
                .andExpect(content().string(not(org.hamcrest.Matchers.containsString("Exception"))))
                .andExpect(content().string(not(org.hamcrest.Matchers.containsString("\tat "))));
    }
}
