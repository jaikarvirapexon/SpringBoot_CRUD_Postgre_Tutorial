package com.example.demo.student;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.example.demo.school.SchoolClient;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping(path = "api/v1/student")
public class StudentController {
    private final StudentService studentService;
    private final SchoolClient schoolClient;

    @Autowired
    public StudentController(StudentService studentService, SchoolClient schoolClient) {
        this.studentService = studentService;
        this.schoolClient = schoolClient;
    }

    @GetMapping
    public List<Student> getStudent() {
        return studentService.getStudents();
    }

    @GetMapping(path = "{studentId}")
    public ResponseEntity<Student> getStudentById(@PathVariable("studentId") Long studentId) {
        Optional<Student> student = studentService.getStudentById(studentId);
        return student.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public void registerStudent(@RequestBody Student student) {
        studentService.addNewStudent(student);
    }

    @DeleteMapping(path = "{studentId}")
    public void deleteStudent(@PathVariable("studentId") Long studentId) {
        studentService.deleteStudent(studentId);
    }

    @PutMapping(path = "{studentId}")
    public void updateStudent(@PathVariable("studentId") Long studentId,
                              @RequestParam(required = false) String name,
                              @RequestParam(required = false) String email){
        studentService.updateStudent(studentId, name, email);
    }

    @GetMapping("/{studentId}/school/{schoolId}")
    public String getStudentSchool(@PathVariable Long studentId, @PathVariable Long schoolId) {

        String school = schoolClient.getSchool(schoolId);

        return "Student " + studentId + " belongs to " + school;
    }
}
