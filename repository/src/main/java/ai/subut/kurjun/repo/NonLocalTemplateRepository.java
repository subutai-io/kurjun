package ai.subut.kurjun.repo;


import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.client.ClientException;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.commons.io.IOUtils;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.transport.http.HTTPConduit;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;

import ai.subut.kurjun.common.service.KurjunConstants;
import ai.subut.kurjun.metadata.common.DefaultMetadata;
import ai.subut.kurjun.metadata.common.subutai.DefaultTemplate;
import ai.subut.kurjun.metadata.common.utils.MetadataUtils;
import ai.subut.kurjun.model.annotation.Nullable;
import ai.subut.kurjun.model.index.ReleaseFile;
import ai.subut.kurjun.model.metadata.Metadata;
import ai.subut.kurjun.model.metadata.SerializableMetadata;
import ai.subut.kurjun.model.security.Identity;
import ai.subut.kurjun.repo.cache.PackageCache;
import ai.subut.kurjun.repo.util.SecureRequestFactory;


/**
 * Non-local templates repository implementation.
 * <p>
 * TODO: Refactor common methods of all non local repos into base one.
 *
 */
class NonLocalTemplateRepository extends NonLocalRepositoryBase
{

    private static final Logger LOGGER = LoggerFactory.getLogger( NonLocalTemplateRepository.class );

    static final String INFO_PATH = "info";
    static final String LIST_PATH = "list";
    static final String GET_PATH = "get";

    @Inject
    private Gson gson;
    private PackageCache cache;

    private final URL url;
    private final Identity identity;

    private String token = null;

    private static final long CONN_TIMEOUT = 3000;


    @Inject
    public NonLocalTemplateRepository( PackageCache cache,
                                       @Assisted( "url" ) String url,
                                       @Assisted @Nullable Identity identity,
                                       @Assisted( "token" ) @Nullable String token )
    {
        this.cache = cache;
        this.identity = identity;
        this.token = token;
        try
        {
            this.url = new URL( url );
        }
        catch ( MalformedURLException ex )
        {
            throw new IllegalArgumentException( "Invalid url", ex );
        }
    }


    @Override
    public Identity getIdentity()
    {
        return identity;
    }


    @Override
    public URL getUrl()
    {
        return url;
    }


    @Override
    public boolean isKurjun()
    {
        return true;
    }


    @Override
    public Set<ReleaseFile> getDistributions()
    {
        throw new UnsupportedOperationException( "Not supported in template repositories." );
    }


    @Override
    public SerializableMetadata getPackageInfo( Metadata metadata )
    {
        SecureRequestFactory secreq = new SecureRequestFactory( this );
        WebClient webClient = secreq.makeClient( INFO_PATH, makeParamsMap( metadata ) );
        if ( identity != null )
        {
            webClient.header( KurjunConstants.HTTP_HEADER_FINGERPRINT, identity.getKeyFingerprint() );
        }

        Response resp = doGet( webClient );
        if ( resp != null && resp.getStatus() == Response.Status.OK.getStatusCode() )
        {
            if ( resp.getEntity() instanceof InputStream )
            {
                try
                {
                    String json = IOUtils.toString( ( InputStream ) resp.getEntity() );
                    return gson.fromJson( json, DefaultTemplate.class );
                }
                catch ( IOException ex )
                {
                    LOGGER.error( "Failed to read response data", ex );
                }
            }
        }
        return null;
    }


    @Override
    public InputStream getPackageStream( Metadata metadata )
    {
        InputStream cachedStream = checkCache( metadata );
        if ( cachedStream != null )
        {
            return cachedStream;
        }

        SecureRequestFactory secreq = new SecureRequestFactory( this );
        WebClient webClient = secreq.makeClient( GET_PATH, makeParamsMap( metadata ) );
        if ( identity != null )
        {
            webClient.header( KurjunConstants.HTTP_HEADER_FINGERPRINT, identity.getKeyFingerprint() );
        }

        Response resp = doGet( webClient );
        if ( resp != null && resp.getStatus() == Response.Status.OK.getStatusCode() )
        {
            if ( resp.getEntity() instanceof InputStream )
            {
                byte[] md5 = cacheStream( ( InputStream ) resp.getEntity() );
                return cache.get( md5 );
            }
        }
        return null;
    }


    @Override
    public List<SerializableMetadata> listPackages()
    {
        SecureRequestFactory secreq = new SecureRequestFactory( this );
        WebClient webClient = secreq.makeClient( LIST_PATH, makeParamsMap( new DefaultMetadata() ) );
        if ( identity != null )
        {
            webClient.header( KurjunConstants.HTTP_HEADER_FINGERPRINT, identity.getKeyFingerprint() );
        }

        Response resp = doGet( webClient );
        if ( resp != null && resp.getStatus() == Response.Status.OK.getStatusCode() )
        {
            if ( resp.getEntity() instanceof InputStream )
            {
                try
                {
                    List<String> items = IOUtils.readLines( ( InputStream ) resp.getEntity() );
                    return parseItems( items.get( 0 ) );
                }
                catch ( IOException ex )
                {
                    LOGGER.error( "Failed to read packages list", ex );
                }
            }
        }
        return Collections.emptyList();
    }


    @Override
    protected Logger getLogger()
    {
        return LOGGER;
    }


    private Response doGet( WebClient webClient )
    {
        try
        {
            HTTPConduit httpConduit = ( HTTPConduit ) WebClient.getConfig( webClient ).getConduit();
            httpConduit.getClient().setConnectionTimeout( CONN_TIMEOUT );
            return webClient.get();
        }
        catch ( ClientException e )
        {
            LOGGER.warn( "Failed to do GET.", e );
            return null;
        }
    }


    private Map<String, String> makeParamsMap( Metadata metadata )
    {
        Map<String, String> params = MetadataUtils.makeParamsMap( metadata );

        if ( token != null )
        {
            params.put( "sptoken", token );
        }

        // Set parameter kc=kurjun_client to indicate this request is going from Kurjun
        params.put( "kc", Boolean.TRUE.toString() );

        return params;
    }


    private List<SerializableMetadata> parseItems( String items )
    {
        Type collectionType = new TypeToken<LinkedList<DefaultTemplate>>()
        {
        }.getType();
        return gson.fromJson( items, collectionType );
    }

}
