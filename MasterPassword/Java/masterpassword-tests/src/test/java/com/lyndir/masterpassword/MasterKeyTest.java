package com.lyndir.masterpassword;

import static org.testng.Assert.*;

import com.google.common.io.Resources;
import com.lyndir.lhunath.opal.system.CodeUtils;
import com.lyndir.lhunath.opal.system.logging.Logger;
import com.lyndir.lhunath.opal.system.util.NNFunctionNN;
import com.lyndir.lhunath.opal.system.util.StringUtils;
import java.net.URL;
import javax.annotation.Nonnull;
import javax.xml.bind.JAXBContext;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;


public class MasterKeyTest {

    @SuppressWarnings("UnusedDeclaration")
    private static final Logger logger = Logger.get( MasterKeyTest.class );

    private MPTestSuite testSuite;

    @BeforeMethod
    public void setUp()
            throws Exception {

        testSuite = new MPTestSuite();
    }

    @Test
    public void testEncode()
            throws Exception {

        testSuite.forEach( "testEncode", new NNFunctionNN<MPTests.Case, Boolean>() {
            @Nonnull
            @Override
            public Boolean apply(@Nonnull final MPTests.Case testCase) {
                MasterKey masterKey = MasterKey.create( testCase.getAlgorithm(), testCase.getFullName(), testCase.getMasterPassword() );

                assertEquals(
                        masterKey.encode( testCase.getSiteName(), testCase.getSiteType(), testCase.getSiteCounter(),
                                          testCase.getSiteVariant(), testCase.getSiteContext() ),
                        testCase.getResult(), "[testEncode] Failed test case: " + testCase );

                return true;
            }
        } );
    }

    @Test
    public void testGetUserName()
            throws Exception {

        MPTests.Case defaultCase = testSuite.getTests().getDefaultCase();

        assertEquals( MasterKey.create( defaultCase.getFullName(), defaultCase.getMasterPassword() ).getFullName(),
                      defaultCase.getFullName(), "[testGetUserName] Failed test case: " + defaultCase );
    }

    @Test
    public void testGetKeyID()
            throws Exception {

        testSuite.forEach( "testGetKeyID", new NNFunctionNN<MPTests.Case, Boolean>() {
            @Nonnull
            @Override
            public Boolean apply(@Nonnull final MPTests.Case testCase) {
                MasterKey masterKey = MasterKey.create( testCase.getFullName(), testCase.getMasterPassword() );

                assertEquals( CodeUtils.encodeHex( masterKey.getKeyID() ),
                              testCase.getKeyID(), "[testGetKeyID] Failed test case: " + testCase );

                return true;
            }
        } );
    }

    @Test
    public void testInvalidate()
            throws Exception {

        try {
            MPTests.Case defaultCase = testSuite.getTests().getDefaultCase();

            MasterKey masterKey = MasterKey.create( defaultCase.getFullName(), defaultCase.getMasterPassword() );
            masterKey.invalidate();
            masterKey.encode( defaultCase.getSiteName(), defaultCase.getSiteType(), defaultCase.getSiteCounter(),
                              defaultCase.getSiteVariant(), defaultCase.getSiteContext() );

            assertTrue( false, "[testInvalidate] Master key should have been invalidated, but was still usable." );
        }
        catch (IllegalStateException ignored) {
        }
    }
}
