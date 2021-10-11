package com.eftlab.mkdocbook;

import java.io.File;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Templates;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.stream.StreamSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Node;
import org.xml.sax.InputSource;

import com.icl.saxon.FeatureKeys;
import com.icl.saxon.TransformerFactoryImpl;

public class App {
  public static void main(String[] args) throws Exception {
    int offset = args.length > 0 && args[0].equals(App.class.getName()) ? 1 : 0;

    if (args.length - offset != 3) {
      System.err.println("Usage:");
      System.err.println("  java " + App.class.getName() + " xmlFileName xsltFileName outFileName");
      System.exit(1);
    }

    //System.setProperty("javax.xml.parsers.SAXParserFactory", "com.icl.saxon.aelfred.SAXParserFactoryImpl");
    //System.setProperty("javax.xml.parsers.DocumentBuilderFactory", "com.icl.saxon.om.DocumentBuilderFactoryImpl");
    //System.setProperty("javax.xml.transform.TransformerFactory", "com.icl.saxon.TransformerFactoryImpl");
    //String catalogXml = App.class.getClassLoader().getResource("docbook/catalog.xml").toString();
    //System.setProperty("xml.catalog.files", catalogXml);
    //System.setProperty("xml.catalog.verbosity", "99");

    File xmlFile = new File(args[offset + 0]);
    File xslFile = new File(args[offset + 1]);
    File outFile = new File(args[offset + 2]);

    System.out.println("XML file: " + xmlFile);
    System.out.println("XSL file: " + xslFile);
    System.out.println("Output file: " + outFile);

    TransformerFactory tfactory = new TransformerFactoryImpl();
    tfactory.setAttribute(FeatureKeys.SOURCE_PARSER_CLASS, "org.apache.xml.resolver.tools.ResolvingXMLReader");
    tfactory.setAttribute(FeatureKeys.STYLE_PARSER_CLASS, "org.apache.xml.resolver.tools.ResolvingXMLReader");
    tfactory.setURIResolver(new org.apache.xml.resolver.tools.CatalogResolver());
    //tfactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
    //tfactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "file");
    //tfactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "file");
    //tfactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "file");

    StreamSource xslSource = new StreamSource(xslFile);
    Templates templates = tfactory.newTemplates(xslSource);
    Transformer transformer = templates.newTransformer();

    StreamSource xmlSource = new StreamSource(xmlFile);
    StreamResult result = new StreamResult(outFile);
    transformer.transform(xmlSource, result);
  }
}
