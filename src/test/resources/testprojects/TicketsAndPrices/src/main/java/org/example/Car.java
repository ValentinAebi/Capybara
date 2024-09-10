package org.example;

public class Car extends Vehicle {

    public Car(int passengersCnt) {
        super(passengersCnt);
    }

    @Override
    public boolean canFly() {
        return false;
    }

}
