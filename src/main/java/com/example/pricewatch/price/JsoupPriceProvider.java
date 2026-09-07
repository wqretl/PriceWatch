package com.example.pricewatch.price;

import com.example.pricewatch.exception.PriceParsingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("!mock-price")
public class JsoupPriceProvider implements PriceProvider {

    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
            + "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36";
    private static final int MAX_REDIRECTS = 5;

    private final ObjectMapper objectMapper;
    private final int timeoutMs;

    public JsoupPriceProvider(ObjectMapper objectMapper, @Value("${pricewatch.parser.timeout-ms}") int timeoutMs) {
        this.objectMapper = objectMapper;
        this.timeoutMs = timeoutMs;
    }

    @Override
    public BigDecimal getPrice(String productUrl) {
        Document document = loadDocument(productUrl);

        return extractMetaPrice(document)
                .or(() -> extractItempropPrice(document))
                .or(() -> extractJsonLdPrice(document))
                .or(() -> extractAttributePrice(document))
                .or(() -> extractFallbackPrice(document))
                .orElseThrow(() -> new PriceParsingException("Price was not found on the product page"));
    }

    private Document loadDocument(String productUrl) {
        String currentUrl = productUrl;
        try {
            for (int redirectCount = 0; redirectCount <= MAX_REDIRECTS; redirectCount++) {
                URI uri = validateUrl(currentUrl);
                Connection.Response response = Jsoup.connect(uri.toString())
                        .userAgent(USER_AGENT)
                        .header("Accept-Language", "en-US,en;q=0.9")
                        .timeout(timeoutMs)
                        .followRedirects(false)
                        .ignoreHttpErrors(true)
                        .execute();

                if (isRedirect(response.statusCode())) {
                    String location = response.header("Location");
                    if (location == null || location.isBlank()) {
                        throw new PriceParsingException("Product page redirected without a location");
                    }
                    currentUrl = uri.resolve(location).toString();
                    continue;
                }
                if (response.statusCode() < 200 || response.statusCode() >= 300) {
                    throw new PriceParsingException("Product page returned HTTP status " + response.statusCode());
                }
                return response.parse();
            }
        } catch (IOException exception) {
            throw new PriceParsingException("Unable to load the product page", exception);
        }
        throw new PriceParsingException("Too many redirects while loading the product page");
    }

    private URI validateUrl(String productUrl) {
        try {
            URI uri = new URI(productUrl);
            if (!"http".equalsIgnoreCase(uri.getScheme()) && !"https".equalsIgnoreCase(uri.getScheme())) {
                throw new PriceParsingException("Only HTTP and HTTPS product URLs are allowed");
            }
            if (uri.getHost() == null || uri.getHost().isBlank()) {
                throw new PriceParsingException("Product URL must include a host");
            }
            if (isLocalAddress(uri.getHost())) {
                throw new PriceParsingException("Local product URLs are not allowed");
            }
            return uri;
        } catch (URISyntaxException exception) {
            throw new PriceParsingException("Product URL is invalid", exception);
        }
    }

    private boolean isLocalAddress(String host) {
        String normalizedHost = host.toLowerCase(Locale.ROOT);
        if (normalizedHost.equals("localhost") || normalizedHost.endsWith(".localhost")) {
            return true;
        }
        try {
            for (InetAddress address : InetAddress.getAllByName(host)) {
                if (address.isAnyLocalAddress()
                        || address.isLoopbackAddress()
                        || address.isLinkLocalAddress()
                        || address.isSiteLocalAddress()
                        || isUniqueLocalIpv6(address)) {
                    return true;
                }
            }
            return false;
        } catch (IOException exception) {
            throw new PriceParsingException("Product URL host cannot be resolved", exception);
        }
    }

    private boolean isUniqueLocalIpv6(InetAddress address) {
        byte[] bytes = address.getAddress();
        return bytes.length == 16 && (bytes[0] & 0xfe) == 0xfc;
    }

    private Optional<BigDecimal> extractMetaPrice(Document document) {
        return extractFromElements(document.select(
                "meta[property=product\\:price\\:amount], meta[name=price], meta[name=product\\:price\\:amount]"
        ));
    }

    private Optional<BigDecimal> extractItempropPrice(Document document) {
        return extractFromElements(document.select("[itemprop=price]"));
    }

    private Optional<BigDecimal> extractJsonLdPrice(Document document) {
        for (Element script : document.select("script[type=application/ld+json]")) {
            try {
                Optional<BigDecimal> price = findJsonLdPrice(objectMapper.readTree(script.data()));
                if (price.isPresent()) {
                    return price;
                }
            } catch (IOException ignored) {
                // Invalid JSON-LD in one script must not prevent other extraction strategies.
            }
        }
        return Optional.empty();
    }

    private Optional<BigDecimal> findJsonLdPrice(JsonNode node) {
        if (node == null || node.isNull()) {
            return Optional.empty();
        }
        if (node.isArray()) {
            for (JsonNode item : node) {
                Optional<BigDecimal> price = findJsonLdPrice(item);
                if (price.isPresent()) {
                    return price;
                }
            }
            return Optional.empty();
        }
        if (!node.isObject()) {
            return Optional.empty();
        }

        if (hasType(node, "Offer") || hasType(node, "AggregateOffer")) {
            Optional<BigDecimal> price = parseJsonPrice(node.get("price"));
            if (price.isPresent()) {
                return price;
            }
            price = parseJsonPrice(node.get("lowPrice"));
            if (price.isPresent()) {
                return price;
            }
        }

        Optional<BigDecimal> offersPrice = findJsonLdPrice(node.get("offers"));
        if (offersPrice.isPresent()) {
            return offersPrice;
        }
        return findJsonLdPrice(node.get("@graph"));
    }

    private boolean hasType(JsonNode node, String expectedType) {
        JsonNode type = node.get("@type");
        if (type == null) {
            return false;
        }
        if (type.isTextual()) {
            return expectedType.equalsIgnoreCase(type.asText());
        }
        return type.isArray() && stream(type).stream()
                .anyMatch(value -> expectedType.equalsIgnoreCase(value.asText()));
    }

    private List<JsonNode> stream(JsonNode array) {
        java.util.ArrayList<JsonNode> values = new java.util.ArrayList<>();
        array.forEach(values::add);
        return values;
    }

    private Optional<BigDecimal> parseJsonPrice(JsonNode price) {
        if (price == null || price.isNull()) {
            return Optional.empty();
        }
        return tryParsePrice(price.asText());
    }

    private Optional<BigDecimal> extractAttributePrice(Document document) {
        return extractFromElements(document.select("[data-price], [data-current-price], [data-product-price]"));
    }

    private Optional<BigDecimal> extractFallbackPrice(Document document) {
        return extractFromElements(document.select("[class*=price], [id*=price]"));
    }

    private Optional<BigDecimal> extractFromElements(List<Element> elements) {
        for (Element element : elements) {
            for (String value : List.of(
                    element.attr("content"),
                    element.attr("value"),
                    element.attr("data-price"),
                    element.attr("data-current-price"),
                    element.attr("data-product-price"),
                    element.text()
            )) {
                Optional<BigDecimal> price = tryParsePrice(value);
                if (price.isPresent()) {
                    return price;
                }
            }
        }
        return Optional.empty();
    }

    private Optional<BigDecimal> tryParsePrice(String rawPrice) {
        if (rawPrice == null || rawPrice.isBlank()) {
            return Optional.empty();
        }
        try {
            String numeric = rawPrice.replace('\u00a0', ' ').replace('\u202f', ' ')
                    .replaceAll("[^0-9,\\.\\s]", "")
                    .replaceAll("\\s+", "");
            if (numeric.isBlank()) {
                return Optional.empty();
            }
            BigDecimal price = new BigDecimal(normalizeSeparators(numeric));
            return price.signum() > 0 ? Optional.of(price) : Optional.empty();
        } catch (NumberFormatException exception) {
            return Optional.empty();
        }
    }

    private String normalizeSeparators(String value) {
        int lastComma = value.lastIndexOf(',');
        int lastDot = value.lastIndexOf('.');
        if (lastComma >= 0 && lastDot >= 0) {
            return lastComma > lastDot
                    ? value.replace(".", "").replace(',', '.')
                    : value.replace(",", "");
        }
        if (lastComma >= 0) {
            return normalizeSingleSeparator(value, ',');
        }
        if (lastDot >= 0) {
            return normalizeSingleSeparator(value, '.');
        }
        return value;
    }

    private String normalizeSingleSeparator(String value, char separator) {
        int lastSeparator = value.lastIndexOf(separator);
        int fractionLength = value.length() - lastSeparator - 1;
        if (fractionLength >= 1 && fractionLength <= 2) {
            String integerPart = value.substring(0, lastSeparator).replace(String.valueOf(separator), "");
            return integerPart + "." + value.substring(lastSeparator + 1);
        }
        return value.replace(String.valueOf(separator), "");
    }

    private boolean isRedirect(int statusCode) {
        return statusCode == 301 || statusCode == 302 || statusCode == 303 || statusCode == 307 || statusCode == 308;
    }
}
