package com.github.valentinaebi.example.hello;

class Hello {

    public static void main(String[] args) {
        var x = foo(args.length);
        try {
            var t = 2 * x + 1;
            int z;
            if (t > 20) {
                z = 25;
            } else {
                z = Math.max(0, t);
            }
            int a = 2047;
            try {
                while (a > z) {
                    a /= 2;
                }
                System.out.println(z - a);
            } catch (ArithmeticException e) {
                // ignore
            }
        } catch (IllegalArgumentException | NullPointerException e) {
            System.out.println("Error 1");
        } catch (IllegalStateException e) {
            System.out.println("Error 2");
        }
    }

    static int foo(int x) {
        var y = x * x + 2 * x - 7;
        return y * y;
    }

}
