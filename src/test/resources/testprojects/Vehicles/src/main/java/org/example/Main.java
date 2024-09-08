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

}