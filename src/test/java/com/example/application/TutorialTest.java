package com.example.application;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tutorialエンティティの単体テスト
 *
 * コンストラクタ、getter/setterの動作を検証します。
 */
class TutorialTest {

    // ===== コンストラクタのテスト =====

    @Test
    @DisplayName("引数付きコンストラクタ - 全フィールドが正しく設定される")
    void constructor_shouldSetAllFields() {
        // When: 引数付きコンストラクタで作成
        Tutorial tutorial = new Tutorial("Test Title", "Test Description", true);

        // Then: 全フィールドが設定される
        assertThat(tutorial.getTitle()).isEqualTo("Test Title");
        assertThat(tutorial.getDescription()).isEqualTo("Test Description");
        assertThat(tutorial.isPublished()).isTrue();
        // 注意: idはデフォルト値（0）
        assertThat(tutorial.getId()).isEqualTo(0L);
    }

    @Test
    @DisplayName("デフォルトコンストラクタ - 空のインスタンスが作成される")
    void defaultConstructor_shouldCreateEmptyTutorial() {
        // When: デフォルトコンストラクタで作成
        Tutorial tutorial = new Tutorial();

        // Then: 全フィールドがnullまたはデフォルト値
        assertThat(tutorial.getId()).isEqualTo(0L);
        assertThat(tutorial.getTitle()).isNull();
        assertThat(tutorial.getDescription()).isNull();
        assertThat(tutorial.isPublished()).isFalse(); // booleanのデフォルト値
    }

    @Test
    @DisplayName("コンストラクタ - null値の許容")
    void constructor_withNullValues_shouldAcceptNull() {
        // When: null値を渡す
        Tutorial tutorial = new Tutorial(null, null, false);

        // Then: nullが設定される
        assertThat(tutorial.getTitle()).isNull();
        assertThat(tutorial.getDescription()).isNull();
        assertThat(tutorial.isPublished()).isFalse();
    }

    // ===== Setterのテスト =====

    @Test
    @DisplayName("setTitle() - タイトルが更新される")
    void setTitle_shouldUpdateTitle() {
        // Given: 既存のチュートリアル
        Tutorial tutorial = new Tutorial("Original Title", "Desc", false);

        // When: タイトルを変更
        tutorial.setTitle("New Title");

        // Then: タイトルが更新される
        assertThat(tutorial.getTitle()).isEqualTo("New Title");
        // 他のフィールドは変更されない
        assertThat(tutorial.getDescription()).isEqualTo("Desc");
        assertThat(tutorial.isPublished()).isFalse();
    }

    @Test
    @DisplayName("setDescription() - 説明が更新される")
    void setDescription_shouldUpdateDescription() {
        // Given
        Tutorial tutorial = new Tutorial("Title", "Original Desc", false);

        // When: 説明を変更
        tutorial.setDescription("New Description");

        // Then: 説明が更新される
        assertThat(tutorial.getDescription()).isEqualTo("New Description");
        assertThat(tutorial.getTitle()).isEqualTo("Title");
    }

    @Test
    @DisplayName("setPublished() - 公開状態が更新される")
    void setPublished_shouldUpdatePublishedStatus() {
        // Given: 未公開のチュートリアル
        Tutorial tutorial = new Tutorial("Title", "Desc", false);
        assertThat(tutorial.isPublished()).isFalse();

        // When: 公開状態に変更
        tutorial.setPublished(true);

        // Then: 公開状態が更新される
        assertThat(tutorial.isPublished()).isTrue();

        // When: 再度未公開に変更
        tutorial.setPublished(false);

        // Then: 未公開状態に戻る
        assertThat(tutorial.isPublished()).isFalse();
    }

    @Test
    @DisplayName("setId() - IDが更新される")
    void setId_shouldUpdateId() {
        // Given
        Tutorial tutorial = new Tutorial("Title", "Desc", false);
        assertThat(tutorial.getId()).isEqualTo(0L);

        // When: IDを設定
        tutorial.setId(123L);

        // Then: IDが更新される
        assertThat(tutorial.getId()).isEqualTo(123L);
    }

    @Test
    @DisplayName("setter - null値の設定")
    void setters_shouldAcceptNullValues() {
        // Given: 値が設定されたチュートリアル
        Tutorial tutorial = new Tutorial("Title", "Desc", true);

        // When: null値を設定
        tutorial.setTitle(null);
        tutorial.setDescription(null);

        // Then: nullが設定される
        assertThat(tutorial.getTitle()).isNull();
        assertThat(tutorial.getDescription()).isNull();
        // booleanはnullにできない（プリミティブ型のため）
        assertThat(tutorial.isPublished()).isTrue();
    }

    // ===== Getterのテスト =====

    @Test
    @DisplayName("getters - 全てのフィールドが取得できる")
    void getters_shouldReturnAllFields() {
        // Given: 値が設定されたチュートリアル
        Tutorial tutorial = new Tutorial("Test Title", "Test Desc", true);
        tutorial.setId(999L);

        // When & Then: 各getterで値が取得できる
        assertThat(tutorial.getId()).isEqualTo(999L);
        assertThat(tutorial.getTitle()).isEqualTo("Test Title");
        assertThat(tutorial.getDescription()).isEqualTo("Test Desc");
        assertThat(tutorial.isPublished()).isTrue();
    }

    // ===== ビジネスロジックのテスト（将来的な拡張を想定） =====

    @Test
    @DisplayName("複数回の更新 - 最後の値が反映される")
    void multipleUpdates_shouldRetainLatestValue() {
        // Given
        Tutorial tutorial = new Tutorial("Title 1", "Desc 1", false);

        // When: 複数回更新
        tutorial.setTitle("Title 2");
        tutorial.setTitle("Title 3");
        tutorial.setDescription("Desc 2");
        tutorial.setPublished(true);
        tutorial.setPublished(false);

        // Then: 最後の値が反映される
        assertThat(tutorial.getTitle()).isEqualTo("Title 3");
        assertThat(tutorial.getDescription()).isEqualTo("Desc 2");
        assertThat(tutorial.isPublished()).isFalse();
    }

    @Test
    @DisplayName("長い文字列の設定 - 制限なく設定できる")
    void setLongStrings_shouldAcceptLongValues() {
        // Given: 非常に長い文字列
        String longTitle = "A".repeat(1000);
        String longDesc = "B".repeat(5000);

        // When: 長い文字列を設定
        Tutorial tutorial = new Tutorial(longTitle, longDesc, false);

        // Then: 設定できる（注意: DB制約で失敗する可能性あり）
        assertThat(tutorial.getTitle()).hasSize(1000);
        assertThat(tutorial.getDescription()).hasSize(5000);
    }

    @Test
    @DisplayName("特殊文字を含むタイトル - 正しく設定される")
    void setTitleWithSpecialCharacters_shouldWork() {
        // Given: 特殊文字を含む文字列
        String specialTitle = "Title with 日本語, emoji 🎉, and symbols !@#$%";

        // When: 設定
        Tutorial tutorial = new Tutorial();
        tutorial.setTitle(specialTitle);

        // Then: そのまま設定される
        assertThat(tutorial.getTitle()).isEqualTo(specialTitle);
    }
}
