package com.example.demo.student;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.Month;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class StudentServiceTest {

    @Mock
    private StudentRepository studentRepository;

    private StudentService studentService;

    @BeforeEach
    void setUp() {
        studentService = new StudentService(studentRepository);
    }

    @Test
    void getStudentById_idExists_returnsOptionalOfStudent() {
        // Arrange
        Student student = new Student(1L, "Mariam", "mariam.jamal@gmail.com",
                LocalDate.of(2000, Month.JANUARY, 5));
        given(studentRepository.findById(1L)).willReturn(Optional.of(student));

        // Act
        Optional<Student> result = studentService.getStudentById(1L);

        // Assert
        assertThat(result).isEqualTo(Optional.of(student));
    }

    @Test
    void getStudentById_idAbsent_returnsOptionalEmpty() {
        // Arrange
        given(studentRepository.findById(999L)).willReturn(Optional.empty());

        // Act
        Optional<Student> result = studentService.getStudentById(999L);

        // Assert
        assertThat(result).isEqualTo(Optional.empty());
    }
}
