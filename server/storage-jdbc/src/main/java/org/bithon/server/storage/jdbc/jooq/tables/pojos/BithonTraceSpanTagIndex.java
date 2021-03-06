/*
 * This file is generated by jOOQ.
 */
package org.bithon.server.storage.jdbc.jooq.tables.pojos;


import java.io.Serializable;
import java.sql.Timestamp;


/**
 * This class is generated by jOOQ.
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class BithonTraceSpanTagIndex implements Serializable {

    private static final long serialVersionUID = -1185179247;

    private Timestamp timestamp;
    private String    f1;
    private String    f2;
    private String    f3;
    private String    f4;
    private String    f5;
    private String    f6;
    private String    f7;
    private String    f8;
    private String    f9;
    private String    f10;
    private String    f11;
    private String    f12;
    private String    f13;
    private String    f14;
    private String    f15;
    private String    f16;
    private String    traceid;

    public BithonTraceSpanTagIndex() {}

    public BithonTraceSpanTagIndex(BithonTraceSpanTagIndex value) {
        this.timestamp = value.timestamp;
        this.f1 = value.f1;
        this.f2 = value.f2;
        this.f3 = value.f3;
        this.f4 = value.f4;
        this.f5 = value.f5;
        this.f6 = value.f6;
        this.f7 = value.f7;
        this.f8 = value.f8;
        this.f9 = value.f9;
        this.f10 = value.f10;
        this.f11 = value.f11;
        this.f12 = value.f12;
        this.f13 = value.f13;
        this.f14 = value.f14;
        this.f15 = value.f15;
        this.f16 = value.f16;
        this.traceid = value.traceid;
    }

    public BithonTraceSpanTagIndex(
        Timestamp timestamp,
        String    f1,
        String    f2,
        String    f3,
        String    f4,
        String    f5,
        String    f6,
        String    f7,
        String    f8,
        String    f9,
        String    f10,
        String    f11,
        String    f12,
        String    f13,
        String    f14,
        String    f15,
        String    f16,
        String    traceid
    ) {
        this.timestamp = timestamp;
        this.f1 = f1;
        this.f2 = f2;
        this.f3 = f3;
        this.f4 = f4;
        this.f5 = f5;
        this.f6 = f6;
        this.f7 = f7;
        this.f8 = f8;
        this.f9 = f9;
        this.f10 = f10;
        this.f11 = f11;
        this.f12 = f12;
        this.f13 = f13;
        this.f14 = f14;
        this.f15 = f15;
        this.f16 = f16;
        this.traceid = traceid;
    }

    public Timestamp getTimestamp() {
        return this.timestamp;
    }

    public void setTimestamp(Timestamp timestamp) {
        this.timestamp = timestamp;
    }

    public String getF1() {
        return this.f1;
    }

    public void setF1(String f1) {
        this.f1 = f1;
    }

    public String getF2() {
        return this.f2;
    }

    public void setF2(String f2) {
        this.f2 = f2;
    }

    public String getF3() {
        return this.f3;
    }

    public void setF3(String f3) {
        this.f3 = f3;
    }

    public String getF4() {
        return this.f4;
    }

    public void setF4(String f4) {
        this.f4 = f4;
    }

    public String getF5() {
        return this.f5;
    }

    public void setF5(String f5) {
        this.f5 = f5;
    }

    public String getF6() {
        return this.f6;
    }

    public void setF6(String f6) {
        this.f6 = f6;
    }

    public String getF7() {
        return this.f7;
    }

    public void setF7(String f7) {
        this.f7 = f7;
    }

    public String getF8() {
        return this.f8;
    }

    public void setF8(String f8) {
        this.f8 = f8;
    }

    public String getF9() {
        return this.f9;
    }

    public void setF9(String f9) {
        this.f9 = f9;
    }

    public String getF10() {
        return this.f10;
    }

    public void setF10(String f10) {
        this.f10 = f10;
    }

    public String getF11() {
        return this.f11;
    }

    public void setF11(String f11) {
        this.f11 = f11;
    }

    public String getF12() {
        return this.f12;
    }

    public void setF12(String f12) {
        this.f12 = f12;
    }

    public String getF13() {
        return this.f13;
    }

    public void setF13(String f13) {
        this.f13 = f13;
    }

    public String getF14() {
        return this.f14;
    }

    public void setF14(String f14) {
        this.f14 = f14;
    }

    public String getF15() {
        return this.f15;
    }

    public void setF15(String f15) {
        this.f15 = f15;
    }

    public String getF16() {
        return this.f16;
    }

    public void setF16(String f16) {
        this.f16 = f16;
    }

    public String getTraceid() {
        return this.traceid;
    }

    public void setTraceid(String traceid) {
        this.traceid = traceid;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("BithonTraceSpanTagIndex (");

        sb.append(timestamp);
        sb.append(", ").append(f1);
        sb.append(", ").append(f2);
        sb.append(", ").append(f3);
        sb.append(", ").append(f4);
        sb.append(", ").append(f5);
        sb.append(", ").append(f6);
        sb.append(", ").append(f7);
        sb.append(", ").append(f8);
        sb.append(", ").append(f9);
        sb.append(", ").append(f10);
        sb.append(", ").append(f11);
        sb.append(", ").append(f12);
        sb.append(", ").append(f13);
        sb.append(", ").append(f14);
        sb.append(", ").append(f15);
        sb.append(", ").append(f16);
        sb.append(", ").append(traceid);

        sb.append(")");
        return sb.toString();
    }
}
