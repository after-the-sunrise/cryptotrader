package com.after_sunrise.cryptocurrency.cryptotrader.framework;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicLong;

import static lombok.AccessLevel.PRIVATE;

/**
 * @author takanori.takase
 * @version 0.0.1
 */
public interface Instruction {


    interface Visitor<T> {

        T visit(CreateInstruction instruction);

        T visit(CancelInstruction instruction);

    }

    abstract class BaseInstruction implements Instruction {

        private static final AtomicLong SEQUENCE = new AtomicLong();

        protected static String generateUid() {
            return String.valueOf(SEQUENCE.incrementAndGet());
        }

        public abstract String getUid();

    }

    @Getter
    @Builder
    @ToString
    @AllArgsConstructor(access = PRIVATE)
    class CreateInstruction extends BaseInstruction {

        private final String uid = generateUid();

        private final BigDecimal price;

        private final BigDecimal size;

        @Override
        public <T> T accept(Visitor<T> visitor) {
            return visitor.visit(this);
        }

    }

    @Getter
    @Builder
    @ToString
    @AllArgsConstructor(access = PRIVATE)
    class CancelInstruction extends BaseInstruction {

        private final String uid = generateUid();

        private final String id;

        @Override
        public <T> T accept(Visitor<T> visitor) {
            return visitor.visit(this);
        }

    }

    <T> T accept(Visitor<T> visitor);

}
