package rssminer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;
import org.apache.lucene.search.spell.Dictionary;
import org.apache.lucene.search.spell.SpellChecker;
import org.apache.lucene.store.FSDirectory;
import org.junit.Before;
import org.junit.Test;

public class SpellCheckerTest {

    private FSDirectory dir;
    private SpellChecker checker;

    @Before
    public void setup() throws IOException {
        dir = FSDirectory.open(new File("/tmp/index-spell"));
        checker = new SpellChecker(dir);
        // checker.indexDictionary(new FileDirectory());

    }

    @Test
    public void testSpellChecker() throws IOException {
        String[] sugguest = checker.suggestSimilar("dictionar", 10);
        for (String str : sugguest) {
            System.out.println(str);
        }
    }

    public static class FileDirectory implements Dictionary {
        private HashSet<String> set;

        public FileDirectory() throws FileNotFoundException, IOException {
            List<String> lines = IOUtils.readLines(new FileInputStream(
                    new File("/home/feng/Downloads/big.txt")));
            StringBuilder sb = new StringBuilder(lines.size() * 100);
            for (String line : lines) {
                sb.append(line.toLowerCase());
            }
            String text = sb.toString();
            Pattern p = Pattern.compile("\\w+");
            Matcher matcher = p.matcher(text);
            set = new HashSet<String>();
            while (matcher.find())
                set.add(matcher.group());
        }

        @Override
        public Iterator<String> getWordsIterator() {
            return set.iterator();
        }
    }
}
