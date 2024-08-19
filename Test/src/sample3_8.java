import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
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

public class sample3_8 {

    static public void main(String[] args) throws FileNotFoundException {

        try {
            // 入力ファイル指定
            File file = new File("input/disease8_5.txt");
            // ファイルの読み込み用のReaderの設定
            BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF8"));

            // 出力ファイル指定
            File fileOUT = new File("output/select_uri-output.ttl");
            // 出力用のファイルのWriterの設定
            FileOutputStream out = new FileOutputStream(fileOUT);
            OutputStreamWriter ow = new OutputStreamWriter(out, "UTF-8");
            BufferedWriter bw = new BufferedWriter(ow);

            while (br.ready()) {
                String line = br.readLine(); // ファイルを1行ずつ読み込む
                line = URLDecoder.decode(line, "UTF-8");
                line =line.replace("https://ja.wikipedia.org/wiki/","http://ja.dbpedia.org/resource/")	 ;  
                System.out.println(line);
                
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
                	
                    ResultSet rs = qexec.execSelect();

                    int n = 0;
                    while (rs.hasNext()) {
                        QuerySolution qs = rs.next();
                        Resource res = qs.getResource("wikiPage"); // クエリ内の変数名に一致させる
                        if (res != null) {
                            // 入力したwordの出力
                            if (n == 0) {
                                bw.write(line + "\n\n");// \t→\n
                            }

                            // 情報の出力
                            //bw.write("<" + res.toString() + ">");
                            bw.write(res.toString()+"\n");
                            // 症状が複数の場合は，「;」でつなぐ．最後は「.」
                            if (rs.hasNext()) {
                                bw.write("");
                            } else {
                                bw.write("\n");
                            }
                            n++;
                        }
                    }

                    qexec.close(); // これがないと，途中でクエリの応答がしなくなるので注意！
                }
            }

            // 入出力のストリームを閉じる【これを忘れると，ファイル処理が正しく終わらない】
            br.close();
            bw.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
