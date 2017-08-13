package com.after_sunrise.cryptocurrency.cryptotrader.framework;

import com.after_sunrise.cryptocurrency.cryptotrader.framework.Instruction.CancelInstruction;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Instruction.CreateInstruction;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Trader.Request;

import java.util.function.Supplier;

/**
 * @author takanori.takase
 * @version 0.0.1
 */
public interface OrderManager extends Supplier<String> {

    Boolean create(Request request, CreateInstruction instruction);

    Boolean cancel(Request request, CancelInstruction instruction);

}
