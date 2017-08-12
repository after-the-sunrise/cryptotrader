package com.after_sunrise.cryptocurrency.cryptotrader.framework.impl;

import com.after_sunrise.cryptocurrency.cryptotrader.framework.Instruction.CancelInstruction;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Instruction.CreateInstruction;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.OrderManager;
import lombok.extern.slf4j.Slf4j;

/**
 * @author takanori.takase
 * @version 0.0.1
 */
@Slf4j
public class OrderManagerImpl implements OrderManager {

    @Override
    public Void create(CreateInstruction instruction) {

        log.debug("Order instruction : {}", instruction);

        return null;

    }

    @Override
    public Void cancel(CancelInstruction instruction) {

        log.debug("Order instruction : {}", instruction);

        return null;

    }

}
