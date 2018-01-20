package com.after_sunrise.cryptocurrency.cryptotrader.service.bitbank;

import cc.bitbank.entity.Assets;
import lombok.ToString;

import java.math.BigDecimal;

/**
 * @author takanori.takase
 * @version 0.0.1
 */
@ToString
public class BitbankAsset {

    private final Assets.Asset delegate;

    public BitbankAsset(Assets.Asset delegate) {
        this.delegate = delegate;
    }

    public String getId() {
        return delegate == null ? null : delegate.asset;
    }

    public BigDecimal getBalance() {
        return delegate == null ? null : delegate.onhandAmount;
    }

}
