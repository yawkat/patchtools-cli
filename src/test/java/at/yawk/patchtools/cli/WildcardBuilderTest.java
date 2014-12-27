package at.yawk.patchtools.cli;

import dk.brics.automaton.Automaton;
import dk.brics.automaton.RunAutomaton;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class WildcardBuilderTest {
    @Test
    public void testGlob() {
        RunAutomaton runAutomaton = WildcardBuilder.create(".")
                .appendWildcard("*").buildRun();

        assertTrue(runAutomaton.run("test"));
        assertTrue(runAutomaton.run("test.4.3.12"));
        assertTrue(runAutomaton.run(""));
    }

    @Test
    public void testGlobPath() {
        Automaton runAutomaton = WildcardBuilder.create(".")
                .appendWildcard("*", "*", false).build();

        assertTrue(runAutomaton.run("test"));
        assertFalse(runAutomaton.run("test.4.3.12"));
        assertTrue(runAutomaton.run(""));
    }

    @Test
    public void testMultiAndSingle() {
        RunAutomaton runAutomaton = WildcardBuilder.create(".")
                .appendWildcard("abc.*.def.**.ghi", "*", false).buildRun();

        assertTrue(runAutomaton.run("abc.abc.def.ghi.jkl.ghi"));
        assertTrue(runAutomaton.run("abc.abc.def.ghi.ghi"));
        assertTrue(runAutomaton.run("abc..def..ghi"));
        assertFalse(runAutomaton.run("abc.abc.jkl.def.ghi.jkl.ghi"));
    }
}