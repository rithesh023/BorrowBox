package com.borrowbox;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import org.springframework.core.io.ByteArrayResource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.datasource.url=jdbc:h2:mem:borrowbox-test;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "app.jwt.secret=borrowbox-integration-test-secret-32-bytes-minimum",
        "app.ai.api-key=",
        "app.upload.directory=target/test-uploads"
})
@ActiveProfiles("test")
class BorrowboxApplicationTests {
    @Autowired TestRestTemplate http;

    @Test
    void healthAndCompleteStudentRentalFlowWork() {
        ResponseEntity<Map<String, Object>> health = http.exchange("/api/health", HttpMethod.GET, null,
                new ParameterizedTypeReference<>() {});
        assertThat(health.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(health.getBody()).containsEntry("status", "ok");

        String ownerToken = registerAndLogin("owner@example.test", "Campus Owner", "strong-owner-password");
        String borrowerToken = registerAndLogin("borrower@example.test", "Campus Borrower", "strong-borrower-password");
        Map<String, Object> descriptionDraft = post("/api/ai/generate-description", borrowerToken,
                Map.of("name", "Test camera", "condition", "Good", "details", "Includes a lens cap"), Map.class, HttpStatus.OK);
        assertThat(descriptionDraft).containsEntry("aiEnhanced", false);
        assertThat(descriptionDraft.get("description").toString()).contains("Test camera", "Includes a lens cap");

        String imageUrl = uploadImage(ownerToken);
        ResponseEntity<byte[]> servedImage = http.getForEntity(imageUrl, byte[].class);
        assertThat(servedImage.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(servedImage.getHeaders().getContentType()).isEqualTo(MediaType.IMAGE_PNG);
        assertThat(servedImage.getBody()).containsExactly((byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a);

        Map<String, Object> listing = post("/api/items", ownerToken, Map.of(
                "category", "Cameras", "name", "Test camera", "description", "Camera for campus projects",
                "pricePerDay", new BigDecimal("300.00"), "securityDeposit", new BigDecimal("400.00"),
                "itemCondition", "Good", "imageUrl", imageUrl, "available", true), Map.class, HttpStatus.CREATED);
        Number itemId = (Number) listing.get("id");

        Map<String, Object> search = http.exchange("/api/items?q=Test%20camera&available=true", HttpMethod.GET, null,
                new ParameterizedTypeReference<Map<String, Object>>() {}).getBody();
        assertThat(search).containsKey("content");

        LocalDate start = LocalDate.now().plusDays(2);
        Map<String, Object> rental = post("/api/rentals", borrowerToken,
                Map.of("itemId", itemId.longValue(), "startDate", start.toString(), "endDate", start.plusDays(2).toString()),
                Map.class, HttpStatus.CREATED);
        Number rentalId = (Number) rental.get("id");
        assertThat(new BigDecimal(rental.get("rentalAmount").toString())).isEqualByComparingTo("900.00");
        assertThat(new BigDecimal(rental.get("totalAmount").toString())).isEqualByComparingTo("1300.00");

        put("/api/rentals/" + rentalId + "/accept", ownerToken, null, HttpStatus.OK);
        Map<String, Object> payment = post("/api/rentals/" + rentalId + "/pay", borrowerToken,
                Map.of("method", "UPI"), Map.class, HttpStatus.OK);
        assertThat(payment).containsEntry("status", "SUCCESS");
        put("/api/rentals/" + rentalId + "/activate", ownerToken, null, HttpStatus.OK);
        put("/api/rentals/" + rentalId + "/complete", borrowerToken, null, HttpStatus.OK);
        post("/api/reviews", borrowerToken, Map.of("rentalId", rentalId.longValue(), "rating", 5, "comment", "Worked well"), Map.class, HttpStatus.CREATED);

        ResponseEntity<String> forbidden = http.exchange("/api/admin/summary", HttpMethod.GET, headers(borrowerToken), String.class);
        assertThat(forbidden.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        ResponseEntity<String> recommendations = http.exchange("/api/recommendations/user/2", HttpMethod.GET, headers(borrowerToken), String.class);
        assertThat(recommendations.getStatusCode()).isEqualTo(HttpStatus.OK);

        Map<String, Object> disposable = post("/api/items", ownerToken, Map.of(
                "category", "Cameras", "name", "Disposable camera", "description", "Temporary listing",
                "pricePerDay", new BigDecimal("10.00"), "securityDeposit", BigDecimal.ZERO,
                "itemCondition", "Good", "imageUrl", "", "available", true), Map.class, HttpStatus.CREATED);
        ResponseEntity<Void> deleted = http.exchange("/api/items/" + disposable.get("id"), HttpMethod.DELETE, entity(ownerToken, null), Void.class);
        assertThat(deleted.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    private String uploadImage(String token) {
        byte[] pngSignature = {(byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a};
        ByteArrayResource file = new ByteArrayResource(pngSignature) {
            @Override public String getFilename() { return "test.png"; }
        };
        MultiValueMap<String, Object> parts = new LinkedMultiValueMap<>();
        parts.add("file", file);
        HttpHeaders headers = new HttpHeaders(); headers.setContentType(MediaType.MULTIPART_FORM_DATA); headers.setBearerAuth(token);
        ResponseEntity<Map<String, String>> response = http.exchange("/api/uploads", HttpMethod.POST,
                new HttpEntity<>(parts, headers), new ParameterizedTypeReference<>() {});
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return response.getBody().get("url");
    }

    private String registerAndLogin(String email, String name, String password) {
        post("/api/auth/register", null, Map.of("name", name, "email", email, "password", password), Map.class, HttpStatus.CREATED);
        Map<String, Object> auth = post("/api/auth/login", null, Map.of("email", email, "password", password), Map.class, HttpStatus.OK);
        return (String) auth.get("accessToken");
    }

    private <T> T post(String path, String token, Object body, Class<T> responseType, HttpStatus expected) {
        ResponseEntity<T> response = http.exchange(path, HttpMethod.POST, entity(token, body), responseType);
        assertThat(response.getStatusCode()).isEqualTo(expected);
        return response.getBody();
    }

    private void put(String path, String token, Object body, HttpStatus expected) {
        ResponseEntity<String> response = http.exchange(path, HttpMethod.PUT, entity(token, body), String.class);
        assertThat(response.getStatusCode()).isEqualTo(expected);
    }

    private HttpEntity<Object> headers(String token) {
        HttpHeaders headers = new HttpHeaders(); headers.setBearerAuth(token); return new HttpEntity<>(headers);
    }

    private HttpEntity<Object> entity(String token, Object body) {
        HttpHeaders headers = new HttpHeaders(); headers.setContentType(MediaType.APPLICATION_JSON);
        if (token != null) headers.setBearerAuth(token);
        return new HttpEntity<>(body, headers);
    }
}
