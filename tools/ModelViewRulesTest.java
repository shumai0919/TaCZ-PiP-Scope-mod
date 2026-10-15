import com.shumai.taczpip.client.ModelViewRules;

public class ModelViewRulesTest {
    private static void check(int original, boolean hybrid, int[] views, int count, int expected) {
        if (ModelViewRules.stage(original, hybrid, views, count) != expected)
            throw new AssertionError("Incorrect model stage for " + original);
    }
    public static void main(String[] args) {
        int[] mk5 = {2, 2, 1};
        check(0, true, mk5, 3, 0);
        check(1, true, mk5, 3, 0);
        check(2, true, mk5, 3, 2);
        check(1, true, new int[]{2, 1}, 2, 1); // HAMR auxiliary sight
        check(2, true, new int[]{1, 2, 2}, 3, 1); // reversed sight order
        check(2, true, new int[]{2, 1, 2}, 3, 0); // nonadjacent same view
        check(2, true, new int[]{2, 3, 1}, 3, 2); // three distinct sights
        check(1, true, null, 2, 1);
        check(1, true, new int[0], 2, 1);
        check(1, true, new int[]{0, 0}, 2, 1);
        check(3, true, mk5, 3, 0);
        check(4, true, mk5, 3, 0);
        check(5, true, mk5, 3, 2);
        for (int counter = 0; counter < 10000; counter++)
            check(counter, true, mk5, 3, counter % 3 == 2 ? 2 : 0);
        check(Integer.MAX_VALUE - 2, true, mk5, 3, 2);
        check(-1, true, mk5, 3, -1);
        check(1, false, new int[]{1, 2}, 2, 0); // existing optical policy
        check(2, true, new int[]{2}, 3, 0); // TaCZ repeats short view arrays
        System.out.println("ModelViewRules: cumulative counter / 10000 switches and edge cases passed");
    }
}
