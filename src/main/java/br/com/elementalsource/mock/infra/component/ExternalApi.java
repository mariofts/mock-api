package br.com.elementalsource.mock.infra.component;

import br.com.elementalsource.mock.configuration.model.CaptureState;
import br.com.elementalsource.mock.generic.model.Request;
import br.com.elementalsource.mock.configuration.repository.CaptureStateRepository;
import br.com.elementalsource.mock.generic.model.ExternalApiResult;
import br.com.elementalsource.mock.infra.model.UriConfiguration;
import br.com.elementalsource.mock.infra.property.ApiProperty;
import okhttp3.Headers;
import okhttp3.OkHttpClient;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

@Component
public class ExternalApi {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExternalApi.class);

    private final ApiProperty apiProperty;
    private final QueryStringBuilder queryStringBuilder;
    private final RestTemplate restTemplate;
    private final HeaderFilter headerFilter;
    private final CaptureStateRepository captureStateRepository;
    private final OkHttpClient okHttpClient;

    @Autowired
    public ExternalApi(ApiProperty apiProperty, QueryStringBuilder queryStringBuilder, RestTemplate restTemplate,
                       HeaderFilter headerFilter, CaptureStateRepository captureStateRepository, OkHttpClient okHttpClient) {
        this.apiProperty = apiProperty;
        this.queryStringBuilder = queryStringBuilder;
        this.restTemplate = restTemplate;
        this.headerFilter = headerFilter;
        this.captureStateRepository = captureStateRepository;
        this.okHttpClient = okHttpClient;
    }

    public Optional<ExternalApiResult> execute(final Request request) {
        final Boolean state = captureStateRepository
                .getCurrent()
                .map(CaptureState::isEnabled)
                .orElse(true);


        final UriConfiguration uriConfiguration = apiProperty
                .getConfiguration(request.getUri())
                .orElse(new UriConfiguration(apiProperty.getHost(), Pattern.compile(".*"), state));
        final Optional<HttpHeaders> httpHeaders = headerFilter.execute(request.getHeaders());

        LOGGER.info("### EXTERNAL API ###");
        LOGGER.info("{}", uriConfiguration);
        request.getBody().ifPresent(LOGGER::info);
        httpHeaders.ifPresent(list -> LOGGER.info(list.toString()));

        final HttpEntity<String> entity = httpHeaders
                .map(headers -> request.getBody().map(body -> new HttpEntity<>(body, headers))
                        .orElse(new HttpEntity<>(headers)))
                .orElse(request.getBody().map(HttpEntity<String>::new).orElse(new HttpEntity<>((String) null)));

        final String parameters = request.getQuery().map(queryStringBuilder::fromMap).orElse("");
        final String url = uriConfiguration
                .getHost()
                .concat(request.getUri())
                .concat(parameters);

        LOGGER.info("URL => {}", url);

//        final WebClient.Builder webClientBuilder = WebClient
//                .builder()
//                .baseUrl(url)
//                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
//
//        webClientBuilder.build().method(HttpMethod.GET).retrieve().bodyToMono(Map.class).block();

        final ResponseEntity<String> apiResult = restTemplate.exchange(url, HttpMethod.valueOf(request.getMethod().name().toUpperCase()), entity,
                String.class);
        return Optional.of(new ExternalApiResult(apiResult, uriConfiguration));
    }

    public Optional<ExternalApiResult> okHttpClientRequest(HttpServletRequest request, Request req){

        final Boolean state = captureStateRepository
                .getCurrent()
                .map(CaptureState::isEnabled)
                .orElse(true);

        Enumeration<String> headerNames = request.getHeaderNames();
        Headers.Builder builder = new Headers.Builder();
        while (headerNames.hasMoreElements()) {
            final String name = headerNames.nextElement();
            builder.add(name, request.getHeader(name));
        }
        Headers h = builder.build();

        final UriConfiguration uriConfiguration = apiProperty
                .getConfiguration(req.getUri())
                .orElse(new UriConfiguration(apiProperty.getHost(), Pattern.compile(".*"), state));

        okhttp3.Request okHttpRequest = new okhttp3.Request.Builder()
                .url(uriConfiguration.getHost()+req.getUri()+"?"+request.getQueryString())
                .get()
                .headers(h)
                .build();

        ResponseEntity<String> apiResult = null;
        try {
            Response response = okHttpClient.newCall(okHttpRequest).execute();
            apiResult = new ResponseEntity<>(response.body().string(), new HttpHeaders(), HttpStatus.OK);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return Optional.of(new ExternalApiResult(apiResult, uriConfiguration));
    }

}
