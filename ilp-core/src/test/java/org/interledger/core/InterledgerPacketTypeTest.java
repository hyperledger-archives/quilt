package org.interledger.core;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * Unit tests for {@link InterledgerPacketType}.
 */
public class InterledgerPacketTypeTest {

    @Test
    public void testInterledgerPacketType() {
        short TEST_PREPARE_CODE = (short) 12;
        final InterledgerPacketType preparePacketType = InterledgerPacketType.fromCode(TEST_PREPARE_CODE);

        short TEST_FULFILL_CODE = (short) 13;
        final InterledgerPacketType fulfillPacketType = InterledgerPacketType.fromCode(TEST_FULFILL_CODE);

        short TEST_REJECT_CODE = (short) 14;
        final InterledgerPacketType rejectPacketType = InterledgerPacketType.fromCode(TEST_REJECT_CODE);

        assertThat(preparePacketType.getType()).isEqualTo(InterledgerPacketType.PREPARE.getType());
        assertThat(fulfillPacketType.getType()).isEqualTo(InterledgerPacketType.FULFILL.getType());
        assertThat(rejectPacketType.getType()).isEqualTo(InterledgerPacketType.REJECT.getType());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testIllegalInterledgerPacketType() {
        short TEST_ILLEGAL_CODE = (short) -1;
        try {
            InterledgerPacketType.fromCode(TEST_ILLEGAL_CODE);
            fail("Should have thrown an exception since illegal code was used");
        } catch (Exception e) {
            assertThat(e.getMessage()).isEqualTo("Unknown StreamPacketType: -1");
            throw e;
        }
    }
}
