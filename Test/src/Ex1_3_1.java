import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class Ex1_3_1 {

	public static void main(String args[]) {
		//Fileの入出力など「エラーが発生する可能性」がある場合，
		//try{．．．．}catch(．．){．．．}というエラー処理の構文を使う必要がある
		try{
			  //入力するファイルの設定
			  File file = new File("input/StarWars.txt");
			  BufferedReader br = new BufferedReader(new FileReader(file));

			  //出力するファイルの設定
			  File file_out = new File("output.txt");
			  BufferedWriter bw = new BufferedWriter(new FileWriter(file_out));


			  String str;
			  int i = 1;

			  while((str = br.readLine()) != null){
			    System.out.println(str);
			    bw.write(i+"\t"+str+"\n");
			    i++;
				//bw.newLine();  //"\n"を出力する代わりにこちらのメソッドを使っても良い
			  }

			  br.close();
			  bw.close();//close()を忘れると正しく出力が終わらない

			}catch(FileNotFoundException e){
			  System.out.println(e);
			}catch(IOException e){
			  System.out.println(e);
			}
//		catch(){}によるエラー処理は，エラーの種類毎に書く以外に，
//		下記のように「すべてのエラー」をまとめて書くことも可能
//			catch(Exception e){
//				  System.out.println(e);
//			}
	}

}
