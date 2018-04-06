# cryptotrader
[![Build Status][travis-icon]][travis-page] [![Coverage Status][coverall-icon]][coverall-page]

## Overview

**Cryptotrader** is a cryptocurrency trading bot, with a plug-in mechanism to load custom trading strategies.
* Standalone Java application.
* Provides a thin framework for plugging-in custom trading strategies.
* Multi-exchange, multi-product trading strategies.
* HTTP REST interface for manual interventions.

Cryptotrader is...
* **NOT** fully automated. Although the orders are autonomously managed by the trading strategies, configurations such as which exchanges/products/strategies/parameters to trade with, how much fund to utilize, and at which timing to deposit/withdraw funds are all discretional.
* **NOT** a low-latency nor HFT trading application. The framework is based on a periodic timer and not event-driven.
* **NOT** fault-tolerant nor highly-available. The single application process is the single-point-of-failure.
* **NOT** for non-programmers. No tutorials, user guides, nor support are available. Read the code to figure out how things work. Fork if things needs to be tweaked. Contributions are always welcome, but not required.
* **NOT** stable nor backward compatible. This project is for the author's quick & dirty playground implementations, therefore destructive changes could be made any time.

So why is this project made public? Well, mostly for fun, something to chat over a cup of :coffee:. 
And also, custom plugins and configurations are not made public, which are the secret sauce to generate alpha.


## Getting Started

### Prerequisites
* Linux machine with command line interface and direct internet access.
    * Modern linux operating system. (cf: CentOS 7)
    * 1GB or more free memory
    * 30GB or more free disk space
* JDK 8 or later
* Gradle 4.x or later

### Configuation
Prepare a custom configuration file to define the trading strategies. 
The [default configuration file](./src/main/resources/cryptotrader-default.properties) can be used as a starting point. 
Note that the default values are simply placeholders with no real meanings, 
therefore it's mandatory to configure each parameters properly for the application to actually start trading.
Once configured, place the custom configuration file at the path `$HOME/.cryptotrader`.

### Installation & Launching
Follow the below procedures to launch the application.
1. Clone the project files from GitHub.
2. Build the application module with `gradle clean war` command.
3. Change current working directory to `etc/home/cryptotrader/cryptotrader/etc/winstone/`.
4. Create a symbolic link to the generated file `ln -s ../../build/libs/cryptotrader-0.0.1-SNAPSHOT.war cryptotrader-LATEST.war`.
5. Launch the application with `sh winstone-start.sh`. The application will launch in background.
6. Check the application log file `logs/cryptotrader-app.log` to monitor the application state.
7. To stop the application, execute `sh winstone-stop.sh` and wait, or simply kill the Java process.

### Plugins
To load custom trading strategies, prepare a jar file following the [SPI][ref-spi] specification, and place the jar file under `libs/` directory.
The application needs to be re-built/restarted for the new plugin jars to be effective.
Also, don't forget to adjust the custom configuration file to adapt for the newly loaded strategies. 


## DISCLAIMER
Use at your own risk, following the [LICENSE](./LICENSE). 
Author has no plan to provide specific support for individual configurations nor provide trading instructions/advisories.


[travis-page]:https://travis-ci.org/after-the-sunrise/cryptotrader
[travis-icon]:https://travis-ci.org/after-the-sunrise/cryptotrader.svg?branch=master
[coverall-page]:https://coveralls.io/github/after-the-sunrise/cryptotrader?branch=master
[coverall-icon]:https://coveralls.io/repos/github/after-the-sunrise/cryptotrader/badge.svg?branch=master
[ref-spi]:https://docs.oracle.com/javase/tutorial/ext/basics/spi.html
