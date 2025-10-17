package com.example.application;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * セキュリティ関連のテスト
 *
 * CORS設定、セキュリティヘッダーなどを検証します。
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
class TutorialControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TutorialRepository repository;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
    }

    // ===== CORS設定のテスト =====

    @Test
    @DisplayName("CORS - 許可されたオリジンからのリクエストを受け入れる")
    void api_shouldAcceptCorsFromAllowedOrigin() throws Exception {
        // Given: 許可されたオリジン
        String allowedOrigin = "http://localhost:4200";

        // When & Then: 許可されたオリジンからのリクエストは成功
        mockMvc.perform(get("/api/tutorials")
                .header("Origin", allowedOrigin))
            .andExpect(status().isOk())
            .andExpect(header().string("Access-Control-Allow-Origin", allowedOrigin));
    }

    @Test
    @DisplayName("CORS - プリフライトリクエスト（OPTIONS）の処理")
    void api_shouldHandlePreflightRequest() throws Exception {
        // Given: プリフライトリクエスト
        String origin = "http://localhost:4200";

        // When & Then: OPTIONSリクエストが正しく処理される
        mockMvc.perform(options("/api/tutorials")
                .header("Origin", origin)
                .header("Access-Control-Request-Method", "POST")
                .header("Access-Control-Request-Headers", "content-type"))
            .andExpect(status().isOk())
            .andExpect(header().exists("Access-Control-Allow-Origin"))
            .andExpect(header().exists("Access-Control-Allow-Methods"));
    }

    @Test
    @DisplayName("CORS - 未許可のオリジンからのリクエスト")
    void api_shouldRejectCorsFromUnknownOrigin() throws Exception {
        // Given: 未許可のオリジン
        String unauthorizedOrigin = "http://malicious-site.com";

        // When & Then: 未許可のオリジンからのリクエストはCORSヘッダーが付与されない
        mockMvc.perform(get("/api/tutorials")
                .header("Origin", unauthorizedOrigin))
            .andExpect(status().isOk()) // APIは動作するが...
            .andExpect(header().doesNotExist("Access-Control-Allow-Origin")); // CORSヘッダーがない
    }

    // ===== セキュリティヘッダーのテスト =====

    @Test
    @DisplayName("セキュリティヘッダー - Content-Typeが正しく設定される")
    void api_shouldReturnCorrectContentType() throws Exception {
        // When & Then: JSONレスポンスには適切なContent-Typeが設定される
        repository.save(new Tutorial("Test", "Desc", false));

        mockMvc.perform(get("/api/tutorials"))
            .andExpect(status().isOk())
            .andExpect(content().contentType("application/json"));
    }

    // 注意: 以下のテストはセキュリティ設定を追加した場合に有効化
    // @Test
    // @DisplayName("セキュリティヘッダー - X-Content-Type-Optionsが設定される")
    // void api_shouldIncludeXContentTypeOptions() throws Exception {
    //     mockMvc.perform(get("/api/tutorials"))
    //         .andExpect(header().string("X-Content-Type-Options", "nosniff"));
    // }

    // @Test
    // @DisplayName("セキュリティヘッダー - X-Frame-Optionsが設定される")
    // void api_shouldIncludeXFrameOptions() throws Exception {
    //     mockMvc.perform(get("/api/tutorials"))
    //         .andExpect(header().string("X-Frame-Options", "DENY"));
    // }
}
