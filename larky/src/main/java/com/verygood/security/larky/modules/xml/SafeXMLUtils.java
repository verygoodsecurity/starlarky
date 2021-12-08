package com.verygood.security.larky.modules.xml;

import java.io.IOException;
import java.io.InputStream;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import lombok.extern.slf4j.Slf4j;

/**
 * Class to instantiate XML manipulation builders to manipulate
 * XML documents in both a thread-safe and secure processing way.
 */
@Slf4j
public class SafeXMLUtils {

    private static final ThreadLocal<DocumentBuilderFactory> DOCUMENT_BUILDER_FACTORY = new ThreadLocal<>();
    private static final ThreadLocal<TransformerFactory> TRANSFORMER_FACTORY = new ThreadLocal<>();
    private static final ThreadLocal<SAXParserFactory> SAX_PARSER_FACTORY = new ThreadLocal<>();

    public static Document parse(String uri) throws TransformerException, IOException, SAXException, ParserConfigurationException {
        DocumentBuilder db = documentBuilder();
        try {
            return db.parse(uri);
        } finally {
            db.reset();
        }
    }

    public static Document parse(InputStream stream) throws TransformerException, IOException, SAXException, ParserConfigurationException {
        DocumentBuilder db = documentBuilder();
        try {
            return db.parse(stream);
        } finally {
            db.reset();
        }
    }

    public static void transform(Source xmlSource, Result outputTarget) throws TransformerException {
        Transformer t = transformer();
        try {
            t.transform(xmlSource, outputTarget);
        } finally {
            t.reset();
        }
    }

    public static void transform(Source xsltSource, Source xmlSource, Result outputTarget) throws TransformerException {
        Transformer t = transformer(xsltSource);
        try {
            t.transform(xmlSource, outputTarget);
        } finally {
            t.reset();
        }
    }

    public static XMLReader xmlReader() throws ParserConfigurationException, SAXException {
        SAXParserFactory spf = SAX_PARSER_FACTORY.get();
        if (spf == null) {
            spf = SAXParserFactory.newInstance();
            spf.setNamespaceAware(true);
            spf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, Boolean.TRUE);
            spf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            spf.setFeature("http://xml.org/sax/features/external-general-entities", false);
            spf.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            spf.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            spf.setXIncludeAware(false);
            SAX_PARSER_FACTORY.set(spf);
        }
        return spf.newSAXParser().getXMLReader();
    }

    public static DocumentBuilder documentBuilder() throws ParserConfigurationException {
        DocumentBuilderFactory dbf = DOCUMENT_BUILDER_FACTORY.get();
        if (dbf == null) {
            dbf = DocumentBuilderFactory.newInstance();
            dbf.setNamespaceAware(true);
            dbf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, Boolean.TRUE);
            dbf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            dbf.setFeature("http://xml.org/sax/features/external-general-entities", false);
            dbf.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            dbf.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            dbf.setXIncludeAware(false);
            dbf.setExpandEntityReferences(false);
            DOCUMENT_BUILDER_FACTORY.set(dbf);
        }
        return dbf.newDocumentBuilder();
    }

    public static Transformer transformer() throws TransformerConfigurationException {
        return transformer(null);
    }

    private static Transformer transformer(Source xsltSource) throws TransformerConfigurationException {
        TransformerFactory tf = TRANSFORMER_FACTORY.get();
        if (tf == null) {
            tf = TransformerFactory.newInstance();
            tf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, Boolean.TRUE);
            try {
                tf.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            } catch (IllegalArgumentException e) {
                log.warn("XSL transformer implementation doesn't support {} feature", XMLConstants.ACCESS_EXTERNAL_DTD);
            }
            try {
                tf.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");
            } catch (IllegalArgumentException e) {
                log.warn("XSL transformer implementation doesn't support {} feature", XMLConstants.ACCESS_EXTERNAL_STYLESHEET);
            }
            TRANSFORMER_FACTORY.set(tf);
        }
        return xsltSource == null ? tf.newTransformer() : tf.newTransformer(xsltSource);
    }

}