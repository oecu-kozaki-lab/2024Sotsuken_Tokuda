import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URLDecoder;

import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.sparql.exec.http.QueryExecutionHTTP;

public class sample3_9 {

    private static final int MAX_RETRIES = 5;
    private static final int WAIT_TIME = 5000; // 5 seconds

    static public void main(String[] args) throws Exception {

        // 入力ファイル指定
        File file = new File("input/disease8_5_2.txt");
        // ファイルの読み込み用のReaderの設定
        BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF8"));

        // 出力ファイル指定
        File fileOUT = new File("output/classification3-output.ttl");
        // 出力用のファイルのWriterの設定
        FileOutputStream out = new FileOutputStream(fileOUT);
        OutputStreamWriter ow = new OutputStreamWriter(out, "UTF-8");
        BufferedWriter bw = new BufferedWriter(ow);

        while (br.ready()) {
            String line = br.readLine(); // ファイルを1行ずつ読み込む
            line = URLDecoder.decode(line, "UTF-8");
            line = line.replace("https://ja.wikipedia.org/wiki/", "http://ja.dbpedia.org/resource/");
            System.out.println(line);
            bw.write("\n" + line + "\n");

            // DBpediaのクエリの作成
            String dbpediaQueryStr = "PREFIX rdfs:<http://www.w3.org/2000/01/rdf-schema#> " +
                                     "PREFIX dbo:<http://dbpedia.org/ontology/> " +
                                     "SELECT ?wikiPage " +
                                     "WHERE { " +
                                     "<" + line + "> dbo:wikiPageWikiLink ?wikiPage . " +
                                     "}";

            // DBpediaのクエリの実行
            Query dbpediaQuery = QueryFactory.create(dbpediaQueryStr);
            try (QueryExecution qexec = QueryExecutionHTTP.create()
                    .endpoint("https://ja.dbpedia.org/sparql/")
                    .query(dbpediaQuery)
                    .param("timeout", "10000")
                    .build()) {

                ResultSet rs = executeWithRetry(qexec);

                while (rs.hasNext()) {
                    QuerySolution qs = rs.next();
                    Resource wikiPage = qs.getResource("wikiPage");

                    if (wikiPage != null) {
                        String wikiPageUri = wikiPage.toString();
                        String wikiPageName = wikiPageUri.replace("http://ja.dbpedia.org/resource/", "");

                        // WikidataのSPARQLクエリの作成
                        String wikidataQueryStr = "PREFIX rdfs:<http://www.w3.org/2000/01/rdf-schema#> " +
                        						  "PREFIX wdt: <http://www.wikidata.org/prop/direct/> " +
                        						  "PREFIX wd: <http://www.wikidata.org/entity/> " +
                                                  "PREFIX wikibase: <http://wikiba.se/ontology#> " +
                                                  "PREFIX bd: <http://www.bigdata.com/rdf#> " +
                                                  "SELECT DISTINCT ?item ?itemLabel " +
                                                  "WHERE { " +
                                                  "?item rdfs:label \"" + wikiPageName + "\"@ja . " +
                                                  "?item wdt:P31 wd:Q112965645 . " +
                                                  "SERVICE wikibase:label { bd:serviceParam wikibase:language \"[AUTO_LANGUAGE],ja\". } " +
                                                  "}";

                        // Wikidataのクエリの実行
                        Query wikidataQuery = QueryFactory.create(wikidataQueryStr);
                        try (QueryExecution qexecWikidata = QueryExecutionHTTP.create()
                                .endpoint("https://query.wikidata.org/sparql")
                                .query(wikidataQuery)
                                .param("timeout", "10000")
                                .build()) {

                            ResultSet rsWikidata = executeWithRetry(qexecWikidata);

                            //bw.write(line + "\n");
                            while (rsWikidata.hasNext()) {
                                QuerySolution qsWikidata = rsWikidata.next();
                                Resource item = qsWikidata.getResource("item");
                                String itemLabel = qsWikidata.getLiteral("itemLabel").getString();
                                bw.write("  - " + item + " (" + itemLabel + ")\n");
                            }
                        }
                    }
                }
            }
        }

        // 入出力のストリームを閉じる【これを忘れると，ファイル処理が正しく終わらない】
        br.close();
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