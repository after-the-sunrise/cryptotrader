package com.after_sunrise.cryptocurrency.cryptotrader.service.bitflyer;

import com.after_sunrise.cryptocurrency.bitflyer4j.entity.Board;
import lombok.Getter;
import lombok.ToString;

import java.time.Instant;

/**
 * @author takanori.takase
 * @version 0.0.1
 */
@Getter
@ToString
public class BitflyerBoard {

    private final Instant timestamp;

    private final Board delegate;

    public BitflyerBoard(Instant timestamp, Board delegate) {
        this.timestamp = timestamp;
        this.delegate = delegate;
    }

}
