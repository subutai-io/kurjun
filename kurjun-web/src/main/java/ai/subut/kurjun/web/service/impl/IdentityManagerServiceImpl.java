package ai.subut.kurjun.web.service.impl;


import java.util.List;

import com.google.inject.Inject;

import ai.subut.kurjun.identity.service.IdentityManager;
import ai.subut.kurjun.model.identity.User;
import ai.subut.kurjun.web.service.IdentityManagerService;


/**
 *
 */
public class IdentityManagerServiceImpl implements IdentityManagerService
{
    private SecurityManager securityManager;
    private IdentityManager identityManager;

    @Inject
    public IdentityManagerServiceImpl(IdentityManager identityManager, SecurityManager securityManager)
    {
        this.identityManager = identityManager;
        this.securityManager = securityManager;
    }

    //*************************************
    @Override
    public List<User> getAllUsers()
    {
        return identityManager.getAllUsers();
    }


    //*************************************
    @Override
    public User getUser(String userId)
    {
        return identityManager.getUser( userId );
    }

}
