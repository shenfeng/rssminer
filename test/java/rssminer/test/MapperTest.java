/*
 * Copyright (c) Feng Shen<shenedu@gmail.com>. All rights reserved.
 * You must not remove this notice, or any other, from this software.
 */

package rssminer.test;

import junit.framework.Assert;
import org.junit.Test;
import rssminer.search.Mapper;

public class MapperTest {

    @Test
    public void testToSimplifed() {
        Assert.assertEquals(Mapper.toSimplified("萬网友留言拚"), "万网友留言拚");
        Assert.assertEquals(Mapper.toSimplified("網上文字轉語音及繁簡翻譯服務"), "网上文字转语音及繁简翻译服务");
        Assert.assertEquals(Mapper.toSimplified("abcdef123網上文字轉語音及繁簡翻譯服務"), "abcdef123网上文字转语音及繁简翻译服务");
        Assert.assertEquals(Mapper.toSimplified("萬网友留言拚"), "万网友留言拚");
        Assert.assertSame(Mapper.toSimplified("网友留言"), "网友留言");

        // http://www.hao123.com/haoserver/jianfanzh.htm
        String simple = "在线繁体字简体字转换,自动识别一简对多繁——快典网";
        String taiwan = "在線繁體字簡體字轉換,自動識別一簡對多繁——快典網";

        int mid = 0;
        simple = simple.substring(mid) + simple.substring(0, mid);
        taiwan = taiwan.substring(mid) + taiwan.substring(0, mid);

        String shouldSimple = Mapper.toSimplified(taiwan);

        for (int i = 0; i < shouldSimple.length(); i++) {
            if (shouldSimple.charAt(i) != simple.charAt(i)) {
                System.out.println("---------" + simple.charAt(i) + "\t" + shouldSimple.charAt(i) + "\t" + i);
            }
        }

//        System.out.println(simple.length() + "\t" + );
        Assert.assertEquals(simple, Mapper.toSimplified(simple));
        Assert.assertEquals(simple, Mapper.toSimplified(taiwan));
        String s = "5. Data Structures — Python v2.7.3 documentation";
        Assert.assertSame(s, Mapper.toSimplified(s));
    }
}
