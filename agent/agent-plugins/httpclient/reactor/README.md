
To know how the reactor httpclient works,
it's better to read the NettyRoutingFilter implementation in SpringWebFlux(since 3.0.0) first.

The below is the code from NettyRoutingFilter implementation in SpringWebFlux(since 3.0.0),
it helps us to understand how the agent in this plugin works.

```java
Flux<HttpClientResponse> responseFlux = getHttpClient(route, exchange)
        .headers(headers -> {
            ...
		}).request(method).uri(url).send((req, nettyOutbound) -> {
			
			return nettyOutbound.send(request.getBody().map(this::getByteBuf));
		}).responseConnection((res, connection) -> {

			return ...
		});
```
