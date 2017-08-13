package com.after_sunrise.cryptocurrency.cryptotrader.framework;

import com.after_sunrise.cryptocurrency.cryptotrader.framework.PortfolioAdviser.Advice;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Trader.Request;

import java.util.List;
import java.util.function.Supplier;

/**
 * @author takanori.takase
 * @version 0.0.1
 */
public interface OrderInstructor extends Supplier<String> {

    List<Instruction> instruct(Context context, Request request, Advice advice);

}
