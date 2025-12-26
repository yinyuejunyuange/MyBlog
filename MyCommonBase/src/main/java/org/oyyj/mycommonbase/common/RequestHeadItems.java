package org.oyyj.mycommonbase.common;

import lombok.Data;

@Data
public class RequestHeadItems {

    public static String X_REAL_IP="X-Real-IP";

    public static String X_USER_ID = "X-User-Id";

    public static String X_TOKEN = "X-Token";

    public static String X_USER_PERMISSION = "X-User-Permission";

    public static String X_USER_ROLE = "X-User-Role";

    public static String X_USER_NAME = "X-User-Name";

    public static String X_AUTHENTICATED = "X-Authenticated";

    public static String X_FORWARDED_FOR = "X-Forwarded-For";

    public static String X_USER_AGENT = "X-User-Agent";

}
