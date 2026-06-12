package com.travelmind.llm;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.travelmind.llm.mimo.MimoLlmClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LlmClientFactoryTest {

    private LlmProperties properties;
    private MimoLlmClient mimoClient;
    private LlmClientFactory factory;

    @BeforeEach
    void setUp() {
        properties = new LlmProperties();
        mimoClient = new MimoLlmClient(properties, new ObjectMapper());
        factory = new LlmClientFactory(properties, mimoClient);
    }

    @Test
    void getClient_DefaultProvider_ShouldUseMimoClient() {
        assertSame(mimoClient, factory.getClient());
    }

    @Test
    void getClient_MimoProvider_ShouldUseMimoClient() {
        properties.setProvider("mimo");

        assertSame(mimoClient, factory.getClient());
    }

    @Test
    void getClient_UnsupportedProvider_ShouldThrowException() {
        properties.setProvider("unknown");

        assertThrows(IllegalArgumentException.class, () -> factory.getClient());
    }
}
