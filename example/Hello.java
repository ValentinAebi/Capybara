
class Hello {

    public static void main(String[] args) {
        var x = foo(args.length);
        var t = 2*x + 1;
        int z;
        if (t > 20){
            z = 25;
        } else {
            z = Math.max(0, t);
        }
        try {
            int a = 2047;
            while (a > z){
                a /= 2;
            }
            System.out.println(z - a);
        } catch (ArithmeticException e){
            System.out.println("Error!");
        }
    }

    static int foo(int x){
        var y = x*x + 2*x - 7;
        return y*y;
    }

}
