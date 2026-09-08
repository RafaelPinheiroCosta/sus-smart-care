package br.com.sussmartcare.gateway;
import java.util.UUID;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
@Component public class CorrelationIdGlobalFilter implements GlobalFilter,Ordered {
  private static final String HEADER="X-Correlation-Id";
  public Mono<Void> filter(ServerWebExchange exchange,GatewayFilterChain chain){
    String value=exchange.getRequest().getHeaders().getFirst(HEADER); if(value==null||value.isBlank()) value=UUID.randomUUID().toString();
    var request=exchange.getRequest().mutate().header(HEADER,value).build(); var response=exchange.getResponse(); response.getHeaders().set(HEADER,value);
    return chain.filter(exchange.mutate().request(request).build());
  }
  public int getOrder(){return -100;}
}
