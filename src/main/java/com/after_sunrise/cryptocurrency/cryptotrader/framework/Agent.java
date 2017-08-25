package com.after_sunrise.cryptocurrency.cryptotrader.framework;

import java.util.List;
import java.util.Map;

/**
 * @author takanori.takase
 * @version 0.0.1
 */
public interface Agent extends Service {

    Map<Instruction, String> manage(Context context, Request request, List<Instruction> values);

    Map<Instruction, Boolean> reconcile(Context context, Request request, Map<Instruction, String> instructions);

}
