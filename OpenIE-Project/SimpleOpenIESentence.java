// javac -cp "../stanford-corenlp-4.5.8/*" SimpleOpenIESentence.java
// java -cp ".;../stanford-corenlp-4.5.8/*" SimpleOpenIESentence

import edu.stanford.nlp.ie.util.RelationTriple;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.naturalli.NaturalLogicAnnotations;
import edu.stanford.nlp.util.CoreMap;

import java.util.*;

public class SimpleOpenIESentence {

  public static void main(String[] args) throws Exception {
    // 👉 Aquí introduces la frase que quieras analizar
    String text = "resulting in increased aggregate demand and potential inflationary pressures.";

    // Configurar el pipeline de Stanford CoreNLP
    Properties props = new Properties();
    props.setProperty("annotators", "tokenize,ssplit,pos,lemma,depparse,natlog,openie");

    StanfordCoreNLP pipeline = new StanfordCoreNLP(props);

    // Procesar el texto
    Annotation doc = new Annotation(text);
    pipeline.annotate(doc);

    // Extraer las oraciones
    List<CoreMap> sentences = doc.get(CoreAnnotations.SentencesAnnotation.class);

    // Extraer tripletas de cada oración
    for (CoreMap sentence : sentences) {
      Collection<RelationTriple> triples = sentence.get(NaturalLogicAnnotations.RelationTriplesAnnotation.class);

      for (RelationTriple triple : triples) {
        String sujeto = triple.subjectLemmaGloss();
        String predicado = triple.relationLemmaGloss();
        String objeto = triple.objectLemmaGloss();
        double confianza = triple.confidence;

        System.out.printf("(%.2f) (%s, %s, %s)%n", confianza, sujeto, predicado, objeto);
      }
    }
  }
}
