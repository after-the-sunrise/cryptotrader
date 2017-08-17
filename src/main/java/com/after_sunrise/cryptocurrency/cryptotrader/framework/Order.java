package com.after_sunrise.cryptocurrency.cryptotrader.framework;

import java.math.BigDecimal;

/**
 * @author takanori.takase
 * @version 0.0.1
 */
public interface Order {

    String getId();

    String getProduct();

    Boolean getActive();

    BigDecimal getOrderPrice();

    BigDecimal getOrderQuantity();

    BigDecimal getFilledQuantity();

    BigDecimal getRemainingQuantity();

}
