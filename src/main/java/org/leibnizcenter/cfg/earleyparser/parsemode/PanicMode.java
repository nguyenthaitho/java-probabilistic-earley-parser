package org.leibnizcenter.cfg.earleyparser.parsemode;

import gnu.trove.map.hash.TIntObjectHashMap;
import org.leibnizcenter.cfg.algebra.semiring.dbl.ExpressionSemiring;
import org.leibnizcenter.cfg.category.nonterminal.NonLexicalToken;
import org.leibnizcenter.cfg.earleyparser.Scan;
import org.leibnizcenter.cfg.earleyparser.callbacks.ScanProbability;
import org.leibnizcenter.cfg.earleyparser.chart.Chart;
import org.leibnizcenter.cfg.earleyparser.chart.state.State;
import org.leibnizcenter.cfg.token.Token;
import org.leibnizcenter.cfg.token.TokenWithCategories;

import java.util.Collections;
import java.util.HashSet;
import java.util.stream.IntStream;

public class PanicMode {
    public static <T> void proceedAllStatesThatWereActiveOnError(Chart<T> chart, int chartIndex, TIntObjectHashMap<Token<T>> tokensPassed) {
        final ExpressionSemiring sr = chart.grammar.semiring;
        new HashSet<>(chart.stateSets.activeStates.activeOnNonLexicalToken)
                .stream()
                .filter(s -> s.comesBefore(chartIndex))
                .forEach(rootStateActiveOnError -> {
                            final double rootForward = chart.getForwardScore(rootStateActiveOnError);
                            final double rootInner = chart.getInnerScore(rootStateActiveOnError);

                            IntStream.range(rootStateActiveOnError.position + 1, chartIndex + 1)
                                    .forEach(position -> {
                                        final State preScanState = State.create(
                                                position,
                                                rootStateActiveOnError.ruleStartPosition,
                                                rootStateActiveOnError.ruleDotPosition,
                                                rootStateActiveOnError.rule
                                        );

                                        int numberOfScannedTokens = position - rootStateActiveOnError.position;
                                        double ruleProv = rootStateActiveOnError.rule.probabilityAsSemiringElement;
                                        final double newInner = sr.times(rootInner, sr.pow(ruleProv, numberOfScannedTokens - 1));
                                        double newForward = sr.times(rootForward, newInner);

                                        chart.addPredictedStateToChart(rootStateActiveOnError, newInner, newForward, preScanState);

                                        Token<T> token = tokensPassed.get(position);

                                        if (position <= chartIndex) {
                                            double scanProbPow = determineScanProbabilityOfMultipleTokens(
                                                    chart,
                                                    rootStateActiveOnError.position,
                                                    position,
                                                    sr,
                                                    token
                                            );

                                            chart.stateSets.createStateAndSetScores(new Scan.Delta<>(
                                                    token,
                                                    rootStateActiveOnError,
                                                    Scan.calculateForwardScore(scanProbPow, sr, newForward),
                                                    Scan.calculateInnerScore(scanProbPow, sr, newInner),
                                                    preScanState.rule, position, preScanState.ruleStartPosition, preScanState.advanceDot()
                                            ));
                                        }
                                    });
                        }
                );
    }

    private static <T> double determineScanProbabilityOfMultipleTokens(Chart<T> chart, int startPositionInclusive, int endPositionInclusive, ExpressionSemiring sr, Token<T> token) {
        final ScanProbability<T> scanProbability = chart.parseOptions.scanProbability;
        double scanProbPow = Double.NaN;
        if (scanProbability != null) {
            //noinspection unchecked
            final double scanProbAtPosition = Scan.getScanProb(
                    scanProbability,
                    new TokenWithCategories<>(token, Collections.singleton(NonLexicalToken.INSTANCE)),
                    endPositionInclusive
            );
            //noinspection unchecked
            scanProbPow = IntStream.range(startPositionInclusive, endPositionInclusive)
                    .mapToDouble(p -> Scan.getScanProb(scanProbability,
                            new TokenWithCategories<>(token, Collections.singleton(NonLexicalToken.INSTANCE)),
                            p
                    ))
                    .reduce(scanProbAtPosition, sr::times);
        }
        return scanProbPow;
    }
}
