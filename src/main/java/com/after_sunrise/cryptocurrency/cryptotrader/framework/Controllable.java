package com.after_sunrise.cryptocurrency.cryptotrader.framework;

import javax.management.MXBean;

/**
 * @author takanori.takase
 * @version 0.0.1
 */
@MXBean
public interface Controllable extends AutoCloseable {

    void trigger();

}
