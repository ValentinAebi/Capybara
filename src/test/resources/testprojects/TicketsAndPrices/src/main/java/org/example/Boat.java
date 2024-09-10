package org.example;

public class Boat extends Vehicle {

    public Boat(int passengersCnt) {
        super(passengersCnt);
    }

    @Override
    public boolean canFly() {
        return false;
    }
}
