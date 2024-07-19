
public class Ex1_2_1 {

	public static void main(String[] args) {
		//処理対象の文字列を定義する→Stringクラスを使う
		String data = "A LONG TIME AGO IN A GALAXY FAR, FAR AWAY . . . .";
		
		System.out.println(data);
		
		//「A」が最初に出てくるのインデックス（何文字目か）を調べる
		int index = data.indexOf("A");
		System.out.println("「A」が最初に出てくるインデックス＝"+index);

		int c=0;//出現回数を数えるための変数
		
		//最大data.length（文字列の長さ）まで繰り返し
		//参考：while(true) としてもよい（無限ループ）良いがint iの扱いに注意が必要 
		for(int i=0 ; i<data.length() ; i++) {	
			//「A」が「c」以降に出現するインデックスを調べる
			i = data.indexOf("A", i); 
			if(i>=0) {
				c++;
				System.out.println("「A」が"+c+"番目に出てくるインデックス＝"+i);				
			}
			else {
				//「A」が出現しないときはインデックスは-1となるので，forを抜ける
				break;
			}			
		}
		
		System.out.println(data+"\nでの「A」の出現回数は "+c+" 回");	
		
		}
}

