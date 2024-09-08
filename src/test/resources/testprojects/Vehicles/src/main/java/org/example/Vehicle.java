package org.example;

public abstract class Vehicle {
    private final int passengersCnt;

    protected Vehicle(int passengersCnt) {
        this.passengersCnt = passengersCnt;
    }

    public abstract boolean canFly();

    public int getPassengersCnt() {
        return passengersCnt;
    }

}
