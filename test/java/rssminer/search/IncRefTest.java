/*
 * Copyright (c) Feng Shen<shenedu@gmail.com>. All rights reserved.
 * You must not remove this notice, or any other, from this software.
 */

package rssminer.search;

import java.io.File;
import java.io.IOException;

import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.store.FSDirectory;
import org.junit.Test;

public class IncRefTest {

    @Test
    public void testReader() throws CorruptIndexException, IOException {

        IndexReader r = IndexReader.open(FSDirectory.open(new File("/var/rssminer/index")));

        System.out.println(r.getRefCount());

        r.incRef();

        System.out.println(r.getRefCount());

        r.decRef();
        System.out.println(r.getRefCount());
    }
}
