package org.interledger.quilt.jackson.link;

import org.interledger.link.LinkType;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdScalarSerializer;

import java.io.IOException;

/**
 * Jackson serializer {@link LinkType}.
 */
public class LinkTypeSerializer extends StdScalarSerializer<LinkType> {

  public LinkTypeSerializer() {
    super(LinkType.class, false);
  }

  @Override
  public void serialize(LinkType linkType, JsonGenerator gen, SerializerProvider provider) throws IOException {
    gen.writeString(linkType.value());
  }
}
