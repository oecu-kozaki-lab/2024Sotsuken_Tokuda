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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.sparql.exec.http.QueryExecutionHTTP;

public class sample3_4 {

    static public void main(String[] args) throws FileNotFoundException {

        try {
            // 入力ファイル指定
            File file = new File("input/disease7_19.txt");
            // ファイルの読み込み用のReaderの設定
            BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF8"));

            // 出力ファイル指定
            File fileOUT = new File("output/category2-output.ttl");
            // 出力用のファイルのWriterの設定
            FileOutputStream out = new FileOutputStream(fileOUT);
            OutputStreamWriter ow = new OutputStreamWriter(out, "UTF-8");
            BufferedWriter bw = new BufferedWriter(ow);

            // カテゴリーごとのカウントを格納するマップ
            Map<String, Integer> categoryCountMap = new HashMap<>();

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
                                  "SELECT ?Category " +
                                  "WHERE { " +
                                  "<" + line + "> dbo:wikiPageWikiLink ?wikiPage . " +
                                  "?wikiPage dct:subject|dct:subject/skos:broader ?Category . " +
                                  "}";

                // クエリの実行
                Query query = QueryFactory.create(queryStr);
                try (QueryExecution qexec = QueryExecutionHTTP.create()
                        .endpoint("https://ja.dbpedia.org/sparql/")
                        .query(query)
                        .param("timeout", "10000")
                        .build()) {

                    ResultSet rs = qexec.execSelect();

                    while (rs.hasNext()) {
                        QuerySolution qs = rs.next();
                        Resource category = qs.getResource("Category");

                        if (category != null) {
                            String categoryUri = category.toString();
                            categoryCountMap.put(categoryUri, categoryCountMap.getOrDefault(categoryUri, 0) + 1);
                        }
                    }

                    qexec.close(); // これがないと，途中でクエリの応答がしなくなるので注意！
                }
            }

            // カテゴリーごとのカウントをリストに変換
            List<CategoryCount> categoryCounts = new ArrayList<>();
            for (Map.Entry<String, Integer> entry : categoryCountMap.entrySet()) {
                categoryCounts.add(new CategoryCount(entry.getKey(), entry.getValue()));
            }

            // リストをカウントの多い順にソート
            Collections.sort(categoryCounts, Comparator.comparingInt(CategoryCount::getCount).reversed());

            // ソートされた結果をファイルに書き込む
            for (CategoryCount categoryCount : categoryCounts) {
                bw.write(categoryCount.getCategory() + "\t" + "数: " + categoryCount.getCount() + " .\n");
            }

            // 入出力のストリームを閉じる【これを忘れると，ファイル処理が正しく終わらない】
            br.close();
            bw.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // カテゴリーごとのカウントを格納するクラス
    static class CategoryCount {
        private final String category;
        private final int count;

        public CategoryCount(String category, int count) {
            this.category = category;
            this.count = count;
        }

        public String getCategory() {
            return category;
        }

        public int getCount() {
            return count;
        }
    }
}
