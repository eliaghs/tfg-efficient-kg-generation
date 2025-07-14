// javac -cp "../stanford-corenlp-4.5.8/*" OpenIECoref.java
// java -cp ".;../stanford-corenlp-4.5.8/*" OpenIECoref

import edu.stanford.nlp.ie.util.RelationTriple;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.naturalli.NaturalLogicAnnotations;
import edu.stanford.nlp.util.CoreMap;
import edu.stanford.nlp.coref.data.CorefChain;
import edu.stanford.nlp.coref.data.CorefChain.CorefMention;
import edu.stanford.nlp.coref.CorefCoreAnnotations;
import edu.stanford.nlp.util.IntPair;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;

public class OpenIECoref {

  public static void main(String[] args) throws Exception {
    String filePath = "reference_texts\\Inflation_cleaned.txt";
    String outputFilePath = "Inflation_S4.csv";

    String text = readFileAsString(filePath);

    Properties props = new Properties();
    props.setProperty("annotators", "tokenize,ssplit,pos,lemma,ner,parse,depparse,coref,natlog,openie");

    StanfordCoreNLP pipeline = new StanfordCoreNLP(props);

    Annotation doc = new Annotation(text);
    pipeline.annotate(doc);

    Map<IntPair, String> mentionToRepresentative = new HashMap<>();

    Map<Integer, CorefChain> corefChains = doc.get(CorefCoreAnnotations.CorefChainAnnotation.class);
    for (CorefChain chain : corefChains.values()) {
      CorefMention rep = chain.getRepresentativeMention();
      String repText = getMentionText(doc, rep);

      for (CorefMention mention : chain.getMentionsInTextualOrder()) {
        if (mention == rep) continue;
        IntPair key = new IntPair(mention.sentNum, mention.startIndex);
        mentionToRepresentative.put(key, repText);
      }
    }

    StringBuilder result = new StringBuilder();
    // result.append("sujeto;predicado;objeto\n");
    result.append("sujeto;predicado;objeto;confianza;frase\n");
    int tripletCount = 0;

    List<CoreMap> sentences = doc.get(CoreAnnotations.SentencesAnnotation.class);
    for (CoreMap sentence : sentences) {
      String fraseOriginal = sentence.toString().replace("\"", "\"\"");  // Comentado
      Collection<RelationTriple> triples = sentence.get(NaturalLogicAnnotations.RelationTriplesAnnotation.class);
      int sentNum = sentence.get(CoreAnnotations.SentenceIndexAnnotation.class) + 1;

      for (RelationTriple triple : triples) {
        String sujeto = triple.subjectLemmaGloss().replace("\"", "\"\"");
        String predicado = triple.relationLemmaGloss().replace("\"", "\"\"");
        String objeto = triple.objectLemmaGloss().replace("\"", "\"\"");
        double confianza = triple.confidence;  // Comentado

        IntPair subjKey = new IntPair(sentNum, triple.subjectTokenSpan().first + 1);
        IntPair objKey = new IntPair(sentNum, triple.objectTokenSpan().first + 1);

        if (mentionToRepresentative.containsKey(subjKey)) {
          sujeto = mentionToRepresentative.get(subjKey).replace("\"", "\"\"");
        }

        if (mentionToRepresentative.containsKey(objKey)) {
          objeto = mentionToRepresentative.get(objKey).replace("\"", "\"\"");
        }

        // CSV solo con sujeto, predicado y objeto
        // String tripletaCSV = String.format(Locale.US,
        //   "\"%s\";\"%s\";\"%s\"",
        //   sujeto, predicado, objeto);

        String tripletaCSV = String.format(Locale.US,
          "\"%s\";\"%s\";\"%s\";%.3f;\"%s\"",
          sujeto, predicado, objeto, confianza, fraseOriginal);

        result.append(tripletaCSV).append("\n");
        tripletCount++;
      }
    }

    writeToFile(outputFilePath, result.toString());

    System.out.println("Total de tripletas generadas: " + tripletCount);
    System.out.println("Tripletas guardadas en: " + outputFilePath);
  }

  public static String readFileAsString(String filePath) throws IOException {
    List<String> lines = Files.readAllLines(Paths.get(filePath));
    return String.join(" ", lines);
  }

  public static void writeToFile(String filePath, String content) throws IOException {
    Files.write(Paths.get(filePath), content.getBytes(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
  }

  public static String getMentionText(Annotation doc, CorefMention mention) {
    List<CoreMap> sentences = doc.get(CoreAnnotations.SentencesAnnotation.class);
    CoreMap sentence = sentences.get(mention.sentNum - 1);
    StringBuilder sb = new StringBuilder();
    for (int i = mention.startIndex - 1; i < mention.endIndex - 1; i++) {
      sb.append(sentence.get(CoreAnnotations.TokensAnnotation.class).get(i).originalText());
      sb.append(" ");
    }
    return sb.toString().trim();
  }
}
