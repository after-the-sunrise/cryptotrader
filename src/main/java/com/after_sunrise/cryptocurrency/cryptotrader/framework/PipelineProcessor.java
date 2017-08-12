package com.after_sunrise.cryptocurrency.cryptotrader.framework;

import com.after_sunrise.cryptocurrency.cryptotrader.framework.Trader.Request;

import java.util.function.Consumer;

/**
 * @author takanori.takase
 * @version 0.0.1
 */
public interface PipelineProcessor extends Consumer<Request> {
}
