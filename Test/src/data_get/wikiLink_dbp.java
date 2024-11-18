package data_get;
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

public class wikiLink_dbp {

    private static final int MAX_RETRIES = 5;
    private static final int WAIT_TIME = 5000; // 5 seconds

    static public void main(String[] args) throws Exception {

        // 出力ファイル指定
        File fileOUT = new File("output_dis/wikiLink_dbp.ttl");
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
                                  "ORDER BY DESC(?dis)" ;  
        
        // Wikidataのクエリの実行
        Query wikidataQuery = QueryFactory.create(wikidataQueryStr);
        try (QueryExecution qexecWikidata = QueryExecutionHTTP.create()
                .endpoint("https://query.wikidata.org/sparql")
                .query(wikidataQuery)
                .param("timeout", "10000")
                .build()) {

            ResultSet rsWikidata = executeWithRetry(qexecWikidata);
            bw.write ("@prefix owl: <http://www.w3.org/2002/07/owl#> .\n"
            		+ "@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> .\n"
            		+ "@prefix wikiLink_dbp: <https://hozo.jp/dis/prop/wikiLink/dbp/> .\n");
            
            while (rsWikidata.hasNext()) {
                QuerySolution qsWikidata = rsWikidata.next();
                String dis = qsWikidata.get("dis").toString();
                dis = URLDecoder.decode(dis, "UTF-8");
                String wikipedia = dis.replace("http://www.wikidata.org/entity/", "http://wikidata.dbpedia.org/resource/");
                //bw.write (wikipedia+"\n");
                
                // DBpediaのクエリの作成
                String dbpediaQueryStr = "PREFIX rdfs:<http://www.w3.org/2000/01/rdf-schema#> " +
                                         "PREFIX dbo:<http://dbpedia.org/ontology/> " +
                                         "PREFIX owl:<http://www.w3.org/2002/07/owl#> " +
                                         "PREFIX dct:<http://purl.org/dc/terms/> " +
                                         "PREFIX skos:<http://www.w3.org/2004/02/skos/core#> " +
                                         "PREFIX dbpedia-ja:<http://ja.dbpedia.org/resource/> " +
                                         "SELECT ?dis ?wikiPage ?disLabel ?symLabel ?symID ?disID " +
                                         "WHERE { " +
                                         "<" + wikipedia + "> owl:sameAs ?dis ." +
                                         "FILTER(contains(str(?dis),\"http://ja.dbpedia.org/\"))" +
                                         "?dis dbo:wikiPageWikiLink ?wikiPage . " +
                                         "?dis rdfs:label ?disLabel . " +
                                         "FILTER (STRLEN(STR(?disLabel)) > 0)" +
                                         "?wikiPage dct:subject|dct:subject/skos:broader dbpedia-ja:Category:症状と徴候 ."+
                                         "?wikiPage rdfs:label ?symLabel . "+
                                         "FILTER (STRLEN(STR(?symLabel)) > 0)" +
                                         "OPTIONAL{ " +
                                         "?symID owl:sameAs ?wikiPage  ."+
                                         "} "+
                                         "?disID owl:sameAs ?dis ."+
                                         "}";
                
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
                        Resource wikiPage = qs.getResource("wikiPage");
                        Resource symID = qs.getResource("symID");
                        Resource disID = qs.getResource("disID");
                        String disLabel = qs.getLiteral("disLabel").getString();
                        String symLabel = qs.getLiteral("symLabel").getString();

                        // Convert URIs
                        String disUri = disID != null ? disID.toString() : "";
                        String disName = disUri.replace("http://wikidata.dbpedia.org/resource/", "http://www.wikidata.org/entity/");
                        String symUri = symID != null ? symID.toString() : "";
                        String symName = symUri.replace("http://wikidata.dbpedia.org/resource/", "http://www.wikidata.org/entity/");

                        // Write to output file
                        bw.write("<" + dis + "> owl:sameAs <" + disP + "> .\n" +
                                 "<" + disP + "> owl:sameAs <" + disName + "> .\n" +
                                 "<" + wikiPage + "> owl:sameAs <" + symName + "> .\n" +
                                 "<" + disP + "> wikiLink_dbp:sym <" + wikiPage + "> .\n" +
                                 "<" + disP + "> rdfs:label \"" + disLabel + "\"@ja .\n" +
                                 "<" + wikiPage + "> rdfs:label \"" + symLabel + "\"@ja .\n");
                    }
                }
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