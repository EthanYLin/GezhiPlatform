package org.example.gezhiplatform.seed;

import java.util.List;
import java.util.Random;

public class RandomUtils {
    private static final Random random = new Random();

    public static <T> T pickOneFrom(List<T> lst) {
        if (lst == null || lst.isEmpty()) {
            return null;
        }
        return lst.get(random.nextInt(lst.size()));
    }

    public static <T> T pickOneFrom(T[] arr) {
        if (arr == null || arr.length == 0) {
            return null;
        }
        return arr[random.nextInt(arr.length)];
    }

    public static boolean roll(int successChance) {
        if (successChance < 0 || successChance > 100) {
            throw new IllegalArgumentException("Success chance must be between 0 and 100.");
        }
        return random.nextInt(100) < successChance;
    }

    public static int randInt(int minInclusive, int maxInclusive) {
        if (minInclusive > maxInclusive) {
            throw new IllegalArgumentException("Min must be less than or equal to Max.");
        }
        return random.nextInt(maxInclusive - minInclusive + 1) + minInclusive;
    }
}
