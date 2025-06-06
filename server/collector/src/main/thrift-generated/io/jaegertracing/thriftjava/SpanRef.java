/**
 * Autogenerated by Thrift Compiler (0.19.0)
 *
 * DO NOT EDIT UNLESS YOU ARE SURE THAT YOU KNOW WHAT YOU ARE DOING
 *  @generated
 */
package io.jaegertracing.thriftjava;

@SuppressWarnings({"cast", "rawtypes", "serial", "unchecked", "unused"})
@javax.annotation.Generated(value = "Autogenerated by Thrift Compiler (0.19.0)", date = "2025-05-28")
public class SpanRef implements org.apache.thrift.TBase<SpanRef, SpanRef._Fields>, java.io.Serializable, Cloneable, Comparable<SpanRef> {
  private static final org.apache.thrift.protocol.TStruct STRUCT_DESC = new org.apache.thrift.protocol.TStruct("SpanRef");

  private static final org.apache.thrift.protocol.TField REF_TYPE_FIELD_DESC = new org.apache.thrift.protocol.TField("refType", org.apache.thrift.protocol.TType.I32, (short)1);
  private static final org.apache.thrift.protocol.TField TRACE_ID_LOW_FIELD_DESC = new org.apache.thrift.protocol.TField("traceIdLow", org.apache.thrift.protocol.TType.I64, (short)2);
  private static final org.apache.thrift.protocol.TField TRACE_ID_HIGH_FIELD_DESC = new org.apache.thrift.protocol.TField("traceIdHigh", org.apache.thrift.protocol.TType.I64, (short)3);
  private static final org.apache.thrift.protocol.TField SPAN_ID_FIELD_DESC = new org.apache.thrift.protocol.TField("spanId", org.apache.thrift.protocol.TType.I64, (short)4);

  private static final org.apache.thrift.scheme.SchemeFactory STANDARD_SCHEME_FACTORY = new SpanRefStandardSchemeFactory();
  private static final org.apache.thrift.scheme.SchemeFactory TUPLE_SCHEME_FACTORY = new SpanRefTupleSchemeFactory();

  private @org.apache.thrift.annotation.Nullable SpanRefType refType; // required
  private long traceIdLow; // required
  private long traceIdHigh; // required
  private long spanId; // required

  /** The set of fields this struct contains, along with convenience methods for finding and manipulating them. */
  public enum _Fields implements org.apache.thrift.TFieldIdEnum {
    /**
     * 
     * @see SpanRefType
     */
    REF_TYPE((short)1, "refType"),
    TRACE_ID_LOW((short)2, "traceIdLow"),
    TRACE_ID_HIGH((short)3, "traceIdHigh"),
    SPAN_ID((short)4, "spanId");

    private static final java.util.Map<String, _Fields> byName = new java.util.HashMap<String, _Fields>();

    static {
      for (_Fields field : java.util.EnumSet.allOf(_Fields.class)) {
        byName.put(field.getFieldName(), field);
      }
    }

    /**
     * Find the _Fields constant that matches fieldId, or null if its not found.
     */
    @org.apache.thrift.annotation.Nullable
    public static _Fields findByThriftId(int fieldId) {
      switch(fieldId) {
        case 1: // REF_TYPE
          return REF_TYPE;
        case 2: // TRACE_ID_LOW
          return TRACE_ID_LOW;
        case 3: // TRACE_ID_HIGH
          return TRACE_ID_HIGH;
        case 4: // SPAN_ID
          return SPAN_ID;
        default:
          return null;
      }
    }

    /**
     * Find the _Fields constant that matches fieldId, throwing an exception
     * if it is not found.
     */
    public static _Fields findByThriftIdOrThrow(int fieldId) {
      _Fields fields = findByThriftId(fieldId);
      if (fields == null) throw new IllegalArgumentException("Field " + fieldId + " doesn't exist!");
      return fields;
    }

    /**
     * Find the _Fields constant that matches name, or null if its not found.
     */
    @org.apache.thrift.annotation.Nullable
    public static _Fields findByName(String name) {
      return byName.get(name);
    }

    private final short _thriftId;
    private final String _fieldName;

    _Fields(short thriftId, String fieldName) {
      _thriftId = thriftId;
      _fieldName = fieldName;
    }

    @Override
    public short getThriftFieldId() {
      return _thriftId;
    }

    @Override
    public String getFieldName() {
      return _fieldName;
    }
  }

  // isset id assignments
  private static final int __TRACEIDLOW_ISSET_ID = 0;
  private static final int __TRACEIDHIGH_ISSET_ID = 1;
  private static final int __SPANID_ISSET_ID = 2;
  private byte __isset_bitfield = 0;
  public static final java.util.Map<_Fields, org.apache.thrift.meta_data.FieldMetaData> metaDataMap;
  static {
    java.util.Map<_Fields, org.apache.thrift.meta_data.FieldMetaData> tmpMap = new java.util.EnumMap<_Fields, org.apache.thrift.meta_data.FieldMetaData>(_Fields.class);
    tmpMap.put(_Fields.REF_TYPE, new org.apache.thrift.meta_data.FieldMetaData("refType", org.apache.thrift.TFieldRequirementType.REQUIRED, 
        new org.apache.thrift.meta_data.EnumMetaData(org.apache.thrift.protocol.TType.ENUM, SpanRefType.class)));
    tmpMap.put(_Fields.TRACE_ID_LOW, new org.apache.thrift.meta_data.FieldMetaData("traceIdLow", org.apache.thrift.TFieldRequirementType.REQUIRED, 
        new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.I64)));
    tmpMap.put(_Fields.TRACE_ID_HIGH, new org.apache.thrift.meta_data.FieldMetaData("traceIdHigh", org.apache.thrift.TFieldRequirementType.REQUIRED, 
        new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.I64)));
    tmpMap.put(_Fields.SPAN_ID, new org.apache.thrift.meta_data.FieldMetaData("spanId", org.apache.thrift.TFieldRequirementType.REQUIRED, 
        new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.I64)));
    metaDataMap = java.util.Collections.unmodifiableMap(tmpMap);
    org.apache.thrift.meta_data.FieldMetaData.addStructMetaDataMap(SpanRef.class, metaDataMap);
  }

  public SpanRef() {
  }

  public SpanRef(
    SpanRefType refType,
    long traceIdLow,
    long traceIdHigh,
    long spanId)
  {
    this();
    this.refType = refType;
    this.traceIdLow = traceIdLow;
    setTraceIdLowIsSet(true);
    this.traceIdHigh = traceIdHigh;
    setTraceIdHighIsSet(true);
    this.spanId = spanId;
    setSpanIdIsSet(true);
  }

  /**
   * Performs a deep copy on <i>other</i>.
   */
  public SpanRef(SpanRef other) {
    __isset_bitfield = other.__isset_bitfield;
    if (other.isSetRefType()) {
      this.refType = other.refType;
    }
    this.traceIdLow = other.traceIdLow;
    this.traceIdHigh = other.traceIdHigh;
    this.spanId = other.spanId;
  }

  @Override
  public SpanRef deepCopy() {
    return new SpanRef(this);
  }

  @Override
  public void clear() {
    this.refType = null;
    setTraceIdLowIsSet(false);
    this.traceIdLow = 0;
    setTraceIdHighIsSet(false);
    this.traceIdHigh = 0;
    setSpanIdIsSet(false);
    this.spanId = 0;
  }

  /**
   * 
   * @see SpanRefType
   */
  @org.apache.thrift.annotation.Nullable
  public SpanRefType getRefType() {
    return this.refType;
  }

  /**
   * 
   * @see SpanRefType
   */
  public SpanRef setRefType(@org.apache.thrift.annotation.Nullable SpanRefType refType) {
    this.refType = refType;
    return this;
  }

  public void unsetRefType() {
    this.refType = null;
  }

  /** Returns true if field refType is set (has been assigned a value) and false otherwise */
  public boolean isSetRefType() {
    return this.refType != null;
  }

  public void setRefTypeIsSet(boolean value) {
    if (!value) {
      this.refType = null;
    }
  }

  public long getTraceIdLow() {
    return this.traceIdLow;
  }

  public SpanRef setTraceIdLow(long traceIdLow) {
    this.traceIdLow = traceIdLow;
    setTraceIdLowIsSet(true);
    return this;
  }

  public void unsetTraceIdLow() {
    __isset_bitfield = org.apache.thrift.EncodingUtils.clearBit(__isset_bitfield, __TRACEIDLOW_ISSET_ID);
  }

  /** Returns true if field traceIdLow is set (has been assigned a value) and false otherwise */
  public boolean isSetTraceIdLow() {
    return org.apache.thrift.EncodingUtils.testBit(__isset_bitfield, __TRACEIDLOW_ISSET_ID);
  }

  public void setTraceIdLowIsSet(boolean value) {
    __isset_bitfield = org.apache.thrift.EncodingUtils.setBit(__isset_bitfield, __TRACEIDLOW_ISSET_ID, value);
  }

  public long getTraceIdHigh() {
    return this.traceIdHigh;
  }

  public SpanRef setTraceIdHigh(long traceIdHigh) {
    this.traceIdHigh = traceIdHigh;
    setTraceIdHighIsSet(true);
    return this;
  }

  public void unsetTraceIdHigh() {
    __isset_bitfield = org.apache.thrift.EncodingUtils.clearBit(__isset_bitfield, __TRACEIDHIGH_ISSET_ID);
  }

  /** Returns true if field traceIdHigh is set (has been assigned a value) and false otherwise */
  public boolean isSetTraceIdHigh() {
    return org.apache.thrift.EncodingUtils.testBit(__isset_bitfield, __TRACEIDHIGH_ISSET_ID);
  }

  public void setTraceIdHighIsSet(boolean value) {
    __isset_bitfield = org.apache.thrift.EncodingUtils.setBit(__isset_bitfield, __TRACEIDHIGH_ISSET_ID, value);
  }

  public long getSpanId() {
    return this.spanId;
  }

  public SpanRef setSpanId(long spanId) {
    this.spanId = spanId;
    setSpanIdIsSet(true);
    return this;
  }

  public void unsetSpanId() {
    __isset_bitfield = org.apache.thrift.EncodingUtils.clearBit(__isset_bitfield, __SPANID_ISSET_ID);
  }

  /** Returns true if field spanId is set (has been assigned a value) and false otherwise */
  public boolean isSetSpanId() {
    return org.apache.thrift.EncodingUtils.testBit(__isset_bitfield, __SPANID_ISSET_ID);
  }

  public void setSpanIdIsSet(boolean value) {
    __isset_bitfield = org.apache.thrift.EncodingUtils.setBit(__isset_bitfield, __SPANID_ISSET_ID, value);
  }

  @Override
  public void setFieldValue(_Fields field, @org.apache.thrift.annotation.Nullable Object value) {
    switch (field) {
    case REF_TYPE:
      if (value == null) {
        unsetRefType();
      } else {
        setRefType((SpanRefType)value);
      }
      break;

    case TRACE_ID_LOW:
      if (value == null) {
        unsetTraceIdLow();
      } else {
        setTraceIdLow((Long)value);
      }
      break;

    case TRACE_ID_HIGH:
      if (value == null) {
        unsetTraceIdHigh();
      } else {
        setTraceIdHigh((Long)value);
      }
      break;

    case SPAN_ID:
      if (value == null) {
        unsetSpanId();
      } else {
        setSpanId((Long)value);
      }
      break;

    }
  }

  @org.apache.thrift.annotation.Nullable
  @Override
  public Object getFieldValue(_Fields field) {
    switch (field) {
    case REF_TYPE:
      return getRefType();

    case TRACE_ID_LOW:
      return getTraceIdLow();

    case TRACE_ID_HIGH:
      return getTraceIdHigh();

    case SPAN_ID:
      return getSpanId();

    }
    throw new IllegalStateException();
  }

  /** Returns true if field corresponding to fieldID is set (has been assigned a value) and false otherwise */
  @Override
  public boolean isSet(_Fields field) {
    if (field == null) {
      throw new IllegalArgumentException();
    }

    switch (field) {
    case REF_TYPE:
      return isSetRefType();
    case TRACE_ID_LOW:
      return isSetTraceIdLow();
    case TRACE_ID_HIGH:
      return isSetTraceIdHigh();
    case SPAN_ID:
      return isSetSpanId();
    }
    throw new IllegalStateException();
  }

  @Override
  public boolean equals(Object that) {
    if (that instanceof SpanRef)
      return this.equals((SpanRef)that);
    return false;
  }

  public boolean equals(SpanRef that) {
    if (that == null)
      return false;
    if (this == that)
      return true;

    boolean this_present_refType = true && this.isSetRefType();
    boolean that_present_refType = true && that.isSetRefType();
    if (this_present_refType || that_present_refType) {
      if (!(this_present_refType && that_present_refType))
        return false;
      if (!this.refType.equals(that.refType))
        return false;
    }

    boolean this_present_traceIdLow = true;
    boolean that_present_traceIdLow = true;
    if (this_present_traceIdLow || that_present_traceIdLow) {
      if (!(this_present_traceIdLow && that_present_traceIdLow))
        return false;
      if (this.traceIdLow != that.traceIdLow)
        return false;
    }

    boolean this_present_traceIdHigh = true;
    boolean that_present_traceIdHigh = true;
    if (this_present_traceIdHigh || that_present_traceIdHigh) {
      if (!(this_present_traceIdHigh && that_present_traceIdHigh))
        return false;
      if (this.traceIdHigh != that.traceIdHigh)
        return false;
    }

    boolean this_present_spanId = true;
    boolean that_present_spanId = true;
    if (this_present_spanId || that_present_spanId) {
      if (!(this_present_spanId && that_present_spanId))
        return false;
      if (this.spanId != that.spanId)
        return false;
    }

    return true;
  }

  @Override
  public int hashCode() {
    int hashCode = 1;

    hashCode = hashCode * 8191 + ((isSetRefType()) ? 131071 : 524287);
    if (isSetRefType())
      hashCode = hashCode * 8191 + refType.getValue();

    hashCode = hashCode * 8191 + org.apache.thrift.TBaseHelper.hashCode(traceIdLow);

    hashCode = hashCode * 8191 + org.apache.thrift.TBaseHelper.hashCode(traceIdHigh);

    hashCode = hashCode * 8191 + org.apache.thrift.TBaseHelper.hashCode(spanId);

    return hashCode;
  }

  @Override
  public int compareTo(SpanRef other) {
    if (!getClass().equals(other.getClass())) {
      return getClass().getName().compareTo(other.getClass().getName());
    }

    int lastComparison = 0;

    lastComparison = Boolean.compare(isSetRefType(), other.isSetRefType());
    if (lastComparison != 0) {
      return lastComparison;
    }
    if (isSetRefType()) {
      lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.refType, other.refType);
      if (lastComparison != 0) {
        return lastComparison;
      }
    }
    lastComparison = Boolean.compare(isSetTraceIdLow(), other.isSetTraceIdLow());
    if (lastComparison != 0) {
      return lastComparison;
    }
    if (isSetTraceIdLow()) {
      lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.traceIdLow, other.traceIdLow);
      if (lastComparison != 0) {
        return lastComparison;
      }
    }
    lastComparison = Boolean.compare(isSetTraceIdHigh(), other.isSetTraceIdHigh());
    if (lastComparison != 0) {
      return lastComparison;
    }
    if (isSetTraceIdHigh()) {
      lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.traceIdHigh, other.traceIdHigh);
      if (lastComparison != 0) {
        return lastComparison;
      }
    }
    lastComparison = Boolean.compare(isSetSpanId(), other.isSetSpanId());
    if (lastComparison != 0) {
      return lastComparison;
    }
    if (isSetSpanId()) {
      lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.spanId, other.spanId);
      if (lastComparison != 0) {
        return lastComparison;
      }
    }
    return 0;
  }

  @org.apache.thrift.annotation.Nullable
  @Override
  public _Fields fieldForId(int fieldId) {
    return _Fields.findByThriftId(fieldId);
  }

  @Override
  public void read(org.apache.thrift.protocol.TProtocol iprot) throws org.apache.thrift.TException {
    scheme(iprot).read(iprot, this);
  }

  @Override
  public void write(org.apache.thrift.protocol.TProtocol oprot) throws org.apache.thrift.TException {
    scheme(oprot).write(oprot, this);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder("SpanRef(");
    boolean first = true;

    sb.append("refType:");
    if (this.refType == null) {
      sb.append("null");
    } else {
      sb.append(this.refType);
    }
    first = false;
    if (!first) sb.append(", ");
    sb.append("traceIdLow:");
    sb.append(this.traceIdLow);
    first = false;
    if (!first) sb.append(", ");
    sb.append("traceIdHigh:");
    sb.append(this.traceIdHigh);
    first = false;
    if (!first) sb.append(", ");
    sb.append("spanId:");
    sb.append(this.spanId);
    first = false;
    sb.append(")");
    return sb.toString();
  }

  public void validate() throws org.apache.thrift.TException {
    // check for required fields
    if (refType == null) {
      throw new org.apache.thrift.protocol.TProtocolException("Required field 'refType' was not present! Struct: " + toString());
    }
    // alas, we cannot check 'traceIdLow' because it's a primitive and you chose the non-beans generator.
    // alas, we cannot check 'traceIdHigh' because it's a primitive and you chose the non-beans generator.
    // alas, we cannot check 'spanId' because it's a primitive and you chose the non-beans generator.
    // check for sub-struct validity
  }

  private void writeObject(java.io.ObjectOutputStream out) throws java.io.IOException {
    try {
      write(new org.apache.thrift.protocol.TCompactProtocol(new org.apache.thrift.transport.TIOStreamTransport(out)));
    } catch (org.apache.thrift.TException te) {
      throw new java.io.IOException(te);
    }
  }

  private void readObject(java.io.ObjectInputStream in) throws java.io.IOException, ClassNotFoundException {
    try {
      // it doesn't seem like you should have to do this, but java serialization is wacky, and doesn't call the default constructor.
      __isset_bitfield = 0;
      read(new org.apache.thrift.protocol.TCompactProtocol(new org.apache.thrift.transport.TIOStreamTransport(in)));
    } catch (org.apache.thrift.TException te) {
      throw new java.io.IOException(te);
    }
  }

  private static class SpanRefStandardSchemeFactory implements org.apache.thrift.scheme.SchemeFactory {
    @Override
    public SpanRefStandardScheme getScheme() {
      return new SpanRefStandardScheme();
    }
  }

  private static class SpanRefStandardScheme extends org.apache.thrift.scheme.StandardScheme<SpanRef> {

    @Override
    public void read(org.apache.thrift.protocol.TProtocol iprot, SpanRef struct) throws org.apache.thrift.TException {
      org.apache.thrift.protocol.TField schemeField;
      iprot.readStructBegin();
      while (true)
      {
        schemeField = iprot.readFieldBegin();
        if (schemeField.type == org.apache.thrift.protocol.TType.STOP) { 
          break;
        }
        switch (schemeField.id) {
          case 1: // REF_TYPE
            if (schemeField.type == org.apache.thrift.protocol.TType.I32) {
              struct.refType = SpanRefType.findByValue(iprot.readI32());
              struct.setRefTypeIsSet(true);
            } else { 
              org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
            }
            break;
          case 2: // TRACE_ID_LOW
            if (schemeField.type == org.apache.thrift.protocol.TType.I64) {
              struct.traceIdLow = iprot.readI64();
              struct.setTraceIdLowIsSet(true);
            } else { 
              org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
            }
            break;
          case 3: // TRACE_ID_HIGH
            if (schemeField.type == org.apache.thrift.protocol.TType.I64) {
              struct.traceIdHigh = iprot.readI64();
              struct.setTraceIdHighIsSet(true);
            } else { 
              org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
            }
            break;
          case 4: // SPAN_ID
            if (schemeField.type == org.apache.thrift.protocol.TType.I64) {
              struct.spanId = iprot.readI64();
              struct.setSpanIdIsSet(true);
            } else { 
              org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
            }
            break;
          default:
            org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
        }
        iprot.readFieldEnd();
      }
      iprot.readStructEnd();

      // check for required fields of primitive type, which can't be checked in the validate method
      if (!struct.isSetTraceIdLow()) {
        throw new org.apache.thrift.protocol.TProtocolException("Required field 'traceIdLow' was not found in serialized data! Struct: " + toString());
      }
      if (!struct.isSetTraceIdHigh()) {
        throw new org.apache.thrift.protocol.TProtocolException("Required field 'traceIdHigh' was not found in serialized data! Struct: " + toString());
      }
      if (!struct.isSetSpanId()) {
        throw new org.apache.thrift.protocol.TProtocolException("Required field 'spanId' was not found in serialized data! Struct: " + toString());
      }
      struct.validate();
    }

    @Override
    public void write(org.apache.thrift.protocol.TProtocol oprot, SpanRef struct) throws org.apache.thrift.TException {
      struct.validate();

      oprot.writeStructBegin(STRUCT_DESC);
      if (struct.refType != null) {
        oprot.writeFieldBegin(REF_TYPE_FIELD_DESC);
        oprot.writeI32(struct.refType.getValue());
        oprot.writeFieldEnd();
      }
      oprot.writeFieldBegin(TRACE_ID_LOW_FIELD_DESC);
      oprot.writeI64(struct.traceIdLow);
      oprot.writeFieldEnd();
      oprot.writeFieldBegin(TRACE_ID_HIGH_FIELD_DESC);
      oprot.writeI64(struct.traceIdHigh);
      oprot.writeFieldEnd();
      oprot.writeFieldBegin(SPAN_ID_FIELD_DESC);
      oprot.writeI64(struct.spanId);
      oprot.writeFieldEnd();
      oprot.writeFieldStop();
      oprot.writeStructEnd();
    }

  }

  private static class SpanRefTupleSchemeFactory implements org.apache.thrift.scheme.SchemeFactory {
    @Override
    public SpanRefTupleScheme getScheme() {
      return new SpanRefTupleScheme();
    }
  }

  private static class SpanRefTupleScheme extends org.apache.thrift.scheme.TupleScheme<SpanRef> {

    @Override
    public void write(org.apache.thrift.protocol.TProtocol prot, SpanRef struct) throws org.apache.thrift.TException {
      org.apache.thrift.protocol.TTupleProtocol oprot = (org.apache.thrift.protocol.TTupleProtocol) prot;
      oprot.writeI32(struct.refType.getValue());
      oprot.writeI64(struct.traceIdLow);
      oprot.writeI64(struct.traceIdHigh);
      oprot.writeI64(struct.spanId);
    }

    @Override
    public void read(org.apache.thrift.protocol.TProtocol prot, SpanRef struct) throws org.apache.thrift.TException {
      org.apache.thrift.protocol.TTupleProtocol iprot = (org.apache.thrift.protocol.TTupleProtocol) prot;
      struct.refType = SpanRefType.findByValue(iprot.readI32());
      struct.setRefTypeIsSet(true);
      struct.traceIdLow = iprot.readI64();
      struct.setTraceIdLowIsSet(true);
      struct.traceIdHigh = iprot.readI64();
      struct.setTraceIdHighIsSet(true);
      struct.spanId = iprot.readI64();
      struct.setSpanIdIsSet(true);
    }
  }

  private static <S extends org.apache.thrift.scheme.IScheme> S scheme(org.apache.thrift.protocol.TProtocol proto) {
    return (org.apache.thrift.scheme.StandardScheme.class.equals(proto.getScheme()) ? STANDARD_SCHEME_FACTORY : TUPLE_SCHEME_FACTORY).getScheme();
  }
}

