package com.after_sunrise.cryptocurrency.cryptotrader.framework.impl;

import com.after_sunrise.cryptocurrency.cryptotrader.framework.Instruction;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.OrderInstructor;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.PortfolioAdviser.Advice;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Trader.Request;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.List;

/**
 * @author takanori.takase
 * @version 0.0.1
 */
@Slf4j
public class OrderInstructorImpl implements OrderInstructor {

    @Override
    public List<Instruction> instruct(Context context, Request request, Advice advice) {
        return Collections.emptyList();
    }

}
