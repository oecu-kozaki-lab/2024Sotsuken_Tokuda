package data_get;
//import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
//import java.io.FileInputStream;
import java.io.FileOutputStream;
//import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
//import org.apache.jena.rdf.model.Resource;
import org.apache.jena.sparql.exec.http.QueryExecutionHTTP;

public class 上位クラス_dis {

    private static final int MAX_RETRIES = 5;
    private static final int WAIT_TIME = 5000; // 5 seconds

    static public void main(String[] args) throws Exception {

        // 出力ファイル指定
        File fileOUT = new File("output_dis/上位クラス_dis.ttl");
        // 出力用のファイルのWriterの設定
        FileOutputStream out = new FileOutputStream(fileOUT);
        OutputStreamWriter ow = new OutputStreamWriter(out, "UTF-8");
        BufferedWriter bw = new BufferedWriter(ow);

        // WikidataのSPARQLクエリの作成
        String wikidataQueryStr = "PREFIX rdfs:<http://www.w3.org/2000/01/rdf-schema#> " +
        						  "PREFIX wdt: <http://www.wikidata.org/prop/direct/> " +
        						  "PREFIX wd: <http://www.wikidata.org/entity/> " +
                                  "PREFIX schema: <http://schema.org/> " +
                                  "PREFIX bd: <http://www.bigdata.com/rdf#> " +
                                  "PREFIX skos: <http://www.w3.org/2004/02/skos/core#> " +
                                  "SELECT DISTINCT ?dis ?disLabel "+
                                  "WHERE {" +
                                  "?dis wdt:P494 ?o ."  +       // ?dis（疾患）にP494のプロパティを持つもの
                                  "?wikipedia schema:about ?dis ;" +
                                  "			  schema:isPartOf <https://ja.wikipedia.org/> ." +
                                  "?dis rdfs:label ?disLabel ." +
                                  "FILTER (lang(?disLabel) = 'ja') "+
                                  "}"+
                                  "ORDER BY DESC(?disLabel)" ;     
        
        // Wikidataのクエリの実行
        Query wikidataQuery = QueryFactory.create(wikidataQueryStr);
        try (QueryExecution qexecWikidata = QueryExecutionHTTP.create()
                .endpoint("https://query.wikidata.org/sparql")
                .query(wikidataQuery)
                .param("timeout", "10000")
                .build()) {

            ResultSet rsWikidata = executeWithRetry(qexecWikidata);
            bw.write ("@prefix wd: <http://www.wikidata.org/entity/> .\n"
            		+ "@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> .\n"
            		+ "@prefix owl: <http://www.w3.org/2002/07/owl#> .\n");
            
            while (rsWikidata.hasNext()) {
                QuerySolution qsWikidata = rsWikidata.next();
                String dis = qsWikidata.get("dis").toString();
                String dis_d =dis.replace("http://www.wikidata.org/entity/", "https://hozo.jp/dis/disease/");
                dis =dis.replace("http://www.wikidata.org/entity/", "wd:");
                String disLabel = qsWikidata.getLiteral("disLabel").getString();
                bw.write (dis+" rdfs:label \""+disLabel+"\"@ja .\n"
                		+ "<" + dis_d + "> rdfs:label \""+disLabel+"\"@ja .\n"
                		+"<" + dis_d+ "> owl:sameAs " + dis + " .\n");
            }
         }
         
       // 入出力のストリームを閉じる【これを忘れると，ファイル処理が正しく終わらない】
       // br.close();
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