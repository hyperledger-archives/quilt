package org.interledger.quilt.jackson.link;

import org.interledger.link.LinkId;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import java.io.IOException;

/**
 * Jackson deserializer for {@link LinkId}.
 */
public class LinkIdDeserializer extends StdDeserializer<LinkId> {

  public LinkIdDeserializer() {
    super(LinkId.class);
  }

  @Override
  public LinkId deserialize(JsonParser jsonParser, DeserializationContext ctxt) throws IOException {
    return LinkId.of(jsonParser.getText());
  }
}
