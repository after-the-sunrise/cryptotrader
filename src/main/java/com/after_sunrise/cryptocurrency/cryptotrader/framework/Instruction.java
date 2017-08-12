package com.after_sunrise.cryptocurrency.cryptotrader.framework;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.math.BigDecimal;

import static lombok.AccessLevel.PRIVATE;

/**
 * @author takanori.takase
 * @version 0.0.1
 */
public interface Instruction {

    interface Visitor<R, P> {

        R visit(P parameter, CreateInstruction instruction);

        R visit(P parameter, CancelInstruction instruction);

    }

    <R, P> R accept(Visitor<R, P> visitor, P parameter);

    @Getter
    @Builder
    @ToString
    @AllArgsConstructor(access = PRIVATE)
    class CreateInstruction implements Instruction {

        private final BigDecimal price;

        private final BigDecimal size;

        @Override
        public <R, P> R accept(Visitor<R, P> visitor, P parameter) {
            return visitor.visit(parameter, this);
        }

    }

    @Getter
    @Builder
    @ToString
    @AllArgsConstructor(access = PRIVATE)
    class CancelInstruction implements Instruction {

        private final String id;

        @Override
        public <R, P> R accept(Visitor<R, P> visitor, P parameter) {
            return visitor.visit(parameter, this);
        }

    }

}
