import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.junit.Test;

public class IteratorTest {
    @Test public void testIteratorCount() {
        List<Integer> nums = new ArrayList<>();
        for (int idx=0; idx<5; ++idx) {
            nums.add(idx);
            assertTrue(idx == nums.get(idx));
        }
        assertEquals(nums.size(), 5);

        int cnt=0;
        for (Iterator<Integer> it=nums.iterator(); it.hasNext();) {
            int next = it.next();
            assertEquals(next, cnt++);
        }
        assertEquals(cnt, nums.size());
    }
}
