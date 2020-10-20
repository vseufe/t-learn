package de.tarent;

import de.tarent.entities.Course;
import io.quarkus.test.junit.QuarkusTest;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import javax.transaction.Transactional;
import java.time.OffsetDateTime;
import java.util.ArrayList;

import static de.tarent.entities.Course.CourseForm.CERTIFICATION;
import static de.tarent.entities.Course.CourseType.EXTERNAL;
import static de.tarent.entities.Course.ExecutionType.REMOTE;
import static io.restassured.RestAssured.given;
import static java.time.OffsetDateTime.parse;
import static java.time.temporal.ChronoUnit.SECONDS;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@QuarkusTest
@TestMethodOrder(OrderAnnotation.class)
public class CoursesResourceTest {

    public static final String CONTENT_TYPE = "Content-Type";

    @Test
    @Order(1)
    public void testGetAllCourses() {
        given().header("Accept", APPLICATION_JSON)
                .when().get("/courses")
                .then()
                .statusCode(200)
                .body("title", containsInAnyOrder("Quarkus Into", "Quarkus for Spring Devs"))
                .body("organizer", containsInAnyOrder("Tim Organizer", "Theo Organizer"))
                .body("contactPerson", containsInAnyOrder("Otto ContactPerson", "Oskar ContactPerson"))
                .body("startDate", containsInAnyOrder("2020-01-01T20:00:00Z", "2020-01-02T20:00:00Z"))
                .body("endDate", containsInAnyOrder("2020-01-01T21:00:00Z", "2020-01-02T21:00:00Z"))
                .body("courseForm", containsInAnyOrder("CERTIFICATION", "CONFERENCE"))
                .body("courseType", containsInAnyOrder("EXTERNAL", "INTERNAL"))
                .body("price", containsInAnyOrder("100€", "free"))
                .body("executionType", containsInAnyOrder("REMOTE", "ONSITE"))
                .body("address", containsInAnyOrder("Rochusstraße 2-4, 53123 Bonn", "Dickobskreuz, 53123 Bonn"))
                .body("targetAudience", containsInAnyOrder("alle", "devs"))
                .body("link", containsInAnyOrder("http://tarent.de", "http://tarent.de"))
                .body("any { it.any { it.key == 'deleted' }}", is(false));
    }

    @Test
    @Order(1)
    public void testGetCourse() {
        given().header("Accept", APPLICATION_JSON)
                .when().get("/courses/1")
                .then()
                .statusCode(200)
                .body("title", equalTo("Quarkus Into"))
                .body("organizer", equalTo("Tim Organizer"))
                .body("contactPerson", equalTo("Otto ContactPerson"))
                .body("startDate", equalTo("2020-01-01T20:00:00Z"))
                .body("endDate", equalTo("2020-01-01T21:00:00Z"))
                .body("courseForm", equalTo("CERTIFICATION"))
                .body("price", equalTo("100€"))
                .body("courseType", equalTo("EXTERNAL"))
                .body("executionType", equalTo("REMOTE"))
                .body("address", equalTo("Rochusstraße 2-4, 53123 Bonn"))
                .body("targetAudience", equalTo("alle"))
                .body("link", equalTo("http://tarent.de"));
    }

    @Test
    public void testGetCourse_DeletedCourse() {
        given().header("Accept", APPLICATION_JSON)
                .when().get("/courses/3")
                .then()
                .statusCode(404)
                .body(is(emptyString()));
    }

    @Test
    public void testGetCourse_UnknownCourse() {
        given().header("Accept", APPLICATION_JSON)
                .when().get("/courses/123456789")
                .then()
                .statusCode(404)
                .body(is(emptyString()));
    }

    @Transactional
    @Test
    public void testCreateNewCourse() {

//        Category category = new Category();
//        category.setName("existiert");
//        category.persistAndFlush();

        ArrayList<String> categoryNames = new ArrayList<>();
        categoryNames.add("good category");

        final Course course = new Course();
        course.title = "CreatedQuarkusCourse";
        course.organizer = "Norbert Neuorganizer";
        course.contactPerson = "Oskar NeuContactPerson";
        course.startDate = parse("2020-01-03T21:00:00Z");
        course.endDate = parse("2020-01-03T22:00:00Z");
        course.courseForm = CERTIFICATION;
        course.courseType = EXTERNAL;
        course.executionType = REMOTE;
        course.address = "Rochusstraße 2-4, 53123 Bonn";
        course.targetAudience = "Alle";
        course.description = "Eine Veranstaltung";
        course.price = "100€";
        course.categoryNames = categoryNames;
        course.link = "http://tarent.de";

        final Integer id = given().body(course).header("Content-Type", APPLICATION_JSON)
                .when().post("/courses")
                .then()
                .statusCode(201)
                .header(CONTENT_TYPE, APPLICATION_JSON)
                .body("title", equalTo("CreatedQuarkusCourse"))
                .body("organizer", equalTo("Norbert Neuorganizer"))
                .body("contactPerson", equalTo("Oskar NeuContactPerson"))
                .body("startDate", equalTo("2020-01-03T21:00:00Z"))
                .body("endDate", equalTo("2020-01-03T22:00:00Z"))
                .body("courseForm", equalTo("CERTIFICATION"))
                .body("courseType", equalTo("EXTERNAL"))
                .body("executionType", equalTo("REMOTE"))
                .body("address", equalTo("Rochusstraße 2-4, 53123 Bonn"))
                .body("targetAudience", equalTo("Alle"))
                .body("description", equalTo("Eine Veranstaltung"))
                .body("categoryNames[0]", equalTo("good category"))
                .body("price", equalTo("100€"))
                .body("link", equalTo("http://tarent.de"))
                .extract().path("id");

        assertNotNull(id);

        given().header("Accept", APPLICATION_JSON)
                .when().get("/courses/{id}", id)
                .then()
                .statusCode(200)
                .body("title", equalTo("CreatedQuarkusCourse"))
                .body("categoryNames", contains("good category"));
    }

    @Test
    public void testCreateNewCourse_HttpLink() {

        checkCreateWithLink("http://tarent.de");
    }

    @Test
    public void testCreateNewCourse_HttpsLink() {

        checkCreateWithLink("https://tarent.de");
    }

    @Test
    public void testCreateNewCourse_FailedValidation_RequiredFields() {

        final Course course = new Course();

        given().body(course).header("Content-Type", APPLICATION_JSON)
                .when().post("/courses")
                .then()
                .statusCode(400)
                .body("message", allOf(
                        containsString("title must not be blank"),
                        containsString("organizer must not be blank"),
                        containsString("courseType must not be null")))
                .body("success", is(false));

    }

    @Test
    public void testCreateNewCourse_FailedValidation_StartEndDate() {
        final OffsetDateTime now = OffsetDateTime.now();

        final Course course = new Course();
        course.title = "CreatedQuarkusCourse";
        course.organizer = "Norbert Neuorganizer";
        course.courseType = EXTERNAL;
        course.startDate = now;
        course.endDate = now.minus(1, SECONDS);

        given().body(course).header("Content-Type", APPLICATION_JSON)
                .when().post("/courses")
                .then()
                .statusCode(400)
                .body("message", equalTo("The start date must not be equal or before the end date"))
                .body("success", is(false));
    }

    @Test
    public void testCreateNewCourse_FailedValidation_WrongProtocol() {
        final Course course = new Course();
        course.title = "CreatedQuarkusCourse";
        course.organizer = "Norbert Neuorganizer";
        course.courseType = EXTERNAL;
        course.link = "ftp://tarent.de";

        given().body(course).header("Content-Type", APPLICATION_JSON)
                .when().post("/courses")
                .then()
                .statusCode(400)
                .body("message", equalTo("link protocol must be \"http\" or \"https\""))
                .body("success", is(false));
    }

    @Test
    public void testCreateNewCourse_FailedValidation_InvalidURL() {
        final Course course = new Course();
        course.title = "CreatedQuarkusCourse";
        course.organizer = "Norbert Neuorganizer";
        course.courseType = EXTERNAL;
        course.link = "https/tarent.de";

        given().body(course).header("Content-Type", APPLICATION_JSON)
                .when().post("/courses")
                .then()
                .statusCode(400)
                .body("message", equalTo("link must be a valid URL"))
                .body("success", is(false));
    }

    @Test
    public void testCreateNewCourse_FailedValidation_InvalidLinkLength() {
        final Course course = new Course();
        course.title = "CreatedQuarkusCourse";
        course.organizer = "Norbert Neuorganizer";
        course.courseType = EXTERNAL;
        course.link = "https://".concat(RandomStringUtils.randomAlphanumeric(1001 - 11)).concat(".de");

        given().body(course).header("Content-Type", APPLICATION_JSON)
                .when().post("/courses")
                .then()
                .statusCode(400)
                .body("message", equalTo("link length must be between 0 and 1000"))
                .body("success", is(false));
    }

    @Test
    public void testCreateNewCourse_CheckValidLinkLength() {
        final Course course = new Course();
        course.title = "CreatedQuarkusCourse";
        course.organizer = "Norbert Neuorganizer";
        course.courseType = EXTERNAL;
        course.link = "https://".concat(RandomStringUtils.randomAlphanumeric(1000 - 11)).concat(".de");
        given().body(course).header("Content-Type", APPLICATION_JSON)
                .when().post("/courses")
                .then()
                .statusCode(201)
                .body("link", hasLength(1000));
    }

    @Test
    public void testCreateNewCourse_FailedValidation_InvalidTargetAudienceLength() {
        final Course course = new Course();
        course.title = "CreatedQuarkusCourse";
        course.organizer = "Norbert Neuorganizer";
        course.courseType = EXTERNAL;
        course.targetAudience = RandomStringUtils.random(2001);

        given().body(course).header("Content-Type", APPLICATION_JSON)
                .when().post("/courses")
                .then()
                .statusCode(400)
                .body("message", equalTo("targetAudience length must be between 0 and 2000"))
                .body("success", is(false));
    }

    @Test
    public void testCreateNewCourse_CheckValidTargetAudienceLength() {
        final Course course = new Course();
        course.title = "CreatedQuarkusCourse";
        course.organizer = "Norbert Neuorganizer";
        course.courseType = EXTERNAL;
        course.targetAudience = RandomStringUtils.random(2000);

        given().body(course).header("Content-Type", APPLICATION_JSON)
                .when().post("/courses")
                .then()
                .statusCode(201)
                .body("targetAudience", hasLength(2000));
    }

    @Test
    public void testCreateNewCourse_FailedValidation_InvalidDescriptionLength() {
        final Course course = new Course();
        course.title = "CreatedQuarkusCourse";
        course.organizer = "Norbert Neuorganizer";
        course.courseType = EXTERNAL;
        course.description = RandomStringUtils.random(2001);

        given().body(course).header("Content-Type", APPLICATION_JSON)
                .when().post("/courses")
                .then()
                .statusCode(400)
                .body("message", equalTo("description length must be between 0 and 2000"))
                .body("success", is(false));
    }

    @Test
    public void testCreateNewCourse_CheckValidDescriptionLength() {
        final Course course = new Course();
        course.title = "CreatedQuarkusCourse";
        course.organizer = "Norbert Neuorganizer";
        course.courseType = EXTERNAL;
        course.description = RandomStringUtils.random(2000);

        given().body(course).header("Content-Type", APPLICATION_JSON)
                .when().post("/courses")
                .then()
                .statusCode(201)
                .body("description", hasLength(2000));
    }

    @Test
    public void testCreateNewCourse_FailedValidation_InvalidCourseForm() {
        String course = "{" +
                "\"title\": \"irrelevant\"," +
                "\"organizer\": \"irrelevant\"," +
                "\"courseType\": \"EXTERNAL\"," +
                "\"courseForm\": \"UNKNOWN_COURSE_FORM\"" +
                "}";

        given().body(course).header("Content-Type", APPLICATION_JSON)
                .when().post("/courses")
                .then()
                .statusCode(400)
                .body("message", equalTo("Cannot deserialize value of type `de.tarent.entities.Course$CourseForm` " +
                        "from String \"UNKNOWN_COURSE_FORM\": not one of the values accepted for Enum class: " +
                        "[SEMINAR, MEETUP, WORKSHOP, STUDY_GROUP, CERTIFICATION, CONFERENCE, LECTURE, LANGUAGE_COURSE]"))
                .body("success", is(false));
    }

    @Test
    public void testUpdateCourse() {

        final Course course = new Course();
        course.title = "UpdateableQuarkusCourse";
        course.organizer = "Dummy";
        course.courseType = EXTERNAL;

        final Integer id = given().body(course).header("Content-Type", APPLICATION_JSON)
                .when().post("/courses")
                .then()
                .statusCode(201)
                .body("title", equalTo("UpdateableQuarkusCourse"))
                .extract().path("id");

        assertNotNull(id);

        course.title = "UpdatedQuarkusCourse";

        given().body(course).header("Content-Type", APPLICATION_JSON)
                .when().put("/courses/{id}", id)
                .then()
                .statusCode(204)
                .body(is(emptyString()));

        given().header("Accept", APPLICATION_JSON)
                .when().get("/courses/{id}", id)
                .then()
                .statusCode(200)
                .body("title", equalTo("UpdatedQuarkusCourse"));
    }

    @Test
    public void testUpdateCourse_DeletedCourse() {

        final Course course = new Course();
        course.title = "UpdateableQuarkusCourse";
        course.organizer = "Dummy";
        course.courseType = EXTERNAL;

        given().body(course).header("Content-Type", APPLICATION_JSON)
                .when().put("/courses/3")
                .then()
                .statusCode(404)
                .body(is(emptyString()));
    }

    @Test
    public void testUpdateCourse_UnknownCourse() {

        final Course course = new Course();
        course.title = "UpdateableQuarkusCourse";
        course.organizer = "Dummy";
        course.courseType = EXTERNAL;

        given().body(course).header("Content-Type", APPLICATION_JSON)
                .when().put("/courses/123456789")
                .then()
                .statusCode(404)
                .body(is(emptyString()));
    }

    @Test
    public void testDeleteCourse() {

        given()
                .when().delete("/courses/2")
                .then()
                .statusCode(204);

        given().header("Accept", APPLICATION_JSON)
                .when().get("/courses/2")
                .then()
                .statusCode(404);
    }

    @Test
    public void testDeleteCourse_UnknownCourse() {

        given()
                .when().delete("/courses/123456789")
                .then()
                .statusCode(204);
    }

    private void checkCreateWithLink(String link) {
        final Course course = new Course();
        course.title = "CreatedQuarkusCourse";
        course.organizer = "Norbert Neuorganizer";
        course.courseType = EXTERNAL;
        course.link = link;

        given().body(course).header("Content-Type", APPLICATION_JSON)
                .when().post("/courses")
                .then()
                .statusCode(201)
                .body("title", equalTo("CreatedQuarkusCourse"))
                .body("organizer", equalTo("Norbert Neuorganizer"))
                .body("courseType", equalTo("EXTERNAL"))
                .body("link", equalTo(link));
    }
}
