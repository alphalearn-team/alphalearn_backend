package com.example.demo.friendship.shared;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.Test;

class OrderedUuidPairTest {

    @Test
    void ordersProblematicUuidPairByCanonicalString() {
        UUID left = UUID.fromString("cea97a61-e69e-40b9-b3ec-ec2e5a0c734f");
        UUID right = UUID.fromString("34a63fc4-97d7-4de1-a2b0-c38f6202a360");

        OrderedUuidPair pair = OrderedUuidPair.of(left, right);

        assertThat(pair.first()).isEqualTo(right);
        assertThat(pair.second()).isEqualTo(left);
    }
}
