package com.after_sunrise.cryptocurrency.cryptotrader.framework.impl;

import com.after_sunrise.cryptocurrency.cryptotrader.core.ServiceFactory;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Adviser.Advice;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Context;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Instruction;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Instructor;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Trader.Request;
import com.google.inject.Inject;
import com.google.inject.Injector;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static com.after_sunrise.cryptocurrency.cryptotrader.framework.Trader.Request.ALL;
import static java.util.Optional.ofNullable;

/**
 * @author takanori.takase
 * @version 0.0.1
 */
@Slf4j
public class InstructorImpl implements Instructor {

    private static final List<Instruction> EMPTY = Collections.emptyList();

    private final Map<String, Instructor> instructors;

    @Inject
    public InstructorImpl(Injector injector) {
        this.instructors = injector.getInstance(ServiceFactory.class).loadMap(Instructor.class);
    }

    @Override
    public String get() {
        return ALL;
    }

    @Override
    public List<Instruction> instruct(Context context, Request request, Advice advice) {

        if (!Request.isValid(request)) {

            log.trace("Invalid request : {}", request);

            return Collections.emptyList();

        }

        Instructor instructor = instructors.get(request.getSite());

        if (instructor == null) {

            log.debug("Instructor not found for site : {}", request.getSite());

            return Collections.emptyList();

        }

        List<Instruction> instructions = instructor.instruct(context, request, advice);

        List<Instruction> values = ofNullable(instructions).orElse(EMPTY);

        log.info("Instruction : [{} {}] {}", request.getSite(), request.getInstrument(), values.size());

        values.forEach(i -> log.debug("{}", i));

        return values;

    }

}
