import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;


public class readRDF {

	static public void main(String[] args) throws FileNotFoundException{
	//RDFを操作する為のModelを作成
		Model model = ModelFactory.createDefaultModel() ;


		File file = new File("input/DancingMen.ttl");//読み込むRDFファイルを指定
		System.out.println(file.getName()+"...");
		

	//RDFの形式を指定して読み込む
		model.read(file.getAbsolutePath(), "TURTLE") ;
//		model.read(file.getAbsolutePath(), "RDF") ;
//		model.read(file.getAbsolutePath(),"N-TRIPLE") ;

	//読み込んだRDFの形式を変換して保存する
		FileOutputStream out;
		out = new FileOutputStream("output/sampleOUTPUT.nt");
		model.write(out,  "N-TRIPLE");
		
	/* 元のファイル名の拡張子を変えて保存したいときのサンプル
		String ftype = ".ttl"; //元の拡張子
		out = new FileOutputStream("output/"+file.getName().replaceAll(ftype, ".ttl"));
		model.write(out,  "TURTLE");

		out = new FileOutputStream("output/"+file.getName().replaceAll(ftype, ".rdf"));
		model.write(out,  "RDF/XML");

		out = new FileOutputStream("output/"+file.getName().replaceAll(ftype, ".nt"));
		model.write(out,  "N-TRIPLE");
		*/
	}
}
