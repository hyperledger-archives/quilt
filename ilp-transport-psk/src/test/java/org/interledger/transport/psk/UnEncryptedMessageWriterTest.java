package org.interledger.transport.psk;

public class UnEncryptedMessageWriterTest {

  // @Test
  // public void test() {
  //
  // ReceiverPskContext context = ReceiverPskContext.seed(new byte[16]);
  //
  // InterledgerPayment payment = PskPayment.builder(context)
  // .addPrivateHeader("private header", "\tprivate\theader\tvalue\t")
  // .data("{some_application_data: 123}".getBytes(StandardCharsets.UTF_8))
  // .build();
  //
  // assertNotNull(payment.getData());
  //
  // /* we happen to know that the PSK message is just a UTF-8 encoded string */
  // String messageString = new String(payment.getData(), StandardCharsets.UTF_8);
  //
  // /* hand-craft what we expect to see. a bit awkward, since the header order isn't terribly
  // * predictable from the outside. */
  // String expected = "PSK/1.0\n"
  // + "Encryption: none\n"
  // + "\n"
  // + "private header: private\theader\tvalue\n"
  // + "\n"
  // + "{some_application_data: 123}";
  //
  // assertEquals(expected, messageString);
  // }

}
