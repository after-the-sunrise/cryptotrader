package com.after_sunrise.cryptocurrency.cryptotrader.framework;

import com.after_sunrise.cryptocurrency.cryptotrader.core.Composite;
import org.testng.annotations.Test;

import java.lang.reflect.Field;
import java.time.Duration;
import java.time.Instant;
import java.util.Random;

import static java.math.BigDecimal.valueOf;
import static java.util.Collections.singletonList;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertSame;

/**
 * @author takanori.takase
 * @version 0.0.1
 */
public class RequestTest {

    @Test
    public void testBuilder() throws Exception {

        Random random = new Random();

        Request.RequestBuilder b = Request.builder();
        b.site(String.valueOf(random.nextInt()));
        b.instrument(String.valueOf(random.nextInt()));
        b.currentTime(Instant.ofEpochSecond(random.nextInt()));
        b.targetTime(Instant.ofEpochSecond(random.nextInt()));
        b.tradingSpread(valueOf(random.nextInt()));
        b.tradingSpreadAsk(valueOf(random.nextInt()));
        b.tradingSpreadBid(valueOf(random.nextInt()));
        b.tradingSigma(valueOf(random.nextInt()));
        b.tradingSamples(random.nextInt());
        b.tradingExposure(valueOf(random.nextInt()));
        b.tradingThreshold(valueOf(random.nextInt()));
        b.tradingMaximum(valueOf(random.nextInt()));
        b.tradingMinimum(valueOf(random.nextInt()));
        b.tradingResistance(valueOf(random.nextInt()));
        b.tradingAversion(valueOf(random.nextInt()));
        b.tradingInstruction(String.valueOf(random.nextInt()));
        b.tradingSplit(random.nextInt());
        b.tradingDuration(Duration.ofMillis(random.nextInt()));
        b.fundingOffset(valueOf(random.nextInt()));
        b.fundingMultiplierProducts(singletonList(new Composite("m", "p")));
        b.fundingPositiveMultiplier(valueOf(random.nextInt()));
        b.fundingNegativeMultiplier(valueOf(random.nextInt()));
        b.fundingPositiveThreshold(valueOf(random.nextInt()));
        b.fundingNegativeThreshold(valueOf(random.nextInt()));
        b.deviationProducts(singletonList(new Composite("d", "p")));
        b.aversionProducts(singletonList(new Composite("a", "p")));
        b.hedgeProducts(singletonList(new Composite("h", "p")));
        b.estimatorComposites(singletonList(new Composite("e", "p")));
        b.estimationAversion(valueOf(random.nextInt()));

        Request target = b.build();

        Request copy = Request.build(target).build();

        for (Field f : Request.class.getDeclaredFields()) {

            f.setAccessible(true);

            Object o1 = f.get(target);

            Object o2 = f.get(copy);

            assertNotNull(o1, f.getName());

            assertNotNull(o2, f.getName());

            assertSame(o1, o2, f.getName());

        }

    }

}
