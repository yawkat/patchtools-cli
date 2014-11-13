package at.yawk.patchtools.cli;

import com.google.common.base.Splitter;
import dk.brics.automaton.Automaton;
import dk.brics.automaton.RunAutomaton;
import dk.brics.automaton.State;
import dk.brics.automaton.Transition;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

/**
 * @author yawkat
 */
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class WildcardBuilder {
    private Automaton automaton = Automaton.makeEmpty();

    private final String separator;

    public static WildcardBuilder create(String separator) {
        if (Objects.requireNonNull(separator).isEmpty()) {
            throw new IllegalArgumentException("Separator must not be empty");
        }

        return new WildcardBuilder(separator);
    }

    private void append(Automaton auto) {
        this.automaton = concatenate(this.automaton, auto);
    }

    private static Automaton concatenate(Automaton a, Automaton b) {
        // work around a bug in BasicOperations.concatenate that returns an empty automaton if one automaton is empty
        if (a.isEmpty()) { return b; }
        if (b.isEmpty()) { return a; }
        return a.concatenate(b);
    }

    public WildcardBuilder appendExact(String... exact) {
        for (String s : exact) {
            append(Automaton.makeString(s));
        }
        return this;
    }

    public WildcardBuilder appendSingleExpression() {
        State beforeSeparator = new State();
        State afterSeparator = new State();
        afterSeparator.setAccept(true);

        State next = afterSeparator;
        for (int i = separator.length() - 1; i >= 0; i--) {
            char c = separator.charAt(i);
            State entry = i == 0 ? beforeSeparator : new State();
            entry.addTransition(new Transition(c, next));
            if (c != '\0') { entry.addTransition(new Transition('\0', (char) (c - 1), beforeSeparator)); }
            if (c != '\uffff') { entry.addTransition(new Transition((char) (c - 1), '\uffff', beforeSeparator)); }
            next = entry;
        }

        Automaton automaton = new Automaton();
        automaton.setDeterministic(true);
        automaton.setInitialState(beforeSeparator);
        append(automaton);

        return this;
    }

    public WildcardBuilder appendMultiExpression() {
        append(Automaton.makeAnyString());

        return this;
    }

    public WildcardBuilder appendWildcard(String wildcard, String singleExpression, String multiExpression) {
        boolean firstMulti = true;
        for (String a : Splitter.on(multiExpression).split(wildcard)) {
            if (!firstMulti) { appendMultiExpression(); }
            firstMulti = false;

            boolean firstSingle = true;
            for (String b : Splitter.on(singleExpression).split(a)) {
                if (!firstSingle) { appendSingleExpression(); }
                firstSingle = false;

                appendExact(b);
            }
        }

        return this;
    }

    public WildcardBuilder appendWildcard(String wildcard, String expression, boolean multiDefault) {
        return appendWildcard(wildcard, expression, multiDefault ? expression : expression + expression);
    }

    public WildcardBuilder appendWildcard(String wildcard) {
        return appendWildcard(wildcard, "*", true);
    }

    public Automaton build() {
        automaton.minimize();
        return automaton.clone();
    }

    public RunAutomaton buildRun() {
        automaton.minimize();
        return new RunAutomaton(automaton);
    }
}
