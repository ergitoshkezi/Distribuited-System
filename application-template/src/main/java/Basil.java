/*
 * SPDX-License-Identifier: Apache-2.0
 */


import java.util.Objects;

public final class Basil {


    private String qr;


    private String extraInfo;


    private String owner;


    public void setQr(String qr) {
        this.qr = qr;
    }

    public void setExtraInfo(String extraInfo) {
        this.extraInfo = extraInfo;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public String getQr() {
        return qr;
    }

    public String getExtraInfo() {
        return extraInfo;
    }

    public String getOwner() {
        return owner;
    }


    public Basil(final String qr,final String extraInfo,
                 final String owner) {
        this.qr = qr;
        this.extraInfo = extraInfo;
        this.owner = owner;

    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }

        if ((obj == null) || (getClass() != obj.getClass())) {
            return false;
        }

        Basil other = (Basil) obj;

        return Objects.deepEquals(
                new String[]{getQr(), getExtraInfo(), getOwner()},
                new String[]{other.getQr(), other.getExtraInfo(), other.getOwner()});
    }

    @Override
    public int hashCode() {
        return Objects.hash(getQr(), getExtraInfo(), getOwner());
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "@" + Integer.toHexString(hashCode()) + " [qr=" + qr + ", extraInfo="
                + extraInfo + ", owner=" + owner + "]";
    }
}
