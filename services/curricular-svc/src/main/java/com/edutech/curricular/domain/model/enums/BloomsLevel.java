package com.edutech.curricular.domain.model.enums;

public enum BloomsLevel {
    REMEMBER(1, 1.0),
    UNDERSTAND(2, 1.2),
    APPLY(3, 1.4),
    ANALYZE(4, 1.6),
    EVALUATE(5, 1.8),
    CREATE(6, 2.0);

    private final int order;
    private final double weight;

    BloomsLevel(int order, double weight) {
        this.order = order;
        this.weight = weight;
    }

    public int getOrder() {
        return order;
    }

    public double getWeight() {
        return weight;
    }
}
