package com.after_sunrise.cryptocurrency.cryptotrader.framework;

import com.after_sunrise.cryptocurrency.cryptotrader.framework.Trader.Request;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * @author takanori.takase
 * @version 0.0.1
 */
public interface OrderManager extends Supplier<String> {

    Map<Instruction, String> manage(Context context, Request request, List<Instruction> values);

    Boolean reconcile(Context context, Request request, Map<Instruction, String> instructions);

}
