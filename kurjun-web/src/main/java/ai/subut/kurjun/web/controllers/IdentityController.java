package ai.subut.kurjun.web.controllers;


import java.util.List;

import ninja.session.FlashScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import ai.subut.kurjun.model.identity.User;
import ai.subut.kurjun.web.service.IdentityManagerService;
import ninja.Result;
import ninja.Results;
import ninja.params.Param;


/**
 * REST Controller for Identity Management
 */
@Singleton
public class IdentityController extends BaseController
{
    private static final Logger LOGGER = LoggerFactory.getLogger( IdentityController.class );

    @Inject
    IdentityManagerService identityManagerService;


    //*************************
    public Result getUsers()
    {
        List<User> users = identityManagerService.getAllUsers();

        return Results.ok().render( users ).json();
    }


    //*************************
    public Result getUser( @Param( "fingerprint" ) String fingerprint )
    {
        User user = identityManagerService.getUser( fingerprint );

        if(user != null)
        {
            return Results.ok().render( user ).json();
        }
        else
        {
            return Results.notFound();
        }

    }


    //*************************
    public Result addUser( @Param( "key" ) String publicKey )
    {
        User user = identityManagerService.addUser( publicKey );

        if(user != null)
        {
            return Results.ok().render( user ).json();
        }
        else
        {
            return Results.internalServerError();
        }
    }


    //*************************
    public Result homePage()
    {
        return Results.html().template( "views/home.ftl" );
    }


    //*************************
    public Result loginPage()
    {
        return Results.html().template( "views/login.ftl" );
    }


    //*************************
    public Result authorizeUser(@Param( "fingerprint" ) String fingerprint, @Param( "message" ) String message,
                                FlashScope flashScope )
    {
        User user = identityManagerService.authenticateUser( fingerprint, message );

        if(user != null)
        {
            return Results.redirect("/");
        }
        else
        {
            flashScope.error( "Failed to authorize.");
            return Results.redirect( "/login" );
        }
    }

}
