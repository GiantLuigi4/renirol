package tfc.renirol.util;

public record Pair<T, V>(T left, V right) {
    public static <T, V> Pair<T, V> of(T left, V right) {
        return new Pair<>(left, right);
    }
}
