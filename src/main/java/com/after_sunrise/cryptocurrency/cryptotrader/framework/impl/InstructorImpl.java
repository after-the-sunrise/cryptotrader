package com.after_sunrise.cryptocurrency.cryptotrader.framework.impl;

import com.after_sunrise.cryptocurrency.cryptotrader.core.ServiceFactory;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Adviser.Advice;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Context;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Instruction;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Instructor;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Request;
import com.google.inject.Inject;
import com.google.inject.Injector;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * @author takanori.takase
 * @version 0.0.1
 */
public class InstructorImpl extends AbstractService implements Instructor {

    private static final List<Instruction> EMPTY = Collections.emptyList();

    private final Map<String, Instructor> instructors;

    @Inject
    public InstructorImpl(Injector injector) {
        this.instructors = injector.getInstance(ServiceFactory.class).loadMap(Instructor.class);
    }

    @Override
    public String get() {
        return WILDCARD;
    }

    @Override
    public List<Instruction> instruct(Context context, Request request, Advice advice) {

        Instructor instructor = instructors.get(request.getSite());

        if (instructor == null) {

            log.debug("Instructor not found for site : {}", request.getSite());

            return Collections.emptyList();

        }

        List<Instruction> values = trimToEmpty(instructor.instruct(context, request, advice));

        log.info("Instruction : [{}.{}] {}", request.getSite(), request.getInstrument(), values.size());

        values.forEach(i -> log.debug("{}", i));

        return values;

    }

}
