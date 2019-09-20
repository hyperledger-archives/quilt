package org.interledger.link.http;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.interledger.core.InterledgerAddress;
import org.interledger.core.InterledgerErrorCode;
import org.interledger.core.InterledgerPreparePacket;
import org.interledger.core.InterledgerResponsePacket;
import org.interledger.encoding.asn.framework.CodecContext;
import org.interledger.link.LinkId;
import org.interledger.link.exceptions.LinkException;
import org.interledger.link.http.auth.BearerTokenSupplier;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.Call;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.io.IOException;

/**
 * Unit tests for {@link IlpOverHttpLink}.
 */
public class IlpOverHttpLinkTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  @Rule
  public MockitoRule mockitoRule = MockitoJUnit.rule();
  @Mock
  IlpOverHttpLinkSettings settings;
  @Mock
  OutgoingLinkSettings outgoingLinkSettings;
  @Mock
  IncomingLinkSettings incomingLinkSettings;
  @Mock
  OkHttpClient httpClient;
  @Mock
  ObjectMapper objectMapper;
  @Mock
  CodecContext codecContext;
  @Mock
  BearerTokenSupplier bearerTokenSupplier;
  @Mock
  InterledgerPreparePacket packet;

  private IlpOverHttpLink link;

  @Before
  public void setup() {
    link = new IlpOverHttpLink(
        () -> InterledgerAddress.of("example.destination"),
        settings,
        httpClient,
        objectMapper,
        codecContext,
        bearerTokenSupplier
    );
    link.setLinkId(LinkId.of("pepe silvia"));
    when(settings.outgoingHttpLinkSettings()).thenReturn(outgoingLinkSettings);
    when(settings.incomingHttpLinkSettings()).thenReturn(incomingLinkSettings);
    when(outgoingLinkSettings.url()).thenReturn(HttpUrl.get("https://cannotspellsurgerywithouturges.com"));
    packet = mock(InterledgerPreparePacket.class);
  }

  @Test
  public void sendFailsOnBadWrite() throws Exception {
    expectedException.expect(LinkException.class);
    doThrow(IOException.class).when(codecContext).write(any(), any());
    link.sendPacket(packet);
  }

  @Test
  public void rejectOnNotAuthed() throws Exception {
    mockCall(401);
    expectedException.expect(LinkException.class);
    expectedException.expectMessage("Unable to connect to remote ILP-over-HTTP Link: " +
        "Invalid Bearer Token. response=Response{" +
        "protocol=h2, code=401, message=stop asking me to set stuff, url=https://existentialcrisis.com/" +
        "}");
    link.sendPacket(packet);
  }

  @Test
  public void rejectOnForbidden() throws Exception {
    mockCall(403);
    expectedException.expect(LinkException.class);
    expectedException.expectMessage("Unable to connect to remote ILP-over-HTTP Link: " +
        "Invalid Bearer Token. response=Response{" +
        "protocol=h2, code=403, message=stop asking me to set stuff, url=https://existentialcrisis.com/" +
        "}");
    link.sendPacket(packet);
  }

  @Test
  public void rejectOnBadRequest() throws Exception {
    mockCall(422);
    InterledgerResponsePacket responsePacket = link.sendPacket(packet);
    assertThat(responsePacket).extracting("code", "message")
        .containsExactly(InterledgerErrorCode.F00_BAD_REQUEST, "{}");
  }

  @Test
  public void rejectOnInternalError() throws Exception {
    mockCall(500);
    InterledgerResponsePacket responsePacket = link.sendPacket(packet);
    assertThat(responsePacket).extracting("code", "message")
        .containsExactly(InterledgerErrorCode.T00_INTERNAL_ERROR, "{}");
  }

  @Test
  public void success() throws Exception {
    mockCall(200);
    InterledgerResponsePacket success = mock(InterledgerResponsePacket.class);
    when(codecContext.read(any(), any())).thenReturn(success);
    InterledgerResponsePacket responsePacket = link.sendPacket(packet);
    assertThat(responsePacket).isEqualTo(success);
    verify(codecContext, times(1)).read(any(), any());
  }

  @Test
  public void fallThrough() throws Exception {
    mockCall(200);
    when(codecContext.read(any(), any())).thenThrow(new IOException("i messed up"));
    expectedException.expect(LinkException.class);
    expectedException.expectMessage("i messed up");
    InterledgerResponsePacket responsePacket = link.sendPacket(packet);
    verify(codecContext, times(1)).read(any(), any());
  }

  @Test
  public void testConnection() throws Exception {
    mockCall(200);
    link.testConnection();
  }

  @Test
  public void testConnectionFailsOn406() throws Exception {
    mockCall(406);
    expectedException.expect(LinkException.class);
    expectedException.expectMessage("Remote peer-link DOES NOT support ILP-over-HTTP. " +
        "tokenSubject=null " +
        "url=https://cannotspellsurgerywithouturges.com/ " +
        "response=Response{" +
        "protocol=h2, " +
        "code=406, " +
        "message=stop asking me to set stuff, " +
        "url=https://existentialcrisis.com/" +
        "}");
    link.testConnection();
  }

  @Test
  public void testConnectionFailsOn415() throws Exception {
    mockCall(415);
    expectedException.expect(LinkException.class);
    expectedException.expectMessage("Remote peer-link DOES NOT support ILP-over-HTTP. " +
        "tokenSubject=null " +
        "url=https://cannotspellsurgerywithouturges.com/ " +
        "response=Response{" +
        "protocol=h2, " +
        "code=415, " +
        "message=stop asking me to set stuff, " +
        "url=https://existentialcrisis.com/" +
        "}");
    link.testConnection();
  }

  @Test
  public void testConnectionFailsOnOther() throws Exception {
    mockCall(422);
    expectedException.expect(LinkException.class);
    expectedException.expectMessage("Unable to connect to ILP-over-HTTP. " +
        "tokenSubject=null " +
        "url=https://cannotspellsurgerywithouturges.com/ " +
        "response=Response{" +
        "protocol=h2, " +
        "code=422, " +
        "message=stop asking me to set stuff, " +
        "url=https://existentialcrisis.com/" +
        "}");
    link.testConnection();
  }

  @Test
  public void testConnectionThrowsUnexpected() throws Exception {
    Call call = mock(Call.class);
    when(httpClient.newCall(any())).thenReturn(call);
    when(call.execute()).thenThrow(new IOException("hey a penny"));
    expectedException.expect(LinkException.class);
    expectedException.expectMessage("hey a penny");
    link.testConnection();
  }

  private Response mockCall(int code) throws Exception {
    Request request = new Request.Builder()
        .url("https://existentialcrisis.com")
        .build();

    Response response = new Response.Builder()
        .request(request)
        .protocol(Protocol.HTTP_2)
        .code(code)
        .message("stop asking me to set stuff")
        .body(ResponseBody.create("{}", MediaType.get("application/json; charset=utf-8")))
        .build();
    Call call = mock(Call.class);
    when(httpClient.newCall(any())).thenReturn(call);
    when(call.execute()).thenReturn(response);
    return response;
  }
}
