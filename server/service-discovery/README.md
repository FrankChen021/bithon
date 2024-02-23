# Description

This directory contains modules that support remote service registration and invocation.

# Use Case

The use case of these modules is to call the management APIs of agents, those are implemented in the `agent-controller` module, from the `web-service` module.

# Why do we need this?

If bithon is deployed in all-in-one module, and there's only instance of bithon, there's no need for such mechanism.

However, in typical production environment,
the `agent-controller` module is deployed with the `collector` module together as multiple instances (because they both provide interfaces for agents), 

In such a deployment, each controller instance manages part of client application instances,
for any of the `web-service` module, it's not able to know which controller the query querying info from one specific client application should go.

To solve that problem, the `web-service` module has to know ALL the deployed controller instances.

# How does it work
1. Firstly, the `agent-controller` module registers itself in a service registry (Currently Nacos is only the option).
2. Secondly, when the query, that is going to get data from one specific client application,
   comes to the `web-service` module,
it gets all instances of `agent-controller` from the service registry, and then broadcast the query to ALL of these `controller` instances.
3. Thirdly, each `agent-controller` instance check if it has connection to the target client application instance, if it has, the controller will forward the command to the agent.
4. Lastly, when the invocation to all instances complete, the `web-service` module merges the returning results together and then writes response to the client.

