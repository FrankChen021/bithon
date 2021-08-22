
## Static Configuration

Configuration in agent.yml file located at 'agent/agent-main/resources' directory

## Dynamic Configuration

Configurations via command line arguments or environment variables. 
Each static configuration item has a corresponding dynamic configuration item.

Given a static configuration `agent.plugin.http.incoming.filter.uri.suffixes`, it could be set by dynamic configuration as

```bash
-Dbithon.agent.plugin.http.incoming.filter.uri.suffixes=.html
```

## Configurations
### Agent Plugin Configuration

| configuration | description | default | example |
|---|---|---|---|
| agent.plugin.http.incoming.filter.uri.suffixes | comma separated string in lower case |  | .html,.json |
| agent.plugin.http.incoming.filter.user-agent.matchers | A Matcher list | | |
