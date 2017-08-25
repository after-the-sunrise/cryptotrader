package com.after_sunrise.cryptocurrency.cryptotrader.framework;

import com.after_sunrise.cryptocurrency.cryptotrader.framework.Adviser.Advice;

import java.util.List;

/**
 * @author takanori.takase
 * @version 0.0.1
 */
public interface Instructor extends Service {

    List<Instruction> instruct(Context context, Request request, Advice advice);

}
