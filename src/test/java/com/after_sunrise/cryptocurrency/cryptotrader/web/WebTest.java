package com.after_sunrise.cryptocurrency.cryptotrader.web;

import com.after_sunrise.cryptocurrency.cryptotrader.Cryptotrader;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.webapp.WebAppContext;
import org.mockito.Mockito;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.CountDownLatch;

import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * @author takanori.takase
 * @version 0.0.1
 */
public class WebTest {

    private Web target;

    @BeforeMethod
    public void setUp() {
        target = new Web();
    }

    @Test
    public void test() throws Exception {

        Cryptotrader trader = Mockito.mock(Cryptotrader.class);
        CountDownLatch latch1 = new CountDownLatch(1);
        CountDownLatch latch2 = new CountDownLatch(1);
        Mockito.doAnswer(i -> {
            latch1.countDown();
            return null;
        }).when(trader).execute();
        Mockito.doAnswer(i -> {
            latch2.countDown();
            return null;
        }).when(trader).shutdown();

        Injector injector = Guice.createInjector(new AbstractModule() {
            @Override
            protected void configure() {
                bind(Cryptotrader.class).toInstance(trader);
            }
        });

        target.withInjector(injector);

        latch1.await(3, SECONDS);
        latch2.await(3, SECONDS);

    }

    public static void main(String[] args) throws Exception {

        Path root = Paths.get("build", "libs");

        Path path = Files.find(root, 1, (p, attributes) -> {

            String name = p.getFileName().toString();

            return name.matches(".*\\.war");

        }).findFirst().get();

        WebAppContext context = new WebAppContext();
        context.setContextPath("/");
        context.setWar(path.toAbsolutePath().toString());

        Server server = new Server(8080);
        server.setHandler(context);

        server.start();
        server.join();

    }

}
