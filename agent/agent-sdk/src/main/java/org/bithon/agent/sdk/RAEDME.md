# Version Strategy

When a new API is added, follow the steps below:
1. Add the new API in the latest version package, e.g. `org.bithon.agent.sdk.tracing.ISpanScopeV2`.
2. Declare this V2 to be sub-interface of the previous version, e.g. `ISpanScopeV2 extends ISpanScopeV1`.
3. Change the parent interface of ISpanScope to the latest version, e.g. `ISpanScope extends ISpanScopeV2`.
