package data_count;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.net.URLDecoder;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.sparql.exec.http.QueryExecutionHTTP;

public class wikiLink_dbp {

    private static final int MAX_RETRIES = 5;
    private static final int WAIT_TIME = 5000; // 5 seconds

    public static void main(String[] args) throws Exception {

        // 出力ファイル指定
        File fileOUT = new File("output_count/wikiLink_dbp_sorted.ttl");
        try (BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(fileOUT), "UTF-8"))) {

            // 全結果を格納するリスト
            List<Map.Entry<String, Integer>> allResults = new ArrayList<>();

            // SPARQLクエリの作成
            String wikidataQueryStr = "PREFIX rdfs:<http://www.w3.org/2000/01/rdf-schema#> " +
                                      "PREFIX wdt: <http://www.wikidata.org/prop/direct/> " +
                                      "PREFIX wd: <http://www.wikidata.org/entity/> " +
                                      "PREFIX schema: <http://schema.org/> " +
                                      "SELECT DISTINCT ?dis ?disLabel " +
                                      "WHERE { " +
                                      "?dis wdt:P494 ?o . " +  // 疾患エンティティ
                                      "?wikipedia schema:about ?dis ; " +
                                      "           schema:isPartOf <https://ja.wikipedia.org/> . " +
                                      "?dis rdfs:label ?disLabel . " +
                                      "FILTER (lang(?disLabel) = 'ja') " +
                                      "}";

            // Wikidataのクエリの実行
            Query wikidataQuery = QueryFactory.create(wikidataQueryStr);
            try (QueryExecution qexecWikidata = QueryExecutionHTTP.create()
                    .endpoint("https://query.wikidata.org/sparql")
                    .query(wikidataQuery)
                    .param("timeout", "10000")
                    .build()) {

                ResultSet rsWikidata = executeWithRetry(qexecWikidata);

                while (rsWikidata.hasNext()) {
                    QuerySolution qsWikidata = rsWikidata.next();
                    String dis = qsWikidata.get("dis").toString();
                    dis = URLDecoder.decode(dis, "UTF-8");
                    String wikipedia = dis.replace("http://www.wikidata.org/entity/", "http://wikidata.dbpedia.org/resource/");

                    // DBpediaのクエリの作成
                    String dbpediaQueryStr = "PREFIX rdfs:<http://www.w3.org/2000/01/rdf-schema#> " +
                                             "PREFIX dbo:<http://dbpedia.org/ontology/> " +
                                             "PREFIX owl:<http://www.w3.org/2002/07/owl#> " +
                                             "PREFIX dct:<http://purl.org/dc/terms/> " +
                                             "PREFIX skos:<http://www.w3.org/2004/02/skos/core#> " +
                                             "PREFIX dbpedia-ja:<http://ja.dbpedia.org/resource/> " +
                                             "SELECT ?dis (COUNT(DISTINCT ?wikiPage) AS ?count) " +
                                             "WHERE { " +
                                             "<" + wikipedia + "> owl:sameAs ?dis ." +
                                             "FILTER(contains(str(?dis),\"http://ja.dbpedia.org/\")) " +
                                             "?dis dbo:wikiPageWikiLink ?wikiPage . " +
                                             "?dis rdfs:label ?disLabel . " +
                                             "FILTER (STRLEN(STR(?disLabel)) > 0)" +
                                             "?wikiPage dct:subject|dct:subject/skos:broader dbpedia-ja:Category:症状と徴候 ." +
                                             "?wikiPage rdfs:label ?symLabel . " +
                                             "FILTER (STRLEN(STR(?symLabel)) > 0)" +
                                             "} GROUP BY ?dis " +
                                             "ORDER BY DESC(?count)";

                    // DBpediaのクエリの実行
                    Query dbpediaQuery = QueryFactory.create(dbpediaQueryStr);
                    try (QueryExecution qexec = QueryExecutionHTTP.create()
                            .endpoint("https://ja.dbpedia.org/sparql/")
                            .query(dbpediaQuery)
                            .param("timeout", "10000")
                            .build()) {

                        ResultSet rsDBpedia = executeWithRetry(qexec);

                        while (rsDBpedia.hasNext()) {
                            QuerySolution qs = rsDBpedia.next();
                            Resource disP = qs.getResource("dis");
                            int count = qs.getLiteral("count").getInt();

                            // Mapエントリとしてリストに格納
                            allResults.add(new AbstractMap.SimpleEntry<>(disP.toString(), count));
                        }
                    }
                }
            }

            // ソート（降順）
            allResults.sort((a, b) -> Integer.compare(b.getValue(), a.getValue()));

            // 出力
            bw.write("Disease, SymptomsCount\n");
            for (Map.Entry<String, Integer> entry : allResults) {
                bw.write(entry.getKey() + ", " + entry.getValue() + "\n");
            }
        }
    }

    private static ResultSet executeWithRetry(QueryExecution qexec) throws Exception {
        int attempts = 0;
        while (true) {
            try {
                return qexec.execSelect();
            } catch (Exception e) {
                attempts++;
                if (attempts > MAX_RETRIES) {
                    throw e;
                }
                System.err.println("Query failed, retrying... (" + attempts + "/" + MAX_RETRIES + ")");
                Thread.sleep(WAIT_TIME);
            }
        }
    }
}
