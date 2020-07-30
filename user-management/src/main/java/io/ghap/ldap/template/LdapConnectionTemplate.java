package io.ghap.ldap.template;

import io.ghap.user.dao.ValidationError;
import org.apache.directory.api.ldap.extras.controls.ppolicy_impl.PasswordPolicyDecorator;
import org.apache.directory.api.ldap.model.cursor.CursorException;
import org.apache.directory.api.ldap.model.cursor.CursorLdapReferralException;
import org.apache.directory.api.ldap.model.cursor.SearchCursor;
import org.apache.directory.api.ldap.model.entry.Attribute;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.message.*;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.api.ldap.model.name.Rdn;
import org.apache.directory.ldap.client.api.EntryCursorImpl;
import org.apache.directory.ldap.client.api.LdapConnection;
import org.apache.directory.ldap.client.template.*;
import org.apache.directory.ldap.client.template.exception.LdapRuntimeException;
import org.apache.directory.ldap.client.template.exception.PasswordException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static java.util.Objects.requireNonNull;

/**
 *
 */
public class LdapConnectionTemplate {
    private static Logger log = LoggerFactory.getLogger(LdapConnectionTemplate.class);

    private final LdapConnection connection;
    private final PasswordPolicyDecorator passwordPolicyRequestControl;
    private final PasswordPolicyResponder passwordPolicyResponder;
    private final ModelFactory modelFactory;

    public LdapConnectionTemplate(LdapConnection connection){
        this.connection = connection;

        this.passwordPolicyRequestControl = new PasswordPolicyDecorator(
                connection.getCodecService() );
        this.passwordPolicyResponder = new PasswordPolicyResponderImpl(
                connection.getCodecService() );
        this.modelFactory = new ModelFactoryImpl();
    }

    public Dn newDn( String dn )
    {
        return modelFactory.newDn(dn);
    }

    public PasswordWarning authenticateConnection( final Dn userDn, final String password ) throws PasswordException
    {
        return passwordPolicyResponder.process(
                () -> {
                    MemoryClearingBuffer passwordBuffer = MemoryClearingBuffer.newInstance(password.toCharArray());
                    try {
                        BindRequest bindRequest = new BindRequestImpl()
                                .setDn(userDn)
                                .setCredentials(passwordBuffer.getBytes())
                                .addControl(passwordPolicyRequestControl);

                        return connection.bind(bindRequest);
                    } finally {
                        passwordBuffer.clear();
                    }
                });
    }

    public PasswordWarning authenticateConnection( final String name, final String password ) throws PasswordException
    {
        return passwordPolicyResponder.process(
                () -> {
                    MemoryClearingBuffer passwordBuffer = MemoryClearingBuffer.newInstance(password.toCharArray());
                    try {
                        BindRequest bindRequest = new BindRequestImpl()
                                .setName(name)
                                .setCredentials(passwordBuffer.getBytes())
                                .addControl(passwordPolicyRequestControl);

                        return connection.bind(bindRequest);
                    } finally {
                        passwordBuffer.clear();
                    }
                });
    }

    public PasswordWarning modifyPassword( Dn userDn, String oldPassword, String newPassword, boolean asAdmin ) throws PasswordException
    {
        if ( !asAdmin )
        {
            authenticateConnection( userDn, oldPassword );
        }

        return modifyPassword(userDn, oldPassword, newPassword);
    }
    public PasswordWarning modifyPassword(final String userDn, final String oldPassword, final String newPassword) throws PasswordException {
        return modifyPassword(newDn(userDn), oldPassword, newPassword);
    }
    public PasswordWarning modifyPassword(final Dn userDn, final String oldPassword, final String newPassword) throws PasswordException {
        return passwordPolicyResponder.process(
                () -> {
                    // Can't use Password Modify:
                    // https://issues.apache.org/jira/browse/DIRSERVER-1935
                    // So revert to regular Modify
                    //MemoryClearingBuffer newPasswordBuffer = MemoryClearingBuffer.newInstance(newPassword.toCharArray());

                    try {
                        ModifyRequest modifyRequest = new ModifyRequestImpl().setName(userDn);

                        if(oldPassword == null){
                            modifyRequest.replace("unicodePwd", ("\"" + newPassword + "\"").getBytes(StandardCharsets.UTF_16LE));
                            //.replace("userPassword", newPasswordBuffer.getBytes())
                        }
                        else {
                            //MemoryClearingBuffer oldPasswordBuffer = MemoryClearingBuffer.newInstance(oldPassword.toCharArray());
                            modifyRequest
                                    //.remove("userPassword", oldPasswordBuffer.getBytes())
                                    //.replace("userPassword", newPasswordBuffer.getBytes());
                                    .remove("unicodePwd", ("\"" + oldPassword + "\"").getBytes(StandardCharsets.UTF_16LE))
                                    .add("unicodePwd", ("\"" + newPassword + "\"").getBytes(StandardCharsets.UTF_16LE));
                        }

                        modifyRequest.addControl(passwordPolicyRequestControl);

                        ModifyResponse response = connection.modify(modifyRequest);
                        return response;
                    //} catch (DecoderException e) {
                        //throw new LdapRuntimeException(new LdapException(e));
                    } finally {
                        //newPasswordBuffer.clear();
                    }
                });
    }

    public SearchRequest newSearchRequest(Dn baseDn, String filter, SearchScope scope){
        return modelFactory.newSearchRequest(baseDn, filter, scope);
    }

    public ModifyRequest newModifyRequest( String dn )
    {
        return modelFactory.newModifyRequest(dn);
    }


    public ModifyRequest newModifyRequest( Dn dn )
    {
        return modelFactory.newModifyRequest(dn);
    }

    public ModifyResponse modify( ModifyRequest modifyRequest ) throws LdapException {
        return connection.modify(modifyRequest);
    }

    public Dn rename(Dn dn, Dn newDn, String field, List<ValidationError> errors) throws LdapException {
        if( dn.equals(newDn) ){
            return dn;
        }
        ModifyDnResponse response = rename(dn, newDn.getRdn());
        ResultCodeEnum code = response.getLdapResult().getResultCode();

        if(code == ResultCodeEnum.ENTRY_ALREADY_EXISTS){
            errors.add(new ValidationError(field, code.getMessage(), "Entry with \""+newDn.getRdn().getNormValue()+"\" name is already exists"));
        }
        else try {
            if (ResultCodeEnum.processResponse(response)) {
                return newDn;
            } else {
                return dn;
            }
        } catch (LdapException e){
            errors.add(new ValidationError(field, code.getMessage(), response.getLdapResult().getDiagnosticMessage()));
        }
        return null;
    }

    public ModifyDnResponse rename(Dn entryDn, Rdn newRdn) throws LdapException {
        ModifyDnRequest modDnRequest = new ModifyDnRequestImpl();
        modDnRequest.setName(   requireNonNull(entryDn, "Cannot process a rename of a null Dn") );
        modDnRequest.setNewRdn( requireNonNull(newRdn,  "Cannot process a rename with a null Rdn") );
        modDnRequest.setDeleteOldRdn( true );

        ModifyDnResponse modifyDnResponse = connection.modifyDn(modDnRequest);
        //then you can do: ResultCodeEnum.processResponse( modifyDnResponse );
        return modifyDnResponse;
    }

    public ModifyResponse modify( Dn dn, RequestBuilder<ModifyRequest> requestBuilder ) throws LdapException {
        ModifyRequest modifyRequest = newModifyRequest( dn );
        try
        {
            requestBuilder.buildRequest( modifyRequest );
        }
        catch ( LdapException e )
        {
            throw new LdapRuntimeException( e );
        }
        return modify(modifyRequest);
    }


    public <T> T searchFirst( Dn baseDn, String filter, SearchScope scope,
                              EntryMapper<T> entryMapper ) throws LdapException {
        return searchFirst(
                modelFactory.newSearchRequest(baseDn, filter, scope),
                entryMapper);
    }

    public <T> T searchFirst( Dn baseDn, String filter, SearchScope scope,
                              EntryMapper<T> entryMapper, String... attributes) throws LdapException {
        return searchFirst(
                modelFactory.newSearchRequest( baseDn, filter, scope, attributes ),
                entryMapper );
    }

    public <T> T searchFirst( SearchRequest searchRequest,
                              EntryMapper<T> entryMapper ) throws LdapException {
        // in case the caller did not set size limit, we cache original value,
        // set to 1, then set back to original value before returning...
        long originalSizeLimit = searchRequest.getSizeLimit();
        try
        {
            searchRequest.setSizeLimit( 1 );
            List<T> entries = search( searchRequest, entryMapper );
            return entries.isEmpty() ? null : entries.get( 0 );
        }
        finally
        {
            searchRequest.setSizeLimit( originalSizeLimit );
        }
    }

    public <T> List<T> search( Dn baseDn, String filter, SearchScope scope,
                               EntryMapper<T> entryMapper ) throws LdapException {
        return search(
                modelFactory.newSearchRequest(baseDn, filter, scope),
                entryMapper);
    }

    public <T> List<T> search( String filter, SearchScope scope,
                               EntryMapper<T> entryMapper ) throws LdapException {
        Dn baseDn = new Dn(connection.getRootDse().get("rootDomainNamingContext").getString());
        return search(
                modelFactory.newSearchRequest(baseDn, filter, scope),
                entryMapper);
    }

    public <T> T lookup( Dn dn, EntryMapper<T> entryMapper )
    {
        return lookup(dn, null, entryMapper);
    }

    public Entry lookup( Dn dn, Control[] controls, String... attributes )
    {
        try
        {
            return attributes == null
                    ? connection.lookup( dn, controls )
                    : connection.lookup( dn, controls, attributes );
        }
        catch ( LdapException e )
        {
            throw new LdapRuntimeException( e );
        }
    }

    public <T> T lookup( Dn dn, String[] attributes, EntryMapper<T> entryMapper )
    {
        try
        {
            Entry entry = attributes == null
                    ? connection.lookup( dn )
                    : connection.lookup( dn, attributes );
            return entry == null ? null : entryMapper.map( entry );
        }
        catch ( LdapException e )
        {
            throw new LdapRuntimeException( e );
        }
    }

    public <T> List<T> search( SearchRequest searchRequest,
                               EntryMapper<T> entryMapper ) throws LdapException {
        List<T> entries = new ArrayList<T>();

        SearchCursor cursor = connection.search( searchRequest );
        Iterator<Entry> it = new EntryCursorImpl( cursor ).iterator();
        while ( cursor.available() && it.hasNext() && (searchRequest.getSizeLimit() <= 0 || entries.size() < searchRequest.getSizeLimit()) )
        {
            try {
                Entry entry = it.next();
                entries.add(entryMapper.map(entry));
            }
            catch (RuntimeException e){
                if( e.getCause() instanceof CursorLdapReferralException){
                    // hide
                    // I don't know the reason but sometime response is SearchResultReference instead of SearchResultEntry
                    CursorLdapReferralException re = (CursorLdapReferralException)e.getCause();
                    do
                    {
                        // We gather all the referrals here. It's up to you to decide to follow them or not
                        String ref = re.getReferralInfo();
                        System.out.println(ref );
                    }
                    while ( re.skipReferral() );
                    try {
                        cursor.next();
                    } catch (CursorException e1) {
                        e1.printStackTrace();
                    }
                }
                else {
                    // rethrow
                    throw e;
                }
            }
        }

        return entries;
    }

    public AddResponse add( Dn dn, RequestBuilder<AddRequest> requestBuilder ) throws LdapException {
        AddRequest addRequest = newAddRequest( newEntry( dn ) );

        requestBuilder.buildRequest( addRequest );

        return add( addRequest );
    }

    public AddResponse add( AddRequest addRequest ) throws LdapException {
        return connection.add(addRequest);
    }

    public AddResponse add( Dn dn, final Attribute... attributes ) throws LdapException {
        return add(dn, request -> {
                    request.getEntry().add(attributes);
                });
    }

    public AddRequest newAddRequest( Entry entry )
    {
        return modelFactory.newAddRequest(entry);
    }

    public Entry newEntry( Dn dn )
    {
        return modelFactory.newEntry(dn);
    }

    public DeleteResponse delete( Dn dn ) throws LdapException {
        return delete(dn, null);
    }

    public DeleteResponse delete( Dn dn, RequestBuilder<DeleteRequest> requestBuilder ) throws LdapException {
        DeleteRequest deleteRequest = newDeleteRequest( dn );
        if ( requestBuilder != null )
        {
            try
            {
                requestBuilder.buildRequest( deleteRequest );
            }
            catch ( LdapException e )
            {
                throw new LdapRuntimeException( e );
            }
        }
        return delete( deleteRequest );
    }

    public DeleteRequest newDeleteRequest( Dn dn )
    {
        return modelFactory.newDeleteRequest(dn);
    }

    public DeleteResponse delete( DeleteRequest deleteRequest ) throws LdapException {
        return connection.delete( deleteRequest );
    }
}
