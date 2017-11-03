package com.after_sunrise.cryptocurrency.cryptotrader.service.oanda;

import com.after_sunrise.cryptocurrency.cryptotrader.service.template.TemplateContext;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.reflect.TypeToken;
import com.google.gson.*;
import org.apache.commons.lang3.StringUtils;

import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

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

    private static final String AUTH_PROP = OandaContext.class.getName() + ".api.secret";

    private static final String AUTH_KEY = "Authorization";

    private static final String AUTH_VAL = "Bearer ";

    private static final String HALTED = "halted";

    private static final Type TYPE_TICKER = new TypeToken<Map<String, List<OandaTick>>>() {
    }.getType();

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
    Optional<OandaTick> queryTick(Key key) {

        OandaTick tick = findCached(OandaTick.class, key, () -> {

            String token = getStringProperty(AUTH_PROP, null);

            if (StringUtils.isEmpty(token)) {
                return null;
            }

            String product = URLEncoder.encode(key.getInstrument(), UTF_8.name());

            Map<String, String> parameters = singletonMap(AUTH_KEY, AUTH_VAL + token);

            String data = request(RequestType.GET, URL_TICKER + product, parameters, null);

            if (StringUtils.isEmpty(data)) {
                return null;
            }

            Map<String, List<OandaTick>> ticks = gson.fromJson(data, TYPE_TICKER);

            return ticks.getOrDefault(KEY_TICKER, emptyList()).stream()
                    .filter(Objects::nonNull)
                    .filter(t -> t.getInstrument() != null)
                    .filter(t -> t.getInstrument().equals(key.getInstrument()))
                    .filter(t -> !HALTED.equals(t.getStatus()))
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
