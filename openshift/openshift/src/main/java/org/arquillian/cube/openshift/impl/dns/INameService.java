package org.arquillian.cube.openshift.impl.dns;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;

/**
 * Provides an invocation handler to register a DNS service 
 *
 * See https://stackoverflow.com/questions/11647629/how-to-configure-hostname-resolution-to-use-a-custom-dns-server-in-java
 */
public interface INameService extends InvocationHandler {
    public static void install(final INameService dns) throws IllegalArgumentException, IllegalAccessException, NoSuchFieldException, SecurityException, ClassNotFoundException {
        final Class<?> inetAddressClass = InetAddress.class;
        Object neu;
        Field nameServiceField;
        try {
            final Class<?> iface = Class.forName("java.net.InetAddress$NameService");
            nameServiceField = inetAddressClass.getDeclaredField("nameService");
            neu = Proxy.newProxyInstance(iface.getClassLoader(), new Class<?>[] { iface }, dns);
        } catch(final ClassNotFoundException|NoSuchFieldException e) {
            nameServiceField = inetAddressClass.getDeclaredField("nameServices");
            final Class<?> iface = Class.forName("sun.net.spi.nameservice.NameService");
            neu = Arrays.asList(Proxy.newProxyInstance(iface.getClassLoader(), new Class<?>[] { iface }, dns));
        }
        nameServiceField.setAccessible(true);
        nameServiceField.set(inetAddressClass, neu);
    }

    /**
     * Lookup a host mapping by name. Retrieve the IP addresses associated with a host
     *
     * @param host the specified hostname
     * @return array of IP addresses for the requested host
     * @throws UnknownHostException  if no IP address for the {@code host} could be found
     */
    InetAddress[] lookupAllHostAddr(final String host) throws UnknownHostException;

    /**
     * Lookup the host corresponding to the IP address provided
     *
     * @param addr byte array representing an IP address
     * @return {@code String} representing the host name mapping
     * @throws UnknownHostException
     *             if no host found for the specified IP address
     */
    String getHostByAddr(final byte[] addr) throws UnknownHostException;

    @Override default public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable {
        switch(method.getName()) {
            case "lookupAllHostAddr": return lookupAllHostAddr((String)args[0]);
            case "getHostByAddr"    : return getHostByAddr    ((byte[])args[0]);
            default                 :
                final StringBuilder o = new StringBuilder();
                o.append(method.getReturnType().getCanonicalName()+" "+method.getName()+"(");
                final Class<?>[] ps = method.getParameterTypes();
                for(int i=0;i<ps.length;++i) {
                    if(i>0) o.append(", ");
                    o.append(ps[i].getCanonicalName()).append(" p").append(i);
                }
                o.append(")");
                throw new UnsupportedOperationException(o.toString());
        }
    }
}
