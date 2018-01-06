# cryptotrader
[![Build Status][travis-icon]][travis-page] [![Coverage Status][coverall-icon]][coverall-page]

## Overview

**Cryptotrader** is a cryptocurrency trading bot, with a plug-in mechanism to load custom trading strategies.
* Standalone Java application.
* Provides a thin framework for plugging-in custom trading strategies.
* Few trading strategies are available out-of-the-box. (Uses the same plugin mechanism.)
* Multi-exchange, multi-product trading strategies.
* HTTP REST interface for manual interventions.

Cryptotrader is...
* **NOT** fully automated. Even though the orders are handled automatically following the trading strategies, configurations such as which exchanges/products/strategies/parameters to use/trade, how much fund to utilize, and at which timing to deposit/withdraw are all discretional.
* **NOT** a low-latency nor HFT trading application. The framework is based on a periodic timer, not event-driven.
* **NOT** for non-programmers. No tutorials, user guides, nor support are available. Read the code to figure out how things work. Fork if things needs to be tweaked. Contributions are always welcome, but not required.
* **NOT** stable nor backward compatible. The project intended for the author's quick & dirty playground implementations, therefore destructive changes could be made any time.

So why is this project made public?
* Not profitable by itself. Tuning is the key to the alpha, which is kept private.
* Not all the strategies are exposed, some are only plugged-in at runtime.
* More people, more attention, more liquidity benefits the author.
* For fun. Something to chat over a cup of coffee.


## Getting Started

### Prerequisites
* Linux
* JDK 8 or later
* [Gradle](https://gradle.org/)

### Installation & Launching
Follow the below procedures to launch the application. The application will run in dry-mode (read-only) by default.
1. Download (checkout) the project files from GitHub.
2. From the command line, build the module with `gradle clean war`.
3. Change current working directory to `etc/home/cryptotrader/cryptotrader/etc/winstone/`.
4. Create a symbolic link to the generated war file `ln -s ../../build/libs/cryptotrader-0.0.1-SNAPSHOT.war cryptotrader-LATEST.war`.
5. Launch the application with `sh winstone-start.sh`.
6. Check the application log file `logs/cryptotrader-app.log` to monitor the application state.

### Configuration
To enable trading of the out-of-the-box trading strategies, create and configure the `${HOME}/.cryptotrader` file. 
Template and parameter descriptions can be found under `src/main/resources/cryptotrader-default.properties`.

### Plugin Jars
To load custom trading strategies, prepare a jar file following the [SPI][ref-spi] specification, 
place the jar file under `libs/` directory, and recreate the war file by `gradle clean war`.
Don't forget to adjust the configuration file to configure the loaded strategy. 


## Framework Mechanics

The framework is based on a periodic timer, to invoke the set of pipelined codes which are provided by the plugged-in trading strategies. 
When the application is launched, the pipeline will be invoked periodically based on the preconfigured interval, until the application is terminated. 

The pipeline consists of the following [SPI][ref-spi] interfaces:
 1. Context : Adapter to capsulize each exchange's API calls.
 2. Estimator : Estimate (predict) market price to use in the following 
 3. Adviser : Calculate the ideal state of what the current position should ideally be.
 4. Instructor : Figure out the differences between the as-is position and to-be position, and generate order instructions.
 5. Agent : Execute the given order instructions, and reconcile the results to confirm all the orders were processed by the exchange.


[travis-page]:https://travis-ci.org/after-the-sunrise/cryptotrader
[travis-icon]:https://travis-ci.org/after-the-sunrise/cryptotrader.svg?branch=master
[coverall-page]:https://coveralls.io/github/after-the-sunrise/cryptotrader?branch=master
[coverall-icon]:https://coveralls.io/repos/github/after-the-sunrise/cryptotrader/badge.svg?branch=master
[ref-spi]:https://docs.oracle.com/javase/tutorial/ext/basics/spi.html
