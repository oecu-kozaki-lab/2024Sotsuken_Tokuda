
public class Ex1_2_2 {

	public static void main(String[] args) {
		//処理対象の文字列を定義する→Stringクラスを使う
		String data = "A LONG TIME AGO IN A GALAXY FAR, FAR AWAY . . . .";
		
		System.out.println(data);
		
		String words[] = data.split(" ");
		
		for(int i=0 ; i<words.length ; i++) {	
			System.out.println(words[i]);				
						
		}		
	}
	
}

