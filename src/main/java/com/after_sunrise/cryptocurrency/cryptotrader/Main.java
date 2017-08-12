package com.after_sunrise.cryptocurrency.cryptotrader;

import lombok.extern.slf4j.Slf4j;

/**
 * @author takanori.takase
 * @version 0.0.1
 */
@Slf4j
public class Main implements Cryptotrader {

    public static final String APPLICATION = "cryptotrader.main";

    public static void main(String... args) throws Exception {

        String appName = System.getProperty(APPLICATION, Main.class.getName());

        Cryptotrader app = (Cryptotrader) Class.forName(appName).newInstance();

        log.info("Starting application : {}", appName);

        try {

            app.execute();

        } finally {

            app.shutdown();

        }

        log.info("Stopped application.");

    }


    @Override
    public void execute() throws Exception {

        log.debug("Execute.");

    }

    @Override
    public void shutdown() {

        log.debug("Shutdown.");

    }

}
