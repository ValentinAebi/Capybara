package org.example;

public class Theater implements Bookable {
    private int freeSeatsCnt;

    public Theater(int totalSeatsCnt) {
        this.freeSeatsCnt = totalSeatsCnt;
    }

    @Override
    public double seatPrice() {
        return 57.50;
    }

    @Override
    public boolean hasFreeSeats() {
        return freeSeatsCnt >= 0;
    }

    @Override
    public void book() {
        if (!hasFreeSeats()) {
            throw new IllegalStateException();
        }
        freeSeatsCnt -= 1;
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        sb.append("Theater ");
        if (hasFreeSeats()) {
            sb.append("with ").append(freeSeatsCnt).append(" free seats left");
        } else {
            sb.append("(sold-out)");
        }
        return sb.toString();
    }

}
