package org.interledger.link.http;

import static org.interledger.link.http.IlpOverHttpLinkSettings.AUTH_TYPE;
import static org.interledger.link.http.IlpOverHttpLinkSettings.ILP_OVER_HTTP;
import static org.interledger.link.http.IlpOverHttpLinkSettings.INCOMING;
import static org.interledger.link.http.IlpOverHttpLinkSettings.OUTGOING;
import static org.interledger.link.http.IlpOverHttpLinkSettings.SHARED_SECRET;
import static org.interledger.link.http.IlpOverHttpLinkSettings.TOKEN_AUDIENCE;
import static org.interledger.link.http.IlpOverHttpLinkSettings.TOKEN_EXPIRY;
import static org.interledger.link.http.IlpOverHttpLinkSettings.TOKEN_ISSUER;
import static org.interledger.link.http.IlpOverHttpLinkSettings.TOKEN_SUBJECT;
import static org.interledger.link.http.IlpOverHttpLinkSettings.URL;
import static org.interledger.link.http.IncomingLinkSettings.HTTP_INCOMING_AUTH_TYPE;
import static org.interledger.link.http.IncomingLinkSettings.HTTP_INCOMING_SHARED_SECRET;
import static org.interledger.link.http.IncomingLinkSettings.HTTP_INCOMING_TOKEN_AUDIENCE;
import static org.interledger.link.http.IncomingLinkSettings.HTTP_INCOMING_TOKEN_ISSUER;
import static org.interledger.link.http.OutgoingLinkSettings.HTTP_OUTGOING_AUTH_TYPE;
import static org.interledger.link.http.OutgoingLinkSettings.HTTP_OUTGOING_SHARED_SECRET;
import static org.interledger.link.http.OutgoingLinkSettings.HTTP_OUTGOING_TOKEN_AUDIENCE;
import static org.interledger.link.http.OutgoingLinkSettings.HTTP_OUTGOING_TOKEN_EXPIRY;
import static org.interledger.link.http.OutgoingLinkSettings.HTTP_OUTGOING_TOKEN_ISSUER;
import static org.interledger.link.http.OutgoingLinkSettings.HTTP_OUTGOING_TOKEN_SUBJECT;
import static org.interledger.link.http.OutgoingLinkSettings.HTTP_OUTGOING_URL;

import com.google.common.collect.ImmutableMap;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

public abstract class AbstractHttpLinkSettingsTest {

  protected Map<String, Object> customSettingsFlat() {
    return ImmutableMap.<String, Object>builder()
        .put(HTTP_INCOMING_AUTH_TYPE, IlpOverHttpLinkSettings.AuthType.JWT_HS_256.name())
        .put(HTTP_INCOMING_TOKEN_ISSUER, "https://incoming-issuer.example.com/")
        .put(HTTP_INCOMING_SHARED_SECRET, "incoming-credential")
        .put(HTTP_INCOMING_TOKEN_AUDIENCE, "https://incoming-audience.example.com/")

        .put(HTTP_OUTGOING_AUTH_TYPE, IlpOverHttpLinkSettings.AuthType.SIMPLE.name())
        .put(HTTP_OUTGOING_TOKEN_SUBJECT, "outgoing-subject")
        .put(HTTP_OUTGOING_SHARED_SECRET, "outgoing-credential")
        .put(HTTP_OUTGOING_TOKEN_ISSUER, "https://outgoing-issuer.example.com/")
        .put(HTTP_OUTGOING_TOKEN_AUDIENCE, "https://outgoing-audience.example.com/")
        .put(HTTP_OUTGOING_TOKEN_EXPIRY, Duration.ofDays(1).toString())
        .put(HTTP_OUTGOING_URL, "https://outgoing.example.com")

        .build();
  }

  protected Map<String, Object> customSettingsHeirarchical() {
    final Map<String, Object> incomingMap = new HashMap<>();
    incomingMap.put(AUTH_TYPE, IlpOverHttpLinkSettings.AuthType.JWT_HS_256.name());
    incomingMap.put(TOKEN_SUBJECT, "incoming-subject");
    incomingMap.put(SHARED_SECRET, "incoming-credential");
    incomingMap.put(TOKEN_ISSUER, "https://incoming-issuer.example.com/");
    incomingMap.put(TOKEN_AUDIENCE, "https://incoming-audience.example.com/");

    final Map<String, Object> outgoingMap = new HashMap<>();
    outgoingMap.put(AUTH_TYPE, IlpOverHttpLinkSettings.AuthType.SIMPLE.name());
    outgoingMap.put(TOKEN_SUBJECT, "outgoing-subject");
    outgoingMap.put(SHARED_SECRET, "outgoing-credential");
    outgoingMap.put(TOKEN_ISSUER, "https://outgoing-issuer.example.com/");
    outgoingMap.put(TOKEN_AUDIENCE, "https://outgoing-audience.example.com/");
    outgoingMap.put(TOKEN_EXPIRY, Duration.ofDays(2).toString());
    outgoingMap.put(URL, "https://outgoing.example.com/");

    final Map<String, Object> httpMap = new HashMap<>();
    httpMap.put(INCOMING, incomingMap);
    httpMap.put(OUTGOING, outgoingMap);

    final Map<String, Object> customSettings = new HashMap<>();
    customSettings.put(ILP_OVER_HTTP, httpMap);

    return customSettings;
  }

}
