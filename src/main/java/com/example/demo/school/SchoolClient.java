package com.example.demo.school;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.net.http.HttpClient;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;

@Service
public class SchoolClient {

    private final RestClient restClient;

    public SchoolClient(RestClient.Builder builder,
                         @Value("${school.client.base-url}") String baseUrl) {
        // this.restClient = builder
        //         .baseUrl("http://localhost:8081")
        //         .build();

          this.restClient = builder
                .baseUrl(baseUrl)
                .requestFactory(new JdkClientHttpRequestFactory(trustAllHttpClient()))
                .build();

               // https://jsonplaceholder.typicode.com/todos/1
    }

    // Corporate proxy MITMs TLS with a cert that doesn't chain to a trusted root
    // (PKIX path building failed). Trust-all is a temporary workaround, not for production.
    public static HttpClient trustAllHttpClient() {
        try {
            TrustManager[] trustAllCerts = new TrustManager[] {
                    new X509TrustManager() {
                        public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
                        public void checkClientTrusted(X509Certificate[] certs, String authType) {}
                        public void checkServerTrusted(X509Certificate[] certs, String authType) {}
                    }
            };
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustAllCerts, new SecureRandom());

            SSLParameters sslParameters = new SSLParameters();
            sslParameters.setEndpointIdentificationAlgorithm("");

            return HttpClient.newBuilder()
                    .sslContext(sslContext)
                    .sslParameters(sslParameters)
                    .build();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Failed to configure trust-all HttpClient", e);
        }
    }

    public String getSchool(Long id) {
        // return restClient
        //         .get()
        //         .uri("/api/v1/school/{id}", id)
        //         .retrieve()
        //         .body(String.class);

           return restClient
                .get()
                 .uri("/todos/{id}", id)
                 .retrieve()
                 .body(String.class);
    }
}
