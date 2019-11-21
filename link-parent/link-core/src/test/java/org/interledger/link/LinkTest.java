package org.interledger.link;

import static org.assertj.core.api.Assertions.assertThat;

import org.interledger.core.InterledgerPreparePacket;
import org.interledger.core.InterledgerResponsePacket;
import org.interledger.link.exceptions.LinkHandlerAlreadyRegisteredException;

import org.junit.Test;

import java.util.Optional;

/**
 * Unit tests for {@link Link}.
 */
public class LinkTest {

  @Test
  public void defaults() {
    final Link linkInterface = new Link() {
      @Override
      public InterledgerResponsePacket sendPacket(InterledgerPreparePacket preparePacket) {
        return null;
      }

      @Override
      public LinkId getLinkId() {
        return null;
      }

      @Override
      public void setLinkId(LinkId linkId) {

      }

      @Override
      public LinkSettings getLinkSettings() {
        return null;
      }

      @Override
      public void registerLinkHandler(LinkHandler dataHandler) throws LinkHandlerAlreadyRegisteredException {

      }

      @Override
      public Optional<LinkHandler> getLinkHandler() {
        return Optional.empty();
      }

      @Override
      public void unregisterLinkHandler() {

      }
    };
    assertThat(linkInterface.getOperatorAddressSupplier().get()).isEqualTo(Link.SELF);
  }

}
