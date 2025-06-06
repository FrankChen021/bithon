/**
 * Autogenerated by Thrift Compiler (0.19.0)
 *
 * DO NOT EDIT UNLESS YOU ARE SURE THAT YOU KNOW WHAT YOU ARE DOING
 *  @generated
 */
package io.jaegertracing.thriftjava;

@SuppressWarnings({"cast", "rawtypes", "serial", "unchecked", "unused"})
@javax.annotation.Generated(value = "Autogenerated by Thrift Compiler (0.19.0)", date = "2025-05-28")
public class Tag implements org.apache.thrift.TBase<Tag, Tag._Fields>, java.io.Serializable, Cloneable, Comparable<Tag> {
  private static final org.apache.thrift.protocol.TStruct STRUCT_DESC = new org.apache.thrift.protocol.TStruct("Tag");

  private static final org.apache.thrift.protocol.TField KEY_FIELD_DESC = new org.apache.thrift.protocol.TField("key", org.apache.thrift.protocol.TType.STRING, (short)1);
  private static final org.apache.thrift.protocol.TField V_TYPE_FIELD_DESC = new org.apache.thrift.protocol.TField("vType", org.apache.thrift.protocol.TType.I32, (short)2);
  private static final org.apache.thrift.protocol.TField V_STR_FIELD_DESC = new org.apache.thrift.protocol.TField("vStr", org.apache.thrift.protocol.TType.STRING, (short)3);
  private static final org.apache.thrift.protocol.TField V_DOUBLE_FIELD_DESC = new org.apache.thrift.protocol.TField("vDouble", org.apache.thrift.protocol.TType.DOUBLE, (short)4);
  private static final org.apache.thrift.protocol.TField V_BOOL_FIELD_DESC = new org.apache.thrift.protocol.TField("vBool", org.apache.thrift.protocol.TType.BOOL, (short)5);
  private static final org.apache.thrift.protocol.TField V_LONG_FIELD_DESC = new org.apache.thrift.protocol.TField("vLong", org.apache.thrift.protocol.TType.I64, (short)6);
  private static final org.apache.thrift.protocol.TField V_BINARY_FIELD_DESC = new org.apache.thrift.protocol.TField("vBinary", org.apache.thrift.protocol.TType.STRING, (short)7);

  private static final org.apache.thrift.scheme.SchemeFactory STANDARD_SCHEME_FACTORY = new TagStandardSchemeFactory();
  private static final org.apache.thrift.scheme.SchemeFactory TUPLE_SCHEME_FACTORY = new TagTupleSchemeFactory();

  private @org.apache.thrift.annotation.Nullable String key; // required
  private @org.apache.thrift.annotation.Nullable TagType vType; // required
  private @org.apache.thrift.annotation.Nullable String vStr; // optional
  private double vDouble; // optional
  private boolean vBool; // optional
  private long vLong; // optional
  private @org.apache.thrift.annotation.Nullable java.nio.ByteBuffer vBinary; // optional

  /** The set of fields this struct contains, along with convenience methods for finding and manipulating them. */
  public enum _Fields implements org.apache.thrift.TFieldIdEnum {
    KEY((short)1, "key"),
    /**
     * 
     * @see TagType
     */
    V_TYPE((short)2, "vType"),
    V_STR((short)3, "vStr"),
    V_DOUBLE((short)4, "vDouble"),
    V_BOOL((short)5, "vBool"),
    V_LONG((short)6, "vLong"),
    V_BINARY((short)7, "vBinary");

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
        case 1: // KEY
          return KEY;
        case 2: // V_TYPE
          return V_TYPE;
        case 3: // V_STR
          return V_STR;
        case 4: // V_DOUBLE
          return V_DOUBLE;
        case 5: // V_BOOL
          return V_BOOL;
        case 6: // V_LONG
          return V_LONG;
        case 7: // V_BINARY
          return V_BINARY;
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
  private static final int __VDOUBLE_ISSET_ID = 0;
  private static final int __VBOOL_ISSET_ID = 1;
  private static final int __VLONG_ISSET_ID = 2;
  private byte __isset_bitfield = 0;
  private static final _Fields optionals[] = {_Fields.V_STR,_Fields.V_DOUBLE,_Fields.V_BOOL,_Fields.V_LONG,_Fields.V_BINARY};
  public static final java.util.Map<_Fields, org.apache.thrift.meta_data.FieldMetaData> metaDataMap;
  static {
    java.util.Map<_Fields, org.apache.thrift.meta_data.FieldMetaData> tmpMap = new java.util.EnumMap<_Fields, org.apache.thrift.meta_data.FieldMetaData>(_Fields.class);
    tmpMap.put(_Fields.KEY, new org.apache.thrift.meta_data.FieldMetaData("key", org.apache.thrift.TFieldRequirementType.REQUIRED, 
        new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.STRING)));
    tmpMap.put(_Fields.V_TYPE, new org.apache.thrift.meta_data.FieldMetaData("vType", org.apache.thrift.TFieldRequirementType.REQUIRED, 
        new org.apache.thrift.meta_data.EnumMetaData(org.apache.thrift.protocol.TType.ENUM, TagType.class)));
    tmpMap.put(_Fields.V_STR, new org.apache.thrift.meta_data.FieldMetaData("vStr", org.apache.thrift.TFieldRequirementType.OPTIONAL, 
        new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.STRING)));
    tmpMap.put(_Fields.V_DOUBLE, new org.apache.thrift.meta_data.FieldMetaData("vDouble", org.apache.thrift.TFieldRequirementType.OPTIONAL, 
        new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.DOUBLE)));
    tmpMap.put(_Fields.V_BOOL, new org.apache.thrift.meta_data.FieldMetaData("vBool", org.apache.thrift.TFieldRequirementType.OPTIONAL, 
        new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.BOOL)));
    tmpMap.put(_Fields.V_LONG, new org.apache.thrift.meta_data.FieldMetaData("vLong", org.apache.thrift.TFieldRequirementType.OPTIONAL, 
        new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.I64)));
    tmpMap.put(_Fields.V_BINARY, new org.apache.thrift.meta_data.FieldMetaData("vBinary", org.apache.thrift.TFieldRequirementType.OPTIONAL, 
        new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.STRING        , true)));
    metaDataMap = java.util.Collections.unmodifiableMap(tmpMap);
    org.apache.thrift.meta_data.FieldMetaData.addStructMetaDataMap(Tag.class, metaDataMap);
  }

  public Tag() {
  }

  public Tag(
    String key,
    TagType vType)
  {
    this();
    this.key = key;
    this.vType = vType;
  }

  /**
   * Performs a deep copy on <i>other</i>.
   */
  public Tag(Tag other) {
    __isset_bitfield = other.__isset_bitfield;
    if (other.isSetKey()) {
      this.key = other.key;
    }
    if (other.isSetVType()) {
      this.vType = other.vType;
    }
    if (other.isSetVStr()) {
      this.vStr = other.vStr;
    }
    this.vDouble = other.vDouble;
    this.vBool = other.vBool;
    this.vLong = other.vLong;
    if (other.isSetVBinary()) {
      this.vBinary = org.apache.thrift.TBaseHelper.copyBinary(other.vBinary);
    }
  }

  @Override
  public Tag deepCopy() {
    return new Tag(this);
  }

  @Override
  public void clear() {
    this.key = null;
    this.vType = null;
    this.vStr = null;
    setVDoubleIsSet(false);
    this.vDouble = 0.0;
    setVBoolIsSet(false);
    this.vBool = false;
    setVLongIsSet(false);
    this.vLong = 0;
    this.vBinary = null;
  }

  @org.apache.thrift.annotation.Nullable
  public String getKey() {
    return this.key;
  }

  public Tag setKey(@org.apache.thrift.annotation.Nullable String key) {
    this.key = key;
    return this;
  }

  public void unsetKey() {
    this.key = null;
  }

  /** Returns true if field key is set (has been assigned a value) and false otherwise */
  public boolean isSetKey() {
    return this.key != null;
  }

  public void setKeyIsSet(boolean value) {
    if (!value) {
      this.key = null;
    }
  }

  /**
   * 
   * @see TagType
   */
  @org.apache.thrift.annotation.Nullable
  public TagType getVType() {
    return this.vType;
  }

  /**
   * 
   * @see TagType
   */
  public Tag setVType(@org.apache.thrift.annotation.Nullable TagType vType) {
    this.vType = vType;
    return this;
  }

  public void unsetVType() {
    this.vType = null;
  }

  /** Returns true if field vType is set (has been assigned a value) and false otherwise */
  public boolean isSetVType() {
    return this.vType != null;
  }

  public void setVTypeIsSet(boolean value) {
    if (!value) {
      this.vType = null;
    }
  }

  @org.apache.thrift.annotation.Nullable
  public String getVStr() {
    return this.vStr;
  }

  public Tag setVStr(@org.apache.thrift.annotation.Nullable String vStr) {
    this.vStr = vStr;
    return this;
  }

  public void unsetVStr() {
    this.vStr = null;
  }

  /** Returns true if field vStr is set (has been assigned a value) and false otherwise */
  public boolean isSetVStr() {
    return this.vStr != null;
  }

  public void setVStrIsSet(boolean value) {
    if (!value) {
      this.vStr = null;
    }
  }

  public double getVDouble() {
    return this.vDouble;
  }

  public Tag setVDouble(double vDouble) {
    this.vDouble = vDouble;
    setVDoubleIsSet(true);
    return this;
  }

  public void unsetVDouble() {
    __isset_bitfield = org.apache.thrift.EncodingUtils.clearBit(__isset_bitfield, __VDOUBLE_ISSET_ID);
  }

  /** Returns true if field vDouble is set (has been assigned a value) and false otherwise */
  public boolean isSetVDouble() {
    return org.apache.thrift.EncodingUtils.testBit(__isset_bitfield, __VDOUBLE_ISSET_ID);
  }

  public void setVDoubleIsSet(boolean value) {
    __isset_bitfield = org.apache.thrift.EncodingUtils.setBit(__isset_bitfield, __VDOUBLE_ISSET_ID, value);
  }

  public boolean isVBool() {
    return this.vBool;
  }

  public Tag setVBool(boolean vBool) {
    this.vBool = vBool;
    setVBoolIsSet(true);
    return this;
  }

  public void unsetVBool() {
    __isset_bitfield = org.apache.thrift.EncodingUtils.clearBit(__isset_bitfield, __VBOOL_ISSET_ID);
  }

  /** Returns true if field vBool is set (has been assigned a value) and false otherwise */
  public boolean isSetVBool() {
    return org.apache.thrift.EncodingUtils.testBit(__isset_bitfield, __VBOOL_ISSET_ID);
  }

  public void setVBoolIsSet(boolean value) {
    __isset_bitfield = org.apache.thrift.EncodingUtils.setBit(__isset_bitfield, __VBOOL_ISSET_ID, value);
  }

  public long getVLong() {
    return this.vLong;
  }

  public Tag setVLong(long vLong) {
    this.vLong = vLong;
    setVLongIsSet(true);
    return this;
  }

  public void unsetVLong() {
    __isset_bitfield = org.apache.thrift.EncodingUtils.clearBit(__isset_bitfield, __VLONG_ISSET_ID);
  }

  /** Returns true if field vLong is set (has been assigned a value) and false otherwise */
  public boolean isSetVLong() {
    return org.apache.thrift.EncodingUtils.testBit(__isset_bitfield, __VLONG_ISSET_ID);
  }

  public void setVLongIsSet(boolean value) {
    __isset_bitfield = org.apache.thrift.EncodingUtils.setBit(__isset_bitfield, __VLONG_ISSET_ID, value);
  }

  public byte[] getVBinary() {
    setVBinary(org.apache.thrift.TBaseHelper.rightSize(vBinary));
    return vBinary == null ? null : vBinary.array();
  }

  public java.nio.ByteBuffer bufferForVBinary() {
    return org.apache.thrift.TBaseHelper.copyBinary(vBinary);
  }

  public Tag setVBinary(byte[] vBinary) {
    this.vBinary = vBinary == null ? (java.nio.ByteBuffer)null   : java.nio.ByteBuffer.wrap(vBinary.clone());
    return this;
  }

  public Tag setVBinary(@org.apache.thrift.annotation.Nullable java.nio.ByteBuffer vBinary) {
    this.vBinary = org.apache.thrift.TBaseHelper.copyBinary(vBinary);
    return this;
  }

  public void unsetVBinary() {
    this.vBinary = null;
  }

  /** Returns true if field vBinary is set (has been assigned a value) and false otherwise */
  public boolean isSetVBinary() {
    return this.vBinary != null;
  }

  public void setVBinaryIsSet(boolean value) {
    if (!value) {
      this.vBinary = null;
    }
  }

  @Override
  public void setFieldValue(_Fields field, @org.apache.thrift.annotation.Nullable Object value) {
    switch (field) {
    case KEY:
      if (value == null) {
        unsetKey();
      } else {
        setKey((String)value);
      }
      break;

    case V_TYPE:
      if (value == null) {
        unsetVType();
      } else {
        setVType((TagType)value);
      }
      break;

    case V_STR:
      if (value == null) {
        unsetVStr();
      } else {
        setVStr((String)value);
      }
      break;

    case V_DOUBLE:
      if (value == null) {
        unsetVDouble();
      } else {
        setVDouble((Double)value);
      }
      break;

    case V_BOOL:
      if (value == null) {
        unsetVBool();
      } else {
        setVBool((Boolean)value);
      }
      break;

    case V_LONG:
      if (value == null) {
        unsetVLong();
      } else {
        setVLong((Long)value);
      }
      break;

    case V_BINARY:
      if (value == null) {
        unsetVBinary();
      } else {
        if (value instanceof byte[]) {
          setVBinary((byte[])value);
        } else {
          setVBinary((java.nio.ByteBuffer)value);
        }
      }
      break;

    }
  }

  @org.apache.thrift.annotation.Nullable
  @Override
  public Object getFieldValue(_Fields field) {
    switch (field) {
    case KEY:
      return getKey();

    case V_TYPE:
      return getVType();

    case V_STR:
      return getVStr();

    case V_DOUBLE:
      return getVDouble();

    case V_BOOL:
      return isVBool();

    case V_LONG:
      return getVLong();

    case V_BINARY:
      return getVBinary();

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
    case KEY:
      return isSetKey();
    case V_TYPE:
      return isSetVType();
    case V_STR:
      return isSetVStr();
    case V_DOUBLE:
      return isSetVDouble();
    case V_BOOL:
      return isSetVBool();
    case V_LONG:
      return isSetVLong();
    case V_BINARY:
      return isSetVBinary();
    }
    throw new IllegalStateException();
  }

  @Override
  public boolean equals(Object that) {
    if (that instanceof Tag)
      return this.equals((Tag)that);
    return false;
  }

  public boolean equals(Tag that) {
    if (that == null)
      return false;
    if (this == that)
      return true;

    boolean this_present_key = true && this.isSetKey();
    boolean that_present_key = true && that.isSetKey();
    if (this_present_key || that_present_key) {
      if (!(this_present_key && that_present_key))
        return false;
      if (!this.key.equals(that.key))
        return false;
    }

    boolean this_present_vType = true && this.isSetVType();
    boolean that_present_vType = true && that.isSetVType();
    if (this_present_vType || that_present_vType) {
      if (!(this_present_vType && that_present_vType))
        return false;
      if (!this.vType.equals(that.vType))
        return false;
    }

    boolean this_present_vStr = true && this.isSetVStr();
    boolean that_present_vStr = true && that.isSetVStr();
    if (this_present_vStr || that_present_vStr) {
      if (!(this_present_vStr && that_present_vStr))
        return false;
      if (!this.vStr.equals(that.vStr))
        return false;
    }

    boolean this_present_vDouble = true && this.isSetVDouble();
    boolean that_present_vDouble = true && that.isSetVDouble();
    if (this_present_vDouble || that_present_vDouble) {
      if (!(this_present_vDouble && that_present_vDouble))
        return false;
      if (this.vDouble != that.vDouble)
        return false;
    }

    boolean this_present_vBool = true && this.isSetVBool();
    boolean that_present_vBool = true && that.isSetVBool();
    if (this_present_vBool || that_present_vBool) {
      if (!(this_present_vBool && that_present_vBool))
        return false;
      if (this.vBool != that.vBool)
        return false;
    }

    boolean this_present_vLong = true && this.isSetVLong();
    boolean that_present_vLong = true && that.isSetVLong();
    if (this_present_vLong || that_present_vLong) {
      if (!(this_present_vLong && that_present_vLong))
        return false;
      if (this.vLong != that.vLong)
        return false;
    }

    boolean this_present_vBinary = true && this.isSetVBinary();
    boolean that_present_vBinary = true && that.isSetVBinary();
    if (this_present_vBinary || that_present_vBinary) {
      if (!(this_present_vBinary && that_present_vBinary))
        return false;
      if (!this.vBinary.equals(that.vBinary))
        return false;
    }

    return true;
  }

  @Override
  public int hashCode() {
    int hashCode = 1;

    hashCode = hashCode * 8191 + ((isSetKey()) ? 131071 : 524287);
    if (isSetKey())
      hashCode = hashCode * 8191 + key.hashCode();

    hashCode = hashCode * 8191 + ((isSetVType()) ? 131071 : 524287);
    if (isSetVType())
      hashCode = hashCode * 8191 + vType.getValue();

    hashCode = hashCode * 8191 + ((isSetVStr()) ? 131071 : 524287);
    if (isSetVStr())
      hashCode = hashCode * 8191 + vStr.hashCode();

    hashCode = hashCode * 8191 + ((isSetVDouble()) ? 131071 : 524287);
    if (isSetVDouble())
      hashCode = hashCode * 8191 + org.apache.thrift.TBaseHelper.hashCode(vDouble);

    hashCode = hashCode * 8191 + ((isSetVBool()) ? 131071 : 524287);
    if (isSetVBool())
      hashCode = hashCode * 8191 + ((vBool) ? 131071 : 524287);

    hashCode = hashCode * 8191 + ((isSetVLong()) ? 131071 : 524287);
    if (isSetVLong())
      hashCode = hashCode * 8191 + org.apache.thrift.TBaseHelper.hashCode(vLong);

    hashCode = hashCode * 8191 + ((isSetVBinary()) ? 131071 : 524287);
    if (isSetVBinary())
      hashCode = hashCode * 8191 + vBinary.hashCode();

    return hashCode;
  }

  @Override
  public int compareTo(Tag other) {
    if (!getClass().equals(other.getClass())) {
      return getClass().getName().compareTo(other.getClass().getName());
    }

    int lastComparison = 0;

    lastComparison = Boolean.compare(isSetKey(), other.isSetKey());
    if (lastComparison != 0) {
      return lastComparison;
    }
    if (isSetKey()) {
      lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.key, other.key);
      if (lastComparison != 0) {
        return lastComparison;
      }
    }
    lastComparison = Boolean.compare(isSetVType(), other.isSetVType());
    if (lastComparison != 0) {
      return lastComparison;
    }
    if (isSetVType()) {
      lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.vType, other.vType);
      if (lastComparison != 0) {
        return lastComparison;
      }
    }
    lastComparison = Boolean.compare(isSetVStr(), other.isSetVStr());
    if (lastComparison != 0) {
      return lastComparison;
    }
    if (isSetVStr()) {
      lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.vStr, other.vStr);
      if (lastComparison != 0) {
        return lastComparison;
      }
    }
    lastComparison = Boolean.compare(isSetVDouble(), other.isSetVDouble());
    if (lastComparison != 0) {
      return lastComparison;
    }
    if (isSetVDouble()) {
      lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.vDouble, other.vDouble);
      if (lastComparison != 0) {
        return lastComparison;
      }
    }
    lastComparison = Boolean.compare(isSetVBool(), other.isSetVBool());
    if (lastComparison != 0) {
      return lastComparison;
    }
    if (isSetVBool()) {
      lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.vBool, other.vBool);
      if (lastComparison != 0) {
        return lastComparison;
      }
    }
    lastComparison = Boolean.compare(isSetVLong(), other.isSetVLong());
    if (lastComparison != 0) {
      return lastComparison;
    }
    if (isSetVLong()) {
      lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.vLong, other.vLong);
      if (lastComparison != 0) {
        return lastComparison;
      }
    }
    lastComparison = Boolean.compare(isSetVBinary(), other.isSetVBinary());
    if (lastComparison != 0) {
      return lastComparison;
    }
    if (isSetVBinary()) {
      lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.vBinary, other.vBinary);
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
    StringBuilder sb = new StringBuilder("Tag(");
    boolean first = true;

    sb.append("key:");
    if (this.key == null) {
      sb.append("null");
    } else {
      sb.append(this.key);
    }
    first = false;
    if (!first) sb.append(", ");
    sb.append("vType:");
    if (this.vType == null) {
      sb.append("null");
    } else {
      sb.append(this.vType);
    }
    first = false;
    if (isSetVStr()) {
      if (!first) sb.append(", ");
      sb.append("vStr:");
      if (this.vStr == null) {
        sb.append("null");
      } else {
        sb.append(this.vStr);
      }
      first = false;
    }
    if (isSetVDouble()) {
      if (!first) sb.append(", ");
      sb.append("vDouble:");
      sb.append(this.vDouble);
      first = false;
    }
    if (isSetVBool()) {
      if (!first) sb.append(", ");
      sb.append("vBool:");
      sb.append(this.vBool);
      first = false;
    }
    if (isSetVLong()) {
      if (!first) sb.append(", ");
      sb.append("vLong:");
      sb.append(this.vLong);
      first = false;
    }
    if (isSetVBinary()) {
      if (!first) sb.append(", ");
      sb.append("vBinary:");
      if (this.vBinary == null) {
        sb.append("null");
      } else {
        org.apache.thrift.TBaseHelper.toString(this.vBinary, sb);
      }
      first = false;
    }
    sb.append(")");
    return sb.toString();
  }

  public void validate() throws org.apache.thrift.TException {
    // check for required fields
    if (key == null) {
      throw new org.apache.thrift.protocol.TProtocolException("Required field 'key' was not present! Struct: " + toString());
    }
    if (vType == null) {
      throw new org.apache.thrift.protocol.TProtocolException("Required field 'vType' was not present! Struct: " + toString());
    }
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

  private static class TagStandardSchemeFactory implements org.apache.thrift.scheme.SchemeFactory {
    @Override
    public TagStandardScheme getScheme() {
      return new TagStandardScheme();
    }
  }

  private static class TagStandardScheme extends org.apache.thrift.scheme.StandardScheme<Tag> {

    @Override
    public void read(org.apache.thrift.protocol.TProtocol iprot, Tag struct) throws org.apache.thrift.TException {
      org.apache.thrift.protocol.TField schemeField;
      iprot.readStructBegin();
      while (true)
      {
        schemeField = iprot.readFieldBegin();
        if (schemeField.type == org.apache.thrift.protocol.TType.STOP) { 
          break;
        }
        switch (schemeField.id) {
          case 1: // KEY
            if (schemeField.type == org.apache.thrift.protocol.TType.STRING) {
              struct.key = iprot.readString();
              struct.setKeyIsSet(true);
            } else { 
              org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
            }
            break;
          case 2: // V_TYPE
            if (schemeField.type == org.apache.thrift.protocol.TType.I32) {
              struct.vType = TagType.findByValue(iprot.readI32());
              struct.setVTypeIsSet(true);
            } else { 
              org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
            }
            break;
          case 3: // V_STR
            if (schemeField.type == org.apache.thrift.protocol.TType.STRING) {
              struct.vStr = iprot.readString();
              struct.setVStrIsSet(true);
            } else { 
              org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
            }
            break;
          case 4: // V_DOUBLE
            if (schemeField.type == org.apache.thrift.protocol.TType.DOUBLE) {
              struct.vDouble = iprot.readDouble();
              struct.setVDoubleIsSet(true);
            } else { 
              org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
            }
            break;
          case 5: // V_BOOL
            if (schemeField.type == org.apache.thrift.protocol.TType.BOOL) {
              struct.vBool = iprot.readBool();
              struct.setVBoolIsSet(true);
            } else { 
              org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
            }
            break;
          case 6: // V_LONG
            if (schemeField.type == org.apache.thrift.protocol.TType.I64) {
              struct.vLong = iprot.readI64();
              struct.setVLongIsSet(true);
            } else { 
              org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
            }
            break;
          case 7: // V_BINARY
            if (schemeField.type == org.apache.thrift.protocol.TType.STRING) {
              struct.vBinary = iprot.readBinary();
              struct.setVBinaryIsSet(true);
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
      struct.validate();
    }

    @Override
    public void write(org.apache.thrift.protocol.TProtocol oprot, Tag struct) throws org.apache.thrift.TException {
      struct.validate();

      oprot.writeStructBegin(STRUCT_DESC);
      if (struct.key != null) {
        oprot.writeFieldBegin(KEY_FIELD_DESC);
        oprot.writeString(struct.key);
        oprot.writeFieldEnd();
      }
      if (struct.vType != null) {
        oprot.writeFieldBegin(V_TYPE_FIELD_DESC);
        oprot.writeI32(struct.vType.getValue());
        oprot.writeFieldEnd();
      }
      if (struct.vStr != null) {
        if (struct.isSetVStr()) {
          oprot.writeFieldBegin(V_STR_FIELD_DESC);
          oprot.writeString(struct.vStr);
          oprot.writeFieldEnd();
        }
      }
      if (struct.isSetVDouble()) {
        oprot.writeFieldBegin(V_DOUBLE_FIELD_DESC);
        oprot.writeDouble(struct.vDouble);
        oprot.writeFieldEnd();
      }
      if (struct.isSetVBool()) {
        oprot.writeFieldBegin(V_BOOL_FIELD_DESC);
        oprot.writeBool(struct.vBool);
        oprot.writeFieldEnd();
      }
      if (struct.isSetVLong()) {
        oprot.writeFieldBegin(V_LONG_FIELD_DESC);
        oprot.writeI64(struct.vLong);
        oprot.writeFieldEnd();
      }
      if (struct.vBinary != null) {
        if (struct.isSetVBinary()) {
          oprot.writeFieldBegin(V_BINARY_FIELD_DESC);
          oprot.writeBinary(struct.vBinary);
          oprot.writeFieldEnd();
        }
      }
      oprot.writeFieldStop();
      oprot.writeStructEnd();
    }

  }

  private static class TagTupleSchemeFactory implements org.apache.thrift.scheme.SchemeFactory {
    @Override
    public TagTupleScheme getScheme() {
      return new TagTupleScheme();
    }
  }

  private static class TagTupleScheme extends org.apache.thrift.scheme.TupleScheme<Tag> {

    @Override
    public void write(org.apache.thrift.protocol.TProtocol prot, Tag struct) throws org.apache.thrift.TException {
      org.apache.thrift.protocol.TTupleProtocol oprot = (org.apache.thrift.protocol.TTupleProtocol) prot;
      oprot.writeString(struct.key);
      oprot.writeI32(struct.vType.getValue());
      java.util.BitSet optionals = new java.util.BitSet();
      if (struct.isSetVStr()) {
        optionals.set(0);
      }
      if (struct.isSetVDouble()) {
        optionals.set(1);
      }
      if (struct.isSetVBool()) {
        optionals.set(2);
      }
      if (struct.isSetVLong()) {
        optionals.set(3);
      }
      if (struct.isSetVBinary()) {
        optionals.set(4);
      }
      oprot.writeBitSet(optionals, 5);
      if (struct.isSetVStr()) {
        oprot.writeString(struct.vStr);
      }
      if (struct.isSetVDouble()) {
        oprot.writeDouble(struct.vDouble);
      }
      if (struct.isSetVBool()) {
        oprot.writeBool(struct.vBool);
      }
      if (struct.isSetVLong()) {
        oprot.writeI64(struct.vLong);
      }
      if (struct.isSetVBinary()) {
        oprot.writeBinary(struct.vBinary);
      }
    }

    @Override
    public void read(org.apache.thrift.protocol.TProtocol prot, Tag struct) throws org.apache.thrift.TException {
      org.apache.thrift.protocol.TTupleProtocol iprot = (org.apache.thrift.protocol.TTupleProtocol) prot;
      struct.key = iprot.readString();
      struct.setKeyIsSet(true);
      struct.vType = TagType.findByValue(iprot.readI32());
      struct.setVTypeIsSet(true);
      java.util.BitSet incoming = iprot.readBitSet(5);
      if (incoming.get(0)) {
        struct.vStr = iprot.readString();
        struct.setVStrIsSet(true);
      }
      if (incoming.get(1)) {
        struct.vDouble = iprot.readDouble();
        struct.setVDoubleIsSet(true);
      }
      if (incoming.get(2)) {
        struct.vBool = iprot.readBool();
        struct.setVBoolIsSet(true);
      }
      if (incoming.get(3)) {
        struct.vLong = iprot.readI64();
        struct.setVLongIsSet(true);
      }
      if (incoming.get(4)) {
        struct.vBinary = iprot.readBinary();
        struct.setVBinaryIsSet(true);
      }
    }
  }

  private static <S extends org.apache.thrift.scheme.IScheme> S scheme(org.apache.thrift.protocol.TProtocol proto) {
    return (org.apache.thrift.scheme.StandardScheme.class.equals(proto.getScheme()) ? STANDARD_SCHEME_FACTORY : TUPLE_SCHEME_FACTORY).getScheme();
  }
}

