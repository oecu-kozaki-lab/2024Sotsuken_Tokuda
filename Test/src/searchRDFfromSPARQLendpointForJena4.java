import java.io.FileNotFoundException;
import java.io.FileOutputStream;

import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.ResultSet;
import org.apache.jena.query.ResultSetFormatter;
import org.apache.jena.sparql.exec.http.QueryExecutionHTTP;


/* SPARQL Endpoint に対するクエリ例
 * 注）Proxyの設定が必要な環境で実行するときは，実行時のJVMのオプションとして
 *      -DproxySet=true -DproxyHost=wwwproxy.osakac.ac.jp -DproxyPort=8080
 *     を追加する，
 *     Eclipseの場合「実行の構成＞引数」で設定可能
 * /
 */

public class searchRDFfromSPARQLendpointForJena4 {

	static public void main(String[] args) throws FileNotFoundException{

		//クエリの作成
		/*String queryStr = "PREFIX wd: <http://www.wikidata.org/entity/> "
				 +"            PREFIX wdt: <http://www.wikidata.org/prop/direct/> "
				 +"           PREFIX wikibase: <http://wikiba.se/ontology#>"
				+ "            PREFIX bd: <http://www.bigdata.com/rdf#>"
				+ "PREFIX schema: <http://schema.org/>"
				+ "           SELECT ?s ?sLabel ?wikipedia WHERE {"
				+ " ?s wdt:P494 ?o ."
				+ "            ?wikipedia schema:about ?s ;"
				+ "schema:isPartOf <https://ja.wikipedia.org/> . "  
				+ "              SERVICE wikibase:label { bd:serviceParam wikibase:language \"ja\". } "
				+ "       }ORDER BY DESC(?sLabel) "
				+ "LIMIT 10";
		*/
		 String queryStr = "PREFIX rdfs:<http://www.w3.org/2000/01/rdf-schema#> " +
                 "PREFIX dbo:<http://dbpedia.org/ontology/> " +
                 "PREFIX dct:<http://purl.org/dc/terms/> " +
                 "PREFIX skos:<http://www.w3.org/2004/02/skos/core#> " +
                 "PREFIX dbpedia-ja:<http://ja.dbpedia.org/resource/> " +
                 "SELECT DISTINCT ?wikiPage " +
                 "WHERE { " +
                 "?subject rdfs:label \"鼻炎\"@ja ." +
                 "?subject dbo:wikiPageWikiLink ?wikiPage . " +
                 "?wikiPage dct:subject|dct:subject/skos:broader dbpedia-ja:Category:症状と徴候 . " +
                 "}";

		Query query = QueryFactory.create(queryStr);

		 // Remote execution.
        try ( QueryExecution qExec = QueryExecutionHTTP.create()
                    .endpoint("https://ja.dbpedia.org/sparql/")
                    .query(query)
                    .param("timeout", "10000")
                    .build() ) {
        		
            // Execute.
            ResultSet rs = qExec.execSelect();
            
         // 結果の出力　※以下のどれか「１つ」を選ぶ（複数選ぶと，2つ目以降の結果が「空」になる）
            //ResultSetFormatter.out(System.out, rs, query);		//表形式で，標準出力に
	     	//ResultSetFormatter.outputAsCSV(System.out, rs);	//CSV形式で，標準出力に
	     	
            
            //ファイルに出力する場合
            try{
            	
            	//出力用のファイルの作成
    	        FileOutputStream out = new FileOutputStream("output/SPARQL-output.txt");

    	        // 結果の出力　※以下のどれか「１つ」を選ぶ（複数選ぶと，2つ目以降の結果が「空」になる）
    	     	ResultSetFormatter.out(out, rs, query); 			//表形式で，ファイルに
    	     	//ResultSetFormatter.outputAsCSV(out, rs);			//CSV形式で，ファイルに

    	     	out.close();

            } catch (Exception e) {
                e.printStackTrace();
            }
            
            
        }
        	
        	
//        	QueryExecution qexec = QueryExecutionFactory.sparqlService("https://query.wikidata.org/sparql"	, query) ;
//            ((QueryEngineHTTP)qexec).addParam("timeout", "10000") ;

           


	}
}
