package com.rogeriofrsouza.bazaar.inventory;

import com.rogeriofrsouza.bazaar.inventory.item.CreateInventoryItemRequest;
import io.hypersistence.tsid.TSID;
import org.junit.jupiter.api.Test;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.exc.InvalidFormatException;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class JacksonConfigTests {

    private final JsonMapper mapper = JsonMapper.builder()
            .addModule(new JacksonConfig().tsidModule())
            .build();

    @Test
    void readsTsidFromString() {
        TSID id = TSID.Factory.getTsid();

        CreateInventoryItemRequest request = mapper.readValue(
                "{\"productId\":\"" + id + "\",\"quantityOnHand\":1}", CreateInventoryItemRequest.class);

        assertThat(request.productId()).isEqualTo(id);
    }

    @Test
    void readsTsidList() {
        TSID first = TSID.Factory.getTsid();
        TSID second = TSID.Factory.getTsid();

        List<TSID> ids = mapper.readValue("[\"" + first + "\",\"" + second + "\"]", new TypeReference<>() {});

        assertThat(ids).containsExactly(first, second);
    }

    @Test
    void rejectsMalformedTsid() {
        assertThatExceptionOfType(InvalidFormatException.class).isThrownBy(() -> mapper.readValue(
                "{\"productId\":\"not-a-tsid\",\"quantityOnHand\":1}", CreateInventoryItemRequest.class));
    }

    @Test
    void writesTsidAsString() {
        TSID id = TSID.Factory.getTsid();

        assertThat(mapper.writeValueAsString(id)).isEqualTo("\"" + id + "\"");
    }
}
