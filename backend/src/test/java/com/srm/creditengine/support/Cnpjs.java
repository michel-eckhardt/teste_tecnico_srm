package com.srm.creditengine.support;

import java.util.concurrent.ThreadLocalRandom;

/** Generates syntactically valid CNPJs (correct check digits) for tests. */
public final class Cnpjs {

    private static final int[] FIRST_WEIGHTS = {5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
    private static final int[] SECOND_WEIGHTS = {6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};

    private Cnpjs() {}

    public static String random() {
        StringBuilder base = new StringBuilder(14);
        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (int i = 0; i < 8; i++) {
            base.append(random.nextInt(10));
        }
        base.append("0001");
        base.append(checkDigit(base, FIRST_WEIGHTS));
        base.append(checkDigit(base, SECOND_WEIGHTS));
        return base.toString();
    }

    private static int checkDigit(CharSequence digits, int[] weights) {
        int sum = 0;
        for (int i = 0; i < weights.length; i++) {
            sum += (digits.charAt(i) - '0') * weights[i];
        }
        int remainder = sum % 11;
        return remainder < 2 ? 0 : 11 - remainder;
    }
}
