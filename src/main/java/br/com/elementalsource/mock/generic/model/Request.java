package br.com.elementalsource.mock.generic.model;

import br.com.elementalsource.mock.infra.component.ConvertJson;
import br.com.elementalsource.mock.infra.component.impl.ConvertJsonImpl;
import br.com.elementalsource.mock.infra.exception.impl.JsonApiApplicationException;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class Request implements Comparable<Request> {

    private static final ConvertJson CONVERT_JSON = new ConvertJsonImpl();

    private RequestMethod method;
    private String uri;
    private Optional<HttpHeaders> headers;
    private Optional<Map<String, String>> query;
    private Optional<String> body;

    private Request() {
        this.headers = Optional.empty();
        this.query = Optional.empty();
        this.body = Optional.empty();
    }

    public Request(RequestMethod method, String uri) {
        this();
        this.method = method;
        this.uri = uri;
    }

    public RequestMethod getMethod() {
        return method;
    }

    public String getUri() {
        return uri;
    }

    public Optional<HttpHeaders> getHeaders() {
        return headers;
    }

    public Optional<Map<String, String>> getQuery() {
        return query;
    }

    public Optional<String> getBody() {
        return body;
    }

    public Integer countQueryFields() {
        return getQuery()
                .map(Map::size)
                .orElse(0);
    }

    public Integer countBodyFields() {
        return countFields(getBody());
    }

    private Integer countFields(Optional<String> attr) {
        return attr
                .map(queryStr -> {
                    try {
                        return CONVERT_JSON.apply(queryStr).size();
                    } catch (JsonApiApplicationException e) {
                        return 0;
                    }
                })
                .orElse(0);
    }

    public Boolean isValid() {
        return
                (headers.filter(h -> h.size() > 0).map(h -> true).orElse(false))
                        || countBodyFields() > 0
                        || countQueryFields() > 0;
    }

    @Override
    public int compareTo(Request o) {
        final Integer resultMethod = this.method.compareTo(o.method);
        if (resultMethod == 0) {
            final Integer resultUrl = this.uri.compareTo(o.uri);
            if (resultUrl == 0) {
                final Integer resultQueries = o.countQueryFields().compareTo(this.countQueryFields());
                if (resultQueries == 0) {
                    return o.countBodyFields().compareTo(this.countBodyFields());
                } else {
                    return resultQueries;
                }
            } else {
                return resultUrl;
            }
        } else {
            return resultMethod;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Request request = (Request) o;
        return method == request.method &&
                Objects.equals(uri, request.uri) &&
                Objects.equals(headers, request.headers) &&
                Objects.equals(query, request.query) &&
                Objects.equals(body, request.body);
    }

    @Override
    public int hashCode() {
        return Objects.hash(method, uri, headers, query, body);
    }

    public static class Builder {

        private  final Request instance;

        public Builder(RequestMethod method, String uri) {
            this.instance = new Request(method, uri);
        }

        public Builder(Request request) {
            this(request.getMethod(), request.getUri());
            withHeader(request.getHeaders());
            withQuery(request.getQuery());
            withBody(request.getBody());
        }

        public Builder withHeader(HttpHeaders headers) {
            return withHeader(Optional.ofNullable(headers));
        }

        public Builder withHeader(Optional<HttpHeaders> headers) {
            instance.headers = headers;
            return this;
        }

        public Builder withQuery(Map<String, String> query) {
            return withQuery(Optional.ofNullable(query));
        }

        public Builder withQuery(Optional<Map<String, String>> query) {
            instance.query = query;
            return this;
        }

        public Builder withBody(Optional<String> body) {
            instance.body = body;
            return this;
        }

        public Request build() {
            return instance;
        }

    }

}
