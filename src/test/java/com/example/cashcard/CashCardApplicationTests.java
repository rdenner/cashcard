package com.example.cashcard;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.annotation.DirtiesContext;

import com.jayway.jsonpath.JsonPath;

import net.minidev.json.JSONArray;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CashCardApplicationTests {

	@Autowired
	TestRestTemplate restTemplate;

	private TestRestTemplate getAuthenticatedRestTemplate() {
		return restTemplate.withBasicAuth("sarah1", "abc123");
	}

	@Test
	void shouldReturnACashCardWhenDataIsSaved() {
		var response = getAuthenticatedRestTemplate()
				.getForEntity("/cashcards/99", String.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

		var documentContext = JsonPath.parse(response.getBody());
		var id = documentContext.read("$.id");
		var amount = documentContext.read("$.amount");
		var owner = documentContext.read("$.owner");
		assertThat(id).isEqualTo(99);
		assertThat(amount).isEqualTo(123.45);
		assertThat(owner).isEqualTo("sarah1");
	}

	@Test
	void shouldReturnNotFoundOnUnknownId() {
		var response = getAuthenticatedRestTemplate().getForEntity("/cashcards/1234", String.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
		assertThat(response.getBody()).isBlank();
	}

	@Test
	void shouldNotReturnACashCardWithAnUnknownId() {
		var response = getAuthenticatedRestTemplate().getForEntity("/cashcards/1000", String.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
		assertThat(response.getBody()).isBlank();
	}

	@Test
	void shouldReturnAllCashCardsWhenListIsRequested() {
		var response = getAuthenticatedRestTemplate().getForEntity("/cashcards", String.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

		var documentContext = JsonPath.parse(response.getBody());
		int cashCardCount = documentContext.read("$.length()");
		assertThat(cashCardCount).isEqualTo(3);

		JSONArray ids = documentContext.read("$..id");
		assertThat(ids).containsExactlyInAnyOrder(99, 100, 101);

		JSONArray amounts = documentContext.read("$..amount");
		assertThat(amounts).containsExactlyInAnyOrder(123.45, 1.0, 150.00);

		JSONArray owners = documentContext.read("$..owner");
		assertThat(owners).containsExactlyInAnyOrder("sarah1", "sarah1", "sarah1");
	}

	@Test
	@DirtiesContext
	void shouldCreateANewCashCard() {
		var newCashCard = new CashCard(null, 250.00, "sarah1");
		var createResponse = getAuthenticatedRestTemplate().postForEntity("/cashcards", newCashCard, Void.class);
		assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);

		var locationOfNewCashCard = createResponse.getHeaders().getLocation();
		var getResponse = getAuthenticatedRestTemplate().getForEntity(locationOfNewCashCard, String.class);

		assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

		var documentContext = JsonPath.parse(getResponse.getBody());
		var id = documentContext.read("$.id");
		var amount = documentContext.read("$.amount");

		assertThat(id).isNotNull();
		assertThat(amount).isEqualTo(250.00);
	}

	@Test
	void shouldReturnAPageOfCashCards() {
		var response = getAuthenticatedRestTemplate().getForEntity("/cashcards?page=0&size=1", String.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

		var documentContext = JsonPath.parse(response.getBody());
		JSONArray page = documentContext.read("$[*]");
		assertThat(page.size()).isEqualTo(1);
	}

	@Test
	void shouldReturnASortedPageOfCashCards() {
		var response = getAuthenticatedRestTemplate().getForEntity("/cashcards?page=0&size=1&sort=amount,desc",
				String.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

		var documentContext = JsonPath.parse(response.getBody());
		JSONArray read = documentContext.read("$[*]");
		assertThat(read.size()).isEqualTo(1);

		var amount = documentContext.read("$[0].amount");
		assertThat(amount).isEqualTo(150.00);
	}

	@Test
	void shouldReturnASortedPageOfCashCardsWithNoParametersAndUseDefaultValues() {
		var response = getAuthenticatedRestTemplate().getForEntity("/cashcards", String.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

		var documentContext = JsonPath.parse(response.getBody());
		JSONArray page = documentContext.read("$[*]");
		assertThat(page.size()).isEqualTo(3);

		JSONArray amounts = documentContext.read("$..amount");
		assertThat(amounts).containsExactly(1.00, 123.45, 150.00);
	}

	@Test
	void shouldNotReturnACashCardWhenUsingBadCredentials() {
		var response = restTemplate
				.withBasicAuth("BAD-USER", "abc123")
				.getForEntity("/cashcards/99", String.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

		response = restTemplate
				.withBasicAuth("sarah1", "BAD-PASSWORD")
				.getForEntity("/cashcards/99", String.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
	}

	@Test
	void shouldRejectUsersWhoAreNotCardOwners() {
		var response = restTemplate
				.withBasicAuth("hank", "qrs456")
				.getForEntity("/cashcards/99", String.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
	}

	@Test
	void shouldNotAllowAccessToCashCardsTheyDoNotOwn() {
		var response = restTemplate
				.withBasicAuth("sarah1", "abc123")
				.getForEntity("/cashcards/102", String.class); // kumar2's data
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
	}

	@Test
	@DirtiesContext
	void shouldUpdateAnExistingCashCard() {
		var cashCardUpdate = new CashCard(null, 19.99, null);
		var request = new HttpEntity<>(cashCardUpdate);
		var response = getAuthenticatedRestTemplate()
				.exchange("/cashcards/99", HttpMethod.PUT, request, Void.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

		var getResponse = restTemplate
				.withBasicAuth("sarah1", "abc123")
				.getForEntity("/cashcards/99", String.class);
		assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
		var documentContext = JsonPath.parse(getResponse.getBody());
		var id = documentContext.read("$.id");
		var amount = documentContext.read("$.amount");
		assertThat(id).isEqualTo(99);
		assertThat(amount).isEqualTo(19.99);
	}

	@Test
	void shouldNotUpdateACashCardThatDoesNotExist() {
		var unknownCard = new CashCard(null, 19.99, null);
		var request = new HttpEntity<>(unknownCard);
		var response = restTemplate
				.withBasicAuth("sarah1", "abc123")
				.exchange("/cashcards/99999", HttpMethod.PUT, request, Void.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
	}

	@Test
	void shouldNotUpdateACashCardThatIsOwnedBySomeoneElse() {
		var kumarsCard = new CashCard(null, 333.33, null);
		var request = new HttpEntity<>(kumarsCard);
		var response = restTemplate
				.withBasicAuth("sarah1", "abc123")
				.exchange("/cashcards/102", HttpMethod.PUT, request, Void.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
	}

	@Test
	@DirtiesContext
	void shouldDeleteAnExistingCashCard() {
		var response = getAuthenticatedRestTemplate()
				.exchange("/cashcards/99", HttpMethod.DELETE, null, Void.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

		var getResponse = getAuthenticatedRestTemplate()
				.getForEntity("/cashcards/99", String.class);
		assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
	}

	@Test
	void shouldNotDeleteACashCardThatDoesNotExist() {
		var deleteResponse = getAuthenticatedRestTemplate()
				.exchange("/cashcards/99999", HttpMethod.DELETE, null, Void.class);
		assertThat(deleteResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
	}

	@Test
	void shouldNotAllowDeletionOfCashCardsTheyDoNotOwn() {
		var deleteResponse = getAuthenticatedRestTemplate()
				.exchange("/cashcards/102", HttpMethod.DELETE, null, Void.class);
		assertThat(deleteResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

		var getResponse = restTemplate
				.withBasicAuth("kumar2", "xyz789")
				.getForEntity("/cashcards/102", String.class);
		assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
	}
}
