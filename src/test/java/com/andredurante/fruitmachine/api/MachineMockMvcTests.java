package com.andredurante.fruitmachine.api;

import java.util.Random;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.andredurante.fruitmachine.service.MachineService;
import com.andredurante.fruitmachine.storage.InMemoryMachineRepository;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.startsWith;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class MachineMockMvcTests {
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        // Real service and isolated repository; only the random source is mocked.
        MachineService service = new MachineService(new InMemoryMachineRepository(), () -> {
            Random random = mock(Random.class);
            when(random.nextInt(3)).thenReturn(0, 0, 1, 2);
            return random;
        });
        mvc = MockMvcBuilders.standaloneSetup(new MachineController(service))
                .setControllerAdvice(new ApiExceptionHandler())
                .build();
    }

    private String configuration(int k) {
        return """
                {
                  "slotCount": 4,
                  "colours": ["BLACK", "WHITE", "GREEN"],
                  "k": %d,
                  "playCostCents": 100,
                  "startingFloatCents": 100000
                }
                """.formatted(k);
    }

    @Test
    void configureThenPlayAndReadUpdatedMachine() throws Exception {
        String location = mvc.perform(post("/machines")
                        .contentType(MediaType.APPLICATION_JSON).content(configuration(2)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", startsWith("/machines/")))
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.floatCents").value(100000))
                .andReturn().getResponse().getHeader("Location");

        mvc.perform(get(location))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.config.slotCount").value(4))
                .andExpect(jsonPath("$.config.colours", contains("BLACK", "WHITE", "GREEN")))
                .andExpect(jsonPath("$.config.k").value(2))
                .andExpect(jsonPath("$.config.playCostCents").value(100))
                .andExpect(jsonPath("$.config.startingFloatCents").value(100000));

        mvc.perform(post(location + "/plays"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.outcome").value("SMALL_PRIZE"))
                .andExpect(jsonPath("$.slots", contains("BLACK", "BLACK", "WHITE", "GREEN")))
                .andExpect(jsonPath("$.prizeCents").value(500))
                .andExpect(jsonPath("$.paidCents").value(500))
                .andExpect(jsonPath("$.freePlaysCredited").value(0))
                .andExpect(jsonPath("$.floatCents").value(99400));

        mvc.perform(get(location))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.floatCents").value(99400))
                .andExpect(jsonPath("$.config.startingFloatCents").value(100000));
    }

    @Test
    void playingUnknownMachineReturns404() throws Exception {
        UUID id = UUID.randomUUID();
        mvc.perform(post("/machines/{id}/plays", id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Machine not found: " + id));
    }

    @Test
    void windowGreaterThanSlotCountReturns400NamingK() throws Exception {
        mvc.perform(post("/machines")
                        .contentType(MediaType.APPLICATION_JSON).content(configuration(5)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", startsWith("k ")))
                .andExpect(jsonPath("$.message", containsString("slotCount")));
    }
}
