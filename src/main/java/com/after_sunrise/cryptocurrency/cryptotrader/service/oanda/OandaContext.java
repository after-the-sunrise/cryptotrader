package com.after_sunrise.cryptocurrency.cryptotrader.service.oanda;

import com.after_sunrise.cryptocurrency.cryptotrader.service.template.TemplateContext;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.reflect.TypeToken;
import com.google.gson.*;
import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.builder.fluent.Configurations;
import org.apache.commons.lang3.StringUtils;

import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static java.lang.System.getProperty;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonMap;

/**
 * @author takanori.takase
 * @version 0.0.1
 */
public class OandaContext extends TemplateContext implements OandaService {

    static final String URL_TICKER = "https://api-fxtrade.oanda.com/v1/prices?instruments=";

    private static final String KEY_TICKER = "prices";

    private static final String AUTH_KEY = "Authorization";

    private static final String AUTH_VAL = "Bearer ";

    private static final String AUTH_PATH = Paths.get(getProperty("user.home"), ".oandajp").toAbsolutePath().toString();

    private static final String AUTH_PROP = "OJP_TOKEN";

    private static final Type TYPE_TICKER = new TypeToken<Map<String, List<OandaTick>>>() {
    }.getType();

    private static final Duration EXPIRY = Duration.ofMinutes(1);

    private final Gson gson;

    public OandaContext() {

        super(ID);

        GsonBuilder builder = new GsonBuilder();

        builder.registerTypeAdapter(Instant.class, new JsonDeserializer<Instant>() {

            private final DateTimeFormatter FMT = DateTimeFormatter.ISO_INSTANT.withZone(ZoneId.of("GMT"));

            @Override
            public Instant deserialize(JsonElement j, Type t, JsonDeserializationContext c) throws JsonParseException {

                ZonedDateTime timestamp = ZonedDateTime.parse(j.getAsString(), FMT);

                return timestamp.toInstant();

            }

        });

        gson = builder.create();

    }

    @VisibleForTesting
    String getToken(String path, String key) {

        try {

            Configuration conf = new Configurations().properties(path);

            return conf.getString(key);

        } catch (Exception e) {

            return null;

        }

    }

    @VisibleForTesting
    Optional<OandaTick> queryTick(Key key) {

        OandaTick tick = findCached(OandaTick.class, key, () -> {

            String token = getToken(AUTH_PATH, AUTH_PROP);

            if (StringUtils.isEmpty(token)) {
                return null;
            }

            String product = URLEncoder.encode(key.getInstrument(), UTF_8.name());

            String data = query(URL_TICKER + product, singletonMap(AUTH_KEY, AUTH_VAL + token));

            if (StringUtils.isEmpty(data)) {
                return null;
            }

            Map<String, List<OandaTick>> ticks = gson.fromJson(data, TYPE_TICKER);

            return ticks.getOrDefault(KEY_TICKER, emptyList()).stream()
                    .filter(Objects::nonNull)
                    .filter(t -> t.getInstrument() != null)
                    .filter(t -> t.getInstrument().equals(key.getInstrument()))
                    .filter(t -> t.getTimestamp() != null)
                    .filter(t -> t.getTimestamp().isAfter(key.getTimestamp().minus(EXPIRY)))
                    .findFirst().orElse(null);

        });

        return Optional.ofNullable(tick);

    }

    @Override
    public BigDecimal getBestAskPrice(Key key) {
        return queryTick(key).map(OandaTick::getAsk).orElse(null);
    }

    @Override
    public BigDecimal getBestBidPrice(Key key) {
        return queryTick(key).map(OandaTick::getBid).orElse(null);
    }

}
