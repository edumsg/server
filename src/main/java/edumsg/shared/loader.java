package edumsg.shared;


import java.io.*;
import java.lang.ClassLoader;


public class loader extends ClassLoader {
    public  Class findClass(String name,String path) {
        byte[] b = new byte[0];
        try {
            b = loadClassFromFile(path);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return defineClass(name, b, 0, b.length);
    }

    private   byte[] loadClassFromFile(String path) throws FileNotFoundException {
        File file = new File(path+".class");
        InputStream inputStream = new FileInputStream(file);
        byte[] buffer;
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        int nextValue = 0;
        try {
            while ( (nextValue = inputStream.read()) != -1 ) {
                byteStream.write(nextValue);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        buffer = byteStream.toByteArray();

        return buffer;
    }
}
