import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
//import org.apache.jena.rdf.model.Resource;
import org.apache.jena.sparql.exec.http.QueryExecutionHTTP;

public class sample3_2 {

    static public void main(String[] args) throws FileNotFoundException {

        try {
            // 入力ファイル指定
            File file = new File("input/disease7_19.txt");
            // ファイルの読み込み用のReaderの設定
            BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF8"));

            // 出力ファイル指定
            File fileOUT = new File("output/disease2-output.ttl");
            // 出力用のファイルのWriterの設定
            FileOutputStream out = new FileOutputStream(fileOUT);
            OutputStreamWriter ow = new OutputStreamWriter(out, "UTF-8");
            BufferedWriter bw = new BufferedWriter(ow);

            // 結果を格納するリスト
            List<DiseaseSymptomCount> resultList = new ArrayList<>();

            while (br.ready()) {
                String line = br.readLine(); // ファイルを1行ずつ読み込む
                line = URLDecoder.decode(line, "UTF-8");
                line = line.replace("https://ja.wikipedia.org/wiki/", "http://ja.dbpedia.org/resource/");
                System.out.println(line);

                // クエリの作成
                String queryStr = "PREFIX rdfs:<http://www.w3.org/2000/01/rdf-schema#> " +
                                  "PREFIX dbo:<http://dbpedia.org/ontology/> " +
                                  "PREFIX dct:<http://purl.org/dc/terms/> " +
                                  "PREFIX skos:<http://www.w3.org/2004/02/skos/core#> " +
                                  "PREFIX dbpedia-ja:<http://ja.dbpedia.org/resource/> " +
                                  "SELECT (COUNT(?wikiPage) AS ?symptomCount) " +
                                  "WHERE { " +
                                  "<" + line + "> dbo:wikiPageWikiLink ?wikiPage . " +
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

                    if (rs.hasNext()) {
                        QuerySolution qs = rs.next();
                        int symptomCount = qs.getLiteral("symptomCount").getInt();

                        // 結果をリストに追加
                        resultList.add(new DiseaseSymptomCount(line, symptomCount));
                    }

                    qexec.close(); // これがないと，途中でクエリの応答がしなくなるので注意！
                }
            }

            // リストを症状と徴候の数が多い順にソート
            Collections.sort(resultList, Comparator.comparingInt(DiseaseSymptomCount::getSymptomCount).reversed());

            // ソートされた結果をファイルに書き込む
            for (DiseaseSymptomCount result : resultList) {
                bw.write(result.getDisease() + "\t" + "症状と徴候の数: " + result.getSymptomCount() + " .\n");
            }

            // 入出力のストリームを閉じる【これを忘れると，ファイル処理が正しく終わらない】
            br.close();
            bw.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 結果を格納するクラス
    static class DiseaseSymptomCount {
        private final String disease;
        private final int symptomCount;

        public DiseaseSymptomCount(String disease, int symptomCount) {
            this.disease = disease;
            this.symptomCount = symptomCount;
        }

        public String getDisease() {
            return disease;
        }

        public int getSymptomCount() {
            return symptomCount;
        }
    }
}
