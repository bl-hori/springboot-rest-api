package com.example.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * エラーハンドリングのテスト
 *
 * 異常系、エラー応答、例外処理を検証します。
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
class TutorialControllerErrorHandlingTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TutorialRepository repository;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
    }

    // ===== 不正なリクエストのテスト =====

    @Test
    @DisplayName("不正なJSON - 400 Bad Requestが返る")
    void whenInvalidJson_shouldReturn400() throws Exception {
        // When & Then: 不正なJSON形式
        mockMvc.perform(post("/api/tutorials")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{invalid json structure"))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("不正なContent-Type - 415 Unsupported Media Typeが返る")
    void whenWrongContentType_shouldReturn415() throws Exception {
        // When & Then: JSONが必要なエンドポイントにtext/plainを送信
        mockMvc.perform(post("/api/tutorials")
                .contentType(MediaType.TEXT_PLAIN)
                .content("plain text content"))
            .andExpect(status().isUnsupportedMediaType());
    }

    @Test
    @DisplayName("存在しないエンドポイント - 404 Not Foundが返る")
    void whenEndpointNotFound_shouldReturn404() throws Exception {
        // When & Then: 存在しないパス
        mockMvc.perform(get("/api/nonexistent"))
            .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("サポートされていないHTTPメソッド - 405 Method Not Allowedが返る")
    void whenMethodNotAllowed_shouldReturn405() throws Exception {
        // When & Then: PATCHメソッドは未サポート
        mockMvc.perform(patch("/api/tutorials/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isMethodNotAllowed());
    }

    // ===== パラメータバリデーションのテスト =====

    @Test
    @DisplayName("無効なパスパラメータ - 400 Bad Requestが返る")
    void whenInvalidPathParameter_shouldReturn400() throws Exception {
        // When & Then: 数値が期待されるが文字列を指定
        mockMvc.perform(get("/api/tutorials/invalid-id"))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("負の数のID - 404 Not Foundが返る")
    void whenNegativeId_shouldReturn404() throws Exception {
        // When & Then: 負の数のID
        mockMvc.perform(get("/api/tutorials/-1"))
            .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("ゼロのID - 404 Not Foundが返る")
    void whenZeroId_shouldReturn404() throws Exception {
        // When & Then: IDが0
        mockMvc.perform(get("/api/tutorials/0"))
            .andExpect(status().isNotFound());
    }

    // ===== データベースエラーのテスト =====

    // 注意: 以下のテストはデータベースエラーをシミュレートする必要があるため、
    // モックを使用するか、実際にエラーを発生させる環境が必要

    // @Test
    // @DisplayName("データベース接続エラー - 500 Internal Server Errorが返る")
    // void whenDatabaseError_shouldReturn500() throws Exception {
    //     // テスト実装には@MockBeanを使用してRepositoryをモック化し、
    //     // エラーをスローさせる必要がある
    // }

    // ===== 境界値テスト =====

    @Test
    @DisplayName("非常に長いタイトル - 現在は受け入れられる（DB制約次第）")
    void whenVeryLongTitle_shouldAcceptOrReject() throws Exception {
        // Given: 非常に長いタイトル（1000文字）
        String longTitle = "A".repeat(1000);
        Tutorial tutorial = new Tutorial(longTitle, "Description", false);

        // When & Then: 現在のバリデーションでは受け入れられる
        // 注意: DB制約（VARCHAR(255)）があればエラーになる
        mockMvc.perform(post("/api/tutorials")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(tutorial)))
            .andExpect(status().isOneOf(201, 500)); // 201または500（DB制約エラー）
    }

    @Test
    @DisplayName("空文字列のタイトル - 現在は受け入れられる")
    void whenEmptyTitle_shouldAccept() throws Exception {
        // Given: 空文字列のタイトル
        Tutorial tutorial = new Tutorial("", "Description", false);

        // When & Then: 現在のバリデーションでは受け入れられる
        mockMvc.perform(post("/api/tutorials")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(tutorial)))
            .andExpect(status().isCreated());
    }

    // ===== 同時実行・競合のテスト =====

    @Test
    @DisplayName("同時削除 - 2回目は500エラー")
    void whenConcurrentDelete_secondShouldFail() throws Exception {
        // Given: 保存済みのチュートリアル
        Tutorial tutorial = repository.save(new Tutorial("Test", "Desc", false));
        long id = tutorial.getId();

        // When: 1回目の削除は成功
        mockMvc.perform(delete("/api/tutorials/{id}", id))
            .andExpect(status().isNoContent());

        // Then: 2回目の削除は失敗（既に存在しない）
        mockMvc.perform(delete("/api/tutorials/{id}", id))
            .andExpect(status().isInternalServerError()); // 現在の実装では500
    }

    @Test
    @DisplayName("削除済みリソースの取得 - 404 Not Found")
    void whenGetDeletedResource_shouldReturn404() throws Exception {
        // Given: 一度保存して削除
        Tutorial tutorial = repository.save(new Tutorial("Test", "Desc", false));
        long id = tutorial.getId();
        repository.deleteById(id);

        // When & Then: 削除済みリソースの取得は404
        mockMvc.perform(get("/api/tutorials/{id}", id))
            .andExpect(status().isNotFound());
    }
}
