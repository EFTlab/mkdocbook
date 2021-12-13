package com.eftlab.mkdocbook;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.HashMap;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
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

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.fop.apps.Fop;
import org.apache.fop.apps.FopFactory;
import org.apache.fop.apps.FopFactoryBuilder;
import org.apache.fop.apps.MimeConstants;
import org.apache.xml.resolver.CatalogManager;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

public class App {

  private File xslFile;
  private File xmlFile;
  private File outFile;
  private HashMap<String, Object> params = new HashMap<String, Object>();

  public static void main(String[] args) throws Exception {
    App app = new App();
    app.parseCommandLine(args);
    app.run();
  }

  public void parseCommandLine(String[] args) {
    Options options = new Options();
    options.addRequiredOption("x", "stylesheet", true, "XSL stylesheet");
    options.addRequiredOption("i", "input", true, "input file");
    options.addRequiredOption("o", "output", true, "output file");
    options.addOption("p", "param", true, "XSL parameters");

    CommandLineParser parser = new DefaultParser();
    HelpFormatter help = new HelpFormatter();
    CommandLine line;

    try {
      line = parser.parse(options, args);
    } catch (ParseException e) {
      System.out.println("Error: " + e.getMessage());
      help.printHelp("mkdocbook", options);
      System.exit(1);
      return;
    }

    xslFile = new File(line.getOptionValue("stylesheet"));
    xmlFile = new File(line.getOptionValue("input"));
    outFile = new File(line.getOptionValue("output"));

    for (String value : line.getOptionValues("param")) {
      String[] parts = value.split("=", 2);
      params.put(parts[0], parts[1]);
    }
  }

  public void run() throws Exception {
    // Configure the CatalogManager to use the bundled stylesheets
    URL dbkCatalog = getClass().getClassLoader().getResource("docbook/catalog.xml");
    CatalogManager catalogManager = CatalogManager.getStaticManager();
    catalogManager.setIgnoreMissingProperties(true);
    catalogManager.setCatalogFiles(dbkCatalog.toString());
    catalogManager.setVerbosity(99);

    // Create a saxon TransformerFactory using our custom resolver
    TransformerFactory transformerFactory = new TransformerFactoryImpl();
    transformerFactory.setURIResolver(new CatalogResolver());

    // Create a SAX Parser Factory so we can configure XInclude handling
    SAXParserFactory parserFactory = SAXParserFactory.newInstance();
    parserFactory.setNamespaceAware(true);
    parserFactory.setXIncludeAware(true);
    parserFactory.setValidating(false);

    // Load the stylesheet and prepare the transformer
    Source xslSource = getInputSource(parserFactory, xslFile);
    Templates templates = transformerFactory.newTemplates(xslSource);
    Transformer transformer = templates.newTransformer();
    for (HashMap.Entry<String, Object> entry : params.entrySet())
      transformer.setParameter(entry.getKey(), entry.getValue());

    // Create the output stream for FOP to write the PDF
    OutputStream outStream = new BufferedOutputStream(new FileOutputStream(outFile));
    FopFactory fopFactory = new FopFactoryBuilder(new File(".").toURI()).build();
    Fop fop = fopFactory.newFop(MimeConstants.MIME_PDF, outStream);

    // Transform the input and send the result to FOP
    Source source = getInputSource(parserFactory, xmlFile);
    Result result = new SAXResult(fop.getDefaultHandler());
    transformer.transform(source, result);
    outStream.close();
  }

  public Source getInputSource(SAXParserFactory parserFactory, File file) throws Exception {
    // Create a SAXSource which uses our custom reader
    StreamSource stream = new StreamSource(file);
    InputSource input = new InputSource(stream.getSystemId());
    input.setCharacterStream(stream.getReader());
    input.setByteStream(stream.getInputStream());
    XMLReader reader = parserFactory.newSAXParser().getXMLReader();
    return new SAXSource(new ResolvingXMLFilter(reader), input);
  }
}

/** A CatalogResolver which won't access the network. */
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

/** A ResolvingXMLFilter which won't access the network. */
class ResolvingXMLFilter extends org.apache.xml.resolver.tools.ResolvingXMLFilter {
  public ResolvingXMLFilter(XMLReader parent) {
    super(parent);
  }
  public InputSource resolveEntity(String publicId, String systemId) {
    InputSource source = super.resolveEntity(publicId, systemId);
    if (source == null ||
        source.getSystemId().startsWith("file:") ||
        source.getSystemId().startsWith("jar:file:"))
      return source;
    return new InputSource();
  }
}
