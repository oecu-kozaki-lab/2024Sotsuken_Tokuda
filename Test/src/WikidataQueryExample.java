import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.sparql.exec.http.QueryExecutionHTTP;

public class WikidataQueryExample {

    public static void main(String[] args) {
        // WikidataのSPARQLエンドポイント
        String service = "https://query.wikidata.org/sparql";
        
        // SPARQLクエリの定義
        String sparqlQuery = 
            "PREFIX wd: <http://www.wikidata.org/entity/> " +
            "PREFIX wdt: <http://www.wikidata.org/prop/direct/> " +
            "PREFIX wikibase: <http://wikiba.se/ontology#> " +
            "PREFIX bd: <http://www.bigdata.com/rdf#> " +
            "SELECT ?item ?itemLabel WHERE { " +
            "  ?item wdt:P31 wd:Q146 . " + // インスタンスが猫
            "  SERVICE wikibase:label { bd:serviceParam wikibase:language \"en\". } " +
            "} LIMIT 10";
        
        // クエリの作成
        Query query = QueryFactory.create(sparqlQuery);
        
        // クエリの実行
        try ( QueryExecution qExec = QueryExecutionHTTP.create()
                    .endpoint("https://query.wikidata.org/sparql")
                    .query(query)
                    .param("timeout", "10000")
                    .build() ) {
        		
            ResultSet results = qExec.execSelect();
            
            // 結果の表示
            while (results.hasNext()) {
                QuerySolution soln = results.nextSolution();
                System.out.println(soln.get("item") + " - " + soln.get("itemLabel"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
