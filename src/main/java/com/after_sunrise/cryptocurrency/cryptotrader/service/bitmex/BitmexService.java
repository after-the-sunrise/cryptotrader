package com.after_sunrise.cryptocurrency.cryptotrader.service.bitmex;

import com.after_sunrise.cryptocurrency.cryptotrader.framework.Service;
import org.apache.commons.lang3.StringUtils;

import java.util.stream.Stream;

/**
 * @author takanori.takase
 * @version 0.0.1
 */
public interface BitmexService extends Service {

    String ID = "bitmex";

    @Override
    default String get() {
        return ID;
    }

    enum SideType {

        BUY,

        SELL;

        private final String id = StringUtils.capitalize(name().toLowerCase());

        public String getId() {
            return id;
        }

        public static SideType find(String id) {
            return Stream.of(values()).filter(e -> e.getId().equals(id)).findAny().orElse(null);
        }

    }

}
