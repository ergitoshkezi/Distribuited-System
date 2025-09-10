/*
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.fabric.samples;

import java.util.Objects;

import org.hyperledger.fabric.contract.annotation.DataType;
import org.hyperledger.fabric.contract.annotation.Property;

import com.owlike.genson.annotation.JsonProperty;

@DataType()
public final class BasilLeg {

    @Property()
    private long timestamp;

    @Property()
    private String gpsPosition;

    @Property()
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

    public BasilLeg(@JsonProperty("timestamp") final long timestamp, @JsonProperty("gpsPosition") final String gpsPosition,
                 @JsonProperty("basil") final Basil basil) {
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
                new String[]{String.valueOf(getTimestamp()), getGpsPosition()},
                new String[]{String.valueOf(other.getTimestamp()), other.getGpsPosition()});
    }

    @Override
    public int hashCode() {
        return Objects.hash(getTimestamp(), getGpsPosition());
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "@" + Integer.toHexString(hashCode()) + " [timestamp=" + timestamp + ", gpsPosition="
                + gpsPosition +  "]";
    }
}
