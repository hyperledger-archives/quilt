package org.interledger.link.http;

import static org.interledger.link.http.IlpOverHttpLinkSettings.AUTH_TOKEN;
import static org.interledger.link.http.IlpOverHttpLinkSettings.AUTH_TYPE;
import static org.interledger.link.http.IlpOverHttpLinkSettings.ILP_OVER_HTTP;
import static org.interledger.link.http.IlpOverHttpLinkSettings.INCOMING;
import static org.interledger.link.http.IlpOverHttpLinkSettings.JWT;
import static org.interledger.link.http.IlpOverHttpLinkSettings.OUTGOING;
import static org.interledger.link.http.IlpOverHttpLinkSettings.SHARED_SECRET;
import static org.interledger.link.http.IlpOverHttpLinkSettings.SIMPLE;
import static org.interledger.link.http.IlpOverHttpLinkSettings.TOKEN_AUDIENCE;
import static org.interledger.link.http.IlpOverHttpLinkSettings.TOKEN_EXPIRY;
import static org.interledger.link.http.IlpOverHttpLinkSettings.TOKEN_ISSUER;
import static org.interledger.link.http.IlpOverHttpLinkSettings.TOKEN_SUBJECT;
import static org.interledger.link.http.IlpOverHttpLinkSettings.URL;
import static org.interledger.link.http.IncomingLinkSettings.HTTP_INCOMING_AUTH_TYPE;
import static org.interledger.link.http.IncomingLinkSettings.HTTP_INCOMING_SHARED_SECRET;
import static org.interledger.link.http.IncomingLinkSettings.HTTP_INCOMING_SIMPLE_AUTH_TOKEN;
import static org.interledger.link.http.IncomingLinkSettings.HTTP_INCOMING_TOKEN_AUDIENCE;
import static org.interledger.link.http.IncomingLinkSettings.HTTP_INCOMING_TOKEN_ISSUER;
import static org.interledger.link.http.IncomingLinkSettings.HTTP_INCOMING_TOKEN_SUBJECT;
import static org.interledger.link.http.OutgoingLinkSettings.HTTP_OUTGOING_AUTH_TYPE;
import static org.interledger.link.http.OutgoingLinkSettings.HTTP_OUTGOING_MULTILATERAL;
import static org.interledger.link.http.OutgoingLinkSettings.HTTP_OUTGOING_SHARED_SECRET;
import static org.interledger.link.http.OutgoingLinkSettings.HTTP_OUTGOING_SIMPLE_AUTH_TOKEN;
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

  static Map<String, Object> customSettingsJwtFlat(IlpOverHttpLinkSettings.AuthType authType) {
    return ImmutableMap.<String, Object>builder()
        .put(HTTP_INCOMING_AUTH_TYPE, authType.toString())
        .put(HTTP_INCOMING_TOKEN_ISSUER, "https://incoming-issuer.example.com/")
        .put(HTTP_INCOMING_SHARED_SECRET, "incoming-credential")
        .put(HTTP_INCOMING_TOKEN_SUBJECT, "incoming-subject")
        .put(HTTP_INCOMING_TOKEN_AUDIENCE, "https://incoming-audience.example.com/")
        .put(HTTP_OUTGOING_AUTH_TYPE, authType.toString())
        .put(HTTP_OUTGOING_TOKEN_SUBJECT, "outgoing-subject")
        .put(HTTP_OUTGOING_SHARED_SECRET, "outgoing-credential")
        .put(HTTP_OUTGOING_TOKEN_ISSUER, "https://outgoing-issuer.example.com/")
        .put(HTTP_OUTGOING_TOKEN_AUDIENCE, "https://outgoing-audience.example.com/")
        .put(HTTP_OUTGOING_TOKEN_EXPIRY, Duration.ofDays(1).toString())
        .put(HTTP_OUTGOING_URL, "https://outgoing.example.com/")
        .put(HTTP_OUTGOING_MULTILATERAL, true)
        .build();
  }

  static Map<String, Object> customSettingsJwtHierarchical(IlpOverHttpLinkSettings.AuthType authType) {
    final Map<String, Object> incomingMap = new HashMap<>();
    final Map<String, Object> incomingAuthMap = new HashMap<>();
    incomingAuthMap.put(TOKEN_SUBJECT, "incoming-subject");
    incomingAuthMap.put(SHARED_SECRET, "incoming-credential");
    incomingAuthMap.put(TOKEN_ISSUER, "https://incoming-issuer.example.com/");
    incomingAuthMap.put(TOKEN_AUDIENCE, "https://incoming-audience.example.com/");
    incomingMap.put(AUTH_TYPE, authType.toString());
    incomingMap.put(JWT, incomingAuthMap);

    final Map<String, Object> outgoingMap = new HashMap<>();
    final Map<String, Object> outgoingAuthMap = new HashMap<>();
    outgoingAuthMap.put(TOKEN_SUBJECT, "outgoing-subject");
    outgoingAuthMap.put(SHARED_SECRET, "outgoing-credential");
    outgoingAuthMap.put(TOKEN_ISSUER, "https://outgoing-issuer.example.com/");
    outgoingAuthMap.put(TOKEN_AUDIENCE, "https://outgoing-audience.example.com/");
    outgoingAuthMap.put(TOKEN_EXPIRY, Duration.ofDays(2).toString());
    outgoingMap.put(JWT, outgoingAuthMap);
    outgoingMap.put(AUTH_TYPE, authType.toString());
    outgoingMap.put(URL, "https://outgoing.example.com/");
    outgoingMap.put("multilateral", true);

    final Map<String, Object> httpMap = new HashMap<>();
    httpMap.put(INCOMING, incomingMap);
    httpMap.put(OUTGOING, outgoingMap);

    final Map<String, Object> customSettings = new HashMap<>();
    customSettings.put(ILP_OVER_HTTP, httpMap);

    return customSettings;
  }

  static Map<String, Object> customSettingsSimpleFlat() {
    return ImmutableMap.<String, Object>builder()
        .put(HTTP_INCOMING_AUTH_TYPE, "SIMPLE")
        .put(HTTP_INCOMING_SIMPLE_AUTH_TOKEN, "incoming-secret")
        .put(HTTP_OUTGOING_AUTH_TYPE, "SIMPLE")
        .put(HTTP_OUTGOING_SIMPLE_AUTH_TOKEN, "outgoing-secret")
        .put(HTTP_OUTGOING_URL, "https://outgoing.example.com/")
        .put(HTTP_OUTGOING_MULTILATERAL, false)
        .build();
  }

  static Map<String, Object> customSettingsSimpleHierarchical() {
    final Map<String, Object> incomingMap = new HashMap<>();
    final Map<String, Object> incomingAuthMap = new HashMap<>();
    incomingAuthMap.put(AUTH_TOKEN, "incoming-secret");
    incomingMap.put(AUTH_TYPE, "SIMPLE");
    incomingMap.put(SIMPLE, incomingAuthMap);

    final Map<String, Object> outgoingMap = new HashMap<>();
    final Map<String, Object> outgoingAuthMap = new HashMap<>();
    outgoingAuthMap.put(AUTH_TOKEN, "outgoing-secret");
    outgoingMap.put(AUTH_TYPE, "SIMPLE");
    outgoingMap.put(SIMPLE, outgoingAuthMap);
    outgoingMap.put(URL, "https://outgoing.example.com/");
    outgoingMap.put("multilateral", false);

    final Map<String, Object> httpMap = new HashMap<>();
    httpMap.put(INCOMING, incomingMap);
    httpMap.put(OUTGOING, outgoingMap);

    final Map<String, Object> customSettings = new HashMap<>();
    customSettings.put(ILP_OVER_HTTP, httpMap);

    return customSettings;
  }
}
