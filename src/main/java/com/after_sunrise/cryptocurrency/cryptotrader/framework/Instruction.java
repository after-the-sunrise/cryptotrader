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


    interface Visitor<T> {

        T visit(CreateInstruction instruction);

        T visit(CancelInstruction instruction);

    }

    @Getter
    @Builder
    @ToString
    @AllArgsConstructor(access = PRIVATE)
    class CreateInstruction implements Instruction {

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
    class CancelInstruction implements Instruction {

        private final String id;

        @Override
        public <T> T accept(Visitor<T> visitor) {
            return visitor.visit(this);
        }

    }

    <T> T accept(Visitor<T> visitor);

}
