/*
 * SPDX-License-Identifier: Apache-2.0
 */


import java.util.Objects;

public final class BasilLeg {


    private long timestamp;

    private String gpsPosition;

    private Basil basil;

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public void setGpsPosition(String gpsPosition) {
        this.gpsPosition = gpsPosition;
    }

    public void setBasil(Basil basil) {
        this.basil = basil;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getGpsPosition() {
        return gpsPosition;
    }

    public Basil getBasil() {
        return basil;
    }

    public BasilLeg(final long timestamp, final String gpsPosition,
                    final Basil basil) {
        this.timestamp = timestamp;
        this.gpsPosition = gpsPosition;
        this.basil = basil;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }

        if ((obj == null) || (getClass() != obj.getClass())) {
            return false;
        }

        BasilLeg other = (BasilLeg) obj;

        return Objects.deepEquals(
                new String[]{String.valueOf(getTimestamp()), getGpsPosition(), String.valueOf(getBasil())},
                new String[]{String.valueOf(other.getTimestamp()), other.getGpsPosition(), String.valueOf(getBasil())});
    }

    @Override
    public int hashCode() {
        return Objects.hash(getTimestamp(), getGpsPosition(),getBasil());
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "@" + Integer.toHexString(hashCode()) + " [timestamp=" + timestamp + ", gpsPosition="
                + gpsPosition + "Basil=" + basil +"]";
    }
}
