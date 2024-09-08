package org.example;

public final class A320 extends Plane {

    public A320() {
        super(150);
    }

    @Override
    public double seatPrice() {
        var price = 0.0;
        price += 75.50;     // crew salary
        price += 112.25;    // fuel
        price += 95.00;        // ecological tax
        return price;
    }

}
