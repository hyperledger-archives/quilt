package org.interledger.psk;

public class UnencryptedMessageReaderTest {

  // @Test(expected = RuntimeException.class)
  // public void test_BadStatusLine() {
  //
  // String testMessage = "PSK/1.0 GARBAGE\n"
  // + "Encryption: something\n"
  // + "\n"
  // + "binary data goes here";
  //
  // PskMessage.builder(testMessage.getBytes(StandardCharsets.UTF_8)).build();
  // }
  //
  // @Test(expected = RuntimeException.class)
  // public void test_BadStatusLineMajorVersion() {
  // String testMessage = "PSK/2.0\n"
  // + "Encryption: something\n"
  // + "\n"
  // + "binary data goes here";
  //
  // PskMessage.builder(testMessage.getBytes(StandardCharsets.UTF_8)).build();
  // }
  //
  // @Test()
  // public void test_StatusLineMinorVersion() {
  //
  // String testMessage = "PSK/1.1\n"
  // + "Encryption: something\n"
  // + "\n"
  // + "binary data goes here";
  //
  // PskMessage.builder(testMessage.getBytes(StandardCharsets.UTF_8));
  // }
  //
  // @Test(expected = RuntimeException.class)
  // public void test_MissingEncryptionHeader() {
  //
  // String testMessage = "PSK/1.0\n"
  // + "Header: stuff\n"
  // + "\n"
  // + "binary data goes here";
  //
  // PskMessage.builder(testMessage.getBytes(StandardCharsets.UTF_8)).build();
  // }
  //
  // @Test()
  // public void test_NoEncryption() {
  //
  // String testMessage = "PSK/1.0\n"
  // + "Encryption: none\n"
  // + "Testheader: test value\n"
  // + "\n"
  // + "PrivateHeader1: some value\n"
  // + "\n"
  // + "binary data goes here";
  //
  // PskMessage message = PskMessage.builder(testMessage.getBytes(StandardCharsets.UTF_8)).build();
  //
  // assertNotNull(message);
  // assertEquals(2, message.getPublicHeaders().size());
  // assertEquals("Testheader", message.getPublicHeaders().get(1).getName());
  // assertEquals("test value", message.getPublicHeaders().get(1).getValue());
  //
  // assertEquals(1, message.getPrivateHeaders().size());
  // assertEquals("PrivateHeader1", message.getPrivateHeaders().get(0).getName());
  // assertEquals("some value", message.getPrivateHeaders().get(0).getValue());
  //
  // assertArrayEquals("binary data goes here".getBytes(StandardCharsets.UTF_8),
  // message.getData());
  // }
  //
  // @Test()
  // public void test_DuplicateHeaders() {
  //
  // String testMessage = "PSK/1.0\n"
  // + "Encryption: none\n"
  // + "Testheader: test value\n"
  // + "Testheader: \tanother value\n"
  // + "Testheader: value3\t\t\t \n"
  // + "\n"
  // + "PrivateHeader1: some value\n"
  // + "\n"
  // + "binary data goes here";
  //
  //
  // PskMessage message = PskMessage.builder(testMessage.getBytes(StandardCharsets.UTF_8)).build();
  //
  // assertNotNull(message);
  // /* we don't collapse duplicate header values at present. note that we remove leading and
  // * trailing whitespace from header values though. */
  // assertEquals(4, message.getPublicHeaders().size());
  //
  // assertEquals("Testheader", message.getPublicHeaders().get(1).getName());
  // assertEquals("test value", message.getPublicHeaders().get(1).getValue());
  //
  // assertEquals("Testheader", message.getPublicHeaders().get(2).getName());
  // assertEquals("another value", message.getPublicHeaders().get(2).getValue());
  //
  // assertEquals("Testheader", message.getPublicHeaders().get(3).getName());
  // assertEquals("value3", message.getPublicHeaders().get(3).getValue());
  // }
  //
  // @Test()
  // public void test_WithEncryption() {
  // /* this might look weird, but the reader wont try read the data unless the encryption header
  // * type is explicitly set to none 'None' */
  //
  // String testMessage = "PSK/1.0\n"
  // + "Encryption: super-duper-secret-algorithm\n"
  // + "Testheader: test value\n"
  // + "\n"
  // + "PrivateHeader1: some value\n"
  // + "\n"
  // + "binary data goes here";
  //
  // PskMessage message = PskMessage.builder(testMessage.getBytes(StandardCharsets.UTF_8)).build();
  //
  // assertNotNull(message);
  // assertEquals(2, message.getPublicHeaders().size());
  // assertEquals("Testheader", message.getPublicHeaders().get(1).getName());
  // assertEquals("test value", message.getPublicHeaders().get(1).getValue());
  //
  // /* in this case, we wouldn't read the private headers (since they should be encrypted, and the
  // * application data is the private headers and body following. */
  // assertEquals(0, message.getPrivateHeaders().size());
  // assertArrayEquals(testMessage.substring(testMessage.indexOf("PrivateHeader"))
  // .getBytes(StandardCharsets.UTF_8), message.getData());
  // }


}
