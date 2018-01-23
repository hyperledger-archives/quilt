package org.interledger.codecs.asn;

import org.interledger.codecs.MyCustomObject;

public class AsnMyCustomObject extends AsnSequence<MyCustomObject> {

  public AsnMyCustomObject() {
    super(new AsnUTF8String(AsnSizeConstraint.unconstrained()), new AsnUint8());
  }

  @Override
  protected MyCustomObject decode() {
    MyCustomObject obj = new MyCustomObject();
    obj.Property1 = getValueAt(0);
    obj.Property2 = getValueAt(1);
    return obj;
  }

  @Override
  protected void encode(MyCustomObject value) {
    setValueAt(0, value.Property1);
    setValueAt(1, value.Property2);
  }

}
