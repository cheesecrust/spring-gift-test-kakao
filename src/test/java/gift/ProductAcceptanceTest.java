package gift;

import gift.model.CategoryRepository;
import gift.model.OptionRepository;
import gift.model.ProductRepository;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ProductAcceptanceTest {

    @LocalServerPort
    int port;

    @Autowired
    OptionRepository optionRepository;

    @Autowired
    ProductRepository productRepository;

    @Autowired
    CategoryRepository categoryRepository;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        // FK 역순 삭제 — 다른 테스트 클래스가 생성한 데이터도 정리
        optionRepository.deleteAll();
        productRepository.deleteAll();
        categoryRepository.deleteAll();
    }

    @Test
    void 상품_목록_조회_빈_목록() {
        // given — 데이터 없음

        // when & then
        given()
        .when()
            .get("/api/products")
        .then()
            .statusCode(200)
            .body("size()", equalTo(0));
    }

    @Test
    void 상품_목록_조회_N개_존재() {
        // given
        var categoryId = createCategory("전자기기");
        createProduct("노트북", 1_500_000, "https://example.com/notebook.png", categoryId);
        createProduct("키보드", 120_000, "https://example.com/keyboard.png", categoryId);

        // when & then
        given()
        .when()
            .get("/api/products")
        .then()
            .statusCode(200)
            .body("size()", equalTo(2))
            .body("name", containsInAnyOrder("노트북", "키보드"))
            .body("price", containsInAnyOrder(1_500_000, 120_000))
            .body("imageUrl", containsInAnyOrder("https://example.com/notebook.png", "https://example.com/keyboard.png"))
            .body("category.name", everyItem(equalTo("전자기기")));
    }

    @Test
    void 상품_생성_성공() {
        // given
        var categoryId = createCategory("전자기기");
        var request = Map.of(
            "name", "노트북",
            "price", 1_500_000,
            "imageUrl", "https://example.com/notebook.png",
            "categoryId", categoryId
        );

        // when
        var response = given()
            .contentType(ContentType.JSON)
            .body(request)
        .when()
            .post("/api/products");

        // then — 기대 동작 (프로덕션 수정 후)
        response.then()
            .statusCode(200)
            .body("id", notNullValue())
            .body("name", equalTo("노트북"))
            .body("price", equalTo(1_500_000))
            .body("imageUrl", equalTo("https://example.com/notebook.png"))
            .body("category.name", equalTo("전자기기"));
    }

    @Test
    void 상품_생성_실패_존재하지_않는_카테고리() {
        // given
        var request = Map.of(
            "name", "노트북",
            "price", 1_500_000,
            "imageUrl", "https://example.com/notebook.png",
            "categoryId", 9999L
        );

        // when
        var response = given()
            .contentType(ContentType.JSON)
            .body(request)
        .when()
            .post("/api/products");

        // then — 기대 동작: 존재하지 않는 categoryId → NoSuchElementException → 500
        response.then()
            .statusCode(500);
    }

    private Long createCategory(String name) {
        return given()
            .contentType(ContentType.JSON)
            .body(Map.of("name", name))
        .when()
            .post("/api/categories")
        .then()
            .statusCode(200)
            .extract()
            .jsonPath()
            .getLong("id");
    }

    private void createProduct(String name, int price, String imageUrl, Long categoryId) {
        given()
            .contentType(ContentType.JSON)
            .body(Map.of(
                "name", name,
                "price", price,
                "imageUrl", imageUrl,
                "categoryId", categoryId
            ))
        .when()
            .post("/api/products")
        .then()
            .statusCode(200);
    }
}
