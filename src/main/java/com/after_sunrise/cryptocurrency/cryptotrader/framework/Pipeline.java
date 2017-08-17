package com.after_sunrise.cryptocurrency.cryptotrader.framework;

import java.time.Instant;

/**
 * @author takanori.takase
 * @version 0.0.1
 */
public interface Pipeline {

    void process(Instant time, String site, String instrument);

}
