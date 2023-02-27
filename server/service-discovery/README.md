# Description

This directory contains modules that support remote service registration and invocation.

# Use Case

The use case of these module is to call the management APIs of agents that're implemented in the `collector` module from the `web-service` module.

# Why do we need this?

If bithon is deployed in all-in-one module, and there's only instance of bithon, there's no need of such mechanism.

However, if bithon(the collector) is deployed in multiple instances, each collector might handle several client applications,
for any of the `web-service` module, it's not able to know where the query, that is going to query info from one specific client application, should go.

To solve that problem, the `web-service` module has to know ALL the deployed `collector` instances.
So the first step is that the `collector` module registers itself in a service registry(Currently Nacos is only the option).
And then, when the query, that is going to get data from one specific client application, comes to the `web-service` module,
it gets all instances of `collector` from the service registry, and then broadcast the query to ALL of these `collector` instances.
At last, when the invocation to all instances complete, the `web-service` module merges the returning results together and then writes response to the client.

