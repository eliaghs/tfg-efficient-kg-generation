
// javac -cp "../stanford-corenlp-4.5.8/*" SimpleOpenIE.java
// java -cp ".;../stanford-corenlp-4.5.8/*" SimpleOpenIE

import edu.stanford.nlp.ie.util.RelationTriple;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.naturalli.NaturalLogicAnnotations;
import edu.stanford.nlp.util.CoreMap;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class SimpleOpenIE {

  public static void main(String[] args) throws Exception {
    String filePath = "reference_texts\\Utilitarianism.txt";  
    String outputFilePath = "Utilitarianism_Sentences.csv";  
    String text = readFileAsString(filePath);

    // Configurar el pipeline de Stanford CoreNLP
    Properties props = new Properties();
    // props.setProperty("annotators", "tokenize,ssplit,pos,lemma,ner,parse,depparse,natlog,openie");  

    // Activar resolución de coreferencias
    props.setProperty("annotators", "tokenize,ssplit,pos,lemma,ner,parse,depparse,coref,natlog,openie");
    props.setProperty("openie.resolve_coref", "true");


    StanfordCoreNLP pipeline = new StanfordCoreNLP(props);

    // Procesar el texto con el pipeline
    Annotation doc = new Annotation(text);
    pipeline.annotate(doc);

    // StringBuilder para construir el resultado del CSV
    StringBuilder result = new StringBuilder();
    result.append("sujeto;predicado;objeto;confianza;frase\n");
    // result.append("sujeto;predicado;objeto\n");


    // Extraer las oraciones
    List<CoreMap> sentences = doc.get(CoreAnnotations.SentencesAnnotation.class);

    // Contar las tripletas
    int tripletCount = 0;

    // Extraer tripletas de OpenIE
    for (CoreMap sentence : sentences) {
      // Obtener las tripletas de OpenIE
      Collection<RelationTriple> triples = sentence.get(NaturalLogicAnnotations.RelationTriplesAnnotation.class);
      
      // Iterar sobre las tripletas encontradas
      for (RelationTriple triple : triples) {
        // Extraer los componentes de la tripleta
        String sujeto = triple.subjectLemmaGloss().replace("\"", "\"\"");
        String predicado = triple.relationLemmaGloss().replace("\"", "\"\"");
        String objeto = triple.objectLemmaGloss().replace("\"", "\"\"");
        double confianza = triple.confidence;

        // Crear una línea CSV para esta tripleta
        String tripletaCSV = String.format(Locale.US,
          "\"%s\";\"%s\";\"%s\";%.6f;\"%s\"",
          sujeto, predicado, objeto, confianza, sentence.toString().replace("\"", "\"\""));

        // String tripletaCSV = String.format(Locale.US,
        //   "\"%s\";\"%s\";\"%s\"",
        //   sujeto, predicado, objeto);



        result.append(tripletaCSV).append("\n");
        tripletCount++;
      }
    }

    // Escribir el resultado en el archivo CSV
    writeToFile(outputFilePath, result.toString());

    System.out.println("Total de tripletas generadas: " + tripletCount);
    System.out.println("Tripletas guardadas en: " + outputFilePath);
  }

  // Función para leer el archivo de texto
  public static String readFileAsString(String filePath) throws IOException {
    List<String> lines = Files.readAllLines(Paths.get(filePath));
    return String.join(" ", lines);
  }

  // Función para escribir el resultado en el archivo CSV
  public static void writeToFile(String filePath, String content) throws IOException {
    Files.write(Paths.get(filePath), content.getBytes());
  }
}
