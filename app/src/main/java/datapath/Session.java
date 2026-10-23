package datapath;

import go.Seq;

public final class Session implements Seq.Proxy {
    static {
        Datapath.touch();
    }
    private final int refnum;

    @Override
    public final int incRefnum() {
        Seq.incGoRef(this.refnum, this);
        return this.refnum;
    }

    Session(int refnum) {
        this.refnum = refnum;
        Seq.trackGoRef(refnum, this);
    }

    public Session() {
        this.refnum = __New();
        Seq.trackGoRef(this.refnum, this);
    }

    private static native int __New();

    public native void setNetwork(long handle);

    public native void stop();

    @Override
    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof Session)) {
            return false;
        }
        Session other = (Session) obj;
        return this.refnum == other.refnum;
    }

    @Override
    public int hashCode() {
        return java.util.Arrays.hashCode(new Object[]{Integer.valueOf(this.refnum)});
    }

    @Override
    public String toString() {
        return "Session#" + this.refnum;
    }
}
