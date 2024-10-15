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

public class sample3_1 {

    static public void main(String[] args) throws FileNotFoundException {

        try {
            // 入力ファイル指定
            File file = new File("input/disease7_19.txt");
            // ファイルの読み込み用のReaderの設定
            BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF8"));

            // 出力ファイル指定
            File fileOUT = new File("output/disease-output.ttl");
            // 出力用のファイルのWriterの設定
            FileOutputStream out = new FileOutputStream(fileOUT);
            OutputStreamWriter ow = new OutputStreamWriter(out, "UTF-8");
            BufferedWriter bw = new BufferedWriter(ow);

            while (br.ready()) {
                String line = br.readLine(); // ファイルを1行ずつ読み込む
                line = URLDecoder.decode(line, "UTF-8");
                line =line.replace("https://ja.wikipedia.org/wiki/","http://ja.dbpedia.org/resource/")	 ;  
                System.out.println(line);
                
                // クエリの作成
                String queryStr = "PREFIX rdfs:<http://www.w3.org/2000/01/rdf-schema#> " +
                                  "PREFIX dbo:<http://dbpedia.org/ontology/> " +
                                  "PREFIX dct:<http://purl.org/dc/terms/> " +
                                  "PREFIX skos:<http://www.w3.org/2004/02/skos/core#> " +
                                  "PREFIX dbpedia-ja:<http://ja.dbpedia.org/resource/> " +
                                  "SELECT DISTINCT ?wikiPage " +
                                  "WHERE { " +
                                  "<" + line + "> dbo:wikiPageWikiLink ?wikiPage ."+
                                  "?wikiPage dct:subject|dct:subject/skos:broader dbpedia-ja:Category:症状と徴候 . " +
                                  "}";

                // クエリの実行
                Query query = QueryFactory.create(queryStr);
                try (QueryExecution qexec = QueryExecutionHTTP.create()
                        .endpoint("https://ja.dbpedia.org/sparql/")
                        .query(query)
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
                                bw.write(line + "\t");
                            }

                            // 情報の出力
                            bw.write("<" + res.toString() + ">");
                            // 症状が複数の場合は，「;」でつなぐ．最後は「.」
                            if (rs.hasNext()) {
                                bw.write(" ; \n");
                            } else {
                                bw.write(" . \n");
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
