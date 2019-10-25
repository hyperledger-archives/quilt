package org.interledger.quilt.jackson.link;

import org.interledger.link.LinkId;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdScalarSerializer;

import java.io.IOException;

/**
 * Jackson serializer {@link LinkId}.
 */
public class LinkIdSerializer extends StdScalarSerializer<LinkId> {

  public LinkIdSerializer() {
    super(LinkId.class, false);
  }

  @Override
  public void serialize(LinkId linkId, JsonGenerator gen, SerializerProvider provider) throws IOException {
    gen.writeString(linkId.value());
  }
}
