package com.example.demo.student;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.demo.school.SchoolClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class StudentServiceIT {

      @LocalServerPort
    private int port;

    @Value("${school.client.base-url}")
    private String schoolClientBaseUrl;

    // @Test
    // void shouldGetStudentSchool() {

    //     RestClient client = RestClient.create();

    //     String response = client.get()
    //             .uri("http://localhost:" + port +
    //                     "/api/v1/student/1/school/1")
    //             .retrieve()
    //             .body(String.class);

    //         assertTrue(response.contains("Student 1 belongs to"));
    //         assertTrue(response.contains("Lincoln High School"));
    // }

    @Test
    void shouldGetStudentSchool() {

        RestClient client = RestClient.builder()
                .requestFactory(new JdkClientHttpRequestFactory(SchoolClient.trustAllHttpClient()))
                .build();

        String response = client.get()
                .uri(schoolClientBaseUrl + "/todos/1")
                .retrieve()
                .body(String.class);

        assertTrue(response.contains("\"id\": 1"));
        assertTrue(response.contains("\"userId\": 1"));
    }
}
