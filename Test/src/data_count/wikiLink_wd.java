package data_count;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;

import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.sparql.exec.http.QueryExecutionHTTP;

public class wikiLink_wd {

    private static final int MAX_RETRIES = 5;
    private static final int WAIT_TIME = 10000; // 5 seconds

    static public void main(String[] args) throws Exception {

        // 出力ファイル指定
        File fileOUT = new File("output_count/wikiLink_wd.ttl");
        FileOutputStream out = new FileOutputStream(fileOUT);
        OutputStreamWriter ow = new OutputStreamWriter(out, "UTF-8");
        BufferedWriter bw = new BufferedWriter(ow);

        // 書き込みの準備
        bw.write("Disease, SymptomsCount\n");

        // `?disP`ごとの`?item`カウント用Mapを準備
        Map<String, Integer> disPCountMap = new HashMap<>();

        // WikidataのSPARQLクエリの作成
        String wikidataQueryStr = "PREFIX wdt: <http://www.wikidata.org/prop/direct/> " +
                                  "PREFIX schema: <http://schema.org/> " +
                                  "SELECT DISTINCT ?dis " +
                                  "WHERE {" +
                                  "?dis wdt:P494 ?o ." +
                                  "?wikipedia schema:about ?dis ;" +
                                  "              schema:isPartOf <https://ja.wikipedia.org/> ." +
                                  "} ";

        Query wikidataQuery = QueryFactory.create(wikidataQueryStr);
        try (QueryExecution qexecWikidata = QueryExecutionHTTP.create()
                .endpoint("https://query.wikidata.org/sparql")
                .query(wikidataQuery)
                .param("timeout", "200000")
                .build()) {

            ResultSet rsWikidata = executeWithRetry(qexecWikidata);

            while (rsWikidata.hasNext()) {
                QuerySolution qsWikidata = rsWikidata.next();
                String dis = qsWikidata.get("dis").toString();
                dis = URLDecoder.decode(dis, "UTF-8");
                String wikipedia = dis.replace("http://www.wikidata.org/entity/", "http://wikidata.dbpedia.org/resource/");

                // DBpediaのクエリ作成
                String dbpediaQueryStr = "PREFIX rdfs:<http://www.w3.org/2000/01/rdf-schema#> " +
                                         "PREFIX dbo:<http://dbpedia.org/ontology/> " +
                                         "PREFIX owl:<http://www.w3.org/2002/07/owl#> " +
                                         "PREFIX dct:<http://purl.org/dc/terms/> " +
                                         "PREFIX skos:<http://www.w3.org/2004/02/skos/core#> " +
                                         "PREFIX dbpedia-ja:<http://ja.dbpedia.org/resource/> " +
                                         "SELECT DISTINCT ?dis ?wikiPage ?symID " +
                                         "WHERE { " +
                                         "<" + wikipedia + "> owl:sameAs ?dis ." +
                                         "FILTER(contains(str(?dis),\"http://ja.dbpedia.org/\"))" +
                                         "?dis dbo:wikiPageWikiLink ?wikiPage . " +
                                         "?dis rdfs:label ?disLabel . " +
                                         "FILTER (STRLEN(STR(?disLabel)) > 0)" +
                                         "?symID owl:sameAs ?wikiPage ." +
                                         "}";

                Query dbpediaQuery = QueryFactory.create(dbpediaQueryStr);
                try (QueryExecution qexec = QueryExecutionHTTP.create()
                        .endpoint("https://ja.dbpedia.org/sparql/")
                        .query(dbpediaQuery)
                        .param("timeout", "200000")
                        .build()) {

                    ResultSet rsDBpedia = executeWithRetry(qexec);

                    while (rsDBpedia.hasNext()) {
                        QuerySolution qs = rsDBpedia.next();
                        Resource disP = qs.getResource("dis");
                        Resource symID = qs.getResource("symID");

                        if (symID != null) {
                            String symUri = symID.toString();
                            String wikiPageName = symUri.replace("http://wikidata.dbpedia.org/resource/", "");

                            // WikidataのSPARQLクエリの作成
                            String wikidataQueryStr2 = "PREFIX rdfs:<http://www.w3.org/2000/01/rdf-schema#> " +
                                                       "PREFIX wdt: <http://www.wikidata.org/prop/direct/> " +
                                                       "PREFIX wd: <http://www.wikidata.org/entity/> " +
                                                       "SELECT DISTINCT ?item " +
                                                       "WHERE { " +
                                                       "BIND(wd:" + wikiPageName + " as ?item) " +
                                                       "?item wdt:P31 wd:Q112965645 . " +
                                                       "?item rdfs:label ?itemLabel ." +
                                                       "FILTER (lang(?itemLabel) = 'ja') " +
                                                       "}";

                            Query wikidataQuery2 = QueryFactory.create(wikidataQueryStr2);
                            try (QueryExecution qexecWikidata2 = QueryExecutionHTTP.create()
                                    .endpoint("https://query.wikidata.org/sparql")
                                    .query(wikidataQuery2)
                                    .param("timeout", "200000")
                                    .build()) {

                                ResultSet rsWikidata2 = executeWithRetry(qexecWikidata2);

                                while (rsWikidata2.hasNext()) {
                                    rsWikidata2.next(); // ?itemが存在することを確認
                                    String disPUri = disP.toString();

                                    // disPのカウントをインクリメント
                                    disPCountMap.put(disPUri, disPCountMap.getOrDefault(disPUri, 0) + 1);
                                }
                            }
                        }
                    }
                }
            }
        }

        // カウント結果を多い順にソートし、ファイルに出力
        disPCountMap.entrySet()
                    .stream()
                    .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                    .forEach(entry -> {
                        try {
                            bw.write(entry.getKey() + ", " + entry.getValue() + "\n");
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    });

        bw.close();
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
