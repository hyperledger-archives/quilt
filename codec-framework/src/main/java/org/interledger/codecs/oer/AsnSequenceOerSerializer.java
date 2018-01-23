package org.interledger.codecs.oer;

import org.interledger.codecs.asn.AsnSequence;
import org.interledger.codecs.framework.AsnObjectSerializer;
import org.interledger.codecs.framework.SerializationContext;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class AsnSequenceOerSerializer implements AsnObjectSerializer<AsnSequence> {

  @Override
  public void read(SerializationContext context, AsnSequence instance, InputStream inputStream)
      throws IOException {
    for (int i = 0; i < instance.size(); i++) {
      context.read(instance.getElementAt(i), inputStream);
    }
  }

  @Override
  public void write(SerializationContext context, AsnSequence instance, OutputStream
      outputStream) throws IOException {
    for (int i = 0; i < instance.size(); i++) {
      context.write(instance.getElementAt(i), outputStream);
    }
  }
}
