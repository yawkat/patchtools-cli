package at.yawk.patchtools.cli;

import dk.brics.automaton.AutomatonMatcher;
import dk.brics.automaton.RunAutomaton;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;

/**
 * @author yawkat
 */
public interface ClassMatcher {
    public static final ClassMatcher ACCEPT = name -> Optional.of(true);

    Optional<Boolean> include(String className);

    @Slf4j
    public static final class Include implements ClassMatcher {
        private final RunAutomaton automaton;

        public Include(String pattern) {
            automaton = WildcardBuilder.create(".").appendWildcard(pattern).buildRun();
        }

        @Override
        public Optional<Boolean> include(String className) {
            AutomatonMatcher matcher = automaton.newMatcher(className);
            boolean matched = matcher.find() && matcher.start() == 0 && matcher.end() == className.length();
            return matched ? Optional.of(true) : Optional.<Boolean>empty();
        }
    }

    @Slf4j
    public static final class Exclude implements ClassMatcher {
        private final RunAutomaton automaton;

        public Exclude(String pattern) {
            automaton = WildcardBuilder.create(".").appendWildcard(pattern).buildRun();
        }

        @Override
        public Optional<Boolean> include(String className) {
            AutomatonMatcher matcher = automaton.newMatcher(className);
            boolean matched = matcher.find() && matcher.start() == 0 && matcher.end() == className.length();
            return matched ? Optional.of(false) : Optional.<Boolean>empty();
        }
    }
}
