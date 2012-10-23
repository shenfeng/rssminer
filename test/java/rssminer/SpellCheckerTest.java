/*
 * Copyright (c) Feng Shen<shenedu@gmail.com>. All rights reserved.
 * You must not remove this notice, or any other, from this software.
 */

package rssminer;


public class SpellCheckerTest {

//    private FSDirectory dir;
//    private SpellChecker checker;
//
//    @Before
//    public void setup() throws IOException {
//        dir = FSDirectory.open(new File("/tmp/index-spell"));
//        checker = new SpellChecker(dir);
//        // checker.indexDictionary(new FileDirectory());
//
//    }
//
//    @Test
//    public void testSpellChecker() throws IOException {
//        String[] sugguest = checker.suggestSimilar("dictionar", 10);
//        for (String str : sugguest) {
//            System.out.println(str);
//        }
//    }
//
//    public static class FileDirectory implements Dictionary {
//        private HashSet<String> set;
//
//        public FileDirectory() throws FileNotFoundException, IOException {
//            List<String> lines = IOUtils.readLines(new FileInputStream(
//                    new File("/home/feng/Downloads/big.txt")));
//            StringBuilder sb = new StringBuilder(lines.size() * 100);
//            for (String line : lines) {
//                sb.append(line.toLowerCase());
//            }
//            String text = sb.toString();
//            Pattern p = Pattern.compile("\\w+");
//            Matcher matcher = p.matcher(text);
//            set = new HashSet<String>();
//            while (matcher.find())
//                set.add(matcher.group());
//        }
//
//        @Override
//        public Iterator<String> getWordsIterator() {
//            return set.iterator();
//        }
//    }
}
