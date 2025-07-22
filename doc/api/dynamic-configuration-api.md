This document describes APIs for dynamic agent configuration in Bithon.

## Agent Configuration API

This API allows you to manage dynamic agent configurations for target applications. Settings are stored persistently and can be retrieved or modified via the following endpoints.

### 1. Get Agent Configuration

**POST**   `/api/agent/configuration/get`

**Headers:**
- `Accept` (optional): Specify the response body format. If not given, returns data in JSON format. 
    
    It's suggested to set this one to `application/yaml` so that the response will be in YAML format and more readable.

- `Content-Type` (optional): Specify the request body format. If not given, defaults to `application/json`.
   `application/yaml` is also supported

**Request Body:**
```yaml
{
  "appName": "<the target application name configured via bithon.application.name property>",
  "environment": "<optional environment configured via bithon.application.env property>",
  "format": "json|yaml|default" // optional, default is "default"
}
```

If `environment` is not specified, all configurations for the specified application will be returned.

**Response:**
Returns a list of configurations for the specified application and environment. Each entry includes:
- `name`: Setting name
- `value`: Setting value (in requested format)
- `format`: Format of the value (`json` or `yaml`)

### 2. Add Agent Configuration

**POST** `/api/agent/configuration/add`

**Headers:**
- `X-Bithon-Token` (optional): Authorization token

**Request Body:**
```json
{
  "appName": "<the application name configured via bithon.application.name property>",
  "environment": "<optional environment configured via bithon.application.env property>",
  "name": "<a customized setting name>",
  "value": "<setting value>",
  "format": "json|yaml" // optional, default is "json"
}
```

**Behavior:**
- Validates the format and value.
- Requires permission (token or current user).
- Fails if the setting already exists.
- Notifies agent controller on success.

#### Examples

The following example demonstrates that for all running instances of application `bithon-collector` in environment `live`, 
- update the setting `tracing.samplingConfigs.default.samplingPercentage` from default 100% to 50%
- update the setting `tracing.headers.request` to record HTTP header 'Content-Type' in the tracing span logs.  

If there's already a setting with the same name `setting1` in the system, the API will return an error.

```text
curl --location 'http://<HOST_AND_PORT>/api/agent/configuration/add' \
--header 'Content-Type: application/yaml' \
--header 'X-Bithon-Token: <TOKEN_IF_REQUIRED>' \
--data '- appName: bithon-collector
environment: live
name: setting1
value: |-
tracing:
  samplingConfigs:
    default:
      samplingPercentage: 50%
  headers:
    request: ["Content-Type"]
format: yaml'
```

Once the setting is added, it may take up to 1 minutes for running instances to apply the new setting.
You can check the applied setting by going to the Web UI, navigating to the "Diagnosis -> App Insight" page, and selecting one running instance of target application, 
and then check its loaded configurations under the "Configuration" tab.

In above example, the HTTP body is in YAML format, you can also use JSON format as below:

```json
{
  "appName": "bithon-collector",
  "environment": "live",
  "name": "setting1",
  "value": {
    "tracing": {
      "samplingConfigs": {
        "default": {
          "samplingPercentage": 50
        }
      }
    }
  },
  "format": "json"
}
```

### 3. Update Agent Configuration

**POST** `/api/agent/configuration/update`

**Headers:**
- `X-Bithon-Token` (optional): Authorization token
- `Accept` (optional): Specify the response body format. If not given, returned data in JSON format.

  It's suggested to set this one to `application/yaml` so that the response will be in more readable YAML format.

- `Content-Type` (optional): Specify the request body format. If not given, defaults to `application/json`.
  `application/yaml` is also supported

**Request Body:**
Same as Add Agent Configuration.

**Behavior:**
- Validates the format and value.
- Requires permission (token or current user).
- Fails if the setting does not exist.
- Notifies agent controller on success.

### 4. Delete Agent Configuration

**POST** `/api/agent/configuration/delete`

**Headers:**
- `X-Bithon-Token` (optional): Authorization token

**Request Body:**
```json
{
  "appName": "<application name>",
  "name": "<setting name>",
  "environment": "<optional environment>"
}
```

**Behavior:**
- Requires permission (token or current user).
- Deletes the specified setting.

> 
> NOTE:
> 
> The deleted configuration will NOT be applied to ANY running instances.
> 
> You have to restart the target application instance to apply the change.


### Permission and Authorization
- If `X-Bithon-Token` is provided, it is used for authorization and authentication.
- If the token associated with the current user does not have permission to perform the operation, an error will be returned.
Under this case, you need to contact your administrator to assign permissions on target applications.


