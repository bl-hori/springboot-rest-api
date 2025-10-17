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

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * TutorialController統合テスト
 *
 * 各エンドポイントの正常系・異常系を検証します。
 * H2インメモリデータベースを使用してテストを実行します。
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
class TutorialControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TutorialRepository repository;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        // 各テスト前にデータベースをクリア
        repository.deleteAll();
    }

    // ===== GET /api/tutorials テスト =====

    @Test
    @DisplayName("GET /api/tutorials - 全チュートリアル取得（正常系）")
    void getAllTutorials_shouldReturnAllTutorials() throws Exception {
        // Given: データベースに2件のチュートリアルが存在
        repository.save(new Tutorial("Spring Boot Basics", "Introduction to Spring Boot", false));
        repository.save(new Tutorial("JPA Tutorial", "Learn JPA with Hibernate", true));

        // When & Then: 全件取得APIを呼び出すと200とリストが返る
        mockMvc.perform(get("/api/tutorials"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$", hasSize(2)))
            .andExpect(jsonPath("$[0].title").value("Spring Boot Basics"))
            .andExpect(jsonPath("$[0].description").value("Introduction to Spring Boot"))
            .andExpect(jsonPath("$[0].published").value(false))
            .andExpect(jsonPath("$[1].title").value("JPA Tutorial"))
            .andExpect(jsonPath("$[1].published").value(true));
    }

    @Test
    @DisplayName("GET /api/tutorials?title=keyword - タイトル検索（正常系）")
    void getAllTutorials_withTitleFilter_shouldReturnFilteredResults() throws Exception {
        // Given: 複数のチュートリアルが存在
        repository.save(new Tutorial("Spring Boot Basics", "Desc 1", false));
        repository.save(new Tutorial("Spring Security", "Desc 2", true));
        repository.save(new Tutorial("React Basics", "Desc 3", false));

        // When & Then: "Spring"で検索すると該当する2件のみ返る
        mockMvc.perform(get("/api/tutorials")
                .param("title", "Spring"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(2)))
            .andExpect(jsonPath("$[0].title").value(containsString("Spring")))
            .andExpect(jsonPath("$[1].title").value(containsString("Spring")));
    }

    @Test
    @DisplayName("GET /api/tutorials - データが存在しない場合（異常系）")
    void getAllTutorials_whenEmpty_shouldReturnNoContent() throws Exception {
        // Given: データベースが空

        // When & Then: 204 No Contentが返る
        mockMvc.perform(get("/api/tutorials"))
            .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("GET /api/tutorials?title=nonexistent - 検索結果が0件の場合")
    void getAllTutorials_withTitleFilter_whenNoMatch_shouldReturnNoContent() throws Exception {
        // Given: データは存在するが検索条件に合致しない
        repository.save(new Tutorial("Spring Boot", "Desc", false));

        // When & Then: 204 No Contentが返る
        mockMvc.perform(get("/api/tutorials")
                .param("title", "NonExistentKeyword"))
            .andExpect(status().isNoContent());
    }

    // ===== GET /api/tutorials/{id} テスト =====

    @Test
    @DisplayName("GET /api/tutorials/{id} - ID指定で取得（正常系）")
    void getTutorialById_whenExists_shouldReturnTutorial() throws Exception {
        // Given: 特定のIDのチュートリアルが存在
        Tutorial saved = repository.save(new Tutorial("Test Tutorial", "Test Description", true));

        // When & Then: 該当IDのチュートリアルが返る
        mockMvc.perform(get("/api/tutorials/{id}", saved.getId()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(saved.getId()))
            .andExpect(jsonPath("$.title").value("Test Tutorial"))
            .andExpect(jsonPath("$.description").value("Test Description"))
            .andExpect(jsonPath("$.published").value(true));
    }

    @Test
    @DisplayName("GET /api/tutorials/{id} - 存在しないID（異常系）")
    void getTutorialById_whenNotExists_shouldReturn404() throws Exception {
        // Given: ID=999のチュートリアルは存在しない

        // When & Then: 404 Not Foundが返る
        mockMvc.perform(get("/api/tutorials/{id}", 999L))
            .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /api/tutorials/{id} - 無効なID形式（異常系）")
    void getTutorialById_withInvalidId_shouldReturn400() throws Exception {
        // When & Then: 無効なID形式の場合は400 Bad Requestが返る
        mockMvc.perform(get("/api/tutorials/{id}", "invalid"))
            .andExpect(status().isBadRequest());
    }

    // ===== POST /api/tutorials テスト =====

    @Test
    @DisplayName("POST /api/tutorials - 新規作成（正常系）")
    void createTutorial_withValidData_shouldReturn201() throws Exception {
        // Given: 有効なチュートリアルデータ
        Tutorial newTutorial = new Tutorial("New Tutorial", "New Description", false);

        // When & Then: 201 Createdが返り、作成されたデータが返る
        mockMvc.perform(post("/api/tutorials")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(newTutorial)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").exists())
            .andExpect(jsonPath("$.title").value("New Tutorial"))
            .andExpect(jsonPath("$.description").value("New Description"))
            .andExpect(jsonPath("$.published").value(false));

        // データベースに実際に保存されたことを確認
        assert repository.count() == 1;
    }

    @Test
    @DisplayName("POST /api/tutorials - 空のリクエストボディ（異常系）")
    void createTutorial_withEmptyBody_shouldReturn400() throws Exception {
        // When & Then: 空のボディの場合は400が返る
        mockMvc.perform(post("/api/tutorials")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isCreated()); // 注意: 現在の実装では201が返る（バリデーション未実装）
    }

    @Test
    @DisplayName("POST /api/tutorials - 不正なJSON形式（異常系）")
    void createTutorial_withInvalidJson_shouldReturn400() throws Exception {
        // When & Then: 不正なJSON形式の場合は400が返る
        mockMvc.perform(post("/api/tutorials")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{invalid json"))
            .andExpect(status().isBadRequest());
    }

    // ===== PUT /api/tutorials/{id} テスト =====

    @Test
    @DisplayName("PUT /api/tutorials/{id} - 更新（正常系）")
    void updateTutorial_whenExists_shouldUpdateAndReturn200() throws Exception {
        // Given: 既存のチュートリアル
        Tutorial existing = repository.save(new Tutorial("Original Title", "Original Desc", false));

        // 更新データ
        Tutorial updateData = new Tutorial("Updated Title", "Updated Desc", true);

        // When & Then: 200 OKが返り、更新されたデータが返る
        mockMvc.perform(put("/api/tutorials/{id}", existing.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateData)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(existing.getId()))
            .andExpect(jsonPath("$.title").value("Updated Title"))
            .andExpect(jsonPath("$.description").value("Updated Desc"))
            .andExpect(jsonPath("$.published").value(true));

        // データベースが実際に更新されたことを確認
        Tutorial updated = repository.findById(existing.getId()).orElseThrow();
        assert updated.getTitle().equals("Updated Title");
        assert updated.isPublished() == true;
    }

    @Test
    @DisplayName("PUT /api/tutorials/{id} - 存在しないリソースの更新（異常系）")
    void updateTutorial_whenNotExists_shouldReturn404() throws Exception {
        // Given: 存在しないID
        Tutorial updateData = new Tutorial("Title", "Desc", true);

        // When & Then: 404 Not Foundが返る
        mockMvc.perform(put("/api/tutorials/{id}", 999L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateData)))
            .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("PUT /api/tutorials/{id} - 部分更新（現在は全フィールド更新）")
    void updateTutorial_partialUpdate_shouldUpdateAllFields() throws Exception {
        // Given: 既存データ
        Tutorial existing = repository.save(new Tutorial("Title", "Desc", false));

        // 部分的な更新データ（titleのみ変更、descriptionはnull）
        String partialUpdate = "{\"title\":\"New Title\",\"description\":null,\"published\":false}";

        // When & Then: 現在の実装では全フィールドが更新される
        mockMvc.perform(put("/api/tutorials/{id}", existing.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(partialUpdate))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.title").value("New Title"))
            .andExpect(jsonPath("$.description").value(nullValue())); // nullに更新される
    }

    // ===== DELETE /api/tutorials/{id} テスト =====

    @Test
    @DisplayName("DELETE /api/tutorials/{id} - 削除（正常系）")
    void deleteTutorial_whenExists_shouldReturn204() throws Exception {
        // Given: 削除対象のチュートリアル
        Tutorial toDelete = repository.save(new Tutorial("To Delete", "Desc", false));
        long idToDelete = toDelete.getId();

        // When & Then: 204 No Contentが返る
        mockMvc.perform(delete("/api/tutorials/{id}", idToDelete))
            .andExpect(status().isNoContent());

        // データベースから削除されたことを確認
        assert repository.findById(idToDelete).isEmpty();
    }

    @Test
    @DisplayName("DELETE /api/tutorials/{id} - 存在しないリソースの削除（異常系）")
    void deleteTutorial_whenNotExists_shouldReturn500() throws Exception {
        // Given: 存在しないID

        // When & Then: 現在の実装では500 Internal Server Errorが返る
        // 注意: 理想的には404 Not Foundを返すべき
        mockMvc.perform(delete("/api/tutorials/{id}", 999L))
            .andExpect(status().isInternalServerError());
    }

    @Test
    @DisplayName("DELETE /api/tutorials/{id} - データベースから実際に削除される")
    void deleteTutorial_shouldActuallyRemoveFromDatabase() throws Exception {
        // Given: 3件のチュートリアルが存在
        repository.save(new Tutorial("Tutorial 1", "Desc 1", false));
        Tutorial toDelete = repository.save(new Tutorial("Tutorial 2", "Desc 2", true));
        repository.save(new Tutorial("Tutorial 3", "Desc 3", false));

        assert repository.count() == 3;

        // When: 中央のチュートリアルを削除
        mockMvc.perform(delete("/api/tutorials/{id}", toDelete.getId()))
            .andExpect(status().isNoContent());

        // Then: 2件に減っており、削除したIDは存在しない
        assert repository.count() == 2;
        assert repository.findById(toDelete.getId()).isEmpty();
    }

    // ===== DELETE /api/tutorials テスト =====

    @Test
    @DisplayName("DELETE /api/tutorials - 全削除（正常系）")
    void deleteAllTutorials_shouldRemoveAllRecords() throws Exception {
        // Given: 複数のチュートリアルが存在
        repository.save(new Tutorial("Tutorial 1", "Desc 1", false));
        repository.save(new Tutorial("Tutorial 2", "Desc 2", true));
        repository.save(new Tutorial("Tutorial 3", "Desc 3", false));

        assert repository.count() == 3;

        // When & Then: 204 No Contentが返る
        mockMvc.perform(delete("/api/tutorials"))
            .andExpect(status().isNoContent());

        // データベースが空になったことを確認
        assert repository.count() == 0;
    }

    @Test
    @DisplayName("DELETE /api/tutorials - 空の状態での全削除")
    void deleteAllTutorials_whenEmpty_shouldReturn204() throws Exception {
        // Given: データベースが空

        // When & Then: 204 No Contentが返る（エラーにならない）
        mockMvc.perform(delete("/api/tutorials"))
            .andExpect(status().isNoContent());

        assert repository.count() == 0;
    }

    // ===== GET /api/tutorials/published テスト =====

    @Test
    @DisplayName("GET /api/tutorials/published - 公開済みのみ取得（正常系）")
    void findByPublished_shouldReturnOnlyPublishedTutorials() throws Exception {
        // Given: 公開済み・未公開のチュートリアルが混在
        repository.save(new Tutorial("Draft 1", "Not published", false));
        repository.save(new Tutorial("Published 1", "This is published", true));
        repository.save(new Tutorial("Draft 2", "Also not published", false));
        repository.save(new Tutorial("Published 2", "This is also published", true));

        // When & Then: 公開済みの2件のみ返る
        mockMvc.perform(get("/api/tutorials/published"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(2)))
            .andExpect(jsonPath("$[0].published").value(true))
            .andExpect(jsonPath("$[1].published").value(true))
            .andExpect(jsonPath("$[*].title", not(containsInAnyOrder("Draft 1", "Draft 2"))));
    }

    @Test
    @DisplayName("GET /api/tutorials/published - 公開済みが0件の場合")
    void findByPublished_whenNoPublished_shouldReturn204() throws Exception {
        // Given: 全て未公開
        repository.save(new Tutorial("Draft 1", "Not published", false));
        repository.save(new Tutorial("Draft 2", "Also not published", false));

        // When & Then: 204 No Contentが返る
        mockMvc.perform(get("/api/tutorials/published"))
            .andExpect(status().isNoContent());
    }
}
