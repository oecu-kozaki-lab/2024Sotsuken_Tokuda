import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.ResultSet;
import org.apache.jena.query.ResultSetFormatter;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;


public class searchRDFusingSPARQL {

	static public void main(String[] args) throws FileNotFoundException{
	//RDFを操作する為のModelを作成
		Model model = ModelFactory.createDefaultModel() ;


		File file = new File("input/DancingMen.ttl");//読み込むRDFファイルを指定
		System.out.println(file.getName()+"...");


	//RDFの形式を指定して読み込む
//		model.read(file.getAbsolutePath(), "RDF") ;
//		model.read("input/IOBC_jp_label.nt","N-TRIPLE") ;
		model.read(file.getAbsolutePath(), "TURTLE") ;


/*　以下のコードは，
 *  　https://github.com/KnowledgeGraphJapan/Apache-Jena-Sample-Programs/tree/master/src/main/java/jp/riken/accc/lod/symposium/sample
 * を参考にして実装

 * */

		//クエリの作成
		String queryStr = "select * where{?s ?p ?o.}LIMIT 100";
        Query query = QueryFactory.create(queryStr);

        //クエリの実行
        QueryExecution qexec = QueryExecutionFactory.create(query, model);

     	try {

        FileOutputStream out;
		out = new FileOutputStream("output/SPARQL-output.txt");

		// クエリの実行.
        ResultSet rs = qexec.execSelect();

        // 結果の出力　※以下のどれか「１つ」を選ぶ（複数選ぶと，2つ目以降の結果が「空」になる）
     	ResultSetFormatter.out(System.out, rs, query);		//表形式で，標準出力に
     	//ResultSetFormatter.out(out, rs, query); 			//表形式で，ファイルに
     	//ResultSetFormatter.outputAsCSV(System.out, rs);	//CSV形式で，標準出力に
     	//ResultSetFormatter.outputAsCSV(out, rs);			//CSV形式で，ファイルに

			out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}
}
