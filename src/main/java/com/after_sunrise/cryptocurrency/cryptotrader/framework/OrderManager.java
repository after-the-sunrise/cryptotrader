package com.after_sunrise.cryptocurrency.cryptotrader.framework;

import com.after_sunrise.cryptocurrency.cryptotrader.framework.Instruction.CancelInstruction;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Instruction.CreateInstruction;

/**
 * @author takanori.takase
 * @version 0.0.1
 */
public interface OrderManager {

    Void create(CreateInstruction instruction);

    Void cancel(CancelInstruction instruction);

}
