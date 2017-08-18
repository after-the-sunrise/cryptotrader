package com.after_sunrise.cryptocurrency.cryptotrader.framework;

import com.after_sunrise.cryptocurrency.cryptotrader.framework.Instruction.CancelInstruction;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Instruction.CreateInstruction;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Instruction.Visitor;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static java.math.BigDecimal.ONE;
import static java.math.BigDecimal.TEN;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;

/**
 * @author takanori.takase
 * @version 0.0.1
 */
public class InstructionTest {

    private Visitor<String> visitor;

    @BeforeMethod
    public void setUp() throws Exception {

        visitor = mock(Visitor.class);

        when(visitor.visit(any(CreateInstruction.class)))
                .thenAnswer(i -> i.getArgumentAt(0, CreateInstruction.class).getUid());

        when(visitor.visit(any(CancelInstruction.class)))
                .thenAnswer(i -> i.getArgumentAt(0, CancelInstruction.class).getUid());

    }

    @Test
    public void testCreate() throws Exception {

        CreateInstruction i = CreateInstruction.builder().price(TEN).size(ONE).build();

        assertEquals(i.getPrice(), TEN);

        assertEquals(i.getSize(), ONE);

        assertEquals(i.accept(visitor), i.getUid());

    }

    @Test
    public void testCancel() throws Exception {

        CancelInstruction i = CancelInstruction.builder().id("foo").build();

        assertEquals(i.getId(), "foo");

        assertEquals(i.accept(visitor), i.getUid());

    }

}
