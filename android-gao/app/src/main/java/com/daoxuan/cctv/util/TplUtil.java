package com.daoxuan.cctv.util;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TplUtil {
    public static String tpl(String tplStr, Map<String, Object> data ) {
        Matcher m= Pattern.compile("\\{([\\w\\.]*)\\}").matcher(tplStr);
        while(m.find()){
            String group=m.group();
            group= group.replaceAll("\\{|\\}", "");
            String value="";
            if(null!=data.get(group)){
                value=String.valueOf(data.get(group));
            }
            tplStr=tplStr.replace(m.group(),value);
        }
        return tplStr;
    }

}
