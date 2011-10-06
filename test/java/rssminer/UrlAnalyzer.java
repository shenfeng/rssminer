package rssminer;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.junit.Test;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Lists;
import com.google.common.collect.Multiset.Entry;

public class UrlAnalyzer {

    @Test
    public void analyzeUrl() throws SQLException {
        List<String> hosts = WarmDnsCache.getAllHosts(WarmDnsCache.ALL_URL);
        HashMultiset<String> words = HashMultiset.create();
        for (String host : hosts) {
            String[] ws = host.split("\\W");
            for (String w : ws) {
                words.add(w);
            }
        }
        ArrayList<Entry<String>> lists = Lists.newArrayList(words.entrySet()
                .iterator());
        Collections.sort(lists, new Comparator<Entry<String>>() {
            public int compare(Entry<String> o1, Entry<String> o2) {
                int c1 = o1.getCount();
                int c2 = o2.getCount();
                if (c1 < c2)
                    return 1;
                else if (c1 > c2)
                    return -1;
                else
                    return 0;
            }
        });

        final int step = 6;
        final int round = 50;
        final int size = step * round;

        List<Entry<String>> print = lists.subList(0, size);
        List<Entry<String>> result = new ArrayList<Entry<String>>(
                print.size());

        int index = 0;
        for (int run = 0; run < round; ++run) {
            index = 0;
            for (Entry<String> e : print) {
                if (index++ % round == run)
                    result.add(e);
            }
        }

        index = 0;
        for (Entry<String> e : result) {
            System.out.printf("%24s: %6s", e.getElement(), e.getCount());

            if (++index % step == 0)
                System.out.println();

        }
    }
}
