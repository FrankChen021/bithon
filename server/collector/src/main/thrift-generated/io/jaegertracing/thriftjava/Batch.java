/**
 * Autogenerated by Thrift Compiler (0.19.0)
 *
 * DO NOT EDIT UNLESS YOU ARE SURE THAT YOU KNOW WHAT YOU ARE DOING
 *  @generated
 */
package io.jaegertracing.thriftjava;

@SuppressWarnings({"cast", "rawtypes", "serial", "unchecked", "unused"})
@javax.annotation.Generated(value = "Autogenerated by Thrift Compiler (0.19.0)", date = "2025-05-28")
public class Batch implements org.apache.thrift.TBase<Batch, Batch._Fields>, java.io.Serializable, Cloneable, Comparable<Batch> {
  private static final org.apache.thrift.protocol.TStruct STRUCT_DESC = new org.apache.thrift.protocol.TStruct("Batch");

  private static final org.apache.thrift.protocol.TField PROCESS_FIELD_DESC = new org.apache.thrift.protocol.TField("process", org.apache.thrift.protocol.TType.STRUCT, (short)1);
  private static final org.apache.thrift.protocol.TField SPANS_FIELD_DESC = new org.apache.thrift.protocol.TField("spans", org.apache.thrift.protocol.TType.LIST, (short)2);
  private static final org.apache.thrift.protocol.TField SEQ_NO_FIELD_DESC = new org.apache.thrift.protocol.TField("seqNo", org.apache.thrift.protocol.TType.I64, (short)3);
  private static final org.apache.thrift.protocol.TField STATS_FIELD_DESC = new org.apache.thrift.protocol.TField("stats", org.apache.thrift.protocol.TType.STRUCT, (short)4);

  private static final org.apache.thrift.scheme.SchemeFactory STANDARD_SCHEME_FACTORY = new BatchStandardSchemeFactory();
  private static final org.apache.thrift.scheme.SchemeFactory TUPLE_SCHEME_FACTORY = new BatchTupleSchemeFactory();

  private @org.apache.thrift.annotation.Nullable Process process; // required
  private @org.apache.thrift.annotation.Nullable java.util.List<Span> spans; // required
  private long seqNo; // optional
  private @org.apache.thrift.annotation.Nullable ClientStats stats; // optional

  /** The set of fields this struct contains, along with convenience methods for finding and manipulating them. */
  public enum _Fields implements org.apache.thrift.TFieldIdEnum {
    PROCESS((short)1, "process"),
    SPANS((short)2, "spans"),
    SEQ_NO((short)3, "seqNo"),
    STATS((short)4, "stats");

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
        case 1: // PROCESS
          return PROCESS;
        case 2: // SPANS
          return SPANS;
        case 3: // SEQ_NO
          return SEQ_NO;
        case 4: // STATS
          return STATS;
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
  private static final int __SEQNO_ISSET_ID = 0;
  private byte __isset_bitfield = 0;
  private static final _Fields optionals[] = {_Fields.SEQ_NO,_Fields.STATS};
  public static final java.util.Map<_Fields, org.apache.thrift.meta_data.FieldMetaData> metaDataMap;
  static {
    java.util.Map<_Fields, org.apache.thrift.meta_data.FieldMetaData> tmpMap = new java.util.EnumMap<_Fields, org.apache.thrift.meta_data.FieldMetaData>(_Fields.class);
    tmpMap.put(_Fields.PROCESS, new org.apache.thrift.meta_data.FieldMetaData("process", org.apache.thrift.TFieldRequirementType.REQUIRED, 
        new org.apache.thrift.meta_data.StructMetaData(org.apache.thrift.protocol.TType.STRUCT, Process.class)));
    tmpMap.put(_Fields.SPANS, new org.apache.thrift.meta_data.FieldMetaData("spans", org.apache.thrift.TFieldRequirementType.REQUIRED, 
        new org.apache.thrift.meta_data.ListMetaData(org.apache.thrift.protocol.TType.LIST, 
            new org.apache.thrift.meta_data.StructMetaData(org.apache.thrift.protocol.TType.STRUCT, Span.class))));
    tmpMap.put(_Fields.SEQ_NO, new org.apache.thrift.meta_data.FieldMetaData("seqNo", org.apache.thrift.TFieldRequirementType.OPTIONAL, 
        new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.I64)));
    tmpMap.put(_Fields.STATS, new org.apache.thrift.meta_data.FieldMetaData("stats", org.apache.thrift.TFieldRequirementType.OPTIONAL, 
        new org.apache.thrift.meta_data.StructMetaData(org.apache.thrift.protocol.TType.STRUCT, ClientStats.class)));
    metaDataMap = java.util.Collections.unmodifiableMap(tmpMap);
    org.apache.thrift.meta_data.FieldMetaData.addStructMetaDataMap(Batch.class, metaDataMap);
  }

  public Batch() {
  }

  public Batch(
    Process process,
    java.util.List<Span> spans)
  {
    this();
    this.process = process;
    this.spans = spans;
  }

  /**
   * Performs a deep copy on <i>other</i>.
   */
  public Batch(Batch other) {
    __isset_bitfield = other.__isset_bitfield;
    if (other.isSetProcess()) {
      this.process = new Process(other.process);
    }
    if (other.isSetSpans()) {
      java.util.List<Span> __this__spans = new java.util.ArrayList<Span>(other.spans.size());
      for (Span other_element : other.spans) {
        __this__spans.add(new Span(other_element));
      }
      this.spans = __this__spans;
    }
    this.seqNo = other.seqNo;
    if (other.isSetStats()) {
      this.stats = new ClientStats(other.stats);
    }
  }

  @Override
  public Batch deepCopy() {
    return new Batch(this);
  }

  @Override
  public void clear() {
    this.process = null;
    this.spans = null;
    setSeqNoIsSet(false);
    this.seqNo = 0;
    this.stats = null;
  }

  @org.apache.thrift.annotation.Nullable
  public Process getProcess() {
    return this.process;
  }

  public Batch setProcess(@org.apache.thrift.annotation.Nullable Process process) {
    this.process = process;
    return this;
  }

  public void unsetProcess() {
    this.process = null;
  }

  /** Returns true if field process is set (has been assigned a value) and false otherwise */
  public boolean isSetProcess() {
    return this.process != null;
  }

  public void setProcessIsSet(boolean value) {
    if (!value) {
      this.process = null;
    }
  }

  public int getSpansSize() {
    return (this.spans == null) ? 0 : this.spans.size();
  }

  @org.apache.thrift.annotation.Nullable
  public java.util.Iterator<Span> getSpansIterator() {
    return (this.spans == null) ? null : this.spans.iterator();
  }

  public void addToSpans(Span elem) {
    if (this.spans == null) {
      this.spans = new java.util.ArrayList<Span>();
    }
    this.spans.add(elem);
  }

  @org.apache.thrift.annotation.Nullable
  public java.util.List<Span> getSpans() {
    return this.spans;
  }

  public Batch setSpans(@org.apache.thrift.annotation.Nullable java.util.List<Span> spans) {
    this.spans = spans;
    return this;
  }

  public void unsetSpans() {
    this.spans = null;
  }

  /** Returns true if field spans is set (has been assigned a value) and false otherwise */
  public boolean isSetSpans() {
    return this.spans != null;
  }

  public void setSpansIsSet(boolean value) {
    if (!value) {
      this.spans = null;
    }
  }

  public long getSeqNo() {
    return this.seqNo;
  }

  public Batch setSeqNo(long seqNo) {
    this.seqNo = seqNo;
    setSeqNoIsSet(true);
    return this;
  }

  public void unsetSeqNo() {
    __isset_bitfield = org.apache.thrift.EncodingUtils.clearBit(__isset_bitfield, __SEQNO_ISSET_ID);
  }

  /** Returns true if field seqNo is set (has been assigned a value) and false otherwise */
  public boolean isSetSeqNo() {
    return org.apache.thrift.EncodingUtils.testBit(__isset_bitfield, __SEQNO_ISSET_ID);
  }

  public void setSeqNoIsSet(boolean value) {
    __isset_bitfield = org.apache.thrift.EncodingUtils.setBit(__isset_bitfield, __SEQNO_ISSET_ID, value);
  }

  @org.apache.thrift.annotation.Nullable
  public ClientStats getStats() {
    return this.stats;
  }

  public Batch setStats(@org.apache.thrift.annotation.Nullable ClientStats stats) {
    this.stats = stats;
    return this;
  }

  public void unsetStats() {
    this.stats = null;
  }

  /** Returns true if field stats is set (has been assigned a value) and false otherwise */
  public boolean isSetStats() {
    return this.stats != null;
  }

  public void setStatsIsSet(boolean value) {
    if (!value) {
      this.stats = null;
    }
  }

  @Override
  public void setFieldValue(_Fields field, @org.apache.thrift.annotation.Nullable Object value) {
    switch (field) {
    case PROCESS:
      if (value == null) {
        unsetProcess();
      } else {
        setProcess((Process)value);
      }
      break;

    case SPANS:
      if (value == null) {
        unsetSpans();
      } else {
        setSpans((java.util.List<Span>)value);
      }
      break;

    case SEQ_NO:
      if (value == null) {
        unsetSeqNo();
      } else {
        setSeqNo((Long)value);
      }
      break;

    case STATS:
      if (value == null) {
        unsetStats();
      } else {
        setStats((ClientStats)value);
      }
      break;

    }
  }

  @org.apache.thrift.annotation.Nullable
  @Override
  public Object getFieldValue(_Fields field) {
    switch (field) {
    case PROCESS:
      return getProcess();

    case SPANS:
      return getSpans();

    case SEQ_NO:
      return getSeqNo();

    case STATS:
      return getStats();

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
    case PROCESS:
      return isSetProcess();
    case SPANS:
      return isSetSpans();
    case SEQ_NO:
      return isSetSeqNo();
    case STATS:
      return isSetStats();
    }
    throw new IllegalStateException();
  }

  @Override
  public boolean equals(Object that) {
    if (that instanceof Batch)
      return this.equals((Batch)that);
    return false;
  }

  public boolean equals(Batch that) {
    if (that == null)
      return false;
    if (this == that)
      return true;

    boolean this_present_process = true && this.isSetProcess();
    boolean that_present_process = true && that.isSetProcess();
    if (this_present_process || that_present_process) {
      if (!(this_present_process && that_present_process))
        return false;
      if (!this.process.equals(that.process))
        return false;
    }

    boolean this_present_spans = true && this.isSetSpans();
    boolean that_present_spans = true && that.isSetSpans();
    if (this_present_spans || that_present_spans) {
      if (!(this_present_spans && that_present_spans))
        return false;
      if (!this.spans.equals(that.spans))
        return false;
    }

    boolean this_present_seqNo = true && this.isSetSeqNo();
    boolean that_present_seqNo = true && that.isSetSeqNo();
    if (this_present_seqNo || that_present_seqNo) {
      if (!(this_present_seqNo && that_present_seqNo))
        return false;
      if (this.seqNo != that.seqNo)
        return false;
    }

    boolean this_present_stats = true && this.isSetStats();
    boolean that_present_stats = true && that.isSetStats();
    if (this_present_stats || that_present_stats) {
      if (!(this_present_stats && that_present_stats))
        return false;
      if (!this.stats.equals(that.stats))
        return false;
    }

    return true;
  }

  @Override
  public int hashCode() {
    int hashCode = 1;

    hashCode = hashCode * 8191 + ((isSetProcess()) ? 131071 : 524287);
    if (isSetProcess())
      hashCode = hashCode * 8191 + process.hashCode();

    hashCode = hashCode * 8191 + ((isSetSpans()) ? 131071 : 524287);
    if (isSetSpans())
      hashCode = hashCode * 8191 + spans.hashCode();

    hashCode = hashCode * 8191 + ((isSetSeqNo()) ? 131071 : 524287);
    if (isSetSeqNo())
      hashCode = hashCode * 8191 + org.apache.thrift.TBaseHelper.hashCode(seqNo);

    hashCode = hashCode * 8191 + ((isSetStats()) ? 131071 : 524287);
    if (isSetStats())
      hashCode = hashCode * 8191 + stats.hashCode();

    return hashCode;
  }

  @Override
  public int compareTo(Batch other) {
    if (!getClass().equals(other.getClass())) {
      return getClass().getName().compareTo(other.getClass().getName());
    }

    int lastComparison = 0;

    lastComparison = Boolean.compare(isSetProcess(), other.isSetProcess());
    if (lastComparison != 0) {
      return lastComparison;
    }
    if (isSetProcess()) {
      lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.process, other.process);
      if (lastComparison != 0) {
        return lastComparison;
      }
    }
    lastComparison = Boolean.compare(isSetSpans(), other.isSetSpans());
    if (lastComparison != 0) {
      return lastComparison;
    }
    if (isSetSpans()) {
      lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.spans, other.spans);
      if (lastComparison != 0) {
        return lastComparison;
      }
    }
    lastComparison = Boolean.compare(isSetSeqNo(), other.isSetSeqNo());
    if (lastComparison != 0) {
      return lastComparison;
    }
    if (isSetSeqNo()) {
      lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.seqNo, other.seqNo);
      if (lastComparison != 0) {
        return lastComparison;
      }
    }
    lastComparison = Boolean.compare(isSetStats(), other.isSetStats());
    if (lastComparison != 0) {
      return lastComparison;
    }
    if (isSetStats()) {
      lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.stats, other.stats);
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
    StringBuilder sb = new StringBuilder("Batch(");
    boolean first = true;

    sb.append("process:");
    if (this.process == null) {
      sb.append("null");
    } else {
      sb.append(this.process);
    }
    first = false;
    if (!first) sb.append(", ");
    sb.append("spans:");
    if (this.spans == null) {
      sb.append("null");
    } else {
      sb.append(this.spans);
    }
    first = false;
    if (isSetSeqNo()) {
      if (!first) sb.append(", ");
      sb.append("seqNo:");
      sb.append(this.seqNo);
      first = false;
    }
    if (isSetStats()) {
      if (!first) sb.append(", ");
      sb.append("stats:");
      if (this.stats == null) {
        sb.append("null");
      } else {
        sb.append(this.stats);
      }
      first = false;
    }
    sb.append(")");
    return sb.toString();
  }

  public void validate() throws org.apache.thrift.TException {
    // check for required fields
    if (process == null) {
      throw new org.apache.thrift.protocol.TProtocolException("Required field 'process' was not present! Struct: " + toString());
    }
    if (spans == null) {
      throw new org.apache.thrift.protocol.TProtocolException("Required field 'spans' was not present! Struct: " + toString());
    }
    // check for sub-struct validity
    if (process != null) {
      process.validate();
    }
    if (stats != null) {
      stats.validate();
    }
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

  private static class BatchStandardSchemeFactory implements org.apache.thrift.scheme.SchemeFactory {
    @Override
    public BatchStandardScheme getScheme() {
      return new BatchStandardScheme();
    }
  }

  private static class BatchStandardScheme extends org.apache.thrift.scheme.StandardScheme<Batch> {

    @Override
    public void read(org.apache.thrift.protocol.TProtocol iprot, Batch struct) throws org.apache.thrift.TException {
      org.apache.thrift.protocol.TField schemeField;
      iprot.readStructBegin();
      while (true)
      {
        schemeField = iprot.readFieldBegin();
        if (schemeField.type == org.apache.thrift.protocol.TType.STOP) { 
          break;
        }
        switch (schemeField.id) {
          case 1: // PROCESS
            if (schemeField.type == org.apache.thrift.protocol.TType.STRUCT) {
              struct.process = new Process();
              struct.process.read(iprot);
              struct.setProcessIsSet(true);
            } else { 
              org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
            }
            break;
          case 2: // SPANS
            if (schemeField.type == org.apache.thrift.protocol.TType.LIST) {
              {
                org.apache.thrift.protocol.TList _list40 = iprot.readListBegin();
                struct.spans = new java.util.ArrayList<Span>(_list40.size);
                @org.apache.thrift.annotation.Nullable Span _elem41;
                for (int _i42 = 0; _i42 < _list40.size; ++_i42)
                {
                  _elem41 = new Span();
                  _elem41.read(iprot);
                  struct.spans.add(_elem41);
                }
                iprot.readListEnd();
              }
              struct.setSpansIsSet(true);
            } else { 
              org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
            }
            break;
          case 3: // SEQ_NO
            if (schemeField.type == org.apache.thrift.protocol.TType.I64) {
              struct.seqNo = iprot.readI64();
              struct.setSeqNoIsSet(true);
            } else { 
              org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
            }
            break;
          case 4: // STATS
            if (schemeField.type == org.apache.thrift.protocol.TType.STRUCT) {
              struct.stats = new ClientStats();
              struct.stats.read(iprot);
              struct.setStatsIsSet(true);
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
    public void write(org.apache.thrift.protocol.TProtocol oprot, Batch struct) throws org.apache.thrift.TException {
      struct.validate();

      oprot.writeStructBegin(STRUCT_DESC);
      if (struct.process != null) {
        oprot.writeFieldBegin(PROCESS_FIELD_DESC);
        struct.process.write(oprot);
        oprot.writeFieldEnd();
      }
      if (struct.spans != null) {
        oprot.writeFieldBegin(SPANS_FIELD_DESC);
        {
          oprot.writeListBegin(new org.apache.thrift.protocol.TList(org.apache.thrift.protocol.TType.STRUCT, struct.spans.size()));
          for (Span _iter43 : struct.spans)
          {
            _iter43.write(oprot);
          }
          oprot.writeListEnd();
        }
        oprot.writeFieldEnd();
      }
      if (struct.isSetSeqNo()) {
        oprot.writeFieldBegin(SEQ_NO_FIELD_DESC);
        oprot.writeI64(struct.seqNo);
        oprot.writeFieldEnd();
      }
      if (struct.stats != null) {
        if (struct.isSetStats()) {
          oprot.writeFieldBegin(STATS_FIELD_DESC);
          struct.stats.write(oprot);
          oprot.writeFieldEnd();
        }
      }
      oprot.writeFieldStop();
      oprot.writeStructEnd();
    }

  }

  private static class BatchTupleSchemeFactory implements org.apache.thrift.scheme.SchemeFactory {
    @Override
    public BatchTupleScheme getScheme() {
      return new BatchTupleScheme();
    }
  }

  private static class BatchTupleScheme extends org.apache.thrift.scheme.TupleScheme<Batch> {

    @Override
    public void write(org.apache.thrift.protocol.TProtocol prot, Batch struct) throws org.apache.thrift.TException {
      org.apache.thrift.protocol.TTupleProtocol oprot = (org.apache.thrift.protocol.TTupleProtocol) prot;
      struct.process.write(oprot);
      {
        oprot.writeI32(struct.spans.size());
        for (Span _iter44 : struct.spans)
        {
          _iter44.write(oprot);
        }
      }
      java.util.BitSet optionals = new java.util.BitSet();
      if (struct.isSetSeqNo()) {
        optionals.set(0);
      }
      if (struct.isSetStats()) {
        optionals.set(1);
      }
      oprot.writeBitSet(optionals, 2);
      if (struct.isSetSeqNo()) {
        oprot.writeI64(struct.seqNo);
      }
      if (struct.isSetStats()) {
        struct.stats.write(oprot);
      }
    }

    @Override
    public void read(org.apache.thrift.protocol.TProtocol prot, Batch struct) throws org.apache.thrift.TException {
      org.apache.thrift.protocol.TTupleProtocol iprot = (org.apache.thrift.protocol.TTupleProtocol) prot;
      struct.process = new Process();
      struct.process.read(iprot);
      struct.setProcessIsSet(true);
      {
        org.apache.thrift.protocol.TList _list45 = iprot.readListBegin(org.apache.thrift.protocol.TType.STRUCT);
        struct.spans = new java.util.ArrayList<Span>(_list45.size);
        @org.apache.thrift.annotation.Nullable Span _elem46;
        for (int _i47 = 0; _i47 < _list45.size; ++_i47)
        {
          _elem46 = new Span();
          _elem46.read(iprot);
          struct.spans.add(_elem46);
        }
      }
      struct.setSpansIsSet(true);
      java.util.BitSet incoming = iprot.readBitSet(2);
      if (incoming.get(0)) {
        struct.seqNo = iprot.readI64();
        struct.setSeqNoIsSet(true);
      }
      if (incoming.get(1)) {
        struct.stats = new ClientStats();
        struct.stats.read(iprot);
        struct.setStatsIsSet(true);
      }
    }
  }

  private static <S extends org.apache.thrift.scheme.IScheme> S scheme(org.apache.thrift.protocol.TProtocol proto) {
    return (org.apache.thrift.scheme.StandardScheme.class.equals(proto.getScheme()) ? STANDARD_SCHEME_FACTORY : TUPLE_SCHEME_FACTORY).getScheme();
  }
}

