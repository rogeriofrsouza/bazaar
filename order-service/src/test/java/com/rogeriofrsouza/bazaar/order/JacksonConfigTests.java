package com.rogeriofrsouza.bazaar.order;

import com.rogeriofrsouza.bazaar.order.order.CreateOrderRequest;
import com.rogeriofrsouza.bazaar.order.order.OrderItemRequest;
import io.hypersistence.tsid.TSID;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.exc.InvalidFormatException;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class JacksonConfigTests {

    private final JsonMapper mapper = JsonMapper.builder()
            .addModule(new JacksonConfig().tsidModule())
            .build();

    @Test
    void readsTsidsInOrderItems() {
        TSID first = TSID.Factory.getTsid();
        TSID second = TSID.Factory.getTsid();

        CreateOrderRequest request = mapper.readValue("""
                {"items": [{"productId": "%s", "quantity": 1}, {"productId": "%s", "quantity": 2}]}
                """.formatted(first, second), CreateOrderRequest.class);

        assertThat(request.items()).containsExactly(
                new OrderItemRequest(first, 1),
                new OrderItemRequest(second, 2));
    }

    @Test
    void rejectsMalformedTsid() {
        assertThatExceptionOfType(InvalidFormatException.class).isThrownBy(() -> mapper.readValue(
                "{\"items\": [{\"productId\": \"not-a-tsid\", \"quantity\": 1}]}", CreateOrderRequest.class));
    }

    @Test
    void writesTsidAsString() {
        TSID id = TSID.Factory.getTsid();

        assertThat(mapper.writeValueAsString(id)).isEqualTo("\"" + id + "\"");
    }
}
