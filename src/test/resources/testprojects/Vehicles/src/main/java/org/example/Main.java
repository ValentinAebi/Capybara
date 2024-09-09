package org.example;

public class Main {

    public static void main(String[] args) {
        var theater = new Theater(400);
        var totalProfit = 0.0;
        while (theater.hasFreeSeats()) {
            theater.book();
            totalProfit += theater.seatPrice();
        }
        System.out.println("Profit: " + totalProfit);
    }

    void buggy(String s, int x, int y) {
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
            System.out.println(s.length());     // issue: example model = (s=null, x=1, y=0)
        }
        if (x + 2 < y){
            // not an issue: x + 2 < y => x < y => s != null due to assignment at (*)
            System.out.println(s.length());
        }
    }

}