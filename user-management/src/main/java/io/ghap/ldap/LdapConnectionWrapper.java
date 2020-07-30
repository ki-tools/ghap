package io.ghap.ldap;

import org.apache.directory.api.asn1.util.Oid;
import org.apache.directory.api.ldap.codec.api.BinaryAttributeDetector;
import org.apache.directory.api.ldap.codec.api.DefaultConfigurableBinaryAttributeDetector;
import org.apache.directory.api.ldap.codec.api.LdapApiService;
import org.apache.directory.api.ldap.model.cursor.EntryCursor;
import org.apache.directory.api.ldap.model.cursor.SearchCursor;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.entry.Modification;
import org.apache.directory.api.ldap.model.entry.ModificationOperation;
import org.apache.directory.api.ldap.model.entry.Value;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.message.*;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.api.ldap.model.name.Rdn;
import org.apache.directory.api.ldap.model.schema.SchemaManager;
import org.apache.directory.ldap.client.api.LdapConnection;
import org.apache.directory.ldap.client.api.LdapConnectionPool;
import org.apache.directory.ldap.client.template.exception.LdapRuntimeException;

import java.io.IOException;
import java.util.List;

/**
 * Get connection from pool and delegate all methods
 * to this connection, except "close()".
 * On "close()" this wrapper returns connection to pool
 */
public class LdapConnectionWrapper implements LdapConnection, AutoCloseable {

    /*
    private static MutableAttributeType nTSecurityDescriptor = new MutableAttributeType("1.2.840.113556.1.2.281");
    static {
        nTSecurityDescriptor.addName("nTSecurityDescriptor");
        LdapSyntax ldapSyntax = new LdapSyntax("1.3.6.1.4.1.1466.115.121.1.40");
        nTSecurityDescriptor.setSyntax(ldapSyntax);

    }
     */

    private final LdapConnectionPool pool;
    private LdapConnection connection;

    /**
     * Get connection from pool
     * @param pool
     */
    public LdapConnectionWrapper(LdapConnectionPool pool) throws LdapException {
        this.pool = pool;
        this.connection = pool.getConnection();

        DefaultConfigurableBinaryAttributeDetector binaryAttributeDetector = (DefaultConfigurableBinaryAttributeDetector) connection.getBinaryAttributeDetector();
        binaryAttributeDetector.addBinaryAttribute("nTSecurityDescriptor");
    }

    @Override
    public boolean isConnected() {
        return connection.isConnected();
    }

    @Override
    public boolean isAuthenticated() {
        return connection.isAuthenticated();
    }

    @Override
    public boolean connect() throws LdapException {
        return connection.connect();
    }

    @Override
    public void close() throws IOException {
        try {
            pool.releaseConnection(connection);
        } catch (LdapException e) {
            throw new LdapRuntimeException(e);
        }
        connection = null;
    }

    @Override
    public void add(Entry entry) throws LdapException {
        connection.add(entry);
    }

    @Override
    public AddResponse add(AddRequest addRequest) throws LdapException {
        return connection.add(addRequest);
    }

    @Override
    public void abandon(int messageId) {
        connection.abandon(messageId);
    }

    @Override
    public void abandon(AbandonRequest abandonRequest) {
        connection.abandon(abandonRequest);
    }

    @Override
    public void bind() throws LdapException {
        connection.bind();
    }

    @Override
    public void anonymousBind() throws LdapException {
        connection.anonymousBind();
    }

    @Override
    public void bind(String name) throws LdapException {
        connection.bind(name);
    }

    @Override
    public void bind(String name, String credentials) throws LdapException {
        connection.bind(name, credentials);
    }

    @Override
    public void bind(Dn name) throws LdapException {
        connection.bind(name);
    }

    @Override
    public void bind(Dn name, String credentials) throws LdapException {
        connection.bind(name, credentials);
    }

    @Override
    public BindResponse bind(BindRequest bindRequest) throws LdapException {
        return connection.bind(bindRequest);
    }

    @Override
    public EntryCursor search(Dn baseDn, String filter, SearchScope scope, String... attributes) throws LdapException {
        return connection.search(baseDn, filter, scope, attributes);
    }

    @Override
    public EntryCursor search(String baseDn, String filter, SearchScope scope, String... attributes) throws LdapException {
        return connection.search(baseDn, filter, scope, attributes);
    }

    @Override
    public SearchCursor search(SearchRequest searchRequest) throws LdapException {
        return connection.search(searchRequest);
    }

    @Override
    public void unBind() throws LdapException {
        connection.unBind();
    }

    @Override
    public void setTimeOut(long timeOut) {
        connection.setTimeOut(timeOut);
    }

    @Override
    public void modify(Dn dn, Modification... modifications) throws LdapException {
        connection.modify(dn, modifications);
    }

    @Override
    public void modify(String dn, Modification... modifications) throws LdapException {
        connection.modify(dn, modifications);
    }

    @Override
    public void modify(Entry entry, ModificationOperation modOp) throws LdapException {
        connection.modify(entry, modOp);
    }

    @Override
    public ModifyResponse modify(ModifyRequest modRequest) throws LdapException {
        return connection.modify(modRequest);
    }

    @Override
    public void rename(String entryDn, String newRdn) throws LdapException {
        connection.rename(entryDn, newRdn);
    }

    @Override
    public void rename(Dn entryDn, Rdn newRdn) throws LdapException {
        connection.rename(entryDn, newRdn);
    }

    @Override
    public void rename(String entryDn, String newRdn, boolean deleteOldRdn) throws LdapException {
        connection.rename(entryDn, newRdn, deleteOldRdn);
    }

    @Override
    public void rename(Dn entryDn, Rdn newRdn, boolean deleteOldRdn) throws LdapException {
        connection.rename(entryDn, newRdn, deleteOldRdn);
    }

    @Override
    public void move(String entryDn, String newSuperiorDn) throws LdapException {
        connection.move(entryDn, newSuperiorDn);
    }

    @Override
    public void move(Dn entryDn, Dn newSuperiorDn) throws LdapException {
        connection.move(entryDn, newSuperiorDn);
    }

    @Override
    public void moveAndRename(Dn entryDn, Dn newDn) throws LdapException {
        connection.moveAndRename(entryDn, newDn);
    }

    @Override
    public void moveAndRename(String entryDn, String newDn) throws LdapException {
        connection.moveAndRename(entryDn, newDn);
    }

    @Override
    public void moveAndRename(Dn entryDn, Dn newDn, boolean deleteOldRdn) throws LdapException {
        connection.moveAndRename(entryDn, newDn, deleteOldRdn);
    }

    @Override
    public void moveAndRename(String entryDn, String newDn, boolean deleteOldRdn) throws LdapException {
        connection.moveAndRename(entryDn, newDn, deleteOldRdn);
    }

    @Override
    public ModifyDnResponse modifyDn(ModifyDnRequest modDnRequest) throws LdapException {
        return connection.modifyDn(modDnRequest);
    }

    @Override
    public void delete(String dn) throws LdapException {
        connection.delete(dn);
    }

    @Override
    public void delete(Dn dn) throws LdapException {
        connection.delete(dn);
    }

    @Override
    public DeleteResponse delete(DeleteRequest deleteRequest) throws LdapException {
        return connection.delete(deleteRequest);
    }

    @Override
    public boolean compare(String dn, String attributeName, String value) throws LdapException {
        return connection.compare(dn, attributeName, value);
    }

    @Override
    public boolean compare(String dn, String attributeName, byte[] value) throws LdapException {
        return connection.compare(dn, attributeName, value);
    }

    @Override
    public boolean compare(String dn, String attributeName, Value<?> value) throws LdapException {
        return connection.compare(dn, attributeName, value);
    }

    @Override
    public boolean compare(Dn dn, String attributeName, String value) throws LdapException {
        return connection.compare(dn, attributeName, value);
    }

    @Override
    public boolean compare(Dn dn, String attributeName, byte[] value) throws LdapException {
        return connection.compare(dn, attributeName, value);
    }

    @Override
    public boolean compare(Dn dn, String attributeName, Value<?> value) throws LdapException {
        return connection.compare(dn, attributeName, value);
    }

    @Override
    public CompareResponse compare(CompareRequest compareRequest) throws LdapException {
        return connection.compare(compareRequest);
    }

    @Override
    public ExtendedResponse extended(String oid) throws LdapException {
        return connection.extended(oid);
    }

    @Override
    public ExtendedResponse extended(String oid, byte[] value) throws LdapException {
        return connection.extended(oid, value);
    }

    @Override
    public ExtendedResponse extended(Oid oid) throws LdapException {
        return connection.extended(oid);
    }

    @Override
    public ExtendedResponse extended(Oid oid, byte[] value) throws LdapException {
        return connection.extended(oid, value);
    }

    @Override
    public ExtendedResponse extended(ExtendedRequest extendedRequest) throws LdapException {
        return connection.extended(extendedRequest);
    }

    @Override
    public boolean exists(String dn) throws LdapException {
        return connection.exists(dn);
    }

    @Override
    public boolean exists(Dn dn) throws LdapException {
        return connection.exists(dn);
    }

    @Override
    public Entry getRootDse() throws LdapException {
        return connection.getRootDse();
    }

    @Override
    public Entry getRootDse(String... attributes) throws LdapException {
        return connection.getRootDse(attributes);
    }

    @Override
    public Entry lookup(Dn dn) throws LdapException {
        return connection.lookup(dn);
    }

    @Override
    public Entry lookup(String dn) throws LdapException {
        return connection.lookup(dn);
    }

    @Override
    public Entry lookup(Dn dn, String... attributes) throws LdapException {
        return connection.lookup(dn, attributes);
    }

    @Override
    public Entry lookup(Dn dn, Control[] controls, String... attributes) throws LdapException {
        return connection.lookup(dn, controls, attributes);
    }

    @Override
    public Entry lookup(String dn, String... attributes) throws LdapException {
        return connection.lookup(dn, attributes);
    }

    @Override
    public Entry lookup(String dn, Control[] controls, String... attributes) throws LdapException {
        return connection.lookup(dn, controls, attributes);
    }

    @Override
    public boolean isControlSupported(String controlOID) throws LdapException {
        return connection.isControlSupported(controlOID);
    }

    @Override
    public List<String> getSupportedControls() throws LdapException {
        return connection.getSupportedControls();
    }

    @Override
    public void loadSchema() throws LdapException {
        connection.loadSchema();
    }

    @Override
    public SchemaManager getSchemaManager() {
        return connection.getSchemaManager();
    }

    @Override
    public LdapApiService getCodecService() {
        return connection.getCodecService();
    }

    @Override
    public boolean doesFutureExistFor(int messageId) {
        return connection.doesFutureExistFor(messageId);
    }

    @Override
    public BinaryAttributeDetector getBinaryAttributeDetector() {
        return connection.getBinaryAttributeDetector();
    }

    @Override
    public void setBinaryAttributeDetector(BinaryAttributeDetector binaryAttributeDetecter) {
        connection.setBinaryAttributeDetector(binaryAttributeDetecter);
    }

    @Override
    public void setSchemaManager(SchemaManager schemaManager) {
        connection.setSchemaManager(schemaManager);
    }
}
