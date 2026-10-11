package com.terranangel.es.config;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.JsonpMapper;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest5_client.Rest5ClientTransport;
import co.elastic.clients.transport.rest5_client.low_level.Rest5Client;
import co.elastic.clients.transport.rest5_client.low_level.Rest5ClientBuilder;
import org.apache.hc.client5.http.auth.AuthScope;
import org.apache.hc.client5.http.auth.UsernamePasswordCredentials;
import org.apache.hc.client5.http.impl.auth.BasicCredentialsProvider;
import org.apache.hc.client5.http.ssl.DefaultClientTlsStrategy;
import org.apache.hc.client5.http.ssl.NoopHostnameVerifier;
import org.apache.hc.core5.http.HttpHost;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;
import org.springframework.util.StringUtils;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.net.URI;
import java.security.cert.X509Certificate;

/**
 * Elasticsearch 装配（Spring Data ES）。
 * <p>
 * 【开关】terranangel.es.enabled=true 才启用，默认 false 不连 ES，应用照常启动。
 * <p>
 * 【为什么手动装配】
 * 启动类排除了 ES 的全部自动配置（否则 enabled=false 时也会尝试连 ES），
 * 所以要自己创建 ES 9 客户端的整条链：
 *   Rest5ClientBuilder(地址+认证) → Rest5Client → Rest5ClientTransport → ElasticsearchClient
 * <p>
 * 【面试点】ES 客户端层次：
 *   Rest5Client          —— 底层 HTTP 客户端（发请求）
 *   Rest5ClientTransport —— 传输层（负责序列化）
 *   ElasticsearchClient  —— 高层客户端（你用的就是这个，ES 8+ 官方 Java Client）
 */
@Configuration
@ConditionalOnProperty(prefix = "terranangel.es", name = "enabled", havingValue = "true")
@EnableElasticsearchRepositories(basePackages = "com.terranangel.es")
public class EsSupportConfig {

    /** ES 地址，如 http://127.0.0.1:9200 */
    @Value("${terranangel.es.uris:http://127.0.0.1:9200}")
    private String uris;

    /** 用户名（ES 8+ 默认开启安全，默认用户名 elastic） */
    @Value("${terranangel.es.username:}")
    private String username;

    /** 密码 */
    @Value("${terranangel.es.password:}")
    private String password;

    /**
     * 底层 REST 客户端（Rest5Client）。
     * 负责真正发 HTTP 请求，配置地址 + 基础认证 + 跳过自签证书校验。
     */
    @Bean
    public Rest5Client rest5Client() throws Exception {
        HttpHost host = toHttpHost(uris);
        Rest5ClientBuilder builder = Rest5Client.builder(host);

        // 有配用户名密码就加基础认证（ES 8+ 默认开启安全）
        if (StringUtils.hasText(username)) {
            builder.setHttpClientConfigCallback(httpClientBuilder -> {
                BasicCredentialsProvider provider = new BasicCredentialsProvider();
                provider.setCredentials(new AuthScope(null, -1),
                        new UsernamePasswordCredentials(username, password.toCharArray()));
                httpClientBuilder.setDefaultCredentialsProvider(provider);
            });
        }

        // ES 8+ 默认用 HTTPS + 自签证书，Java 默认不信任 → 这里跳过证书校验（本地开发用）
        // 生产环境应该把 ES 证书导入到 truststore，而不是跳过校验
        if (uris.startsWith("https://")) {
            builder.setConnectionManagerCallback(connectionManagerBuilder -> {
                try {
                    connectionManagerBuilder.setTlsStrategy(
                            new DefaultClientTlsStrategy(trustAllSslContext(), NoopHostnameVerifier.INSTANCE));
                } catch (Exception e) {
                    throw new IllegalStateException("初始化 ES TLS 策略失败", e);
                }
            });
        }

        return builder.build();
    }

    /**
     * 信任所有证书的 SSLContext（仅用于本地开发跳过自签证书校验）。
     * 生产环境请把 ES 证书导入 truststore，不要用这个。
     */
    private static SSLContext trustAllSslContext() throws Exception {
        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, new TrustManager[]{new X509TrustManager() {
            @Override
            public void checkClientTrusted(X509Certificate[] chain, String authType) {
            }

            @Override
            public void checkServerTrusted(X509Certificate[] chain, String authType) {
            }

            @Override
            public X509Certificate[] getAcceptedIssuers() {
                return new X509Certificate[0];
            }
        }}, new java.security.SecureRandom());
        return sslContext;
    }

    /**
     * JSON 序列化器（ES 客户端把对象转成 JSON 发出去，用 Jackson）。
     */
    @Bean
    public JsonpMapper jsonpMapper() {
        return new JacksonJsonpMapper();
    }

    /**
     * 传输层（把 Rest5Client + JsonpMapper 组合起来）。
     */
    @Bean
    public ElasticsearchTransport elasticsearchTransport(Rest5Client rest5Client, JsonpMapper jsonpMapper) {
        return new Rest5ClientTransport(rest5Client, jsonpMapper);
    }

    /**
     * 高层 ES 客户端（业务代码就用它操作 ES）。
     */
    @Bean
    public ElasticsearchClient elasticsearchClient(ElasticsearchTransport transport) {
        return new ElasticsearchClient(transport);
    }

    /**
     * ElasticsearchTemplate（Spring Data ES 的操作模板）。
     * <p>
     * ElasticsearchRepository 底层需要它（bean 名必须是 elasticsearchTemplate）。
     * 因为排除了 DataElasticsearchAutoConfiguration，所以这里手动创建。
     */
    @Bean
    public ElasticsearchTemplate elasticsearchTemplate(ElasticsearchClient elasticsearchClient) {
        return new ElasticsearchTemplate(elasticsearchClient);
    }

    /** 把 "http://127.0.0.1:9200" 解析成 HttpHost */
    private HttpHost toHttpHost(String uri) {
        if (!uri.startsWith("http://") && !uri.startsWith("https://")) {
            uri = "http://" + uri;
        }
        URI u = URI.create(uri);
        return new HttpHost(u.getScheme(), u.getHost(), u.getPort());
    }
}
