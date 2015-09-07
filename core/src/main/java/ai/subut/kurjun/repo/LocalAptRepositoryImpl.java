package ai.subut.kurjun.repo;


import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;

import ai.subut.kurjun.model.index.ReleaseFile;
import ai.subut.kurjun.model.metadata.PackageMetadata;
import ai.subut.kurjun.model.repository.LocalRepository;
import ai.subut.kurjun.repo.http.PathBuilder;
import ai.subut.kurjun.riparser.service.ReleaseIndexParser;


/**
 * Local non-virtual apt repository.
 *
 */
class LocalAptRepositoryImpl extends RepositoryBase implements LocalRepository
{
    private static final Logger LOGGER = LoggerFactory.getLogger( LocalAptRepositoryImpl.class );

    private ReleaseIndexParser releaseIndexParser;

    private final Path baseDirectory;
    private final URL url;

    private final List<String> releases = new ArrayList<>();


    /**
     * Constructs local apt repository at the specified base directory.
     *
     * @param releaseIndexParser release index parser
     * @param baseDirectory local apt base directory
     */
    @Inject
    public LocalAptRepositoryImpl( ReleaseIndexParser releaseIndexParser, @Assisted String baseDirectory )
    {
        Path path = Paths.get( baseDirectory );
        if ( !Files.exists( path ) )
        {
            throw new IllegalArgumentException( "Apt base directory does not exist: " + baseDirectory );
        }
        if ( !Files.isDirectory( path ) )
        {
            throw new IllegalArgumentException( "Specified pathis not a directory: " + baseDirectory );
        }

        this.releaseIndexParser = releaseIndexParser;
        this.baseDirectory = path;

        // TODO: set correct localhost url
        try
        {
            this.url = new URL( "http", "localhost", "" );
        }
        catch ( MalformedURLException ex )
        {
            LOGGER.error( "Failed to create URL for the localhost", ex );
            throw new IllegalStateException( ex );
        }
    }


    @Override
    public URL getUrl()
    {
        return url;
    }


    @Override
    public boolean isKurjun()
    {
        return false;
    }


    @Override
    public Set<ReleaseFile> getDistributions()
    {
        Set<ReleaseFile> result = new HashSet<>();
        try
        {
            if ( releases.isEmpty() )
            {
                readDistributionsFile();
            }
            for ( String release : releases )
            {
                try ( InputStream is = openReleaseIndexFileStream( release ) )
                {
                    ReleaseFile rf = releaseIndexParser.parse( is );
                    result.add( rf );
                }
            }
        }
        catch ( IOException ex )
        {
            LOGGER.error( "Failed to read releases of a remote repo", ex );
        }
        return result;
    }


    @Override
    public Path getBaseDirectory()
    {
        return baseDirectory;
    }


    @Override
    public PackageMetadata put( InputStream is ) throws IOException
    {
        throw new IOException( "Not supported in non-virtual local apt repository." );
    }


    /**
     * Reads releases from {@code conf/distributions} file of this apt repository.
     *
     * @throws IOException on any read failures
     * @return list of release names like 'trusty', 'utopic', etc.
     */
    private void readDistributionsFile() throws IOException
    {
        Pattern pattern = Pattern.compile( "Codename:\\s*(\\w+)" );

        Path path = baseDirectory.resolve( "conf/distributions" );

        List<String> ls = new ArrayList<>();
        try ( BufferedReader br = new BufferedReader( new FileReader( path.toFile() ) ) )
        {
            String line;
            while ( ( line = br.readLine() ) != null )
            {
                Matcher matcher = pattern.matcher( line );
                if ( matcher.matches() )
                {
                    ls.add( matcher.group( 1 ) );
                }
            }
        }

        releases.clear();
        releases.addAll( ls );
    }


    private InputStream openReleaseIndexFileStream( String release ) throws IOException
    {
        String path = PathBuilder.instance().setRelease( release ).forReleaseIndexFile().build();
        Path p = baseDirectory.resolve( path );
        return new FileInputStream( p.toFile() );
    }


}
