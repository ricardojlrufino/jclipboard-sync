package com.github.ricardojlrufino.clipsync.desktop;

import com.sun.jna.*;
import java.util.*;

public class NativeSystemTry {

   public interface TryLibrary extends Library {
        // GoSlice class maps to:
        // C type struct { void *data; GoInt len; GoInt cap; }
        public class GoSlice extends Structure {
            public static class ByValue extends GoSlice implements Structure.ByValue {}
            public Pointer data;
            public long len;
            public long cap;
            protected List getFieldOrder(){
                return Arrays.asList(new String[]{"data","len","cap"});
            }
        }

        // GoString class maps to:
        // C type struct { const char *p; GoInt n; }
        public class GoString extends Structure {
            public static class ByValue extends GoString implements Structure.ByValue {}
            public String p;
            public long n;
            protected List getFieldOrder(){
                return Arrays.asList(new String[]{"p","n"});
            }

        }

        // Foreign functions
        public long Log(GoString.ByValue str);
        public void Open();
        public void SetTitle(GoString.ByValue str);
    }
 
   static public void main(String argv[]) {

        TryLibrary lib = (TryLibrary) Native.loadLibrary(
            "./tryicon/tryicon.so", TryLibrary.class);

       TryLibrary.GoString.ByValue title = new TryLibrary.GoString.ByValue();
       title.p = "Clipboard";
       title.n = title.p.length();
       lib.SetTitle(title);
       lib.Open();

        // Call Log
        TryLibrary.GoString.ByValue str = new TryLibrary.GoString.ByValue();
        str.p = "Hello Java!";
        str.n = str.p.length();
        System.out.printf("msgid %d\n", lib.Log(str));

    }
}