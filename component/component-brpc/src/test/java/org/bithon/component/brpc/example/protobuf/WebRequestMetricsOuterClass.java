/*
 *    Copyright 2020 bithon.org
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: WebRequestMetrics.proto

package org.bithon.component.brpc.example.protobuf;

public final class WebRequestMetricsOuterClass {
  private WebRequestMetricsOuterClass() {}
  public static void registerAllExtensions(
      org.bithon.shaded.com.google.protobuf.ExtensionRegistryLite registry) {
  }

  public static void registerAllExtensions(
      org.bithon.shaded.com.google.protobuf.ExtensionRegistry registry) {
    registerAllExtensions(
        (org.bithon.shaded.com.google.protobuf.ExtensionRegistryLite) registry);
  }
  static final org.bithon.shaded.com.google.protobuf.Descriptors.Descriptor
    internal_static_org_bithon_component_brpc_example_protobuf_WebRequestMetrics_descriptor;
  static final 
    org.bithon.shaded.com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
      internal_static_org_bithon_component_brpc_example_protobuf_WebRequestMetrics_fieldAccessorTable;

  public static org.bithon.shaded.com.google.protobuf.Descriptors.FileDescriptor
      getDescriptor() {
    return descriptor;
  }
  private static  org.bithon.shaded.com.google.protobuf.Descriptors.FileDescriptor
      descriptor;
  static {
    String[] descriptorData = {
      "\n\027WebRequestMetrics.proto\022*org.bithon.co" +
      "mponent.brpc.example.protobuf\"\231\001\n\021WebReq" +
      "uestMetrics\022\013\n\003uri\030\001 \001(\t\022\024\n\014costNanoTime" +
      "\030\002 \001(\003\022\020\n\010requests\030\003 \001(\003\022\020\n\010count4xx\030\004 \001" +
      "(\003\022\020\n\010count5xx\030\005 \001(\003\022\024\n\014requestBytes\030\006 \001" +
      "(\003\022\025\n\rresponseBytes\030\007 \001(\003B\002P\001b\006proto3"
    };
    descriptor = org.bithon.shaded.com.google.protobuf.Descriptors.FileDescriptor
      .internalBuildGeneratedFileFrom(descriptorData,
        new org.bithon.shaded.com.google.protobuf.Descriptors.FileDescriptor[] {
        });
    internal_static_org_bithon_component_brpc_example_protobuf_WebRequestMetrics_descriptor =
      getDescriptor().getMessageTypes().get(0);
    internal_static_org_bithon_component_brpc_example_protobuf_WebRequestMetrics_fieldAccessorTable = new
      org.bithon.shaded.com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
        internal_static_org_bithon_component_brpc_example_protobuf_WebRequestMetrics_descriptor,
        new String[] { "Uri", "CostNanoTime", "Requests", "Count4Xx", "Count5Xx", "RequestBytes", "ResponseBytes", });
  }

  // @@protoc_insertion_point(outer_class_scope)
}