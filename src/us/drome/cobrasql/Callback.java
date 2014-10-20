package us.drome.cobrasql;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

/**
 * Wrapper to allow easier invocation of a callback method by needing to pass fewer parameters.
 * 
 * @author TheAcademician
 * @since 0.1
 */
public class Callback {
    private final Object instance;
    private final Method toInvoke;
    
    /**
     * Constructs a new <tt>Callback</tt> object that can be used to invoke a method via reflection.
     * 
     * @param instance Class instance where the <tt>method</tt> is located.
     * @param method The name of the method to execute. This method <b>MUST</b> accept a
     * single parameter of <tt>List&lt;Row&gt;</tt>.
     * @throws NoSuchMethodException
     */
    public Callback(Class instance, String method) throws NoSuchMethodException {
        this.instance = instance;
        toInvoke = instance.getMethod(method, List.class);
    }
    
    /**
     * Executes the method referenced in this <tt>Callback</tt> object.
     * 
     * @param param The parameter to pass to the invoked method.
     * @throws IllegalAccessException
     * @throws IllegalArgumentException
     * @throws InvocationTargetException
     */
    public void invoke(Object param) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        this.toInvoke.invoke(instance, param);
    }
}
