
public class Foo {

    void nullReceivers(String s, int x, int y) {
        if (s == null) {
            System.out.println("It's null");
        }
        if (x < y) {
            System.out.println("Less than");
            if (s != null) {
                System.out.println(s.length());     // not an issue: s is checked right above
            } else {
                s = "Hello";    // (*)
            }
        } else if (x == y) {
            System.out.println("Equal");
        } else {
            System.out.println("Greater than");
            System.out.println(s.length());     // #issue[INVK_NULL_REC]: example model = (s=null, x=1, y=0)
            System.out.println(s.length());     // not an issue: cannot be executed due to previous line
        }
        if (x + 2 < y) {
            // not an issue: x + 2 < y => x < y => s != null due to assignment at (*)
            System.out.println(s.length());
        }
    }

    void arrays(int[] array, int i) {
        var len = array.length;
        var head = array[0];
        int end = 0;
        if (i == len + 1) {
            end = i;
        } else if (i == -1) {
            array = null;
        }
        end = array[end];   // #issue[ARRAY_INDEX_OUT] #issue[INDEXING_NULL_ARRAY]
        System.out.println(Math.max(head - end, len));
    }

    void loops(int n) {
        int x = n;
        var b = new Bar();
        for (int i = 0; i < n; i++) {
            x += i;
            var isNull = (b == null);
            b = (b == null ? new Bar() : null);
            if (isNull) {
                b.x = -1;   // OK
            }
        }
        if (n % 3 == 1) {
            b.x *= 2;   // #issue[FLD_NULL_OWNER]
        }
        boolean nullFlag = false;
        if (b == null) {
            nullFlag = true;
        }
        while (x < 1000) {
            x *= 2;
        }
        if (nullFlag) {
            System.out.println(b.x);    // #issue[FLD_NULL_OWNER]
            b = new Bar();
        } else {
            b.x += 10;  // OK
        }
        System.out.println(b.x);    // OK
    }

    static class Bar {
        int x = 42;
    }

}
