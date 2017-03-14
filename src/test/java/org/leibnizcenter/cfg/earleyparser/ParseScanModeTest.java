package org.leibnizcenter.cfg.earleyparser;

import org.junit.Assert;
import org.junit.Test;
import org.leibnizcenter.cfg.algebra.semiring.dbl.ProbabilitySemiring;
import org.leibnizcenter.cfg.category.Category;
import org.leibnizcenter.cfg.category.nonterminal.NonLexicalToken;
import org.leibnizcenter.cfg.category.nonterminal.NonTerminal;
import org.leibnizcenter.cfg.category.terminal.Terminal;
import org.leibnizcenter.cfg.category.terminal.stringterminal.CaseInsensitiveStringTerminal;
import org.leibnizcenter.cfg.category.terminal.stringterminal.ExactStringTerminal;
import org.leibnizcenter.cfg.earleyparser.callbacks.ParseOptions;
import org.leibnizcenter.cfg.earleyparser.scan.ScanMode;
import org.leibnizcenter.cfg.earleyparser.scan.TokenNotInLexiconException;
import org.leibnizcenter.cfg.grammar.Grammar;
import org.leibnizcenter.cfg.token.Token;
import org.leibnizcenter.cfg.token.Tokens;

import java.util.ArrayList;
import java.util.List;

/**
 */
public class ParseScanModeTest {
    private static final NonTerminal S = Category.nonTerminal("S");
    private static final Terminal<String> a = new CaseInsensitiveStringTerminal("a");
    private static final Terminal<String> b = new ExactStringTerminal("b");
    private static final Terminal<String> c = new CaseInsensitiveStringTerminal("c");
    private static final Terminal<String> period = new ExactStringTerminal(".");

    private final static NonTerminal A = Category.nonTerminal("A");

    @Test
    public void scanModeDrop() throws Exception {
        double p = (0.6);
        Grammar<String> grammar = new Grammar.Builder<String>()
                .addRule(p, S, a)
                .build();
        List<Token<String>> tokens = Tokens.tokenize("b a");

        ParseOptions<String> cb = new ParseOptions.Builder<String>().withScanMode(ScanMode.DROP).build();
        ParseTreeWithScore parse = new Parser<>(grammar).getViterbiParseWithScore(S, tokens, cb);
        Assert.assertEquals(p, parse.getProbability(), 0.00001);
    }

    @Test(expected = TokenNotInLexiconException.class)
    public void scanModeStrict() throws Exception {
        double p = (0.6);
        Grammar<String> grammar = new Grammar.Builder<String>()
                .addRule(p, S, a)
                .build();
        List<Token<String>> tokens = Tokens.tokenize("b a");

        ParseOptions<String> cb = new ParseOptions.Builder<String>().withScanMode(ScanMode.STRICT).build();
        ParseTreeWithScore parse = new Parser<>(grammar).getViterbiParseWithScore(S, tokens, cb);
        Assert.assertEquals(p, parse.getProbability(), 0.00001);
    }

    @Test
    public void scanModeWildcard() throws Exception {
        double p = (0.6);
        final double q = 0.666;
        Grammar<String> grammar = new Grammar.Builder<String>()
                .addRule(p, S, a, a)
                .addRule(q, S, b, a)
                .build();
        List<Token<String>> tokens = Tokens.tokenize("z a");

        ParseOptions<String> cb = new ParseOptions.Builder<String>().withScanMode(ScanMode.WILDCARD).build();
        ParseTreeWithScore parse = new Parser<>(grammar).getViterbiParseWithScore(S, tokens, cb);
        Assert.assertEquals(q, parse.getProbability(), 0.00001);

        System.out.println(parse);
    }

    @Test
    public void scanModeSynchronizeTokensPre() throws Exception {
        Grammar<String> grammar = new Grammar.Builder<String>()
                .addRule(0.8, S, A)
                .addRule(0.7, S, S, A)
                .addRule(0.6, A, a, a, period)
                .addRule(0.5, A, b, b, NonLexicalToken.INSTANCE, period)
                .build();
        List<Token<String>> tokens = Tokens.tokenize("a a . b b z a . a a .");

        ParseOptions<String> cb = new ParseOptions.Builder<String>().withScanMode(ScanMode.SYNCHRONIZE).build();
        ParseTreeWithScore parse = new Parser<>(grammar).getViterbiParseWithScore(S, tokens, cb);

        System.out.println(parse);
        Assert.assertEquals(0.7 * .7 * 0.8 * 0.6 * Math.pow(.5, 2) * .6, parse.getProbability(), 0.00001);
    }

// TODO
//    @Test
//    public void scanModeSynchronizeTokens() throws Exception {
//        Grammar<String> grammar = new Grammar.Builder<String>()
//                .addRule(0.8, S, A)
//                .addRule(0.7, S, S, A)
//                .addRule(0.6, A, a, a, period)
//                .addRule(0.5, A, NonLexicalToken.get(), period)
//                .build();
//        List<Token<String>> tokens = Tokens.tokenize("a a . a . a a .");
//
//        ParseOptions<String> cb = new ParseOptions.Builder<String>().withScanMode(ScanMode.SYNCHRONIZE).build();
//        ParseTreeWithScore parse = new Parser<>(grammar).getViterbiParseWithScore(S, tokens, cb);
//
//        System.out.println(parse);
//        Assert.assertEquals(0.7 * .7 * 0.8 * 0.6 * Math.pow(.5, 2) * .6, parse.getProbability(), 0.00001);
//    }
//
// todo
//    @Test
//    public void scanModeSynchronizeTokens() throws Exception {
//        Grammar<String> grammar = new Grammar.Builder<String>()
//                .addRule(0.5, S, A)
//                .addRule(0.5, S, S, A)
//                .addRule(0.1, A, a, a, period)
//                .addRule(0.9, A, b, b, period)
//                .addRule(0.9, A, NonLexicalToken.get(), period)
//                .build();
//
//        final List<Token<String>> s0 = Tokens.tokenize("b b . ");
//        final List<Token<String>> s1 = Tokens.tokenize("a a . ");
//        final List<Token<String>> s2 = Tokens.tokenize("a a b a a . ");
//        final List<Token<String>> s3 = Tokens.tokenize("a a .");
//        final List<Token<String>> tokens = new ArrayList<>(s1.size() + s2.size() + s3.size());
//        tokens.addAll(s1);
//        tokens.addAll(s2);
//        tokens.addAll(s3);
//
//        ParseOptions<String> cb = new ParseOptions.Builder<String>().withScanMode(ScanMode.SYNCHRONIZE).build();
//        ParseTreeWithScore parse = new Parser<>(grammar).getViterbiParseWithScore(S, tokens, cb);
//
//        System.out.println(parse);
//        Assert.assertEquals(.5 * .5 * Math.pow(.9, s1.size() + s2.size() - 1) * .1, parse.getProbability(), 0.00001);
//        assertTrue(.5 * .5 * Math.pow(.9, s1.size() + s2.size() - 1) * .1 < .5 * Math.pow(.9, tokens.size() - 1));
//    }

    @Test
    public void scanModeSynchronizeTokens() throws Exception {
        Grammar<String> grammar = new Grammar.Builder<String>()
                .addRule(1.0, S, A)
                .addRule(0.9, S, S, A)
                .addRule(0.1, A, a, a, period)
                .addRule(1, A, c, c, period)
                .addRule(0.9, A, NonLexicalToken.INSTANCE, period)
                .build();

        final List<Token<String>> s0 = Tokens.tokenize("c c . ");
        final List<Token<String>> s1 = Tokens.tokenize("a a . ");
        final List<Token<String>> s2 = Tokens.tokenize("a a b a a . ");
        final List<Token<String>> s3 = Tokens.tokenize("a a .");
        final List<Token<String>> tokens = new ArrayList<>(s0.size() + s1.size() + s2.size() + s3.size());
        tokens.addAll(s0);
        tokens.addAll(s1);
        tokens.addAll(s2);
        tokens.addAll(s3);

        ParseOptions<String> cb = new ParseOptions.Builder<String>().withScanMode(ScanMode.SYNCHRONIZE).build();
        ParseTreeWithScore parse = new Parser<>(grammar).getViterbiParseWithScore(S, tokens, cb);

        System.out.println(parse);
        Assert.assertEquals(0.9 * 0.9 * 1.0 * Math.pow(.9, s1.size() + s2.size() - 1) * .1, parse.getProbability(), 0.00001);
        //assertTrue(.5 * .5 * Math.pow(.9, s1.size() + s2.size() - 1) * .1 < .5 * Math.pow(.9, tokens.size() - 1));
    }

    @Test
    public void scanModeSynchronizeWhereErrorIsBetweenLexicalTokens() throws Exception {
        Grammar<String> grammar = new Grammar.Builder<String>()
                .addRule(0.8, S, A)
                .addRule(0.7, S, S, A)
                .addRule(0.6, A, a, a, period)
                .addRule(0.5, A, NonLexicalToken.INSTANCE, period)
                .build();
        List<Token<String>> tokens = Tokens.tokenize("a a . a b c d e f g h i j k l m n o p q r s t u v w x y z a . a a .");

        ParseOptions<String> cb = new ParseOptions.Builder<String>().withScanMode(ScanMode.SYNCHRONIZE).build();
        ParseTreeWithScore parse = new Parser<>(grammar).getViterbiParseWithScore(S, tokens, cb);

        System.out.println(parse);
        Assert.assertEquals(
                .7 * .7 * .8 * .6 * Math.pow(.5, 27) * .6,
                parse.getProbability(), 0.00001);
    }

    @Test
    public void scanModeSynchronizeWhereNonLexicalIsFirstCategoryOfErrorRule() throws Exception {
        Grammar<String> grammar = new Grammar.Builder<String>()
                .addRule(1.0, S, A)
                .addRule(0.2, A, NonLexicalToken.INSTANCE, period)
                .build();
        List<Token<String>> tokens = Tokens.tokenize("z a .");

        ParseOptions<String> cb = new ParseOptions.Builder<String>().withScanMode(ScanMode.SYNCHRONIZE).build();
        ParseTreeWithScore parse = new Parser<>(grammar).getViterbiParseWithScore(S, tokens, cb);

        System.out.println(parse);
        Assert.assertEquals(0.2 * 0.2, parse.getProbability(), 0.00001);
    }

    @Test
    public void scanModeSynchronizeWhereNonLexicalIsFirstCategoryOfErrorRuleTwiceApplied() throws Exception {
        Grammar<String> grammar = new Grammar.Builder<String>()
                .addRule(0.2, S, A, A)
                .addRule(0.2, A, NonLexicalToken.INSTANCE, period)
                .build();
        List<Token<String>> tokens = Tokens.tokenize("z a . a .");

        ParseOptions<String> cb = new ParseOptions.Builder<String>().withScanMode(ScanMode.SYNCHRONIZE).build();
        ParseTreeWithScore parse = new Parser<>(grammar).getViterbiParseWithScore(S, tokens, cb);

        System.out.println(parse);
        Assert.assertEquals(0.2 * 0.2 * 0.2 * 0.2, parse.getProbability(), 0.00001);
    }

    @Test
    public void scanModeSynchronizeWhereErrorIsBetweenRuleApplicationsAndLexicalTokens() throws Exception {
        Grammar<String> grammar = new Grammar.Builder<String>()
                .withSemiring(ProbabilitySemiring.get())
                .addRule(0.9, S, a, S, a)
                .addRule(0.9, S, b, S, b)
                .addRule(0.5, S, c)

                .addRule(0.5, S, a, NonLexicalToken.INSTANCE, a)
                .addRule(0.5, S, b, NonLexicalToken.INSTANCE, b)
                .build();
        List<Token<String>> tokens = Tokens.tokenize(" a    b c b a s b b   a");

        ParseOptions<String> cb = new ParseOptions.Builder<String>().withScanMode(ScanMode.SYNCHRONIZE).build();
        ParseTreeWithScore parse = new Parser<>(grammar).getViterbiParseWithScore(S, tokens, cb);

        System.out.println(parse);
        Assert.assertEquals(0.9 * Math.pow(.5, 5), parse.getProbability(), 0.00001);
    }
}
