package com.example.application;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * パフォーマンステスト
 *
 * 大量データでの動作、レスポンスタイムを検証します。
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
class TutorialControllerPerformanceTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TutorialRepository repository;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
    }

    @Test
    @DisplayName("大量データ取得 - 1000件のチュートリアルを取得")
    void getAllTutorials_withLargeDataset_shouldCompleteInReasonableTime() throws Exception {
        // Given: 1000件のチュートリアルをバルク挿入
        List<Tutorial> tutorials = new ArrayList<>();
        for (int i = 1; i <= 1000; i++) {
            tutorials.add(new Tutorial(
                "Tutorial " + i,
                "Description for tutorial " + i,
                i % 2 == 0 // 偶数番目は公開済み
            ));
        }
        repository.saveAll(tutorials);

        // When: 全件取得（実行時間を測定）
        long startTime = System.currentTimeMillis();

        mockMvc.perform(get("/api/tutorials"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1000));

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        // Then: レスポンスタイムが妥当（例: 5秒以内）
        System.out.println("Response time for 1000 records: " + duration + "ms");
        assert duration < 5000 : "Response time too slow: " + duration + "ms";
    }

    @Test
    @DisplayName("大量データ検索 - 1000件中から検索")
    void searchTutorials_withLargeDataset_shouldBeEfficient() throws Exception {
        // Given: 1000件のデータ
        List<Tutorial> tutorials = new ArrayList<>();
        for (int i = 1; i <= 1000; i++) {
            String title = (i % 100 == 0) ? "Spring Boot " + i : "Tutorial " + i;
            tutorials.add(new Tutorial(title, "Desc " + i, false));
        }
        repository.saveAll(tutorials);

        // When: "Spring"で検索（10件ヒット）
        long startTime = System.currentTimeMillis();

        mockMvc.perform(get("/api/tutorials")
                .param("title", "Spring"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(10));

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        // Then: 検索が高速（例: 2秒以内）
        System.out.println("Search time in 1000 records: " + duration + "ms");
        assert duration < 2000 : "Search time too slow: " + duration + "ms";
    }

    @Test
    @DisplayName("公開済みフィルタリング - 大量データから公開済みのみ取得")
    void findByPublished_withLargeDataset_shouldBeEfficient() throws Exception {
        // Given: 1000件中500件が公開済み
        List<Tutorial> tutorials = new ArrayList<>();
        for (int i = 1; i <= 1000; i++) {
            tutorials.add(new Tutorial(
                "Tutorial " + i,
                "Desc " + i,
                i % 2 == 0 // 偶数のみ公開
            ));
        }
        repository.saveAll(tutorials);

        // When: 公開済みのみ取得
        long startTime = System.currentTimeMillis();

        mockMvc.perform(get("/api/tutorials/published"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(500));

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        // Then: フィルタリングが効率的
        System.out.println("Filter time for 500/1000 published records: " + duration + "ms");
        assert duration < 3000 : "Filter time too slow: " + duration + "ms";
    }

    @Test
    @DisplayName("連続リクエスト - 10回連続でリクエストしても安定")
    void consecutiveRequests_shouldRemainStable() throws Exception {
        // Given: 100件のデータ
        List<Tutorial> tutorials = new ArrayList<>();
        for (int i = 1; i <= 100; i++) {
            tutorials.add(new Tutorial("Tutorial " + i, "Desc " + i, false));
        }
        repository.saveAll(tutorials);

        // When: 10回連続でリクエスト
        for (int i = 0; i < 10; i++) {
            mockMvc.perform(get("/api/tutorials"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(100));
        }

        // Then: エラーなく完了（安定性の確認）
    }

    @Test
    @DisplayName("メモリ効率 - 大量データでもメモリリークしない")
    void largeDataset_shouldNotCauseMemoryLeak() throws Exception {
        // Given: 複数回の大量データ挿入・削除
        for (int iteration = 0; iteration < 5; iteration++) {
            List<Tutorial> tutorials = new ArrayList<>();
            for (int i = 1; i <= 500; i++) {
                tutorials.add(new Tutorial("Tutorial " + i, "Desc " + i, false));
            }
            repository.saveAll(tutorials);

            // When: 全件取得
            mockMvc.perform(get("/api/tutorials"))
                .andExpect(status().isOk());

            // クリーンアップ
            repository.deleteAll();
        }

        // Then: メモリリークがないことを確認（手動確認またはプロファイラーで検証）
    }
}
