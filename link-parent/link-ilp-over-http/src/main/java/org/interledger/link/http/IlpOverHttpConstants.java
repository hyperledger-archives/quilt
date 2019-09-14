package org.interledger.link.http;

import static com.google.common.net.MediaType.OCTET_STREAM;

import okhttp3.MediaType;

/**
 * Common headers used in the BLAST protocol.
 */
public interface IlpOverHttpConstants {

  String OCTET_STREAM_STRING = OCTET_STREAM.toString();
  MediaType APPLICATION_OCTET_STREAM = MediaType.parse(OCTET_STREAM_STRING);

  String BEARER = "Bearer ";
  String ILP_OPERATOR_ADDRESS_VALUE = "ILP-Operator-Address";
}
