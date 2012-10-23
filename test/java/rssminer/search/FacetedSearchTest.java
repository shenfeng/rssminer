/*
 * Copyright (c) Feng Shen<shenedu@gmail.com>. All rights reserved.
 * You must not remove this notice, or any other, from this software.
 */

package rssminer.search;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.facet.index.CategoryDocumentBuilder;
import org.apache.lucene.facet.index.params.DefaultFacetIndexingParams;
import org.apache.lucene.facet.index.params.FacetIndexingParams;
import org.apache.lucene.facet.search.FacetsCollector;
import org.apache.lucene.facet.search.params.CountFacetRequest;
import org.apache.lucene.facet.search.params.FacetSearchParams;
import org.apache.lucene.facet.search.results.FacetResult;
import org.apache.lucene.facet.search.results.FacetResultNode;
import org.apache.lucene.facet.taxonomy.CategoryPath;
import org.apache.lucene.facet.taxonomy.TaxonomyReader;
import org.apache.lucene.facet.taxonomy.TaxonomyWriter;
import org.apache.lucene.facet.taxonomy.directory.DirectoryTaxonomyReader;
import org.apache.lucene.facet.taxonomy.directory.DirectoryTaxonomyWriter;
import org.apache.lucene.index.*;
import org.apache.lucene.search.*;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.Version;
import rssminer.jsoup.HtmlUtils;
import rssminer.tools.Utils;

import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static rssminer.search.Searcher.*;

public class FacetedSearchTest {

    public static void main(String[] args) throws SQLException, IOException {

        RAMDirectory indexDir = new RAMDirectory();
        RAMDirectory taxoDir = new RAMDirectory();

//        Directory indexDir = FSDirectory.open(new File("/tmp/idx"));
//        Directory taxoDir = FSDirectory.open(new File("/tmp/taxidx"));

        IndexWriter writer = new IndexWriter(indexDir, new IndexWriterConfig(Version.LUCENE_36, analyzer));

        TaxonomyWriter taxo = new DirectoryTaxonomyWriter(taxoDir, IndexWriterConfig.OpenMode.CREATE);

        Connection db = Utils.getRssminerDB();

        String sql = "select d.id, d.summary, f.author, f.tags from feed_data d join feeds f on f.id = d.id where f.id < 200000";
        ResultSet rs = db.createStatement().executeQuery(sql);

        int count = 0;
        while (rs.next()) {
            count++;
            String author = rs.getString("author");
            String tags = rs.getString("tags");
            String summary = rs.getString("summary");
            int id = rs.getInt("id");


            Document doc = createDocument(id, author, summary, tags);


            CategoryDocumentBuilder builder = new CategoryDocumentBuilder(taxo);
            List<CategoryPath> paths = createPath(tags, author);
            builder.setCategoryPaths(paths);
            builder.build(doc);

//            System.out.println(doc);
            writer.addDocument(doc);


//            builder.setCategoryPaths(new cre)


//            System.out.println(author);
        }

        writer.commit();
        writer.close();

        taxo.commit();
        taxo.close();


        IndexReader reader = IndexReader.open(indexDir);
        IndexSearcher searcher = new IndexSearcher(reader);

        TaxonomyReader taxo2 = new DirectoryTaxonomyReader(taxoDir);

        System.out.println("count: " + count + "\t" + reader.numDocs());
        for (int i = 0; i < 1; i++) {

            long start = System.currentTimeMillis();
            Query q = new TermQuery(new Term(CONTENT, "java"));

            TopScoreDocCollector tdc = TopScoreDocCollector.create(10, true);

            FacetIndexingParams ip = new DefaultFacetIndexingParams();
            FacetSearchParams facetSearchParams = new FacetSearchParams();
            facetSearchParams.addFacetRequest(new CountFacetRequest(new CategoryPath(AUTHOR), 100));
            facetSearchParams.addFacetRequest(new CountFacetRequest(new CategoryPath("tag"), 100));

            FacetsCollector facetsCollector = new FacetsCollector(facetSearchParams, reader, taxo2);
            searcher.search(q, MultiCollector.wrap(tdc, facetsCollector));
            List<FacetResult> res = facetsCollector.getFacetResults();

//            System.out.println(res.size());
            for (FacetResult re : res) {
                FacetResultNode node = re.getFacetResultNode();
                for (FacetResultNode n : node.getSubResults()) {

                    System.out.println(n.getValue() + "\t" + n.getLabel());
                }

            }

            long time = System.currentTimeMillis() - start;
            System.out.println(tdc.topDocs().totalHits + "\t" + time + "ms");
        }
//        System.out.println(res);

//        System.out.println(indexDir.sizeInBytes());


//        System.out.println(taxoDir.sizeInBytes());

        indexDir.close();
        taxoDir.close();
    }

    private static List<CategoryPath> createPath(String tags, String author) {
        List<CategoryPath> paths = new ArrayList<CategoryPath>();
        if (author != null && author.length() > 0) {
            paths.add(new CategoryPath(AUTHOR, author));
        }
        if (tags != null && tags.length() > 0) {
            List<String> tagss = rssminer.Utils.split(tags, ';');
            for (String tag : tagss) {
                paths.add(new CategoryPath("tag", tag));
            }

        }
        return paths;
    }

    private static Document createDocument(int feeId, String author,
                                           String summary, String tags) {
        Document doc = new Document();
        // not intern, already interned
        Field fid = new Field(FEED_ID, false, Integer.toString(feeId),
                Field.Store.YES, Field.Index.NOT_ANALYZED, Field.TermVector.NO);
        doc.add(fid);


        if (author != null && author.length() > 0) {
            author = Mapper.toSimplified(author);
            List<String> authors = rssminer.Utils.simpleSplit(author);
            for (String a : authors) {
                Field f = new Field(AUTHOR, false, a.toLowerCase(), Field.Store.NO,
                        Field.Index.NOT_ANALYZED, Field.TermVector.NO);
                f.setBoost(AUTHOR_BOOST);
                doc.add(f);
            }
        }

        if (tags != null && tags.length() > 0) {
            tags = Mapper.toSimplified(tags);
            List<String> ts = rssminer.Utils.simpleSplit(tags);
            for (String tag : ts) {
                Field f = new Field(TAG, false, tag.toLowerCase(), Field.Store.NO,
                        Field.Index.NOT_ANALYZED, Field.TermVector.NO);
                f.setBoost(TAG_BOOST);
                doc.add(f);
            }
        }

        if (summary != null) {
            try {
                // String content = Utils.extractText(summary);
                String content = HtmlUtils.text(summary);
                content = Mapper.toSimplified(content);
                Field f = new Field(CONTENT, false, content, Field.Store.NO,
                        Field.Index.ANALYZED, Field.TermVector.NO);
                doc.add(f);
            } catch (Exception ignore) {
                logger.error("feed:" + feeId, ignore);
            }
        }
        return doc;
    }


}
