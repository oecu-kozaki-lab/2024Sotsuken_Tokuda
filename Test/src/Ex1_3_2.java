import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.HashMap;

public class Ex1_3_2 {

	public static void main(String args[]) {
		//出現した単語を格納するためのHashMap
		HashMap<String,Integer> map = new HashMap<>();


		//Fileの入出力など「エラーが発生する可能性」がある場合，
		//try{．．．．}catch(．．){．．．}というエラー処理の構文を使う必要がある
		try{
			  //入力するファイルの設定
			  File file = new File("input/StarWars.txt");
			  BufferedReader br = new BufferedReader(new FileReader(file));

			//出力するファイルの設定
			  File file_out = new File("output/output3-2.txt");
			  BufferedWriter bw = new BufferedWriter(new FileWriter(file_out));


			  String str;
			  String key;

			  while((str = br.readLine()) != null){
				//「,」「.」も削除するために正規表現でsplitの区切り文字を複数指定
				//　→その際「.」は正規表現の特殊文字のため「\\.」でエスケープする
				String data[] = str.split(" |\\.|,");
				for(int i=0;i<data.length;i++) {
					key = data[i];
					System.out.println(data[i]+"\t"+key);

					//このまま処理すると""（長さ0の文字列）も切り出されるのでkeyの対象から外す
					if(!key.equals("")) {
						if(map.get(key)!=null) {
							System.out.println("==>"+map.get(key));
							map.put(key,map.get(key)+1);
						}
						else {
							map.put(key,1);
						}
					}
				}
			  }

			  br.close();

			//キーの一覧を取得してファイル出力
			//map.keySet()でキーの一覧をSetとして取得
			//Setを1つ目から順にfor文で回すには下記のような書き方をする
			 for (String k : map.keySet()) {
				 bw.write(k+" = "+map.get(k)+"\n");
		        }

			 bw.close();

			}catch(Exception e){
				  System.out.println(e);
			}




	}



}
