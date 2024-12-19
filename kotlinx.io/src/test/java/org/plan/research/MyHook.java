package org.plan.research;

import com.code_intelligence.jazzer.api.HookType;
import com.code_intelligence.jazzer.api.MethodHook;
import java.lang.invoke.MethodHandle;

public class MyHook {

/*
    @MethodHook(
        type = HookType.REPLACE,
        targetClassName = "java.security.SecureRandom",
        targetMethod = "nextLong",
        targetMethodDescriptor = "()J")
    public static long getRandomNumber(
        MethodHandle handle, Object thisObject, Object[] args, int hookId) {
        return 4; // chosen by fair dice roll.
        // guaranteed to be random.
        // https://xkcd.com/221/
    }
*/


    @MethodHook(
        type = HookType.REPLACE,
        targetClassName = "java.lang.Throwable",
        targetMethod = "fillInStackTrace",
        targetMethodDescriptor = "()Ljava/lang/Throwable;"
    )
    public static Throwable doNothing(
        MethodHandle handle, Object thisObject, Object[] args, int hookId) {
        return (Throwable) thisObject; // chosen by fair dice roll.
        // guaranteed to be random.
        // https://xkcd.com/221/
    }

    static Exception exception = new Exception();

    @MethodHook(
        type = HookType.REPLACE,
        targetClassName = "java.lang.Exception",
        targetMethod = "<init>"
    )
    public static Exception noTrace(
        MethodHandle handle, Object thisObject, Object[] args, int hookId) {
        return exception;
    }

    @MethodHook(
        type = HookType.REPLACE,
        targetClassName = "java.lang.IllegalArgumentException",
        targetMethod = "<init>"
    )
    public static IllegalArgumentException nooo(
        MethodHandle handle, Object thisObject, Object[] args, int hookId) {
        return iae;
    }

    static IllegalArgumentException iae = new IllegalArgumentException();


    @MethodHook(
        type = HookType.REPLACE,
        targetClassName = "java.lang.NumberFormatException",
        targetMethod = "<init>"
    )
    public static NumberFormatException nfe(
        MethodHandle handle, Object thisObject, Object[] args, int hookId) {
        return nfe;
    }

    static NumberFormatException nfe = new NumberFormatException();
}
