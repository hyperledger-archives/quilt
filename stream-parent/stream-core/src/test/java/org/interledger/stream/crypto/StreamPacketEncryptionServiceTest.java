package org.interledger.stream.crypto;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import org.interledger.encoding.asn.framework.CodecContext;
import org.interledger.stream.StreamPacket;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.io.IOException;

/**
 * Unit tests for {@link StreamPacketEncryptionService}.
 */
public class StreamPacketEncryptionServiceTest {

  @Mock
  private CodecContext streamCodecContextMock;

  @Mock
  private StreamSharedSecretCrypto streamSharedSecretCryptoMock;

  private StreamPacketEncryptionService streamPacketEncryptionService;

  @Before
  public void setUp() {
    this.streamPacketEncryptionService = new StreamPacketEncryptionService(
      streamCodecContextMock, streamSharedSecretCryptoMock
    );
  }

  @Test
  public void toEncrypted() throws IOException {
    streamPacketEncryptionService.toEncrypted(StreamSharedSecret.of(new byte[32]), mock(StreamPacket.class));

    verify(streamCodecContextMock).write(any(), any());
    verify(streamSharedSecretCryptoMock).encrypt(any(), any());

    verifyNoMoreInteractions(streamCodecContextMock);
    verifyNoMoreInteractions(streamSharedSecretCryptoMock);
  }

  @Test
  public void fromEncrypted() throws IOException {
    streamPacketEncryptionService.fromEncrypted(StreamSharedSecret.of(new byte[32]), new byte[32]);

    verify(streamCodecContextMock).read(any(), any());
    verify(streamSharedSecretCryptoMock).decrypt(any(), any());

    verifyNoMoreInteractions(streamCodecContextMock);
    verifyNoMoreInteractions(streamSharedSecretCryptoMock);
  }
}