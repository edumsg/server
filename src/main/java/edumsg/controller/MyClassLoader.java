package edumsg.controller;

public class MyClassLoader extends ClassLoader {
    public Class<?> loadClass(byte[] byteCode, String className) {
        return defineClass(className, byteCode, 0, byteCode.length);
    }
}
