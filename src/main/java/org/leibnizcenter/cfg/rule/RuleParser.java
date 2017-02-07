package org.leibnizcenter.cfg.rule;

import org.leibnizcenter.cfg.algebra.semiring.dbl.DblSemiring;
import org.leibnizcenter.cfg.algebra.semiring.dbl.ProbabilitySemiring;
import org.leibnizcenter.cfg.category.Category;
import org.leibnizcenter.cfg.category.nonterminal.NonTerminal;
import org.leibnizcenter.cfg.category.terminal.Terminal;
import org.leibnizcenter.cfg.category.terminal.stringterminal.RegexTerminal;
import org.leibnizcenter.cfg.earleyparser.ParseTree;
import org.leibnizcenter.cfg.earleyparser.Parser;
import org.leibnizcenter.cfg.grammar.Grammar;
import org.leibnizcenter.cfg.token.Token;

import java.util.*;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.Character.isWhitespace;

/**
 * <p>
 * For parsing a rule from a {@link String}
 * </p>
 * Created by maarten on 6-2-17.
 */
public class RuleParser {
    private static final NonTerminal RHS = NonTerminal.of("S");
    private static final NonTerminal Cate = NonTerminal.of("Category");
    private static final NonTerminal CategoryContent = NonTerminal.of("CategoryContent");
    private static final NonTerminal Regex = NonTerminal.of("Regex");
    private static final NonTerminal RegexContent = NonTerminal.of("RegexContent");
    private static final NonTerminal NonRegexDelimiter = NonTerminal.of("NonRegexDelimiter");
    /**
     * Any category that start alphanumerically
     */
    private static final Pattern SIMPLE_CATEGORY = Pattern.compile("^\\p{Alnum}.*", Pattern.CASE_INSENSITIVE);
    private static final Pattern REGEX_MODIFIER = Pattern.compile("[xmsudi]+", Pattern.CASE_INSENSITIVE);
    private static final Terminal<String> CategorySimple = (token) -> SIMPLE_CATEGORY.matcher(token.obj).matches();

    private static final Terminal<String> RegexModifiers = (token) -> REGEX_MODIFIER.matcher(token.obj).matches();
    private static final Terminal<String> RegexDelimiter = (token) -> token instanceof RhsToken && ((RhsToken) token).isRegexDelimiter;
    private static final Terminal<String> WhiteSpace = (token) -> token instanceof RhsToken && ((RhsToken) token).isWhitespace;
    private static final Terminal<String> DankContent = (token) -> token instanceof RhsToken
            && !((RhsToken) token).isWhitespace
            && !((RhsToken) token).isRegexDelimiter;

    private static final Grammar<String> grammarRHS = new Grammar.Builder<String>()
            .addRule(Rule.create(ProbabilitySemiring.get(), RHS, Regex))
            .addRule(Rule.create(ProbabilitySemiring.get(), RHS, Cate))
            .addRule(Rule.create(ProbabilitySemiring.get(), RHS, RHS, WhiteSpace, RHS))

            .addRule(Rule.create(ProbabilitySemiring.get(), Cate, CategoryContent))
            .addRule(Rule.create(ProbabilitySemiring.get(), CategoryContent, CategoryContent, RegexDelimiter))
            .addRule(Rule.create(ProbabilitySemiring.get(), CategoryContent, DankContent))
            .addRule(Rule.create(ProbabilitySemiring.get(), CategoryContent, CategoryContent, CategoryContent))

            .addRule(Rule.create(ProbabilitySemiring.get(), Regex, RegexDelimiter, RegexContent, RegexDelimiter))
            .addRule(Rule.create(ProbabilitySemiring.get(), Regex, RegexDelimiter, RegexContent, RegexDelimiter, RegexModifiers))
            .addRule(Rule.create(ProbabilitySemiring.get(), RegexContent, NonRegexDelimiter))
            .addRule(Rule.create(ProbabilitySemiring.get(), RegexContent, RegexContent, RegexContent))
            .addRule(Rule.create(ProbabilitySemiring.get(), NonRegexDelimiter, DankContent))
            .addRule(Rule.create(ProbabilitySemiring.get(), NonRegexDelimiter, WhiteSpace))

            .build();

    private static final Pattern RULE = Pattern.compile("\\s*([^\\s]+)\\s*(?:->|→)((?:\\s*[^\\s(]+\\s*)+)\\s*(?:\\(([0-9](?:[.,][0-9]+)?)\\))?\\s*");

    private static final Pattern WHITESPACE = Pattern.compile("\\s+");

    private RuleParser() {
    }

    static List<RhsToken> lexRhs(char[] chars) {
        List<RhsToken> l = new ArrayList<>();

        StringBuilder sb = new StringBuilder(chars.length);
        for (int i = 0; i < chars.length; i++) {
            char c = chars[i];
            final boolean isEscapedRegexDelimiter = sb.length() > 0 && chars[i - 1] == '\\';
            if ((c == '/') && !(isEscapedRegexDelimiter)) {
                if (sb.length() > 0) {
                    l.add(new RhsToken(sb.toString()));
                    sb = new StringBuilder(chars.length - 1 - i);
                }
                l.add(new RhsToken(Character.toString(c)));
            } else if ((isWhitespace(c) && sb.length() > 0 && !isWhitespace(chars[i - 1]))
                    || (!isWhitespace(c) && sb.length() > 0 && isWhitespace(chars[i - 1]))) {

                l.add(new RhsToken(sb.toString()));
                sb = new StringBuilder(chars.length - 1 - i);

                if (isEscapedRegexDelimiter) sb.deleteCharAt(sb.length() - 1);
                sb.append(c);
            } else {
                sb.append(c);
            }
        }
        if (sb.length() > 0) l.add(new RhsToken(sb.toString()));
        return l;
    }

    static Category[] parseRHS(Function<String, Category> parseCategory, String rhsStr) {
        ParseTree viterbi = new Parser(grammarRHS)
                .getViterbiParse(RHS, lexRhs(rhsStr.toCharArray()));
        if (viterbi == null) throw new IllegalArgumentException("Could not parse grammar");
        viterbi = viterbi.flatten(RuleParser::getFlattenOption);
        List<Category> rhsList = viterbi.getChildren().stream()
                .map(parseTree -> getCategory(parseCategory, parseTree))
                .collect(Collectors.toList());

        Category[] RHS = new Category[rhsList.size()];
        RHS = rhsList.toArray(RHS);
        return RHS;
    }

    private static Category getCategory(Function<String, Category> parseCategory, ParseTree parseTree) {
        final boolean isSimpleCategory = parseTree.category.equals(Cate);
        final boolean isRegex = parseTree.category.equals(Regex);
        if (!isSimpleCategory && !isRegex) throw new IllegalStateException("Error while parsing grammar");


        return isRegex ? parseRegexTerminal(parseTree) : parseCategory.apply(parseTree.children.stream()
                .map(t -> (ParseTree.Token<String>) t)
                .map(t -> t.token.obj)
                .collect(Collectors.joining()));
    }

    private static RegexTerminal parseRegexTerminal(ParseTree parseTree) {
        final List<ParseTree> children = parseTree.children;

        Set<String> modifiers = new HashSet<>();
        int i = children.size() - 1;
        for (; i >= 0; i--) {
            final ParseTree child = children.get(i);
            if (child.category.equals(RegexDelimiter)) break;
            modifiers.add(((ParseTree.Token<String>) child).token.obj.toLowerCase(Locale.ROOT));
        }
        int flag = 0;
        if (modifiers.contains("x")) flag = flag | Pattern.COMMENTS;
        if (modifiers.contains("m")) flag = flag | Pattern.MULTILINE;
        if (modifiers.contains("s")) flag = flag | Pattern.DOTALL;
        if (modifiers.contains("u")) flag = flag | Pattern.UNICODE_CASE;
        if (modifiers.contains("d")) flag = flag | Pattern.UNIX_LINES;
        if (modifiers.contains("i")) flag = flag | Pattern.CASE_INSENSITIVE;

        return new RegexTerminal(
                children.subList(1, i).stream()
                        .map(t -> ((ParseTree.Token<String>) t))
                        .map(t -> t.token.obj)
                        .collect(Collectors.joining()),
                flag);
    }

    private static ParseTree.FlattenOption getFlattenOption(List<ParseTree> parents, ParseTree parseTree) {
        final ParseTree parent = (parents.size() > 0) ? parents.get(parents.size() - 1) : null;
        if (parseTree instanceof ParseTree.Token && parent != null) {
            if (parent.category.equals(Regex)) return ParseTree.FlattenOption.KEEP;
            else if (parent.category.equals(Cate))
                return ((ParseTree.Token) parseTree).token instanceof RhsToken && ((RhsToken) ((ParseTree.Token) parseTree).token).isWhitespace
                        ? ParseTree.FlattenOption.REMOVE
                        : ParseTree.FlattenOption.KEEP;
            else
                return ParseTree.FlattenOption.REMOVE;
        } else if (Stream.of(Regex, Cate).filter(c -> parseTree.category.equals(c)).findAny().isPresent())
            return ParseTree.FlattenOption.KEEP;
        else if (parseTree instanceof ParseTree.NonToken)
            return ParseTree.FlattenOption.KEEP_ONLY_CHILDREN;
        else
            return ParseTree.FlattenOption.REMOVE;
    }

    public static Rule fromString(String line, Function<String, Category> parseCategory, DblSemiring semiring) {
        Matcher m = RULE.matcher(line);
        if (!m.matches())
            throw new IllegalArgumentException("String was not a valid rule: " + line);
        else {
            final NonTerminal LHS = new NonTerminal(m.group(1));

            Category[] RHS = parseRHS(parseCategory, m.group(2).trim());

            final String prob = m.group(3);
            final double probability = semiring.fromProbability(prob == null ? 1.0 : Double.parseDouble(prob));
            return new Rule(
                    probability,
                    LHS,
                    RHS
            );
        }
    }

    static class RhsToken extends Token<String> {
        final boolean isWhitespace;
        final boolean isRegexDelimiter;

        public RhsToken(String s) {
            super(s);
            this.isWhitespace = WHITESPACE.matcher(s).matches();
            this.isRegexDelimiter = s.charAt(0) == '/';
        }

        @Override
        public String toString() {
            return super.obj;
        }
    }
}
