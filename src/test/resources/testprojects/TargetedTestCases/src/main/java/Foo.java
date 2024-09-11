
public class Foo {

    void foo(String s, int x, int y) {
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

    void bar(int[] array, int i) {
        var len = array.length;
        var head = array[0];
        int end = 0;
        if (i == len + 1) {
            end = i;
        }
        end = array[end];   // #issue[ARRAY_READ_INDEX_OUT]
        System.out.println(Math.max(head - end, len));
    }

}
