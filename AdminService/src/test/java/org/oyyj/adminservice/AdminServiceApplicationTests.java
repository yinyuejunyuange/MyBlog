package org.oyyj.adminservice;

import org.junit.jupiter.api.Test;
import org.oyyj.adminservice.util.RSAUtil;
import org.springframework.boot.test.context.SpringBootTest;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Map;

@SpringBootTest
class AdminServiceApplicationTests {

    @Test
    void contextLoads() throws Exception {

        String str="oyyj";

        Map<String, String> stringStringMap = RSAUtil.genKeyPair();

        String aPrivate = RSAUtil.Encryption(str, stringStringMap.get("public"));

        System.out.println(aPrivate);
        System.out.println("------------------------------------------------");

        String aPublic = RSAUtil.Decryption(aPrivate, stringStringMap.get("private"));
        System.out.println(aPublic);

    }

}
