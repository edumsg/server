package edumsg.redis;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.rmi.server.UID;

import static java.net.URLEncoder.encode;

/**
 * Created by Ahmed on 9/4/2016.
 */
public class demo {
    public static void main(String[] args) {

        try {
            String url = URLEncoder.encode(new UID().toString(), "UTF-8");
            System.out.println(url);
            System.out.println(url.length());
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

    }

}
