package com.lyndir.masterpassword;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.primitives.UnsignedInteger;
import com.lyndir.lhunath.opal.system.logging.Logger;
import com.lyndir.lhunath.opal.system.util.ConversionUtils;
import com.lyndir.lhunath.opal.system.util.NNFunctionNN;
import java.io.IOException;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.Nonnull;
import javax.xml.parsers.*;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.ext.DefaultHandler2;


/**
 * @author lhunath, 2015-12-22
 */
public class MPTestSuite implements Callable<Boolean> {

    @SuppressWarnings("UnusedDeclaration")
    private static final Logger logger                = Logger.get( MPTestSuite.class );
    private static final String DEFAULT_RESOURCE_NAME = "mpw_tests.xml";

    private MPTests  tests;
    private Listener listener;

    public MPTestSuite()
            throws UnavailableException {
        this( DEFAULT_RESOURCE_NAME );
    }

    public MPTestSuite(String resourceName)
            throws UnavailableException {
        try {
            tests = new MPTests();
            tests.cases = Lists.newLinkedList();
            SAXParser parser = SAXParserFactory.newInstance().newSAXParser();
            parser.parse( Thread.currentThread().getContextClassLoader().getResourceAsStream( resourceName ), new DefaultHandler2() {
                private Deque<String> currentTags = Lists.newLinkedList();
                private Deque<StringBuilder> currentTexts = Lists.newLinkedList();
                private MPTests.Case currentCase;

                @Override
                public void startElement(final String uri, final String localName, final String qName, final Attributes attributes)
                        throws SAXException {
                    super.startElement( uri, localName, qName, attributes );
                    currentTags.push( qName );
                    currentTexts.push( new StringBuilder() );

                    if ("case".equals( qName )) {
                        currentCase = new MPTests.Case();
                        currentCase.identifier = attributes.getValue( "id" );
                        currentCase.parent = attributes.getValue( "parent" );
                    }
                }

                @Override
                public void endElement(final String uri, final String localName, final String qName)
                        throws SAXException {
                    super.endElement( uri, localName, qName );
                    Preconditions.checkState( qName.equals( currentTags.pop() ) );
                    String text = currentTexts.pop().toString();

                    if ("case".equals( qName ))
                        tests.cases.add( currentCase );
                    if ("algorithm".equals( qName ))
                        currentCase.algorithm = ConversionUtils.toInteger( text ).orNull();
                    if ("fullName".equals( qName ))
                        currentCase.fullName = text;
                    if ("masterPassword".equals( qName ))
                        currentCase.masterPassword = text;
                    if ("keyID".equals( qName ))
                        currentCase.keyID = text;
                    if ("siteName".equals( qName ))
                        currentCase.siteName = text;
                    if ("siteCounter".equals( qName ))
                        currentCase.siteCounter = text.isEmpty()? null: UnsignedInteger.valueOf( text );
                    if ("siteType".equals( qName ))
                        currentCase.siteType = text;
                    if ("siteVariant".equals( qName ))
                        currentCase.siteVariant = text;
                    if ("siteContext".equals( qName ))
                        currentCase.siteContext = text;
                    if ("result".equals( qName ))
                        currentCase.result = text;
                }

                @Override
                public void characters(final char[] ch, final int start, final int length)
                        throws SAXException {
                    super.characters( ch, start, length );

                    currentTexts.peek().append( ch, start, length );
                }
            } );
        }
        catch (IllegalArgumentException | ParserConfigurationException | SAXException | IOException e) {
            throw new UnavailableException( e );
        }

        for (MPTests.Case testCase : tests.getCases())
            testCase.initializeParentHierarchy( tests );
    }

    public void setListener(final Listener listener) {
        this.listener = listener;
    }

    public MPTests getTests() {
        return tests;
    }

    public boolean forEach(String testName, NNFunctionNN<MPTests.Case, Boolean> testFunction) {
        List<MPTests.Case> cases = tests.getCases();
        for (int c = 0; c < cases.size(); c++) {
            MPTests.Case testCase = cases.get( c );
            if (testCase.getResult().isEmpty())
                continue;

            progress( Logger.Target.INFO, c, cases.size(), //
                      "[%s] on %s...", testName, testCase.getIdentifier() );

            if (!testFunction.apply( testCase )) {
                progress( Logger.Target.ERROR, cases.size(), cases.size(), //
                          "[%s] on %s: FAILED!", testName, testCase.getIdentifier() );

                return false;
            }

            progress( Logger.Target.INFO, c + 1, cases.size(), //
                      "[%s] on %s: passed!", testName, testCase.getIdentifier() );
        }

        return true;
    }

    private void progress(final Logger.Target target, final int current, final int max, final String format, final Object... args) {
        logger.log( target, format, args );

        if (listener != null)
            listener.progress( current, max, format, args );
    }

    @Override
    public Boolean call()
            throws Exception {
        return forEach( "mpw", new NNFunctionNN<MPTests.Case, Boolean>() {
            @Nonnull
            @Override
            public Boolean apply(@Nonnull final MPTests.Case testCase) {
                MasterKey masterKey = MasterKey.create( testCase.getAlgorithm(), testCase.getFullName(), testCase.getMasterPassword() );
                String sitePassword = masterKey.encode( testCase.getSiteName(), testCase.getSiteType(), testCase.getSiteCounter(),
                                                        testCase.getSiteVariant(), testCase.getSiteContext() );

                return testCase.getResult().equals( sitePassword );
            }
        } );
    }

    public static class UnavailableException extends Exception {

        public UnavailableException(final Throwable cause) {
            super( cause );
        }
    }


    public interface Listener {

        void progress(int current, int max, String messageFormat, Object... args);
    }
}
