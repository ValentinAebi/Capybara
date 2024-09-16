
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

    void shouldKnowThatArrayLengthIsNonNeg(String[] strings) {
        var l = strings.length;
        if (l < 0) {
            System.out.println(strings[-1]);    // Should be OK (unreachable)
        }
    }

    void shouldSaveArrayLengthOnCreation(String[] strings, boolean b) {
        var ints = new int[2];
        var l = strings.length;
        if (b) {
            ints[l + 2] = -75;  // #issue[ARRAY_INDEX_OUT]
        }
        for (int i = 0; i < 2; i++) {
            System.out.println(ints[i]);  // OK
        }
        for (int i = 0; i < 3; i++) {
            System.out.println(ints[i]);  // #issue[ARRAY_INDEX_OUT]
        }
    }

    void tableSwitch(int i) {
        var arr = new int[i + 2];
        for (int j = 0; j < i; j++) {
            arr[j] = 2 * j + 1;
        }
        int x = i * i;
        switch (i) {
            case 2:
            case 3:
                System.out.println("Hello");
                break;
            case 1:
                System.out.println(arr[3]);     // #issue[ARRAY_INDEX_OUT]
                break;
            case -1:
                System.out.println(arr[0]);
                break;
            case 5:
            case 6:
            case 7:
            case 8:
                x += (i % 3);
                break;
        }
        System.out.println(x);
    }

    void lookupSwitch(int i, Object o) {
        var arr = new int[i + 2];
        for (int j = 0; j < i; j++) {
            arr[j] = 2 * j + 1;
        }
        int x = i * i;
        if (i == 100) {
            i += 1;
        }
        if (o == null) {
            i = 100;
        }
        switch (i) {
            case 21:
            case 39:
                System.out.println("Hello");
                break;
            case 1:
                System.out.println(arr[3]);     // #issue[ARRAY_INDEX_OUT]
                break;
            case 57:
            case 68:
            case 79:
            case 82:
                x += (i % 3);
                break;
            case 100:
                o.hashCode();   // #issue[INVK_NULL_REC]
                break;
        }
        System.out.println(x);
    }

    Bar[] arrayNegativeLen(int len, int[] indices) {
        if (len < -10) {
            throw new IllegalArgumentException();
        } else if (len < -5) {
            System.err.println("Bad length for array");
        }
        for (int i = 0; i < len; i++) {
            System.out.println(i);
        }
        if (len < -50) {
            return new Bar[-1];     // should be OK, unreachable
        }
        if (len < -7) {
            return new Bar[len];    // #issue[NEG_ARRAY_LEN]
        }
        return new Bar[0];
    }

    int divByZero(int x, int y) {
        return switch (x) {
            case -1, 2 -> 12 / x;   // OK
            case 1 -> {
                var t = (y == 0) ?
                        x / (y - 1) :
                        y / (x - 1);    // #issue[DIV_BY_ZERO]
                yield 2 * t;
            }
            default -> (x > -2 && x <= 2) ?
                    (25 + 2 * y) % x :  // #issue[DIV_BY_ZERO] (if this is executed, then it must be that x == 0)
                    x % y;
        };
    }

    void assumeFailureConditionIsWrongAfterFailPoint(int x) {
        var array = new int[x];
        System.out.println(array[25]);
        if (x < 26) {
            var t = array[array.length + 1];  // unreachable because x < 26 => len(array) < 26 ==> array[25] is out of bounds
            System.out.println(t);
        }
    }

    int affineF(int x) {
        return 2 * x + 1;
    }

}
