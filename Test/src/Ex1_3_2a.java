import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.HashMap;

public class Ex1_3_2a {

	public static void main(String args[]) {
		//出現した単語を格納するためのHashMap
		HashMap<String,Integer> map = new HashMap<>();


		//Fileの入出力など「エラーが発生する可能性」がある場合，
		//try{．．．．}catch(．．){．．．}というエラー処理の構文を使う必要がある
		try{
			  //入力するファイルの設定
			  File file = new File("StarWars.txt");
			  BufferedReader br = new BufferedReader(new FileReader(file));

			  String str;

			  while((str = br.readLine()) != null){
				String data[] = str.split(" ");
				for(int i=0;i<data.length;i++) {
					System.out.println(data[i]);

					if(map.get(data[i])!=null) {
						System.out.println("==>"+map.get(data[i]));
						map.put(data[i],map.get(data[i])+1);
					}
					else {
						map.put(data[i],1);
					}
				}
			  }

			  br.close();

			}catch(Exception e){
				  System.out.println(e);
			}
		
		//キーの一覧を取得して出力
		 for (String key : map.keySet()) {        
	            System.out.println("キー:" + key+" = "+map.get(key));       
	        }       
	}

	
	
}
