package data_count;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.net.URLDecoder;

import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.sparql.exec.http.QueryExecutionHTTP;

public class dbp_sym {

    private static final int MAX_RETRIES = 5;
    private static final int WAIT_TIME = 5000; // 5 seconds

    static public void main(String[] args) throws Exception {

        // 出力ファイル指定
        File fileOUT = new File("output_count/dbp_sym.ttl");
        // 出力用のファイルのWriterの設定
        FileOutputStream out = new FileOutputStream(fileOUT);
        OutputStreamWriter ow = new OutputStreamWriter(out, "UTF-8");
        BufferedWriter bw = new BufferedWriter(ow);

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
                                  "} ORDER BY DESC(?dis)";

        // Wikidataのクエリの実行
        Query wikidataQuery = QueryFactory.create(wikidataQueryStr);
        try (QueryExecution qexecWikidata = QueryExecutionHTTP.create()
                .endpoint("https://query.wikidata.org/sparql")
                .query(wikidataQuery)
                .param("timeout", "10000")
                .build()) {

            ResultSet rsWikidata = executeWithRetry(qexecWikidata);
            bw.write ("Disease, SymptomsCount\n");
            
            while (rsWikidata.hasNext()) {
                QuerySolution qsWikidata = rsWikidata.next();
                String dis = qsWikidata.get("dis").toString();
                dis = URLDecoder.decode(dis, "UTF-8");
                String wikipedia = dis.replace("http://www.wikidata.org/entity/", "http://wikidata.dbpedia.org/resource/");
                
                // DBpediaのクエリの作成
                String dbpediaQueryStr = "PREFIX rdfs:<http://www.w3.org/2000/01/rdf-schema#> " +
                                         "PREFIX prop-ja: <http://ja.dbpedia.org/property/> " +
                                         "PREFIX owl:<http://www.w3.org/2002/07/owl#> " +
                                         "SELECT ?dis (COUNT(DISTINCT ?symPage) AS ?count) " +
                                         "WHERE { " +
                                         "<" + wikipedia + "> owl:sameAs ?dis ." +
                                         "FILTER(contains(str(?dis),\"http://ja.dbpedia.org/\")) " +
                                         "?dis rdfs:label ?disLabel ." +
                                         "?dis prop-ja:symptoms ?symPage ." +
                                         "FILTER (isIRI(?symPage)) " +
                                         "?symPage rdfs:label ?symLabel . "+
                                         "} GROUP BY ?dis ORDER BY DESC(?count)";
                
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
                        
                        // 出力形式の調整
                        bw.write(disP +", "+ count+"\n");
                    }
                }
            }   
        }
                
        // 入出力のストリームを閉じる
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
