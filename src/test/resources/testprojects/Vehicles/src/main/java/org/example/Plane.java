package org.example;

import java.time.LocalDateTime;

public class Plane extends Vehicle implements Bookable {
    private static final double AVG_PRICE = 189.00;

    private int bookedSeatsCnt = 0;

    protected Plane(int passengersCnt) {
        super(passengersCnt);
    }

    @Override
    public boolean canFly() {
        return true;
    }

    @Override
    public double seatPrice() {
        return computeDefaultPrice(AVG_PRICE, 1.5, 0.8);
    }

    @Override
    public boolean hasFreeSeats() {
        return bookedSeatsCnt < getPassengersCnt();
    }

    @Override
    public void book() {
        // no check that seats are free because it's what airlines do...
        bookedSeatsCnt += 1;
    }

    private double computeDefaultPrice(double avgPrice, double summerFactor, double winterFactor) {
        var currentMonth = LocalDateTime.now().getMonth().getValue();
        var isSummer = 5 <= currentMonth && currentMonth <= 10;
        return avgPrice * (isSummer ? summerFactor : winterFactor);
    }

}
