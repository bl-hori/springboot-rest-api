package com.example.application;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.TestPropertySource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TutorialRepositoryの単体テスト
 *
 * カスタムクエリメソッドの動作を検証します。
 * @DataJpaTestアノテーションにより、JPAコンポーネントのみがロードされます。
 */
@DataJpaTest
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
class TutorialRepositoryTest {

    @Autowired
    private TutorialRepository repository;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
    }

    // ===== findByPublished() テスト =====

    @Test
    @DisplayName("findByPublished(true) - 公開済みのみ取得")
    void findByPublished_shouldReturnOnlyPublished() {
        // Given: 公開済み・未公開が混在
        repository.save(new Tutorial("Published Tutorial", "This is published", true));
        repository.save(new Tutorial("Draft Tutorial", "This is draft", false));
        repository.save(new Tutorial("Another Published", "Also published", true));

        // When: 公開済みのみ検索
        List<Tutorial> result = repository.findByPublished(true);

        // Then: 公開済みの2件のみ返る
        assertThat(result).hasSize(2);
        assertThat(result).allMatch(Tutorial::isPublished);
        assertThat(result).extracting(Tutorial::getTitle)
            .containsExactlyInAnyOrder("Published Tutorial", "Another Published");
    }

    @Test
    @DisplayName("findByPublished(false) - 未公開のみ取得")
    void findByPublished_shouldReturnOnlyUnpublished() {
        // Given: 公開済み・未公開が混在
        repository.save(new Tutorial("Published", "Published desc", true));
        repository.save(new Tutorial("Draft 1", "Draft desc 1", false));
        repository.save(new Tutorial("Draft 2", "Draft desc 2", false));

        // When: 未公開のみ検索
        List<Tutorial> result = repository.findByPublished(false);

        // Then: 未公開の2件のみ返る
        assertThat(result).hasSize(2);
        assertThat(result).allMatch(t -> !t.isPublished());
        assertThat(result).extracting(Tutorial::getTitle)
            .containsExactlyInAnyOrder("Draft 1", "Draft 2");
    }

    @Test
    @DisplayName("findByPublished() - 該当データが0件の場合")
    void findByPublished_whenNoMatch_shouldReturnEmptyList() {
        // Given: 全て未公開
        repository.save(new Tutorial("Draft 1", "Draft", false));
        repository.save(new Tutorial("Draft 2", "Draft", false));

        // When: 公開済みを検索
        List<Tutorial> result = repository.findByPublished(true);

        // Then: 空リストが返る（nullではない）
        assertThat(result).isEmpty();
        assertThat(result).isNotNull();
    }

    // ===== findByTitleContaining() テスト =====

    @Test
    @DisplayName("findByTitleContaining() - 部分一致検索")
    void findByTitleContaining_shouldReturnMatchingTutorials() {
        // Given: 様々なタイトルのチュートリアル
        repository.save(new Tutorial("Spring Boot Tutorial", "Desc 1", false));
        repository.save(new Tutorial("Spring Security Guide", "Desc 2", true));
        repository.save(new Tutorial("React Basics", "Desc 3", false));
        repository.save(new Tutorial("Angular Tutorial", "Desc 4", true));

        // When: "Spring"で検索
        List<Tutorial> result = repository.findByTitleContaining("Spring");

        // Then: Springを含む2件が返る
        assertThat(result).hasSize(2);
        assertThat(result).extracting(Tutorial::getTitle)
            .containsExactlyInAnyOrder("Spring Boot Tutorial", "Spring Security Guide");
    }

    @Test
    @DisplayName("findByTitleContaining() - 部分文字列での検索")
    void findByTitleContaining_withPartialString_shouldMatch() {
        // Given
        repository.save(new Tutorial("Introduction to Programming", "Desc", false));
        repository.save(new Tutorial("Advanced Programming", "Desc", true));
        repository.save(new Tutorial("Web Development", "Desc", false));

        // When: "gramming"で検索（部分文字列）
        List<Tutorial> result = repository.findByTitleContaining("gramming");

        // Then: 2件がマッチ
        assertThat(result).hasSize(2);
        assertThat(result).extracting(Tutorial::getTitle)
            .allMatch(title -> title.contains("gramming"));
    }

    @Test
    @DisplayName("findByTitleContaining() - 大文字小文字の区別")
    void findByTitleContaining_caseInsensitive_shouldNotMatch() {
        // Given
        repository.save(new Tutorial("Spring Boot Tutorial", "Desc", false));
        repository.save(new Tutorial("spring security guide", "Desc", true));

        // When: 小文字の"spring"で検索
        List<Tutorial> result = repository.findByTitleContaining("spring");

        // Then: 注意: デフォルトでは大文字小文字を区別する
        // MySQLの設定によって動作が異なる可能性がある
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTitle()).isEqualTo("spring security guide");
    }

    @Test
    @DisplayName("findByTitleContaining() - 検索結果が0件")
    void findByTitleContaining_whenNoMatch_shouldReturnEmptyList() {
        // Given
        repository.save(new Tutorial("Java Tutorial", "Desc", false));
        repository.save(new Tutorial("Python Guide", "Desc", true));

        // When: 存在しないキーワードで検索
        List<Tutorial> result = repository.findByTitleContaining("NonExistentKeyword");

        // Then: 空リストが返る
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findByTitleContaining() - 空文字列での検索")
    void findByTitleContaining_withEmptyString_shouldReturnAll() {
        // Given
        repository.save(new Tutorial("Tutorial 1", "Desc 1", false));
        repository.save(new Tutorial("Tutorial 2", "Desc 2", true));
        repository.save(new Tutorial("Tutorial 3", "Desc 3", false));

        // When: 空文字列で検索
        List<Tutorial> result = repository.findByTitleContaining("");

        // Then: 全件が返る（空文字列は全てに含まれる）
        assertThat(result).hasSize(3);
    }

    // ===== 基本的なCRUD操作のテスト =====

    @Test
    @DisplayName("save() - エンティティの保存")
    void save_shouldPersistEntity() {
        // Given
        Tutorial tutorial = new Tutorial("Test Tutorial", "Test Description", false);

        // When: 保存
        Tutorial saved = repository.save(tutorial);

        // Then: IDが割り当てられ、保存される
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getId()).isGreaterThan(0);
        assertThat(repository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("findById() - ID検索")
    void findById_whenExists_shouldReturnTutorial() {
        // Given: 保存済みのチュートリアル
        Tutorial saved = repository.save(new Tutorial("Test", "Desc", false));

        // When: IDで検索
        Tutorial found = repository.findById(saved.getId()).orElseThrow();

        // Then: 同じデータが取得できる
        assertThat(found.getId()).isEqualTo(saved.getId());
        assertThat(found.getTitle()).isEqualTo("Test");
        assertThat(found.getDescription()).isEqualTo("Desc");
        assertThat(found.isPublished()).isFalse();
    }

    @Test
    @DisplayName("findAll() - 全件取得")
    void findAll_shouldReturnAllTutorials() {
        // Given: 3件のチュートリアル
        repository.save(new Tutorial("Tutorial 1", "Desc 1", false));
        repository.save(new Tutorial("Tutorial 2", "Desc 2", true));
        repository.save(new Tutorial("Tutorial 3", "Desc 3", false));

        // When: 全件取得
        List<Tutorial> all = repository.findAll();

        // Then: 3件全て取得できる
        assertThat(all).hasSize(3);
    }

    @Test
    @DisplayName("deleteById() - ID指定削除")
    void deleteById_shouldRemoveTutorial() {
        // Given: 保存済みのチュートリアル
        Tutorial saved = repository.save(new Tutorial("To Delete", "Desc", false));
        long id = saved.getId();

        assertThat(repository.count()).isEqualTo(1);

        // When: 削除
        repository.deleteById(id);

        // Then: 削除される
        assertThat(repository.count()).isEqualTo(0);
        assertThat(repository.findById(id)).isEmpty();
    }

    @Test
    @DisplayName("deleteAll() - 全削除")
    void deleteAll_shouldRemoveAllTutorials() {
        // Given: 複数のチュートリアル
        repository.save(new Tutorial("Tutorial 1", "Desc 1", false));
        repository.save(new Tutorial("Tutorial 2", "Desc 2", true));
        repository.save(new Tutorial("Tutorial 3", "Desc 3", false));

        assertThat(repository.count()).isEqualTo(3);

        // When: 全削除
        repository.deleteAll();

        // Then: 全て削除される
        assertThat(repository.count()).isEqualTo(0);
        assertThat(repository.findAll()).isEmpty();
    }
}
