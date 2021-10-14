package com.eftlab.mkdocbook;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.net.URL;

import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamSource;

import com.icl.saxon.TransformerFactoryImpl;

import org.apache.fop.apps.Fop;
import org.apache.fop.apps.FopFactory;
import org.apache.fop.apps.FopFactoryBuilder;
import org.apache.fop.apps.MimeConstants;
import org.apache.xml.resolver.CatalogManager;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

public class App {

  public static void main(String[] args) throws Exception {
    new App().run(args);
  }

  public void run(String[] args) throws Exception {
    int offset = args.length > 0 && args[0].equals(getClass().getName()) ? 1 : 0;

    if (args.length - offset != 3) {
      System.err.println("Usage:");
      System.err.println("  java " + getClass().getName() + " xmlFileName xsltFileName outFileName");
      System.exit(1);
    }

    File xmlFile = new File(args[offset + 0]);
    File xslFile = new File(args[offset + 1]);
    File outFile = new File(args[offset + 2]);

    System.out.println("XML file: " + xmlFile);
    System.out.println("XSL file: " + xslFile);
    System.out.println("Output file: " + outFile);

    initCatalogManager();

    FopFactoryBuilder fopBuilder = new FopFactoryBuilder(new File(".").toURI());
    FopFactory fopFactory = fopBuilder.build();
    OutputStream out = new BufferedOutputStream(new FileOutputStream(outFile));
    Fop fop = fopFactory.newFop(MimeConstants.MIME_PDF, out);

    TransformerFactory tfactory = new TransformerFactoryImpl();
    tfactory.setURIResolver(new CatalogResolver());

    Source xslSource = getInputSource(xslFile);
    Templates templates = tfactory.newTemplates(xslSource);

    Source xmlSource = getInputSource(xmlFile);
    Result result = new SAXResult(fop.getDefaultHandler());

    Transformer transformer = templates.newTransformer();
    transformer.setParameter("base.uri", xmlFile.getParentFile().toURI());
    transformer.setParameter("logo.path", new File(xslFile.getParentFile(), "eftlab_logo.png").toURI());
    transformer.transform(xmlSource, result);
    out.close();
  }

  public void initCatalogManager() {
    URL docbookCatalog = getClass().getClassLoader().getResource("docbook/catalog.xml");
    CatalogManager cm = CatalogManager.getStaticManager();
    cm.setIgnoreMissingProperties(true);
    cm.setCatalogFiles(docbookCatalog.toString());
    cm.setVerbosity(99);
  }

  public Source getInputSource(File file) throws Exception {
    StreamSource stream = new StreamSource(file);
    InputSource input = new InputSource(stream.getSystemId());
    input.setCharacterStream(stream.getReader());
    input.setByteStream(stream.getInputStream());
    XMLReader reader = new ResolvingXMLReader();
    return new SAXSource(reader, input);
  }
}

class CatalogResolver extends org.apache.xml.resolver.tools.CatalogResolver {
  public String getResolvedEntity(String publicId, String systemId) {
    String path = super.getResolvedEntity(publicId, systemId);
    if (path == null ||
        path.startsWith("file:") ||
        path.startsWith("jar:file:"))
      return path;
    return "";
  }
  public InputSource resolveEntity(String publicId, String systemId) {
    InputSource source = super.resolveEntity(publicId, systemId);
    if (source == null ||
        source.getSystemId().startsWith("file:") ||
        source.getSystemId().startsWith("jar:file:"))
      return source;
    return new InputSource();
  }
  public Source resolve(String href, String base) throws TransformerException {
    Source source = super.resolve(href, base);
    if (source == null ||
        source.getSystemId().startsWith("file:") ||
        source.getSystemId().startsWith("jar:file:"))
      return source;
    return new StreamSource();
  }
}

class ResolvingXMLReader extends org.apache.xml.resolver.tools.ResolvingXMLReader {
  public InputSource resolveEntity(String publicId, String systemId) {
    InputSource source = super.resolveEntity(publicId, systemId);
    if (source == null ||
        source.getSystemId().startsWith("file:") ||
        source.getSystemId().startsWith("jar:file:"))
      return source;
    return new InputSource();
  }
}
