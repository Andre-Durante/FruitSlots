package com.andredurante.fruitmachine.api;

import java.net.URI;
import java.net.http.*;
import java.util.UUID;
import java.util.Random;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import com.andredurante.fruitmachine.service.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class MachineApiTests {
    @Autowired Environment environment;
    @TestConfiguration
    static class DeterministicService {
        @Bean @Primary MachineService testService(MachineRepository repository) {
            return new MachineService(repository, () -> {
                Random random = mock(Random.class);
                when(random.nextInt(3)).thenReturn(0, 0, 1, 2, 0, 1, 0, 1);
                return random;
            });
        }
    }
    private HttpResponse<String> request(String method, String path, String body) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:"
                + environment.getProperty("local.server.port") + path))
                .header("Content-Type", "application/json")
                .method(method, body == null ? HttpRequest.BodyPublishers.noBody()
                        : HttpRequest.BodyPublishers.ofString(body)).build();
        return HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
    }
    private String body(long cost, long balance) {
        return "{\"slotCount\":4,\"colours\":[\"A\",\"B\",\"C\"],\"k\":2,\"playCostCents\":"
                + cost + ",\"startingFloatCents\":" + balance + "}";
    }
    @Test
    void createReadPlayAndReadPersistedState() throws Exception {
        var created = request("POST", "/machines", body(200, 749));
        assertThat(created.statusCode()).isEqualTo(201);
        String location = created.headers().firstValue("location").orElseThrow();
        var get = request("GET", location, null);
        assertThat(get.statusCode()).isEqualTo(200);
        assertThat((Integer) JsonPath.read(get.body(), "$.floatCents")).isEqualTo(749);
        assertThat((Integer) JsonPath.read(get.body(), "$.config.playCostCents")).isEqualTo(200);
        var play = request("POST", location + "/plays", null);
        assertThat(play.statusCode()).isEqualTo(200);
        assertThat((String) JsonPath.read(play.body(), "$.outcome")).isEqualTo("SMALL_PRIZE");
        assertThat((Integer) JsonPath.read(play.body(), "$.paidCents")).isEqualTo(549);
        assertThat((Integer) JsonPath.read(play.body(), "$.freePlaysCredited")).isEqualTo(3);
        assertThat((Integer) JsonPath.read(request("GET", location, null).body(), "$.floatCents")).isZero();
        assertThat((Integer) JsonPath.read(request("GET", location, null).body(), "$.freePlays")).isEqualTo(3);
        for (int i = 0; i < 3; i++) {
            assertThat(request("POST", location + "/plays", null).statusCode()).isEqualTo(200);
        }
        assertThat((Integer) JsonPath.read(request("GET", location, null).body(), "$.freePlays")).isZero();
        assertThat(request("POST", location + "/plays", null).statusCode()).isEqualTo(400);
    }
    @Test
    void zeroCostIsRejectedButZeroFloatIsAllowed() throws Exception {
        var invalid = request("POST", "/machines", body(0, 0));
        assertThat(invalid.statusCode()).isEqualTo(400);
        assertThat(invalid.body()).contains("playCostCents");
        assertThat(request("POST", "/machines", body(100, 0)).statusCode()).isEqualTo(201);
    }
    @ParameterizedTest
    @ValueSource(strings = {
        "{", "{}",
        "{\"slotCount\":0,\"colours\":[\"A\",\"B\"],\"k\":1,\"playCostCents\":100,\"startingFloatCents\":0}",
        "{\"slotCount\":4,\"colours\":[\"A\",\"B\"],\"k\":5,\"playCostCents\":100,\"startingFloatCents\":0}",
        "{\"slotCount\":4,\"colours\":[\"A\",\"A\"],\"k\":2,\"playCostCents\":100,\"startingFloatCents\":0}",
        "{\"slotCount\":4,\"colours\":[\"A\"],\"k\":2,\"playCostCents\":100,\"startingFloatCents\":0}",
        "{\"slotCount\":4,\"colours\":[\"A\",\"B\"],\"k\":2,\"playCostCents\":-1,\"startingFloatCents\":0}",
        "{\"slotCount\":4,\"colours\":[\"A\",\"B\"],\"k\":2,\"playCostCents\":100,\"startingFloatCents\":-1}",
        "{\"slotCount\":4,\"colours\":[\"A\",\"B\"],\"k\":0,\"playCostCents\":100,\"startingFloatCents\":0}",
        "{\"slotCount\":4,\"colours\":[\"A\",\"\"],\"k\":2,\"playCostCents\":100,\"startingFloatCents\":0}"
    })
    void invalidRequestsReturnClear400(String body) throws Exception {
        var response = request("POST", "/machines", body);
        assertThat(response.statusCode()).isEqualTo(400);
        assertThat((String) JsonPath.read(response.body(), "$.message")).isNotBlank();
    }
    @Test
    void fractionalCentsAndOverflowingPrizeAreRejected() throws Exception {
        assertThat(request("POST", "/machines", body(100, 1000)
                .replace("1000", "1000.5")).statusCode()).isEqualTo(400);
        assertThat(request("POST", "/machines", body(Long.MAX_VALUE, 1000)).statusCode()).isEqualTo(400);
    }

    @Test
    void missingAndMalformedIdsHaveUsefulErrors() throws Exception {
        String path = "/machines/" + UUID.randomUUID();
        assertThat(request("GET", path, null).statusCode()).isEqualTo(404);
        assertThat(request("POST", path + "/plays", null).statusCode()).isEqualTo(404);
        assertThat(request("GET", "/machines/not-a-uuid", null).statusCode()).isEqualTo(400);
    }
}
