package org.leibnizcenter.cfg.earleyparser;


import org.leibnizcenter.cfg.algebra.semiring.dbl.Resolvable;

@SuppressWarnings("WeakerAccess")
public class ExpressionWrapper extends Resolvable {
    private double literal = Double.NaN;
    private Resolvable expression = null;

    public ExpressionWrapper(Resolvable expression) {
        if (expression == null) throw new NullPointerException();
        this.expression = expression;
    }

    public ExpressionWrapper(double literal) {
        this.literal = literal;
    }

    @SuppressWarnings("unused")
    public Resolvable getExpression() {
        if (lock) throw new IllegalStateException("Value already locked");
        if (expression == null) throw new NullPointerException();
        return expression;
    }

    @SuppressWarnings("unused")
    public void setExpression(Resolvable expression) {
        if (lock) throw new IllegalStateException("Value already locked");
        if (expression == null) throw new NullPointerException();
        this.literal = Double.NaN;
        this.expression = expression;
    }

    public double resolve() {
        if (lock) return cached;
        if (expression == null) return literal;
        else return expression.resolveFinal();
    }

    public boolean hasExpression() {
        return expression != null;
    }

    public double getLiteral() {
        return literal;
    }
}
