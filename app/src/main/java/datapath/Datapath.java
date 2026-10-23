package datapath;

import go.Seq;

public abstract class Datapath {
    static {
        Seq.touch();
        _init();
    }

    private Datapath() {}

    private static native void _init();

    public static native Session start(long tunFD, long mtu);

    public static void touch() {}
}
